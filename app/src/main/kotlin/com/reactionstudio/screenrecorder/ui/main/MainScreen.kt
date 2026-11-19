package com.reactionstudio.screenrecorder.ui.main

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.model.RecordingState
import com.reactionstudio.screenrecorder.ui.RecordingsScreen
import com.reactionstudio.screenrecorder.ui.SettingsScreen
import com.reactionstudio.screenrecorder.utils.AppUpdateManager
import com.reactionstudio.screenrecorder.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    onStartRecordingClick: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val updateFeedback by viewModel.updateFeedback.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.reactionstudio.screenrecorder.R.drawable.app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Snap Loom",
                            fontWeight = FontWeight.Bold
                        )
                        if (recordingState is RecordingState.Recording) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Studio") },
                    label = { Text("Studio") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Recordings") },
                    label = { Text("Recordings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StudioHomeTab(
                    viewModel = viewModel,
                    recordingState = recordingState,
                    onStartRecordingClick = onStartRecordingClick,
                    onRequestPermissions = onRequestPermissions,
                    onOpenOverlaySettings = onOpenOverlaySettings
                )
                1 -> SettingsScreen(viewModel = viewModel)
                2 -> RecordingsScreen(viewModel = viewModel)
            }
        }
    }

    // Video Recording Completed Dialog
    if (recordingState is RecordingState.Completed) {
        val completed = recordingState as RecordingState.Completed
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompletedDialog() },
            title = { Text("Reaction Video Saved! 🎉") },
            text = {
                Text(
                    "Your reaction recording has been saved to Movies/ReactionScreenRecordings and is ready for YouTube."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissCompletedDialog()
                    viewModel.selectTab(2) // Jump to Gallery
                }) {
                    Text("View in Recordings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCompletedDialog() }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // App Update Feedback Dialog (e.g. "You're on the latest version")
    updateFeedback?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissFeedback() },
            title = { Text("App Update") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { viewModel.dismissFeedback() }) {
                    Text("OK")
                }
            }
        )
    }

    // New Update Available Dialog
    updateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🚀 Update Available (v${update.latestVersion})")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "A new version of Snap Loom is available for your device.",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Current Version: v${update.currentVersion}\nLatest Version: v${update.latestVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (update.releaseNotes.isNotBlank()) {
                        Text("What's New:", fontWeight = FontWeight.Bold)
                        Text(
                            update.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppUpdateManager.startDownload(context, update.apkDownloadUrl, update.latestVersion)
                        viewModel.dismissUpdateDialog()
                    }
                ) {
                    Text("Update Now (Download APK)")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun StudioHomeTab(
    viewModel: MainScreenViewModel,
    recordingState: RecordingState,
    onStartRecordingClick: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val isFacecamPreviewActive by viewModel.isFacecamPreviewActive.collectAsState()
    val scrollState = rememberScrollState()

    val hasOverlay = PermissionHelper.hasOverlayPermission(context)
    val hasCamera = PermissionHelper.hasCameraPermission(context)
    val hasAudio = PermissionHelper.hasAudioPermission(context)
    val hasNotification = PermissionHelper.hasNotificationPermission(context)
    val allPermissionsGranted = hasOverlay && hasCamera && hasAudio && hasNotification

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permissions Warning Banner if any are missing
        if (!allPermissionsGranted) {
            PermissionsBanner(
                hasOverlay = hasOverlay,
                hasCamera = hasCamera,
                hasAudio = hasAudio,
                hasNotification = hasNotification,
                onRequestRuntimePermissions = onRequestPermissions,
                onOpenOverlaySettings = onOpenOverlaySettings
            )
        }

        // Hero Recording Status Card
        HeroRecordingCard(
            recordingState = recordingState,
            config = config,
            onStartClick = {
                if (!allPermissionsGranted) {
                    if (!hasOverlay) {
                        onOpenOverlaySettings()
                    } else {
                        onRequestPermissions()
                    }
                } else {
                    onStartRecordingClick()
                }
            },
            onPauseResumeClick = { viewModel.togglePauseResume(context) },
            onStopClick = { viewModel.stopRecording(context) }
        )

        // Quick Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Reaction Quick Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Facecam Toggle + Preview Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Facecam (Reaction Camera)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (config.enableFacecam) "Floating window enabled" else "Facecam disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.enableFacecam,
                        onCheckedChange = { viewModel.updateEnableFacecam(it) }
                    )
                }

                // Preview Facecam Button
                if (config.enableFacecam && hasOverlay && hasCamera) {
                    OutlinedButton(
                        onClick = { viewModel.toggleFacecamPreview(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFacecamPreviewActive) "Close Facecam Preview" else "Test / Preview Facecam Now"
                        )
                    }
                }

                // Audio Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Microphone Voice Recording",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (config.recordAudio) "Mic commentary active" else "Muted audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.recordAudio,
                        onCheckedChange = { viewModel.updateRecordAudio(it) }
                    )
                }

                // Floating Controls Widget Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "On-Screen Floating Control Bar",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Pause, Stop & Facecam pill on screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.showFloatingControls,
                        onCheckedChange = { viewModel.updateShowFloatingControls(it) }
                    )
                }
            }
        }

        // Active Preset Card (with Click to Settings)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectTab(1) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Recording Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${config.resolution.displayName} • ${config.fps.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${config.bitrate.displayName} • ${config.audioMode.displayName} • ${config.orientation.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Customize Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HeroRecordingCard(
    recordingState: RecordingState,
    config: com.reactionstudio.screenrecorder.model.RecordingConfig,
    onStartClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused
    val isCountdown = recordingState is RecordingState.Countdown

    val gradientBrush = if (isRecording || isPaused) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF880E4F),
                Color(0xFF212121)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFD32F2F),
                Color(0xFF1E1E24)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // State Indicator
                when (recordingState) {
                    is RecordingState.Idle -> {
                        Text(
                            text = "READY FOR REACTION",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFCDD2),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Record screen & facecam together for YouTube reactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onStartClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START RECORDING",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is RecordingState.Countdown -> {
                        Text(
                            text = "STARTING IN",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFCDD2),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "${recordingState.remainingSeconds}",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    is RecordingState.Recording -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Text(
                                text = "RECORDING LIVE",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                        val sec = recordingState.durationMillis / 1000
                        val timerText = String.format("%02d:%02d", sec / 60, sec % 60)
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onPauseResumeClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFA000),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }
                            Button(
                                onClick = onStopClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                    is RecordingState.Paused -> {
                        Text(
                            text = "RECORDING PAUSED",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 2.sp
                        )
                        val sec = recordingState.durationMillis / 1000
                        val timerText = String.format("%02d:%02d", sec / 60, sec % 60)
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onPauseResumeClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume")
                            }
                            Button(
                                onClick = onStopClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                    is RecordingState.Stopping -> {
                        Text(
                            text = "SAVING VIDEO...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    is RecordingState.Error -> {
                        Text(
                            text = "Error: ${recordingState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFCDD2)
                        )
                        Button(
                            onClick = onStartClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PermissionsBanner(
    hasOverlay: Boolean,
    hasCamera: Boolean,
    hasAudio: Boolean,
    hasNotification: Boolean,
    onRequestRuntimePermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "Screen recording, Camera (Facecam), Audio, and Floating Window overlay permissions are required for reaction videos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasCamera || !hasAudio || !hasNotification) {
                    Button(
                        onClick = onRequestRuntimePermissions,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Camera & Mic")
                    }
                }
                if (!hasOverlay) {
                    Button(
                        onClick = onOpenOverlaySettings,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Allow Overlay")
                    }
                }
            }
        }
    }
}
