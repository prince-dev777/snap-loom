package com.reactionstudio.screenrecorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Automatically relaunches Snap Loom after an in-app update is installed.
 * Listens for ACTION_MY_PACKAGE_REPLACED which Android OS broadcasts to the replaced app.
 */
class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("AppUpdateReceiver", "Snap Loom updated to new version. Auto-relaunching app...")
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.e("AppUpdateReceiver", "Failed to auto-relaunch app after update: ${e.message}")
            }
        }
    }
}
