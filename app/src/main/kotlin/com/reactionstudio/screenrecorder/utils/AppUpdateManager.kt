package com.reactionstudio.screenrecorder.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val htmlUrl: String,
    val isUpdateAvailable: Boolean
)

object AppUpdateManager {

    private const val GITHUB_REPO_API = "https://api.github.com/repos/prince-dev777/snap-loom/releases/latest"
    const val GITHUB_FALLBACK_DOWNLOAD_URL = "https://github.com/prince-dev777/snap-loom/releases/latest/download/SnapLoom.apk"
    const val GITHUB_RELEASES_PAGE = "https://github.com/prince-dev777/snap-loom/releases"

    /**
     * Checks GitHub API for the latest release in a background coroutine.
     */
    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentAppVersion(context)

            val url = URL(GITHUB_REPO_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SnapLoom-Android-App")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            connection.disconnect()

            val json = JSONObject(response.toString())
            val rawTag = json.optString("tag_name", "1.0.0")
            val cleanTag = rawTag.removePrefix("v").trim()
            val releaseTitle = json.optString("name", "Snap Loom $rawTag")
            val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_PAGE)

            // Look for .apk asset in release
            var apkUrl = GITHUB_FALLBACK_DOWNLOAD_URL
            val assets = json.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", GITHUB_FALLBACK_DOWNLOAD_URL)
                        break
                    }
                }
            }

            val updateAvailable = isNewerVersion(remoteVersion = cleanTag, currentVersion = currentVersion)

            UpdateInfo(
                latestVersion = cleanTag,
                currentVersion = currentVersion,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                htmlUrl = htmlUrl,
                isUpdateAvailable = updateAvailable
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCurrentAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName?.removePrefix("v")?.trim() ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.0" vs "1.0.0").
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split('.').mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split('.').mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            return remoteVersion != currentVersion
        }
        return false
    }

    /**
     * Initiates the download via Android DownloadManager with system notification
     * and automatically launches the package installer once download is complete.
     */
    fun startDownload(context: Context, downloadUrl: String, version: String) {
        val appContext = context.applicationContext
        try {
            // Check unknown source install permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!appContext.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        appContext,
                        "Please allow 'Install unknown apps' for Snap Loom to install updates automatically",
                        Toast.LENGTH_LONG
                    ).show()
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${appContext.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    appContext.startActivity(manageIntent)
                }
            }

            val fileName = "SnapLoom-$version.apk"
            val destFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destFile.exists()) {
                destFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Snap Loom v$version")
                setDescription("Downloading new update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                val downloadId = dm.enqueue(request)
                Toast.makeText(
                    appContext,
                    "Downloading Snap Loom v$version... Package installer will open automatically when finished.",
                    Toast.LENGTH_LONG
                ).show()

                // Auto install listener
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(recvContext: Context?, intent: Intent?) {
                        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                        if (id == downloadId) {
                            try {
                                appContext.unregisterReceiver(this)
                            } catch (_: Exception) {}
                            installApk(appContext, destFile)
                        }
                    }
                }

                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    appContext.registerReceiver(receiver, filter)
                }
            } else {
                openBrowser(appContext, downloadUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to browser
            openBrowser(appContext, downloadUrl)
        }
    }

    /**
     * Prompts the system package installer for the downloaded APK using FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "Downloaded file not found. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start package installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Fallback to browser download.
     */
    fun openBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
