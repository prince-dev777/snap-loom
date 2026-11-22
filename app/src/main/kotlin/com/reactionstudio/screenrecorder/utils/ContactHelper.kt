package com.reactionstudio.screenrecorder.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Helper to handle direct contact and 1-Click UPI purchases with developer Prince.
 */
object ContactHelper {
    const val TELEGRAM_USERNAME = "atheist_09"
    const val TELEGRAM_URL = "https://t.me/$TELEGRAM_USERNAME"
    const val UPI_ID = "saware.tj@gmail.com"
    const val UPI_PAYEE_NAME = "Prince Dev"

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

    /**
     * 1-Click UPI payment launcher for Google Pay, PhonePe, Paytm, BHIM, Cred, etc.
     */
    fun openUpiPayment(
        context: Context,
        amount: String = "99",
        note: String = "SnapLoom VIP Pro"
    ) {
        val upiUri = Uri.parse(
            "upi://pay?pa=$UPI_ID&pn=${Uri.encode(UPI_PAYEE_NAME)}&am=$amount&cu=INR&tn=${Uri.encode(note)}"
        )
        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(upiIntent, "Pay ₹$amount via UPI (GPay/PhonePe/Paytm)"))
        } catch (e: Exception) {
            copyUpiId(context)
            Toast.makeText(
                context,
                "UPI ID copied: $UPI_ID. Send payment and share screenshot on Telegram!",
                Toast.LENGTH_LONG
            ).show()
            openTelegram(context, "Hello Prince! I am paying ₹$amount for Snap Loom Pro via UPI ($UPI_ID).")
        }
    }

    fun copyUpiId(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", UPI_ID)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "UPI ID copied: $UPI_ID", Toast.LENGTH_SHORT).show()
    }
}
