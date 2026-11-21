package com.reactionstudio.screenrecorder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Helper to handle direct contact with developer Prince via Telegram.
 */
object ContactHelper {
    const val TELEGRAM_USERNAME = "atheist_09"
    const val TELEGRAM_URL = "https://t.me/$TELEGRAM_USERNAME"

    fun openTelegram(
        context: Context,
        message: String = "Hello Prince! I am using Snap Loom Screen Recorder and I want to purchase Pro access."
    ) {
        val encodedMessage = Uri.encode(message)
        val tgAppUri = Uri.parse("tg://resolve?domain=$TELEGRAM_USERNAME&text=$encodedMessage")
        val tgWebUri = Uri.parse("https://t.me/$TELEGRAM_USERNAME?text=$encodedMessage")

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
                Toast.makeText(context, "Contact on Telegram: @$TELEGRAM_USERNAME", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
