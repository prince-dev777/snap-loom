package com.reactionstudio.screenrecorder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.R
import com.reactionstudio.screenrecorder.utils.ActivationResult
import com.reactionstudio.screenrecorder.utils.LicenseManager
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    onLicenseActivated: () -> Unit
) {
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val deviceId = remember { licenseManager.getDeviceId() }
    val coroutineScope = rememberCoroutineScope()

    var keyInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var copiedDevice by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Snap Loom Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF38BDF8), CircleShape)
            )

            // Header Title
            Text(
                text = "Snap Loom",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Screen & Facecam Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF43F5E),
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "To access high-definition 60 FPS recording and Facecam overlay, enter your licensed authorization key.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Device ID Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                        Column {
                            Text(
                                text = "Your Device ID (Hardware Locked)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                            copiedDevice = true
                            Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (copiedDevice) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Device ID",
                            tint = if (copiedDevice) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // License Key Input
            OutlinedTextField(
                value = keyInput,
                onValueChange = {
                    keyInput = it.uppercase()
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter License Key (e.g. SNAP-XXXX-YYYY)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Feedback / Error
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFFF87171),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFF34D399),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Activate Button
            Button(
                onClick = {
                    if (keyInput.isBlank()) {
                        errorMessage = "Please enter your license key"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = licenseManager.activateLicense(keyInput)
                        isLoading = false
                        when (result) {
                            is ActivationResult.Success -> {
                                successMessage = result.message
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                onLicenseActivated()
                            }
                            is ActivationResult.Error -> {
                                errorMessage = result.errorMessage
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying License...")
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activate License", fontWeight = FontWeight.Bold)
                }
            }

            // Get License Key via Telegram (@atheist_09)
            OutlinedButton(
                onClick = {
                    val message = "Hello Prince, I want to get a license key for Snap Loom!\nMy Device ID: $deviceId"
                    val encodedMessage = Uri.encode(message)
                    val tgAppUri = Uri.parse("tg://resolve?domain=atheist_09&text=$encodedMessage")
                    val tgWebUri = Uri.parse("https://t.me/atheist_09?text=$encodedMessage")

                    val appIntent = Intent(Intent.ACTION_VIEW, tgAppUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(appIntent)
                    } catch (e: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, tgWebUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(webIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Contact on Telegram: @atheist_09", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get License Key via Telegram (@atheist_09)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
