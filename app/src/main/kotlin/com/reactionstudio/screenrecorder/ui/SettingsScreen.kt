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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val config by viewModel.config.collectAsState()
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

        // Audio Section
        SettingsCard(
            title = "Audio Recording Source",
            icon = Icons.Default.Mic,
            subtitle = "Choose microphone commentary or internal game/video sound"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
