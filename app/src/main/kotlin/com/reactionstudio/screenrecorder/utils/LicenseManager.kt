package com.reactionstudio.screenrecorder.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class AppAccessState {
    data class TrialActive(val daysRemaining: Int, val hoursRemaining: Int) : AppAccessState()
    object TrialExpired : AppAccessState()
    data class Subscribed(val planName: String, val expiresAt: String?, val daysRemaining: Int?) : AppAccessState()
    data class PendingApproval(val requestedAt: String) : AppAccessState()
    object Revoked : AppAccessState()
}

sealed class ActivationResult {
    data class Success(val message: String) : ActivationResult()
    data class Error(val errorMessage: String) : ActivationResult()
}

class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("snap_loom_license_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "AIzaSyDi0vQM-Rj2hdol7CWcQIh4qtOv5NvxwWU"
        private const val PROJECT_ID = "prince-master-hub"
        private const val TARGET_APP_PROJECT = "snap-loom"
        private const val FIRESTORE_DEVICES_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/devices"
        private const val FIRESTORE_LICENSES_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/licenses"
        private const val THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000L // 72 Hours

        @Volatile
        private var instance: LicenseManager? = null

        fun getInstance(context: Context): LicenseManager {
            return instance ?: synchronized(this) {
                instance ?: LicenseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Unique hardware Android ID for device locking.
     */
    fun getDeviceId(): String {
        return try {
            val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!rawId.isNullOrBlank()) "DEV-" + rawId.takeLast(8).uppercase() else "DEV-UNKNOWN"
        } catch (e: Exception) {
            "DEV-STANDARD"
        }
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }

    /**
     * Initializes or reads first launch time for the 3-day free trial.
     */
    fun getOrCreateFirstLaunchTime(): Long {
        var firstLaunch = prefs.getLong("first_launch_time", 0L)
        if (firstLaunch == 0L) {
            firstLaunch = System.currentTimeMillis()
            prefs.edit().putLong("first_launch_time", firstLaunch).apply()
        }
        return firstLaunch
    }

    /**
     * Determines whether the app can be used (Trial Active, Subscribed) or must be locked.
     */
    fun getAppAccessState(): AppAccessState {
        // 1. Check if user is Subscribed/Approved
        val isApproved = prefs.getBoolean("is_approved", false)
        val isRevoked = prefs.getBoolean("is_revoked", false)
        if (isRevoked) {
            return AppAccessState.Revoked
        }

        if (isApproved) {
            val subExpiresAt = prefs.getString("sub_expires_at", null)
            val planName = prefs.getString("plan_name", "Snap Loom Pro") ?: "Snap Loom Pro"
            if (subExpiresAt != null) {
                val daysLeft = calculateDaysRemaining(subExpiresAt)
                if (daysLeft != null && daysLeft < 0) {
                    return AppAccessState.TrialExpired // Subscription ended
                }
                return AppAccessState.Subscribed(planName, subExpiresAt, daysLeft)
            }
            return AppAccessState.Subscribed(planName, null, null) // Lifetime
        }

        // 2. Check Pending Approval
        val isPending = prefs.getBoolean("is_pending_approval", false)
        if (isPending) {
            val reqAt = prefs.getString("requested_at", "Recently") ?: "Recently"
            return AppAccessState.PendingApproval(reqAt)
        }

        // 3. Check 3-Day Free VIP Trial
        val firstLaunch = getOrCreateFirstLaunchTime()
        val now = System.currentTimeMillis()
        val elapsed = now - firstLaunch
        val remainingMs = THREE_DAYS_MS - elapsed

        if (remainingMs > 0) {
            val hours = (remainingMs / (1000 * 60 * 60)).toInt()
            val days = (remainingMs / (1000 * 60 * 60 * 24)).toInt()
            return AppAccessState.TrialActive(daysRemaining = days, hoursRemaining = hours)
        }

        return AppAccessState.TrialExpired
    }

    fun isAppAccessible(): Boolean {
        val state = getAppAccessState()
        return state is AppAccessState.TrialActive || state is AppAccessState.Subscribed
    }

    /**
     * User requests activation: Sends device details to Firebase Firestore and sets status to PENDING.
     */
    suspend fun requestDeviceActivation(): ActivationResult = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId()
        val model = getDeviceModel()
        val nowIso = getIsoTimestamp(System.currentTimeMillis())

        try {
            val url = URL("$FIRESTORE_DEVICES_URL/$deviceId?key=$API_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val fields = JSONObject().apply {
                put("deviceId", JSONObject().put("stringValue", deviceId))
                put("deviceModel", JSONObject().put("stringValue", model))
                put("projectId", JSONObject().put("stringValue", TARGET_APP_PROJECT))
                put("status", JSONObject().put("stringValue", "PENDING"))
                put("requestedAt", JSONObject().put("stringValue", nowIso))
            }
            val payload = JSONObject().put("fields", fields)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val code = conn.responseCode
            conn.disconnect()

            if (code == HttpURLConnection.HTTP_OK || code == 200 || code == 201) {
                prefs.edit()
                    .putBoolean("is_pending_approval", true)
                    .putString("requested_at", nowIso)
                    .apply()
                ActivationResult.Success("Activation request sent to Prince!")
            } else {
                ActivationResult.Error("Could not submit request (Error $code)")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Even if network fails, mark locally as requested so user can open Telegram
            prefs.edit()
                .putBoolean("is_pending_approval", true)
                .putString("requested_at", nowIso)
                .apply()
            ActivationResult.Success("Ready to connect on Telegram!")
        }
    }

    /**
     * Checks if Prince has clicked "Approve" for this Device ID in the Admin Dashboard.
     */
    suspend fun checkDeviceApprovalStatus(): Boolean = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId()
        try {
            val url = URL("$FIRESTORE_DEVICES_URL/$deviceId?key=$API_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext false
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            val json = JSONObject(sb.toString())
            val fields = json.optJSONObject("fields") ?: return@withContext false

            val status = fields.optJSONObject("status")?.optString("stringValue", "") ?: ""
            if (status == "APPROVED") {
                val subExpiresAtObj = fields.optJSONObject("subscriptionExpiresAt")
                val subExpiresAt = if (subExpiresAtObj != null && subExpiresAtObj.has("stringValue")) {
                    subExpiresAtObj.optString("stringValue", "")
                } else null

                val plan = fields.optJSONObject("plan")?.optString("stringValue", "Snap Loom Pro") ?: "Snap Loom Pro"

                // Save locally
                prefs.edit()
                    .putBoolean("is_approved", true)
                    .putBoolean("is_pending_approval", false)
                    .putBoolean("is_revoked", false)
                    .putString("sub_expires_at", subExpiresAt)
                    .putString("plan_name", plan)
                    .apply()
                return@withContext true
            } else if (status == "REVOKED") {
                prefs.edit()
                    .putBoolean("is_approved", false)
                    .putBoolean("is_revoked", true)
                    .apply()
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Legacy/Backup: Activates a manual License Key (e.g. SNAP-XXXX-YYYY).
     */
    suspend fun activateWithKey(inputKey: String): ActivationResult = withContext(Dispatchers.IO) {
        val cleanKey = inputKey.trim().uppercase()
        if (cleanKey.isBlank()) return@withContext ActivationResult.Error("Enter a valid key")

        val deviceId = getDeviceId()
        try {
            val url = URL("$FIRESTORE_LICENSES_URL/$cleanKey?key=$API_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 404) {
                conn.disconnect()
                return@withContext ActivationResult.Error("Key not found")
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            reader.close()
            conn.disconnect()

            val fields = JSONObject(sb.toString()).optJSONObject("fields")
                ?: return@withContext ActivationResult.Error("Invalid key")

            val status = fields.optJSONObject("status")?.optString("stringValue", "ACTIVE") ?: "ACTIVE"
            if (status == "REVOKED") return@withContext ActivationResult.Error("Key is revoked")

            val expiresAtObj = fields.optJSONObject("expiresAt")
            val expiresAt = if (expiresAtObj != null && expiresAtObj.has("stringValue")) {
                expiresAtObj.optString("stringValue", "")
            } else null

            prefs.edit()
                .putBoolean("is_approved", true)
                .putBoolean("is_pending_approval", false)
                .putBoolean("is_revoked", false)
                .putString("sub_expires_at", expiresAt)
                .putString("plan_name", "Licensed ($cleanKey)")
                .apply()

            ActivationResult.Success("Key activated successfully!")
        } catch (e: Exception) {
            ActivationResult.Error("Connection error: ${e.message}")
        }
    }

    private fun getIsoTimestamp(timeMs: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return format.format(Date(timeMs))
    }

    private fun calculateDaysRemaining(isoDateString: String): Int? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(isoDateString.take(19)) ?: return null
            val diffMs = date.time - System.currentTimeMillis()
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            null
        }
    }
}
