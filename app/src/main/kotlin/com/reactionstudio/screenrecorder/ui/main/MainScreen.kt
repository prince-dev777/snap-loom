package com.reactionstudio.screenrecorder.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.DialogProperties
import com.reactionstudio.screenrecorder.model.RecordingState
import com.reactionstudio.screenrecorder.ui.RecordingsScreen
import com.reactionstudio.screenrecorder.ui.SettingsScreen
import com.reactionstudio.screenrecorder.utils.AppAccessState
import com.reactionstudio.screenrecorder.utils.AppUpdateManager
import com.reactionstudio.screenrecorder.utils.ContactHelper
import com.reactionstudio.screenrecorder.utils.LicenseManager
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
    var hasAcceptedTerms by remember { mutableStateOf(viewModel.hasAcceptedTerms()) }

    // Immediately check for updates when entering MainScreen
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(silent = true)
    }

    // Mandatory Terms & Privacy Consent Dialog
    if (!hasAcceptedTerms) {
        TermsAndConditionsConsentDialog(
            onAccept = {
                viewModel.acceptTerms()
                hasAcceptedTerms = true
            }
        )
    }

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

    // New Update Available Dialog with Cancel Limit (Max 3 cancellations before mandatory app close)
    updateInfo?.let { update ->
        val dismissCount = viewModel.getUpdateDismissCount(update.latestVersion)
        val isMandatory = dismissCount >= 3

        AlertDialog(
            onDismissRequest = {
                if (isMandatory) {
                    (context as? Activity)?.finishAffinity()
                } else {
                    val newCount = viewModel.onDismissUpdateLater(update.latestVersion)
                    if (newCount >= 3) {
                        (context as? Activity)?.finishAffinity()
                    }
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (isMandatory) "⚠️ Mandatory Update Required" else "🚀 Update Available (v${update.latestVersion})")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMandatory) {
                        Text(
                            "You have dismissed this update 3 times. Please update Snap Loom to v${update.latestVersion} to continue using the studio.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "A new version of Snap Loom is available for your device.",
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                    if (!isMandatory) {
                        Text(
                            "Notice: You can skip this update ${3 - dismissCount} more time(s) before it becomes mandatory.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
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
                    Text("Update Now (Fast OTA)")
                }
            },
            dismissButton = {
                if (isMandatory) {
                    TextButton(onClick = { (context as? Activity)?.finishAffinity() }) {
                        Text("Exit App", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = {
                        val count = viewModel.onDismissUpdateLater(update.latestVersion)
                        if (count >= 3) {
                            (context as? Activity)?.finishAffinity()
                        }
                    }) {
                        Text("Later (${3 - dismissCount} left)")
                    }
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
    val updateInfo by viewModel.updateInfo.collectAsState()
    val scrollState = rememberScrollState()

    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }

    val hasOverlay = PermissionHelper.hasOverlayPermission(context)
    val hasCamera = PermissionHelper.hasCameraPermission(context)
    val hasAudio = PermissionHelper.hasAudioPermission(context)
    val hasNotification = PermissionHelper.hasNotificationPermission(context)
    val allPermissionsGranted = hasOverlay && hasCamera && hasAudio && hasNotification

    if (showRestrictedSettingsDialog) {
        RestrictedSettingsDialog(
            onDismiss = { showRestrictedSettingsDialog = false },
            onOpenAppInfo = {
                context.startActivity(PermissionHelper.openAppSettingsIntent(context))
            },
            onOpenOverlay = onOpenOverlaySettings,
            onOpenXiaomiOtherPerms = {
                PermissionHelper.openXiaomiOtherPermissions(context)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIP / Trial Expiry Status Pill
        VipTimerBadge(context = context)

        // Refer & Earn Pro (+7 Days) Card
        ReferAndEarnCard(viewModel = viewModel)

        // New Update Available Alert Banner (right on Home Screen!)
        if (updateInfo?.isUpdateAvailable == true) {
            val update = updateInfo!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚀 New Update Available (v${update.latestVersion})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Current: v${update.currentVersion} • Tap to update now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            AppUpdateManager.startDownload(context, update.apkDownloadUrl, update.latestVersion)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Update", fontSize = 12.sp)
                    }
                }
            }
        }

        // Essential permissions check (Audio + Notification)
        val essentialPermissionsGranted = hasAudio && hasNotification

        // Permissions Warning Banner: Only show red banner if essential audio/mic/notification is missing
        if (!essentialPermissionsGranted) {
            PermissionsBanner(
                hasOverlay = hasOverlay,
                hasCamera = hasCamera,
                hasAudio = hasAudio,
                hasNotification = hasNotification,
                onRequestRuntimePermissions = onRequestPermissions,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onOpenRestrictedGuide = { showRestrictedSettingsDialog = true }
            )
        } else if (!hasOverlay) {
            // Non-blocking tip for optional Floating Facecam & Brush
            OptionalOverlayTipBanner(
                onOpenGuide = { showRestrictedSettingsDialog = true },
                onStartDirectly = onStartRecordingClick
            )
        }

        // Hero Recording Status Card - NEVER BLOCKS RECORDING FOR OVERLAY!
        HeroRecordingCard(
            recordingState = recordingState,
            config = config,
            onStartClick = {
                val needRuntime = !hasAudio || !hasNotification || (config.enableFacecam && !hasCamera)
                if (needRuntime) {
                    onRequestPermissions()
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
fun OptionalOverlayTipBanner(
    onOpenGuide: () -> Unit,
    onStartDirectly: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💡", fontSize = 16.sp)
                Text(
                    text = "Floating Facecam & Pen (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Screen recording 100% chalegi! Agar screen par floating Facecam bubble ya live pen chahiye, toh 10 second me unblock karein.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenGuide,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Unlock Guide (10s)", fontSize = 12.sp)
                }
                Button(
                    onClick = onStartDirectly,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Record Now ⏺️", fontSize = 12.sp)
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
    onOpenOverlaySettings: () -> Unit,
    onOpenRestrictedGuide: () -> Unit
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
                text = "Screen recording, Camera (Facecam), Audio, aur Floating Window overlay permissions zaroori hain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!hasCamera || !hasAudio || !hasNotification) {
                    Button(
                        onClick = onRequestRuntimePermissions,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Camera & Mic", fontSize = 12.sp)
                    }
                }
                if (!hasOverlay) {
                    Button(
                        onClick = onOpenOverlaySettings,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allow Overlay", fontSize = 12.sp)
                    }
                }
            }

            // Xiaomi & Restricted settings fix button
            OutlinedButton(
                onClick = onOpenRestrictedGuide,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("⚠️ 'App was denied access' error? Tap for 10s fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VipTimerBadge(context: Context) {
    val accessState = remember { LicenseManager.getInstance(context).getAppAccessState() }

    when (accessState) {
        is AppAccessState.TrialActive -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "⏳", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "3-Day Free VIP Trial Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${accessState.daysRemaining} days left (${accessState.hoursRemaining} hours remaining)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PRO FREE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 1-Click UPI & Telegram Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ContactHelper.openUpiPayment(context, "99")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text(
                                "⚡ Pay ₹99 UPI",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                ContactHelper.openTelegram(
                                    context,
                                    "Hello Prince! I am currently using the 3-day trial of Snap Loom and would like to buy Pro access."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB)
                            )
                        ) {
                            Text(
                                "Chat Telegram",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        is AppAccessState.Subscribed -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "👑", fontSize = 22.sp)
                        Column {
                            Text(
                                text = accessState.planName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = if (accessState.daysRemaining != null) "${accessState.daysRemaining} days left (Expires: ${accessState.expiresAt})" else "Lifetime Pro Access Unlocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "VIP PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
        is AppAccessState.PendingApproval -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEAB308).copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "⏳", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "Activation Pending Approval",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEAB308)
                                )
                                Text(
                                    text = "Waiting for Prince to approve in Admin Dashboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            ContactHelper.openTelegram(
                                context,
                                "Hello Prince! I requested activation for Snap Loom and am waiting for approval."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Remind Prince on Telegram (@atheist_09)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun RestrictedSettingsDialog(
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenXiaomiOtherPerms: () -> Unit
) {
    val context = LocalContext.current
    val isXiaomi = remember { PermissionHelper.isXiaomiOrPoco() }
    val isRealme = remember { PermissionHelper.isRealmeOrOppoOrOnePlus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(if (isRealme) "Fix 'App was denied access' (Realme/Oppo)" else if (isXiaomi) "Fix 'Denied Access' (HyperOS)" else "Fix Restricted Settings (Android 14)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Android 13/14 (Realme UI / ColorOS / HyperOS) me sideloaded APK ke liye Overlay aur Pop-up windows locked rehte hain.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Isko 5 second me aise unlock karein:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text("1️⃣ Jo popup aaya hai, uspe 'Close' dabayein.", style = MaterialTheme.typography.bodySmall)
                    Text("2️⃣ Neeche '1. Open App Info' button dabayein (ya Home screen par Snap Loom ko daba ke 'App info' kholein).", style = MaterialTheme.typography.bodySmall)
                    Text("3️⃣ Top-right me 3-Dots (⋮) par tap karein -> 'Allow restricted settings' (प्रतिबंधित सेटिंग्स की अनुमति दें) select karein.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("4️⃣ Apna Fingerprint ya Screen PIN lagayein.", style = MaterialTheme.typography.bodySmall)
                    Text("5️⃣ Ab '2. Open Overlay' dabayein -> Toggle 100% ON ho jayega!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (isXiaomi) {
                        Text("6️⃣ Xiaomi users: 'Other Permissions' me 'Display pop-up windows in background' ko Allow karein.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = "💡 Tip: Bina overlay ke bhi Screen Recording notification bar se 100% chalegi!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onOpenAppInfo()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. Open App Info (Allow Restricted)")
                }
                OutlinedButton(
                    onClick = {
                        onOpenOverlay()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. Open Overlay Settings")
                }
                if (isXiaomi) {
                    OutlinedButton(
                        onClick = {
                            onOpenXiaomiOtherPerms()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3. Xiaomi Pop-up Permissions")
                    }
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun TermsAndConditionsConsentDialog(onAccept: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Legal Terms & Privacy Consent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Welcome to Snap Loom Studio! Before using the app, you must review and agree to our Terms of Service & Privacy Policy:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛡️ 100% On-Device & Private",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All screen recordings, camera facecam feeds, and audio commentary remain strictly on your local device. We never record, collect, upload, or transmit any video, audio, or screen data to any cloud or third-party server.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "⚖️ Sole User Legal Responsibility",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "You are solely and strictly responsible for the content you capture, broadcast, or share using Snap Loom. You must obtain consent before recording private conversations or third-party intellectual property.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "📑 Developer Indemnification",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "The developer (Prince / Reaction Studio) is held harmless and indemnified against any claims, copyright disputes, penalties, or damages arising from your usage or recordings.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreed = !agreed },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I have read, understood, and agree to the Terms of Service, User Liability, and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = agreed,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Accept & Enter Studio")
            }
        }
    )
}

@Composable
fun ReferAndEarnCard(viewModel: MainScreenViewModel) {
    val context = LocalContext.current
    val bonusDays = remember { mutableStateOf(viewModel.getReferralBonusDays()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6B21A8).copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎁", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "Refer & Earn Pro (+7 Days)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7)
                        )
                        Text(
                            text = if (bonusDays.value > 0) "You earned +${bonusDays.value} bonus VIP days!" else "Share with creators & extend VIP trial free",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFA855F7).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+7 DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                }
            }

            Text(
                text = "Share Snap Loom on WhatsApp or Telegram with friends or creators. Instantly get +7 extra Pro days added to your VIP access!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )

            Button(
                onClick = {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🚀 Check out Snap Loom — Studio Screen & Reaction Recorder with AI Dual-Mic Noise Shield, 4K 60FPS recording, and Floating Facecam! Download it free: https://prince-portfolio-jade.vercel.app"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Snap Loom with Friends"))
                        val newBonus = viewModel.awardReferralBonus()
                        bonusDays.value = newBonus
                        Toast.makeText(context, "🎉 +7 Days Pro Bonus added to your account!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA855F7)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share via WhatsApp / Telegram (+7 Days)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
