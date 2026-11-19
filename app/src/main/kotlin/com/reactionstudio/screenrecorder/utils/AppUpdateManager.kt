package com.reactionstudio.screenrecorder.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
     * Initiates the download via Android DownloadManager with system notification.
     */
    fun startDownload(context: Context, downloadUrl: String, version: String) {
        try {
            val fileName = "SnapLoom-$version.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Snap Loom $version")
                setDescription("Downloading new update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                dm.enqueue(request)
                Toast.makeText(
                    context,
                    "Downloading Snap Loom update in background... Check notifications.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                openBrowser(context, downloadUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to browser
            openBrowser(context, downloadUrl)
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
