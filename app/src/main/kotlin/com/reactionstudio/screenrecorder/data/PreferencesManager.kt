package com.reactionstudio.screenrecorder.data

import android.content.Context
import android.content.SharedPreferences
import com.reactionstudio.screenrecorder.model.AudioSourceMode
import com.reactionstudio.screenrecorder.model.CameraLens
import com.reactionstudio.screenrecorder.model.CountdownOption
import com.reactionstudio.screenrecorder.model.FacecamBorderColor
import com.reactionstudio.screenrecorder.model.FacecamShape
import com.reactionstudio.screenrecorder.model.FacecamSize
import com.reactionstudio.screenrecorder.model.OrientationMode
import com.reactionstudio.screenrecorder.model.RecordingConfig
import com.reactionstudio.screenrecorder.model.VideoBitrate
import com.reactionstudio.screenrecorder.model.VideoFps
import com.reactionstudio.screenrecorder.model.VideoResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reaction_studio_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<RecordingConfig> = _configFlow.asStateFlow()

    fun loadConfig(): RecordingConfig {
        val resName = prefs.getString(KEY_RESOLUTION, VideoResolution.RES_1080P.name) ?: VideoResolution.RES_1080P.name
        val resolution = runCatching { VideoResolution.valueOf(resName) }.getOrDefault(VideoResolution.RES_1080P)

        val fpsName = prefs.getString(KEY_FPS, VideoFps.FPS_60.name) ?: VideoFps.FPS_60.name
        val fps = runCatching { VideoFps.valueOf(fpsName) }.getOrDefault(VideoFps.FPS_60)

        val bitName = prefs.getString(KEY_BITRATE, VideoBitrate.HIGH.name) ?: VideoBitrate.HIGH.name
        val bitrate = runCatching { VideoBitrate.valueOf(bitName) }.getOrDefault(VideoBitrate.HIGH)

        val recordAudio = prefs.getBoolean(KEY_RECORD_AUDIO, true)
        val audioModeName = prefs.getString(KEY_AUDIO_MODE, AudioSourceMode.MIC.name) ?: AudioSourceMode.MIC.name
        val audioMode = runCatching { AudioSourceMode.valueOf(audioModeName) }.getOrDefault(AudioSourceMode.MIC)

        val orientName = prefs.getString(KEY_ORIENTATION, OrientationMode.AUTO.name) ?: OrientationMode.AUTO.name
        val orientation = runCatching { OrientationMode.valueOf(orientName) }.getOrDefault(OrientationMode.AUTO)

        val shakeToStop = prefs.getBoolean(KEY_SHAKE_TO_STOP, true)

        val enableFacecam = prefs.getBoolean(KEY_ENABLE_FACECAM, true)

        val shapeName = prefs.getString(KEY_FACECAM_SHAPE, FacecamShape.CIRCLE.name) ?: FacecamShape.CIRCLE.name
        val facecamShape = runCatching { FacecamShape.valueOf(shapeName) }.getOrDefault(FacecamShape.CIRCLE)

        val sizeName = prefs.getString(KEY_FACECAM_SIZE, FacecamSize.MEDIUM.name) ?: FacecamSize.MEDIUM.name
        val facecamSize = runCatching { FacecamSize.valueOf(sizeName) }.getOrDefault(FacecamSize.MEDIUM)

        val colorName = prefs.getString(KEY_FACECAM_COLOR, FacecamBorderColor.RED.name) ?: FacecamBorderColor.RED.name
        val facecamBorderColor = runCatching { FacecamBorderColor.valueOf(colorName) }.getOrDefault(FacecamBorderColor.RED)

        val lensName = prefs.getString(KEY_CAMERA_LENS, CameraLens.FRONT.name) ?: CameraLens.FRONT.name
        val cameraLens = runCatching { CameraLens.valueOf(lensName) }.getOrDefault(CameraLens.FRONT)

        val showControls = prefs.getBoolean(KEY_SHOW_FLOATING_CONTROLS, true)

        val countName = prefs.getString(KEY_COUNTDOWN, CountdownOption.THREE_SEC.name) ?: CountdownOption.THREE_SEC.name
        val countdown = runCatching { CountdownOption.valueOf(countName) }.getOrDefault(CountdownOption.THREE_SEC)

        return RecordingConfig(
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            recordAudio = recordAudio,
            audioMode = audioMode,
            orientation = orientation,
            shakeToStop = shakeToStop,
            enableFacecam = enableFacecam,
            facecamShape = facecamShape,
            facecamSize = facecamSize,
            facecamBorderColor = facecamBorderColor,
            cameraLens = cameraLens,
            showFloatingControls = showControls,
            countdown = countdown
        )
    }

    fun updateConfig(newConfig: RecordingConfig) {
        prefs.edit().apply {
            putString(KEY_RESOLUTION, newConfig.resolution.name)
            putString(KEY_FPS, newConfig.fps.name)
            putString(KEY_BITRATE, newConfig.bitrate.name)
            putBoolean(KEY_RECORD_AUDIO, newConfig.recordAudio)
            putString(KEY_AUDIO_MODE, newConfig.audioMode.name)
            putString(KEY_ORIENTATION, newConfig.orientation.name)
            putBoolean(KEY_SHAKE_TO_STOP, newConfig.shakeToStop)
            putBoolean(KEY_ENABLE_FACECAM, newConfig.enableFacecam)
            putString(KEY_FACECAM_SHAPE, newConfig.facecamShape.name)
            putString(KEY_FACECAM_SIZE, newConfig.facecamSize.name)
            putString(KEY_FACECAM_COLOR, newConfig.facecamBorderColor.name)
            putString(KEY_CAMERA_LENS, newConfig.cameraLens.name)
            putBoolean(KEY_SHOW_FLOATING_CONTROLS, newConfig.showFloatingControls)
            putString(KEY_COUNTDOWN, newConfig.countdown.name)
            apply()
        }
        _configFlow.value = newConfig
    }

    companion object {
        private const val KEY_RESOLUTION = "pref_resolution"
        private const val KEY_FPS = "pref_fps"
        private const val KEY_BITRATE = "pref_bitrate"
        private const val KEY_RECORD_AUDIO = "pref_record_audio"
        private const val KEY_AUDIO_MODE = "pref_audio_mode"
        private const val KEY_ORIENTATION = "pref_orientation"
        private const val KEY_SHAKE_TO_STOP = "pref_shake_to_stop"
        private const val KEY_ENABLE_FACECAM = "pref_enable_facecam"
        private const val KEY_FACECAM_SHAPE = "pref_facecam_shape"
        private const val KEY_FACECAM_SIZE = "pref_facecam_size"
        private const val KEY_FACECAM_COLOR = "pref_facecam_color"
        private const val KEY_CAMERA_LENS = "pref_camera_lens"
        private const val KEY_SHOW_FLOATING_CONTROLS = "pref_show_floating_controls"
        private const val KEY_COUNTDOWN = "pref_countdown"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
