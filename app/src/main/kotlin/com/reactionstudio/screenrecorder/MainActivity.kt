package com.reactionstudio.screenrecorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.service.ScreenRecordService
import com.reactionstudio.screenrecorder.ui.ActivationScreen
import com.reactionstudio.screenrecorder.ui.SplashScreen
import com.reactionstudio.screenrecorder.ui.main.MainScreen
import com.reactionstudio.screenrecorder.ui.main.MainScreenViewModel
import com.reactionstudio.screenrecorder.ui.theme.ScreenReactionStudioTheme
import com.reactionstudio.screenrecorder.utils.AppUpdateManager
import com.reactionstudio.screenrecorder.utils.LicenseManager
import com.reactionstudio.screenrecorder.utils.PermissionHelper
import com.reactionstudio.screenrecorder.utils.UpdateInfo

class MainActivity : ComponentActivity() {

    private val viewModel: MainScreenViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenRecordService.projectionResultCode = result.resultCode
            ScreenRecordService.projectionResultData = result.data

            val startIntent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }

            // Move task to back so user is ready to record whatever game/app/YouTube video is on screen
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Screen recording permission was denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && audioGranted) {
            Toast.makeText(this, "Camera and Audio permissions granted! 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Camera/Mic permission needed. In App Info -> Permissions, please enable them.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Overlay permission granted! Facecam is ready. 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "If blocked by 'Restricted settings', tap App Info -> 3 dots ⋮ -> 'Allow restricted settings'",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Silently register device in Admin Dashboard and sync cloud approval status
        lifecycleScope.launch(Dispatchers.IO) {
            LicenseManager.getInstance(applicationContext).silentSyncWithCloud()
        }

        setContent {
            ScreenReactionStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val updateInfo by viewModel.updateInfo.collectAsState()
                    var showSplash by remember { mutableStateOf(true) }
                    var isAccessGranted by remember {
                        mutableStateOf(LicenseManager.getInstance(this).isAppAccessible())
                    }

                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else if (updateInfo?.isUpdateAvailable == true) {
                        // MANDATORY BLOCKING UPDATE: User must update to get the latest features before entering
                        MandatoryUpdateScreen(
                            update = updateInfo!!,
                            onExitApp = { finishAffinity() }
                        )
                    } else if (!isAccessGranted) {
                        ActivationScreen(
                            onLicenseActivated = { isAccessGranted = true }
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onStartRecordingClick = { startScreenCaptureRequest() },
                            onRequestPermissions = { requestAppPermissions() },
                            onOpenOverlaySettings = { openOverlaySettings() }
                        )
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = PermissionHelper.getRequiredRuntimePermissions()
        runtimePermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun openOverlaySettings() {
        overlaySettingsLauncher.launch(PermissionHelper.openOverlaySettingsIntent(this))
    }

    private fun startScreenCaptureRequest() {
        if (!PermissionHelper.hasAudioPermission(this)) {
            requestAppPermissions()
            return
        }

        // Overlay is OPTIONAL. Screen recording works 100% with Notification Controls!
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(
                this,
                "Screen Recording started! Use notification bar to pause/stop.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }
}

@Composable
fun MandatoryUpdateScreen(
    update: UpdateInfo,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMb by remember { mutableFloatStateOf(0f) }
    var totalMb by remember { mutableFloatStateOf(21.8f) }
    var downloadStatusText by remember { mutableStateOf("Ready to update") }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B),
            Color(0xFF020617)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = com.reactionstudio.screenrecorder.R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                text = "🚀 Mandatory Update Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Snap Loom v${update.latestVersion} is required to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5B4FC),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current: v${update.currentVersion}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                        Text("Latest: v${update.latestVersion}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }

                    Text(
                        text = "What's New in v${update.latestVersion}:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = if (update.releaseNotes.isNotBlank()) update.releaseNotes else "• Facecam Screen Recording Fix (100% captured)\n• High-Speed 10x Fast OTA Downloader\n• Floating Teleprompter for Creators\n• 1-Click 9:16 YouTube Shorts Exporter",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time High-speed Progress Bar
            if (isDownloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF6366F1),
                        trackColor = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.1f MB / %.1f MB", downloadedMb, totalMb),
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Button(
                onClick = {
                    isDownloading = true
                    downloadStatusText = "Connecting to high-speed stream..."
                    coroutineScope.launch {
                        AppUpdateManager.downloadUpdateFast(
                            context = context,
                            downloadUrl = update.apkDownloadUrl,
                            version = update.latestVersion,
                            onProgress = { p, dMb, tMb ->
                                downloadProgress = p
                                downloadedMb = dMb
                                totalMb = tMb
                                downloadStatusText = "Downloading: ${(p * 100).toInt()}%"
                            },
                            onFinished = { file ->
                                downloadStatusText = "Complete! Opening package installer..."
                                AppUpdateManager.installApk(context, file)
                                isDownloading = false
                            },
                            onError = { _ ->
                                // Fallback to DownloadManager
                                AppUpdateManager.startDownload(context, update.apkDownloadUrl, update.latestVersion)
                                isDownloading = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(downloadStatusText, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Now (Fast 10x OTA)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Text(
                text = "Once update finishes installing, Snap Loom will auto-restart automatically.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            TextButton(onClick = onExitApp) {
                Text("Exit App", color = Color(0xFFEF4444))
            }
        }
    }
}
