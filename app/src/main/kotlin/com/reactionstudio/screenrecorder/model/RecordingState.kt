package com.reactionstudio.screenrecorder.model

import android.net.Uri

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Countdown(val remainingSeconds: Int) : RecordingState
    data class Recording(val durationMillis: Long) : RecordingState
    data class Paused(val durationMillis: Long) : RecordingState
    data class Stopping(val durationMillis: Long) : RecordingState
    data class Completed(val videoUri: Uri?, val filePath: String?, val durationMillis: Long) : RecordingState
    data class Error(val message: String) : RecordingState
}
