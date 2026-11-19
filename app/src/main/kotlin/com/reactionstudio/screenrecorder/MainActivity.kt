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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reactionstudio.screenrecorder.service.ScreenRecordService
import com.reactionstudio.screenrecorder.ui.ActivationScreen
import com.reactionstudio.screenrecorder.ui.SplashScreen
import com.reactionstudio.screenrecorder.ui.main.MainScreen
import com.reactionstudio.screenrecorder.ui.main.MainScreenViewModel
import com.reactionstudio.screenrecorder.ui.theme.ScreenReactionStudioTheme
import com.reactionstudio.screenrecorder.utils.LicenseManager
import com.reactionstudio.screenrecorder.utils.PermissionHelper

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
            Toast.makeText(this, "Camera and Audio permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Overlay permission granted! Facecam is ready.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScreenReactionStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var isAccessGranted by remember {
                        mutableStateOf(LicenseManager.getInstance(this).isAppAccessible())
                    }

                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
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
        if (!PermissionHelper.hasCameraPermission(this) || !PermissionHelper.hasAudioPermission(this)) {
            requestAppPermissions()
            return
        }

        if (!PermissionHelper.hasOverlayPermission(this)) {
            openOverlaySettings()
            return
        }

        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }
}
