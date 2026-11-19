package com.reactionstudio.screenrecorder.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoTrimmer {

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long
    ): Uri? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var outputUri: Uri? = null

        try {
            extractor.setDataSource(context, sourceUri, null)
            val trackCount = extractor.trackCount

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFileName = "Reaction_Trimmed_$timestamp.mp4"

            var tempFile: File? = null
            var muxerPath: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tempFile = File(context.cacheDir, outputFileName)
                muxerPath = tempFile.absolutePath
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "ReactionScreenRecordings"
                )
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, outputFileName)
                muxerPath = outFile.absolutePath
            }

            muxer = MediaMuxer(muxerPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = HashMap<Int, Int>()
            var maxBufferSize = 1024 * 1024

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    trackIndexMap[i] = muxerTrackIndex

                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val bufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (bufferSize > maxBufferSize) {
                            maxBufferSize = bufferSize
                        }
                    }
                }
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Seek to start position
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endMs * 1000L) {
                    // Reached end time
                    break
                }

                val muxerTrackIndex = trackIndexMap[trackIndex]
                if (muxerTrackIndex != null) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = sampleTime - (startMs * 1000L).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            // Move temp file to MediaStore on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tempFile != null && tempFile.exists()) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outputFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/ReactionScreenRecordings"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                val insertedUri = context.contentResolver.insert(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    values
                )
                if (insertedUri != null) {
                    context.contentResolver.openOutputStream(insertedUri)?.use { out ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    tempFile.delete()
                    outputUri = insertedUri
                }
            } else if (tempFile != null) {
                outputUri = Uri.fromFile(tempFile)
            } else {
                outputUri = Uri.fromFile(File(muxerPath))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                muxer?.release()
                extractor.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@withContext null
        }

        outputUri
    }
}
