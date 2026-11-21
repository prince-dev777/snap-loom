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

        val noiseShield = prefs.getBoolean(KEY_ENABLE_NOISE_SHIELD, true)
        val studioHdAudio = prefs.getBoolean(KEY_ENABLE_STUDIO_HD_AUDIO, true)
        val micGain = prefs.getFloat(KEY_MIC_VOLUME_GAIN, 1.0f)
        val teleprompter = prefs.getBoolean(KEY_ENABLE_TELEPROMPTER, false)
        val watermark = prefs.getBoolean(KEY_ENABLE_WATERMARK, false)
        val watermarkText = prefs.getString(KEY_WATERMARK_TEXT, "@SnapLoomCreator") ?: "@SnapLoomCreator"
        val ghostMode = prefs.getBoolean(KEY_GHOST_MODE, false)

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
            countdown = countdown,
            enableNoiseShield = noiseShield,
            enableStudioHdAudio = studioHdAudio,
            micVolumeGain = micGain,
            enableTeleprompter = teleprompter,
            enableCustomWatermark = watermark,
            customWatermarkText = watermarkText,
            ghostMode = ghostMode
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
            putBoolean(KEY_ENABLE_NOISE_SHIELD, newConfig.enableNoiseShield)
            putBoolean(KEY_ENABLE_STUDIO_HD_AUDIO, newConfig.enableStudioHdAudio)
            putFloat(KEY_MIC_VOLUME_GAIN, newConfig.micVolumeGain)
            putBoolean(KEY_ENABLE_TELEPROMPTER, newConfig.enableTeleprompter)
            putBoolean(KEY_ENABLE_WATERMARK, newConfig.enableCustomWatermark)
            putString(KEY_WATERMARK_TEXT, newConfig.customWatermarkText)
            putBoolean(KEY_GHOST_MODE, newConfig.ghostMode)
            apply()
        }
        _configFlow.value = newConfig
    }

    fun hasAcceptedTerms(): Boolean {
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }

    fun getReferralBonusDays(): Int {
        return prefs.getInt(KEY_REFERRAL_BONUS_DAYS, 0)
    }

    fun addReferralBonusDays(days: Int): Int {
        val current = getReferralBonusDays() + days
        prefs.edit().putInt(KEY_REFERRAL_BONUS_DAYS, current).apply()
        return current
    }

    fun getUpdateDismissCount(version: String): Int {
        val savedVer = prefs.getString(KEY_DISMISSED_UPDATE_VERSION, null)
        if (savedVer != version) {
            return 0
        }
        return prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0)
    }

    fun incrementUpdateDismissCount(version: String): Int {
        val current = getUpdateDismissCount(version) + 1
        prefs.edit()
            .putString(KEY_DISMISSED_UPDATE_VERSION, version)
            .putInt(KEY_UPDATE_DISMISS_COUNT, current)
            .apply()
        return current
    }

    fun resetUpdateDismissCount() {
        prefs.edit()
            .remove(KEY_DISMISSED_UPDATE_VERSION)
            .remove(KEY_UPDATE_DISMISS_COUNT)
            .apply()
    }

    companion object {
        private const val KEY_DISMISSED_UPDATE_VERSION = "pref_dismissed_update_version"
        private const val KEY_UPDATE_DISMISS_COUNT = "pref_update_dismiss_count"
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
        private const val KEY_ENABLE_NOISE_SHIELD = "pref_enable_noise_shield"
        private const val KEY_ENABLE_STUDIO_HD_AUDIO = "pref_enable_studio_hd_audio"
        private const val KEY_TERMS_ACCEPTED = "pref_terms_accepted"
        private const val KEY_MIC_VOLUME_GAIN = "pref_mic_volume_gain"
        private const val KEY_ENABLE_TELEPROMPTER = "pref_enable_teleprompter"
        private const val KEY_ENABLE_WATERMARK = "pref_enable_watermark"
        private const val KEY_WATERMARK_TEXT = "pref_watermark_text"
        private const val KEY_GHOST_MODE = "pref_ghost_mode"
        private const val KEY_REFERRAL_BONUS_DAYS = "pref_referral_bonus_days"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
