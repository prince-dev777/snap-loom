package com.reactionstudio.screenrecorder.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reactionstudio.screenrecorder.data.PreferencesManager
import com.reactionstudio.screenrecorder.data.VideoRepository
import com.reactionstudio.screenrecorder.model.AudioSourceMode
import com.reactionstudio.screenrecorder.model.CameraLens
import com.reactionstudio.screenrecorder.model.CountdownOption
import com.reactionstudio.screenrecorder.model.FacecamBorderColor
import com.reactionstudio.screenrecorder.model.FacecamShape
import com.reactionstudio.screenrecorder.model.FacecamSize
import com.reactionstudio.screenrecorder.model.OrientationMode
import com.reactionstudio.screenrecorder.model.RecordedVideo
import com.reactionstudio.screenrecorder.model.RecordingConfig
import com.reactionstudio.screenrecorder.model.RecordingState
import com.reactionstudio.screenrecorder.model.VideoBitrate
import com.reactionstudio.screenrecorder.model.VideoFps
import com.reactionstudio.screenrecorder.model.VideoResolution
import com.reactionstudio.screenrecorder.service.ScreenRecordService
import com.reactionstudio.screenrecorder.utils.AppUpdateManager
import com.reactionstudio.screenrecorder.utils.PermissionHelper
import com.reactionstudio.screenrecorder.utils.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)
    private val videoRepo = VideoRepository(application)

    val config: StateFlow<RecordingConfig> = prefsManager.configFlow

    val recordingState: StateFlow<RecordingState> = ScreenRecordService.recordingState

    private val _videos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    val videos: StateFlow<List<RecordedVideo>> = _videos.asStateFlow()

    private val _isFacecamPreviewActive = MutableStateFlow(false)
    val isFacecamPreviewActive: StateFlow<Boolean> = _isFacecamPreviewActive.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateFeedback = MutableStateFlow<String?>(null)
    val updateFeedback: StateFlow<String?> = _updateFeedback.asStateFlow()

    init {
        loadVideos()
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = AppUpdateManager.checkForUpdates(getApplication())
            _isCheckingUpdate.value = false
            if (info != null) {
                if (info.isUpdateAvailable) {
                    _updateInfo.value = info
                } else if (!silent) {
                    _updateFeedback.value = "You are on the latest version of Snap Loom (${info.currentVersion}) 🎉"
                }
            } else if (!silent) {
                _updateFeedback.value = "Could not reach update server. Check your internet connection."
            }
        }
    }

    fun getUpdateDismissCount(version: String): Int {
        return prefsManager.getUpdateDismissCount(version)
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun onDismissUpdateLater(version: String): Int {
        val count = prefsManager.incrementUpdateDismissCount(version)
        _updateInfo.value = null
        return count
    }

    fun dismissFeedback() {
        _updateFeedback.value = null
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        if (index == 2) {
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _videos.value = videoRepo.getRecordedVideos()
        }
    }

    fun deleteVideo(video: RecordedVideo) {
        viewModelScope.launch {
            val success = videoRepo.deleteVideo(video.uri)
            if (success) {
                loadVideos()
            }
        }
    }

    fun updateResolution(resolution: VideoResolution) {
        prefsManager.updateConfig(config.value.copy(resolution = resolution))
    }

    fun updateFps(fps: VideoFps) {
        prefsManager.updateConfig(config.value.copy(fps = fps))
    }

    fun updateBitrate(bitrate: VideoBitrate) {
        prefsManager.updateConfig(config.value.copy(bitrate = bitrate))
    }

    fun updateRecordAudio(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(recordAudio = enabled))
    }

    fun updateAudioMode(mode: AudioSourceMode) {
        prefsManager.updateConfig(
            config.value.copy(
                audioMode = mode,
                recordAudio = mode != AudioSourceMode.MUTE
            )
        )
    }

    fun updateOrientation(orientation: OrientationMode) {
        prefsManager.updateConfig(config.value.copy(orientation = orientation))
    }

    fun updateShakeToStop(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(shakeToStop = enabled))
    }

    fun updateFacecamBorderColor(color: FacecamBorderColor) {
        prefsManager.updateConfig(config.value.copy(facecamBorderColor = color))
    }

    fun trimVideo(video: RecordedVideo, startMs: Long, endMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultUri = com.reactionstudio.screenrecorder.utils.VideoTrimmer.trimVideo(
                context = getApplication(),
                sourceUri = video.uri,
                startMs = startMs,
                endMs = endMs
            )
            if (resultUri != null) {
                loadVideos()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateEnableFacecam(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(enableFacecam = enabled))
    }

    fun updateFacecamShape(shape: FacecamShape) {
        prefsManager.updateConfig(config.value.copy(facecamShape = shape))
    }

    fun updateFacecamSize(size: FacecamSize) {
        prefsManager.updateConfig(config.value.copy(facecamSize = size))
    }

    fun updateCameraLens(lens: CameraLens) {
        prefsManager.updateConfig(config.value.copy(cameraLens = lens))
    }

    fun updateCountdown(countdown: CountdownOption) {
        prefsManager.updateConfig(config.value.copy(countdown = countdown))
    }

    fun updateShowFloatingControls(show: Boolean) {
        prefsManager.updateConfig(config.value.copy(showFloatingControls = show))
    }

    fun updateNoiseShield(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(enableNoiseShield = enabled))
    }

    fun updateStudioHdAudio(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(enableStudioHdAudio = enabled))
    }

    fun updateMicVolumeGain(gain: Float) {
        prefsManager.updateConfig(config.value.copy(micVolumeGain = gain))
    }

    fun updateEnableTeleprompter(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(enableTeleprompter = enabled))
    }

    fun updateEnableWatermark(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(enableCustomWatermark = enabled))
    }

    fun updateWatermarkText(text: String) {
        prefsManager.updateConfig(config.value.copy(customWatermarkText = text))
    }

    fun updateGhostMode(enabled: Boolean) {
        prefsManager.updateConfig(config.value.copy(ghostMode = enabled))
    }

    fun hasAcceptedTerms(): Boolean {
        return prefsManager.hasAcceptedTerms()
    }

    fun acceptTerms() {
        prefsManager.setTermsAccepted(true)
    }

    fun awardReferralBonus(): Int {
        return prefsManager.addReferralBonusDays(7)
    }

    fun getReferralBonusDays(): Int {
        return prefsManager.getReferralBonusDays()
    }

    fun toggleFacecamPreview(context: Context) {
        if (!PermissionHelper.hasOverlayPermission(context) || !PermissionHelper.hasCameraPermission(context)) {
            return
        }
        val intent = Intent(context, ScreenRecordService::class.java)
        if (_isFacecamPreviewActive.value) {
            intent.action = ScreenRecordService.ACTION_HIDE_FACECAM_PREVIEW
            _isFacecamPreviewActive.value = false
        } else {
            intent.action = ScreenRecordService.ACTION_SHOW_FACECAM_PREVIEW
            _isFacecamPreviewActive.value = true
        }
        context.startService(intent)
    }

    fun stopRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
        loadVideos()
    }

    fun togglePauseResume(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_TOGGLE_PAUSE_RESUME
        }
        context.startService(intent)
    }

    fun dismissCompletedDialog() {
        ScreenRecordService.setRecordingState(RecordingState.Idle)
        loadVideos()
    }
}
