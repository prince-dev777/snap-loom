package com.reactionstudio.screenrecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.reactionstudio.screenrecorder.MainActivity
import com.reactionstudio.screenrecorder.data.PreferencesManager
import com.reactionstudio.screenrecorder.model.CameraLens
import com.reactionstudio.screenrecorder.model.RecordingConfig
import com.reactionstudio.screenrecorder.model.RecordingState
import com.reactionstudio.screenrecorder.model.VideoResolution
import com.reactionstudio.screenrecorder.overlay.BrushOverlayManager
import com.reactionstudio.screenrecorder.overlay.FacecamOverlayManager
import com.reactionstudio.screenrecorder.overlay.FloatingControlsManager
import com.reactionstudio.screenrecorder.utils.ShakeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : LifecycleService() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var facecamManager: FacecamOverlayManager? = null
    private var controlsManager: FloatingControlsManager? = null
    private var brushManager: BrushOverlayManager? = null
    private var shakeDetector: ShakeDetector? = null

    private var currentConfig: RecordingConfig = RecordingConfig()
    private var currentVideoUri: Uri? = null
    private var currentFilePath: String? = null

    private var recordingStartTime = 0L
    private var accumulatedDuration = 0L
    private var isRecordingPaused = false
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        facecamManager = FacecamOverlayManager(
            context = this,
            onCloseClicked = {
                // If user closes facecam via overlay X
            },
            onCameraFlipped = { lens ->
                val prefs = PreferencesManager.getInstance(this)
                prefs.updateConfig(prefs.loadConfig().copy(cameraLens = lens))
            }
        )

        brushManager = BrushOverlayManager(
            context = this,
            onClosed = {}
        )

        shakeDetector = ShakeDetector(this) {
            stopRecording()
        }

        controlsManager = FloatingControlsManager(
            context = this,
            onPauseResumeClicked = { togglePauseResume() },
            onStopClicked = { stopRecording() },
            onToggleFacecamClicked = { toggleFacecam() },
            onBrushClicked = { toggleBrush() }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val prefs = PreferencesManager.getInstance(this)
                currentConfig = prefs.loadConfig()
                startForegroundWithNotification("Preparing Reaction Recording...")
                lifecycleScope.launch {
                    handleStartRecording()
                }
            }
            ACTION_STOP -> {
                stopRecording()
            }
            ACTION_PAUSE -> {
                pauseRecording()
            }
            ACTION_RESUME -> {
                resumeRecording()
            }
            ACTION_TOGGLE_FACECAM -> {
                toggleFacecam()
            }
            ACTION_TOGGLE_PAUSE_RESUME -> {
                togglePauseResume()
            }
            ACTION_SHOW_FACECAM_PREVIEW -> {
                val prefs = PreferencesManager.getInstance(this)
                currentConfig = prefs.loadConfig()
                showFacecam()
            }
            ACTION_HIDE_FACECAM_PREVIEW -> {
                hideFacecam()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(content: String) {
        val notification = buildNotification(content)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            type
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
    }

    private fun buildNotification(content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action
        val stopIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }
        val pStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume Action
        val pauseIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_TOGGLE_PAUSE_RESUME }
        val pPause = PendingIntent.getService(
            this, 2, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Toggle Facecam Action
        val facecamIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_TOGGLE_FACECAM }
        val pFacecam = PendingIntent.getService(
            this, 3, facecamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseLabel = if (isRecordingPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Snap Loom")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .setContentIntent(pOpenApp)
            .addAction(android.R.drawable.ic_media_pause, pauseLabel, pPause)
            .addAction(android.R.drawable.checkbox_on_background, "Stop", pStop)
            .addAction(android.R.drawable.ic_menu_camera, "Facecam", pFacecam)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private suspend fun handleStartRecording() {
        val code = projectionResultCode
        val data = projectionResultData

        if (code == null || data == null) {
            _recordingState.value = RecordingState.Error("Media projection permission not available.")
            stopSelf()
            return
        }

        // Countdown handling
        val countdownSec = currentConfig.countdown.seconds
        if (countdownSec > 0) {
            for (sec in countdownSec downTo 1) {
                _recordingState.value = RecordingState.Countdown(sec)
                updateNotification("Recording starts in $sec seconds...")
                delay(1000)
            }
        }

        withContext(Dispatchers.Main) {
            initMediaProjection(code, data)
            initOverlays()
            startRecorder()
        }
    }

    private fun initMediaProjection(code: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(code, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopRecording()
            }
        }, null)
    }

    private fun initOverlays() {
        if (com.reactionstudio.screenrecorder.utils.PermissionHelper.hasOverlayPermission(this)) {
            if (currentConfig.enableFacecam) {
                showFacecam()
            }
            if (currentConfig.showFloatingControls) {
                controlsManager?.showControls()
            }
        }
    }

    private fun showFacecam() {
        facecamManager?.showFacecam(
            owner = this,
            lens = currentConfig.cameraLens,
            shape = currentConfig.facecamShape,
            size = currentConfig.facecamSize,
            borderColor = currentConfig.facecamBorderColor
        )
    }

    private fun hideFacecam() {
        facecamManager?.hideFacecam()
    }

    private fun toggleFacecam() {
        if (facecamManager?.isShowing() == true) {
            hideFacecam()
        } else {
            showFacecam()
        }
    }

    private fun toggleBrush() {
        if (brushManager?.isShowing() == true) {
            brushManager?.hideBrushOverlay()
        } else {
            brushManager?.showBrushOverlay()
        }
    }

    private fun startRecorder() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        var recordWidth = metrics.widthPixels
        var recordHeight = metrics.heightPixels

        when (currentConfig.resolution) {
            VideoResolution.RES_1080P -> {
                val scale = 1080f / metrics.widthPixels.coerceAtMost(metrics.heightPixels)
                recordWidth = (metrics.widthPixels * scale).toInt()
                recordHeight = (metrics.heightPixels * scale).toInt()
            }
            VideoResolution.RES_720P -> {
                val scale = 720f / metrics.widthPixels.coerceAtMost(metrics.heightPixels)
                recordWidth = (metrics.widthPixels * scale).toInt()
                recordHeight = (metrics.heightPixels * scale).toInt()
            }
            VideoResolution.RES_480P -> {
                val scale = 480f / metrics.widthPixels.coerceAtMost(metrics.heightPixels)
                recordWidth = (metrics.widthPixels * scale).toInt()
                recordHeight = (metrics.heightPixels * scale).toInt()
            }
            VideoResolution.RES_NATIVE -> {
                recordWidth = metrics.widthPixels
                recordHeight = metrics.heightPixels
            }
        }

        // Apply orientation lock if specified
        if (currentConfig.orientation == com.reactionstudio.screenrecorder.model.OrientationMode.PORTRAIT) {
            if (recordWidth > recordHeight) {
                val temp = recordWidth
                recordWidth = recordHeight
                recordHeight = temp
            }
        } else if (currentConfig.orientation == com.reactionstudio.screenrecorder.model.OrientationMode.LANDSCAPE) {
            if (recordWidth < recordHeight) {
                val temp = recordWidth
                recordWidth = recordHeight
                recordHeight = temp
            }
        }

        // Encoders require even dimensions (divisible by 2)
        if (recordWidth % 2 != 0) recordWidth -= 1
        if (recordHeight % 2 != 0) recordHeight -= 1

        val density = metrics.densityDpi

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Reaction_$timestamp.mp4"

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder = recorder

        val shouldRecordAudio = currentConfig.recordAudio &&
                currentConfig.audioMode != com.reactionstudio.screenrecorder.model.AudioSourceMode.MUTE

        try {
            if (shouldRecordAudio) {
                val audioSource = if (currentConfig.enableNoiseShield) {
                    // Triggers secondary noise-canceling mic & hardware Qualcomm/MediaTek DSP noise reduction
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && currentConfig.audioMode == com.reactionstudio.screenrecorder.model.AudioSourceMode.INTERNAL) {
                        MediaRecorder.AudioSource.VOICE_RECOGNITION
                    } else {
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    }
                } else {
                    if (currentConfig.audioMode == com.reactionstudio.screenrecorder.model.AudioSourceMode.INTERNAL &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ) {
                        MediaRecorder.AudioSource.VOICE_RECOGNITION
                    } else {
                        MediaRecorder.AudioSource.MIC
                    }
                }
                try {
                    recorder.setAudioSource(audioSource)
                } catch (_: Exception) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (shouldRecordAudio) {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                val audioBitrate = if (currentConfig.enableStudioHdAudio) 256_000 else 128_000
                val samplingRate = if (currentConfig.enableStudioHdAudio) 48_000 else 44_100
                recorder.setAudioEncodingBitRate(audioBitrate)
                recorder.setAudioSamplingRate(samplingRate)
            }

            recorder.setVideoSize(recordWidth, recordHeight)
            recorder.setVideoFrameRate(currentConfig.fps.fps)
            recorder.setVideoEncodingBitRate(currentConfig.bitrate.bps)

            // Setup output destination via MediaStore (Android 10+) or File
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ReactionScreenRecordings")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), values)
                currentVideoUri = uri
                val pfd = contentResolver.openFileDescriptor(uri!!, "w")
                recorder.setOutputFile(pfd!!.fileDescriptor)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ReactionScreenRecordings")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                currentFilePath = file.absolutePath
                recorder.setOutputFile(file.absolutePath)
            }

            recorder.prepare()

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenReactionRecorder",
                recordWidth,
                recordHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            recorder.start()

            if (currentConfig.shakeToStop) {
                shakeDetector?.start()
            }

            recordingStartTime = System.currentTimeMillis()
            accumulatedDuration = 0L
            isRecordingPaused = false
            _recordingState.value = RecordingState.Recording(0L)

            startTimerJob()
        } catch (e: Exception) {
            e.printStackTrace()
            _recordingState.value = RecordingState.Error("Failed to start recording: ${e.localizedMessage}")
            stopRecording()
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                if (!isRecordingPaused) {
                    val currentDuration = accumulatedDuration + (System.currentTimeMillis() - recordingStartTime)
                    _recordingState.value = RecordingState.Recording(currentDuration)
                    controlsManager?.updateTimer(currentDuration)

                    val sec = currentDuration / 1000
                    val text = String.format("%02d:%02d", sec / 60, sec % 60)
                    updateNotification("Recording: $text")
                }
            }
        }
    }

    private fun togglePauseResume() {
        if (isRecordingPaused) {
            resumeRecording()
        } else {
            pauseRecording()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isRecordingPaused) {
            try {
                mediaRecorder?.pause()
                accumulatedDuration += System.currentTimeMillis() - recordingStartTime
                isRecordingPaused = true
                _recordingState.value = RecordingState.Paused(accumulatedDuration)
                controlsManager?.setPaused(true)
                updateNotification("Recording Paused")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecordingPaused) {
            try {
                mediaRecorder?.resume()
                recordingStartTime = System.currentTimeMillis()
                isRecordingPaused = false
                _recordingState.value = RecordingState.Recording(accumulatedDuration)
                controlsManager?.setPaused(false)
                updateNotification("Recording Resumed")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        val finalDuration = if (isRecordingPaused) {
            accumulatedDuration
        } else {
            accumulatedDuration + (System.currentTimeMillis() - recordingStartTime)
        }

        _recordingState.value = RecordingState.Stopping(finalDuration)

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Can happen if recording is stopped immediately after start
            e.printStackTrace()
        }

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaProjection = null
        projectionResultCode = null
        projectionResultData = null

        // Overlays & Sensors
        shakeDetector?.stop()
        brushManager?.hideBrushOverlay()
        facecamManager?.hideFacecam()
        controlsManager?.hideControls()

        // Finalize MediaStore entry
        currentVideoUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }

        _recordingState.value = RecordingState.Completed(
            videoUri = currentVideoUri,
            filePath = currentFilePath,
            durationMillis = finalDuration
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Reaction Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording controls and status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector?.stop()
        brushManager?.hideBrushOverlay()
        facecamManager?.hideFacecam()
        controlsManager?.hideControls()
        timerJob?.cancel()
    }

    companion object {
        const val CHANNEL_ID = "screen_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.reactionstudio.screenrecorder.ACTION_START"
        const val ACTION_STOP = "com.reactionstudio.screenrecorder.ACTION_STOP"
        const val ACTION_PAUSE = "com.reactionstudio.screenrecorder.ACTION_PAUSE"
        const val ACTION_RESUME = "com.reactionstudio.screenrecorder.ACTION_RESUME"
        const val ACTION_TOGGLE_PAUSE_RESUME = "com.reactionstudio.screenrecorder.ACTION_TOGGLE_PAUSE_RESUME"
        const val ACTION_TOGGLE_FACECAM = "com.reactionstudio.screenrecorder.ACTION_TOGGLE_FACECAM"
        const val ACTION_SHOW_FACECAM_PREVIEW = "com.reactionstudio.screenrecorder.ACTION_SHOW_FACECAM_PREVIEW"
        const val ACTION_HIDE_FACECAM_PREVIEW = "com.reactionstudio.screenrecorder.ACTION_HIDE_FACECAM_PREVIEW"

        var projectionResultCode: Int? = null
        var projectionResultData: Intent? = null

        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        fun setRecordingState(state: RecordingState) {
            _recordingState.value = state
        }
    }
}
