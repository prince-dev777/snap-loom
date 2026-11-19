package com.reactionstudio.screenrecorder.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.reactionstudio.screenrecorder.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    suspend fun getRecordedVideos(): List<RecordedVideo> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<RecordedVideo>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        // Filter for videos created by our app (e.g. prefix "Reaction_")
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("Reaction_%")
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Untitled Video"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) * 1000 // Convert sec to ms
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                    videoList.add(
                        RecordedVideo(
                            id = id,
                            uri = contentUri,
                            name = name,
                            durationMillis = duration,
                            sizeBytes = size,
                            dateModified = dateModified,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    suspend fun deleteVideo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
