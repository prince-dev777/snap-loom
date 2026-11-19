package com.reactionstudio.screenrecorder.model

import android.net.Uri

data class RecordedVideo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int
)
