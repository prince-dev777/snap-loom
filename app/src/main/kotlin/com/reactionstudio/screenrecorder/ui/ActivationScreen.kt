package com.reactionstudio.screenrecorder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.reactionstudio.screenrecorder.utils.ContactHelper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.R
import com.reactionstudio.screenrecorder.utils.ActivationResult
import com.reactionstudio.screenrecorder.utils.AppAccessState
import com.reactionstudio.screenrecorder.utils.LicenseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    onLicenseActivated: () -> Unit
) {
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var accessState by remember { mutableStateOf(licenseManager.getAppAccessState()) }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var showManualKeyInput by remember { mutableStateOf(false) }
    var manualKey by remember { mutableStateOf("") }
    var manualKeyError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Real-time silent auto-poll every 6 seconds if waiting for approval
    LaunchedEffect(accessState) {
        if (accessState is AppAccessState.PendingApproval) {
            while (true) {
                delay(6000)
                val approved = licenseManager.checkDeviceApprovalStatus()
                if (approved) {
                    Toast.makeText(context, "🎉 Approved! Welcome to Snap Loom Pro!", Toast.LENGTH_LONG).show()
                    onLicenseActivated()
                    break
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
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
                text = "Snap Loom Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            // Dynamic Friendly Subtitle (No technical jargon or device IDs)
            if (accessState is AppAccessState.PendingApproval) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Approval Pending",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "Your request has been submitted to Prince. Once approved, your app will unlock automatically!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Your 3-Day Free Trial Has Ended ⭐",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Continue enjoying unlimited 60 FPS recording, floating Facecam, screen brush, and lossless video trimmer with Snap Loom Pro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // PRIMARY ACTION BUTTONS
            if (accessState is AppAccessState.PendingApproval) {
                // Check Status Button (Auto-Unlock trigger)
                Button(
                    onClick = {
                        isCheckingStatus = true
                        coroutineScope.launch {
                            val approved = licenseManager.checkDeviceApprovalStatus()
                            isCheckingStatus = false
                            if (approved) {
                                Toast.makeText(context, "🎉 Approved! Welcome to Snap Loom Pro!", Toast.LENGTH_LONG).show()
                                onLicenseActivated()
                            } else {
                                Toast.makeText(context, "Still pending approval from Prince.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isCheckingStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    if (isCheckingStatus) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking Status...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Status (Auto-Unlock)", fontWeight = FontWeight.Bold)
                    }
                }

                // 1-Click Instant UPI Payment
                Button(
                    onClick = {
                        coroutineScope.launch {
                            licenseManager.requestDeviceActivation()
                            accessState = licenseManager.getAppAccessState()
                        }
                        ContactHelper.openUpiPayment(context, "99")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("⚡ Pay ₹99 via UPI (Instant GPay/PhonePe)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Telegram Chat Button
                OutlinedButton(
                    onClick = { openTelegramContact(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with Prince (@atheist_09)", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                }
            } else {
                // 1-Click Instant UPI Payment
                Button(
                    onClick = {
                        coroutineScope.launch {
                            licenseManager.requestDeviceActivation()
                            accessState = licenseManager.getAppAccessState()
                        }
                        ContactHelper.openUpiPayment(context, "99")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("⚡ Pay ₹99 via UPI (Instant GPay/PhonePe)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Contact Prince on Telegram
                Button(
                    onClick = {
                        coroutineScope.launch {
                            licenseManager.requestDeviceActivation()
                            accessState = licenseManager.getAppAccessState()
                        }
                        openTelegramContact(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Pro via Telegram (@atheist_09)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Optional Manual Key Toggle
            TextButton(
                onClick = { showManualKeyInput = !showManualKeyInput }
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showManualKeyInput) "Hide Key Input" else "Have an Activation Key? Enter Here",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AnimatedVisibility(visible = showManualKeyInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it.uppercase(); manualKeyError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter Key (e.g. SNAP-XXXX-YYYY)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    manualKeyError?.let {
                        Text(it, color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = licenseManager.activateWithKey(manualKey)
                                when (res) {
                                    is ActivationResult.Success -> {
                                        Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                        onLicenseActivated()
                                    }
                                    is ActivationResult.Error -> {
                                        manualKeyError = res.errorMessage
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Activate Key")
                    }
                }
            }
        }
    }
}

private fun openTelegramContact(context: Context) {
    ContactHelper.openTelegram(
        context = context,
        message = "Hello Prince, my 3-day trial ended and I want to activate Snap Loom Pro!"
    )
}
