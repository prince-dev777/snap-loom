package com.reactionstudio.screenrecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import com.reactionstudio.screenrecorder.utils.AppUpdateManager
import com.reactionstudio.screenrecorder.utils.LicenseManager
import com.reactionstudio.screenrecorder.utils.AppAccessState
import com.reactionstudio.screenrecorder.utils.ContactHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.model.AudioSourceMode
import com.reactionstudio.screenrecorder.model.CameraLens
import com.reactionstudio.screenrecorder.model.CountdownOption
import com.reactionstudio.screenrecorder.model.FacecamBorderColor
import com.reactionstudio.screenrecorder.model.FacecamShape
import com.reactionstudio.screenrecorder.model.FacecamSize
import com.reactionstudio.screenrecorder.model.OrientationMode
import com.reactionstudio.screenrecorder.model.VideoBitrate
import com.reactionstudio.screenrecorder.model.VideoFps
import com.reactionstudio.screenrecorder.model.VideoResolution
import com.reactionstudio.screenrecorder.ui.main.MainScreenViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: MainScreenViewModel) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val currentVersion = AppUpdateManager.getCurrentAppVersion(context)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Video & Reaction Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Customize video quality, FPS, and facecam for your YouTube reactions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Video Resolution Section
        SettingsCard(
            title = "Video Resolution",
            icon = Icons.Default.HighQuality,
            subtitle = "Higher resolution gives sharper YouTube videos"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoResolution.values().forEach { res ->
                    FilterChip(
                        selected = config.resolution == res,
                        onClick = { viewModel.updateResolution(res) },
                        label = { Text(res.displayName) }
                    )
                }
            }
        }

        // Frame Rate (FPS) Section
        SettingsCard(
            title = "Frame Rate (FPS)",
            icon = Icons.Default.Speed,
            subtitle = "60 FPS is ideal for smooth gaming and reaction video"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoFps.values().forEach { fps ->
                    FilterChip(
                        selected = config.fps == fps,
                        onClick = { viewModel.updateFps(fps) },
                        label = { Text(fps.displayName) }
                    )
                }
            }
        }

        // Video Bitrate Section
        SettingsCard(
            title = "Video Bitrate",
            icon = Icons.Default.Videocam,
            subtitle = "Higher bitrate means better visual clarity without pixelation"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoBitrate.values().forEach { bitrate ->
                    FilterChip(
                        selected = config.bitrate == bitrate,
                        onClick = { viewModel.updateBitrate(bitrate) },
                        label = { Text(bitrate.displayName) }
                    )
                }
            }
        }

        // Studio Audio & Noise Shield Section
        SettingsCard(
            title = "Studio Audio & AI Noise Shield",
            icon = Icons.Default.Mic,
            subtitle = "Real-time AI Noise Cancellation & 48 kHz Broadcast Master Audio"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Audio Recording Source",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioSourceMode.values().forEach { mode ->
                        FilterChip(
                            selected = config.audioMode == mode,
                            onClick = { viewModel.updateAudioMode(mode) },
                            label = { Text(mode.displayName) }
                        )
                    }
                }

                if (config.audioMode != AudioSourceMode.MUTE) {
                    // AI Studio Noise Shield Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "🎙️ AI Studio Noise Shield",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Kills ceiling fan, AC, and room echo in real-time using dual-mic phase cancellation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.enableNoiseShield,
                            onCheckedChange = { viewModel.updateNoiseShield(it) }
                        )
                    }

                    // Studio 48kHz HD Audio Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "💎 48 kHz Broadcast Master (256 kbps)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deep podcast vocal warmth and clarity (upgrades from 128 kbps to 256 kbps AAC).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.enableStudioHdAudio,
                            onCheckedChange = { viewModel.updateStudioHdAudio(it) }
                        )
                    }

                    // Dual Audio Mixer - Mic Volume Gain
                    Text(
                        text = "Mic Commentary Volume Gain",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f to "75% Soft", 1.0f to "100% Normal", 1.5f to "150% Boost", 2.0f to "200% Ultra").forEach { (gain, label) ->
                            FilterChip(
                                selected = config.micVolumeGain == gain,
                                onClick = { viewModel.updateMicVolumeGain(gain) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Creator Studio Tools (Teleprompter, Ghost Mode, Watermark)
        SettingsCard(
            title = "Creator Studio Tools",
            icon = Icons.Default.Widgets,
            subtitle = "Floating Teleprompter, Ghost Mode & Custom Watermark"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Floating Teleprompter Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "📜 Floating Script Teleprompter",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Auto-scrolling script over any game or video. Never forget your talking points!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.enableTeleprompter,
                        onCheckedChange = { viewModel.updateEnableTeleprompter(it) }
                    )
                }

                // Ghost Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "👻 Ghost Controls (Invisible Overlays)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hides floating buttons while recording so your final video looks 100% clean.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.ghostMode,
                        onCheckedChange = { viewModel.updateGhostMode(it) }
                    )
                }

                // Custom Channel Watermark Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "🏷️ Custom Channel Watermark",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Embed your personal channel name or handle on recorded videos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.enableCustomWatermark,
                        onCheckedChange = { viewModel.updateEnableWatermark(it) }
                    )
                }

                if (config.enableCustomWatermark) {
                    OutlinedTextField(
                        value = config.customWatermarkText,
                        onValueChange = { viewModel.updateWatermarkText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Channel Handle or Watermark Text") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Screen Orientation Section
        SettingsCard(
            title = "Screen Orientation",
            icon = Icons.Default.ScreenRotation,
            subtitle = "Portrait for Shorts/Reels, Landscape for YouTube and Games"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrientationMode.values().forEach { mode ->
                    FilterChip(
                        selected = config.orientation == mode,
                        onClick = { viewModel.updateOrientation(mode) },
                        label = { Text(mode.displayName) }
                    )
                }
            }
        }

        // Shake to Stop Section
        SettingsCard(
            title = "Shake to Stop Recording",
            icon = Icons.Default.Vibration,
            subtitle = "Stop recording cleanly by shaking phone (no notification drawer needed)"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (config.shakeToStop) "Shake to Stop Active" else "Disabled",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = config.shakeToStop,
                    onCheckedChange = { viewModel.updateShakeToStop(it) }
                )
            }
        }

        // Facecam Configuration Section
        SettingsCard(
            title = "Facecam (Reaction Camera)",
            icon = Icons.Default.CameraAlt,
            subtitle = "Show your face in a floating window over any app"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Facecam Overlay",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = config.enableFacecam,
                        onCheckedChange = { viewModel.updateEnableFacecam(it) }
                    )
                }

                if (config.enableFacecam) {
                    Text(
                        text = "Facecam Shape",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FacecamShape.values().forEach { shape ->
                            FilterChip(
                                selected = config.facecamShape == shape,
                                onClick = { viewModel.updateFacecamShape(shape) },
                                label = { Text(shape.displayName) }
                            )
                        }
                    }

                    Text(
                        text = "Facecam Size",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FacecamSize.values().forEach { size ->
                            FilterChip(
                                selected = config.facecamSize == size,
                                onClick = { viewModel.updateFacecamSize(size) },
                                label = { Text(size.displayName) }
                            )
                        }
                    }

                    Text(
                        text = "Facecam Border Glow Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FacecamBorderColor.values().forEach { color ->
                            FilterChip(
                                selected = config.facecamBorderColor == color,
                                onClick = { viewModel.updateFacecamBorderColor(color) },
                                label = { Text(color.displayName) }
                            )
                        }
                    }

                    Text(
                        text = "Default Camera",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraLens.values().forEach { lens ->
                            FilterChip(
                                selected = config.cameraLens == lens,
                                onClick = { viewModel.updateCameraLens(lens) },
                                label = { Text(lens.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Quick Controls & Countdown
        SettingsCard(
            title = "Controls & Timer",
            icon = Icons.Default.Widgets,
            subtitle = "Floating control bar and start countdown"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Floating Control Bar",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = config.showFloatingControls,
                        onCheckedChange = { viewModel.updateShowFloatingControls(it) }
                    )
                }

                Text(
                    text = "Start Countdown Timer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountdownOption.values().forEach { opt ->
                        FilterChip(
                            selected = config.countdown == opt,
                            onClick = { viewModel.updateCountdown(opt) },
                            label = { Text(opt.displayName) }
                        )
                    }
                }
            }
        }

        // Snap Loom VIP Access Section (User-friendly, no scary device IDs)
        val licenseManager = remember { LicenseManager.getInstance(context) }
        val accessState = remember { licenseManager.getAppAccessState() }

        SettingsCard(
            title = "Snap Loom VIP Access",
            icon = Icons.Default.Shield,
            subtitle = when (accessState) {
                is com.reactionstudio.screenrecorder.utils.AppAccessState.TrialActive ->
                    "Free VIP Trial Active (${accessState.daysRemaining} days remaining)"
                is com.reactionstudio.screenrecorder.utils.AppAccessState.Subscribed ->
                    if (accessState.daysRemaining != null) "Snap Loom Pro (${accessState.daysRemaining} days left)" else "Snap Loom Pro (Lifetime)"
                is com.reactionstudio.screenrecorder.utils.AppAccessState.PendingApproval ->
                    "Activation Pending Approval"
                else -> "Trial Ended"
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (accessState) {
                        is com.reactionstudio.screenrecorder.utils.AppAccessState.TrialActive ->
                            "You are currently on the 3-day free trial. Enjoy all 60 FPS recording and Facecam reaction tools!"
                        is com.reactionstudio.screenrecorder.utils.AppAccessState.Subscribed ->
                            "Full Pro Access unlocked. All recording, Facecam and editing features are active."
                        else ->
                            "Free trial period has ended. Request activation to unlock full recording capabilities."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var isSyncing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // Primary Buy Pro / Contact Prince on Telegram
                Button(
                    onClick = {
                        scope.launch {
                            licenseManager.requestDeviceActivation()
                        }
                        ContactHelper.openTelegram(
                            context,
                            "Hello Prince! I am using Snap Loom Screen Recorder and I want to purchase Pro access."
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy Pro / Chat on Telegram (@atheist_09)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            licenseManager.requestDeviceActivation()
                            licenseManager.silentSyncWithCloud()
                            isSyncing = false
                            Toast.makeText(context, "Device registered in Admin Dashboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Status with Admin Portal")
                    }
                }
            }
        }

        // Developer Support & Inquiry Section
        SettingsCard(
            title = "Help & Direct Support",
            icon = Icons.Default.Send,
            subtitle = "Official Telegram: @atheist_09"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Have questions, feedback, or want to buy Pro license directly? You can directly chat with Prince on Telegram.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        ContactHelper.openTelegram(
                            context,
                            "Hello Prince! I need support / have an inquiry regarding Snap Loom."
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with Prince on Telegram", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Snap Loom Updates & About Section
        SettingsCard(
            title = "App Updates & About",
            icon = Icons.Default.SystemUpdate,
            subtitle = "Installed Version: v$currentVersion • Developed by prince-dev777"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Snap Loom checks for new releases on GitHub. When an update is published, you can download and install it instantly without losing your videos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.checkForUpdates(silent = false) },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Updates")
                        }
                    }

                    OutlinedButton(
                        onClick = { AppUpdateManager.openBrowser(context, AppUpdateManager.GITHUB_RELEASES_PAGE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Releases")
                    }
                }
            }
        }

        // Legal Compliance & Privacy Terms Card
        var showTermsDialog by remember { mutableStateOf(false) }

        SettingsCard(
            title = "Legal Compliance & Privacy Terms",
            icon = Icons.Default.Security,
            subtitle = "User Content Responsibility & 100% On-Device Privacy Agreement"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Snap Loom complies with strict digital content laws. All recordings are kept 100% locally on your phone. You retain full ownership and legal responsibility for your content.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read Terms of Service & Privacy Policy", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Terms of Service & Privacy Agreement") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("1. User Ownership & Legal Responsibility:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("You (the user) retain full ownership and sole legal responsibility for all screen and audio recordings created with Snap Loom. You agree NOT to capture unlawful, confidential, or copyrighted content without authorization.")

                        Text("2. 100% On-Device Privacy Guarantee:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Snap Loom does NOT upload, transmit, or inspect your video recordings. All video rendering, facecam overlays, and audio DSP are executed locally on your device hardware.")

                        Text("3. Microphone & Camera Authorization:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Microphone and Camera hardware are only accessed when you explicitly start recording or activate the facecam preview.")

                        Text("4. Limitation of Developer Liability:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("By using this software, you release the developer from any claims, copyright notices, damages, or liabilities arising from the publication or distribution of your videos.")
                    }
                },
                confirmButton = {
                    Button(onClick = { showTermsDialog = false }) {
                        Text("I Understand & Agree")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}
