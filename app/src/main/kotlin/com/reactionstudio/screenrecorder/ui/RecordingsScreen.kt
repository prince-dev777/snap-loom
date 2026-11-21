package com.reactionstudio.screenrecorder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.model.RecordedVideo
import com.reactionstudio.screenrecorder.ui.main.MainScreenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(viewModel: MainScreenViewModel) {
    val videos by viewModel.videos.collectAsState()
    val context = LocalContext.current
    var videoToDelete by remember { mutableStateOf<RecordedVideo?>(null) }
    var videoToTrim by remember { mutableStateOf<RecordedVideo?>(null) }
    var isTrimming by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Reaction Recordings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${videos.size} videos ready for YouTube",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.loadVideos() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Videos"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start a recording with facecam to create your first reaction video!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoItemCard(
                        video = video,
                        onPlayClick = { playVideo(context, video.uri) },
                        onShareClick = { shareVideo(context, video.uri, video.name) },
                        onTrimClick = { videoToTrim = video },
                        onDeleteClick = { videoToDelete = video }
                    )
                }
            }
        }
    }

    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video?") },
            text = { Text("Are you sure you want to delete '${video.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                        Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    videoToTrim?.let { video ->
        TrimVideoDialog(
            video = video,
            isTrimming = isTrimming,
            onDismiss = { videoToTrim = null },
            onConfirmTrim = { startMs, endMs ->
                isTrimming = true
                viewModel.trimVideo(video, startMs, endMs) { success ->
                    isTrimming = false
                    videoToTrim = null
                    if (success) {
                        Toast.makeText(context, "Video trimmed & saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to trim video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun TrimVideoDialog(
    video: RecordedVideo,
    isTrimming: Boolean,
    onDismiss: () -> Unit,
    onConfirmTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val totalSec = (video.durationMillis / 1000).coerceAtLeast(1)
    var startSec by remember { mutableStateOf(0f) }
    var endSec by remember { mutableStateOf(totalSec.toFloat()) }

    AlertDialog(
        onDismissRequest = { if (!isTrimming) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Trim Reaction Video")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (isTrimming) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Trimming video losslessly...")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start: ${formatDuration((startSec * 1000).toLong())}")
                        Text("End: ${formatDuration((endSec * 1000).toLong())}")
                    }

                    Text("Start Cut (Seconds):", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = startSec,
                        onValueChange = {
                            if (it < endSec) startSec = it
                        },
                        valueRange = 0f..totalSec.toFloat()
                    )

                    Text("End Cut (Seconds):", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = endSec,
                        onValueChange = {
                            if (it > startSec) endSec = it
                        },
                        valueRange = 0f..totalSec.toFloat()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmTrim((startSec * 1000).toLong(), (endSec * 1000).toLong())
                },
                enabled = !isTrimming && endSec > startSec
            ) {
                Text("Trim & Save")
            }
        },
        dismissButton = {
            if (!isTrimming) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun VideoItemCard(
    video: RecordedVideo,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onTrimClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Thumbnail Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val durationFormatted = formatDuration(video.durationMillis)
                val sizeFormatted = formatFileSize(video.sizeBytes)
                val dateFormatted = formatDate(video.dateModified)

                Text(
                    text = "$durationFormatted • $sizeFormatted • $dateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1-Click 9:16 Shorts Exporter
                Button(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, video.uri)
                                putExtra(Intent.EXTRA_SUBJECT, video.name)
                                putExtra(Intent.EXTRA_TEXT, "${video.name.removeSuffix(".mp4")} #Shorts #Reaction #SnapLoom")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export to YouTube Shorts / Reels"))
                            Toast.makeText(context, "Ready for YouTube Shorts & Reels (#Shorts tag attached)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to export video", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("📱 Shorts", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onTrimClick) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Trim Video",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun playVideo(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No video player app found", Toast.LENGTH_SHORT).show()
    }
}

private fun shareVideo(context: Context, uri: Uri, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Reaction video recorded with Screen Reaction Studio!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share video", Toast.LENGTH_SHORT).show()
    }
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format("%.1f MB", mb)
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
