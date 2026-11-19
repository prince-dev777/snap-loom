package com.reactionstudio.screenrecorder.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reactionstudio.screenrecorder.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(800) // Brief pause to enjoy the branding
        onSplashFinished()
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF14141E),
            Color(0xFF0A0A0F),
            Color(0xFF000000)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Snap Loom Logo",
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFFF0055))
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "Snap Loom",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Reaction & Screen Studio • 60 FPS HD",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF4081),
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
