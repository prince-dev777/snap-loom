package com.reactionstudio.screenrecorder.utils

import android.content.Context
import android.content.SharedPreferences
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

sealed class LicenseStatus {
    object Unlicensed : LicenseStatus()
    data class Active(
        val key: String,
        val clientName: String,
        val expiresAt: String?,
        val daysRemaining: Int?
    ) : LicenseStatus()
    data class Expired(val key: String, val expiryDate: String) : LicenseStatus()
    data class Revoked(val reason: String) : LicenseStatus()
}

sealed class ActivationResult {
    data class Success(val message: String, val daysRemaining: Int?) : ActivationResult()
    data class Error(val errorMessage: String) : ActivationResult()
}

class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("snap_loom_license_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "AIzaSyDi0vQM-Rj2hdol7CWcQIh4qtOv5NvxwWU"
        private const val PROJECT_ID = "prince-master-hub"
        private const val TARGET_APP_PROJECT = "snap-loom"
        private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/licenses"

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

    /**
     * Reads the current cached license state.
     */
    fun getLicenseStatus(): LicenseStatus {
        val key = prefs.getString("key", null) ?: return LicenseStatus.Unlicensed
        val clientName = prefs.getString("client_name", "Valued Creator") ?: "Valued Creator"
        val expiresAt = prefs.getString("expires_at", null)
        val isRevoked = prefs.getBoolean("is_revoked", false)

        if (isRevoked) {
            return LicenseStatus.Revoked("License suspended by administrator")
        }

        if (expiresAt != null) {
            val remaining = calculateDaysRemaining(expiresAt)
            if (remaining != null && remaining < 0) {
                return LicenseStatus.Expired(key, expiresAt)
            }
            return LicenseStatus.Active(key, clientName, expiresAt, remaining)
        }

        return LicenseStatus.Active(key, clientName, null, null)
    }

    fun isLicenseValid(): Boolean {
        return getLicenseStatus() is LicenseStatus.Active
    }

    /**
     * Activates a license key with Firebase Firestore and binds device ID.
     */
    suspend fun activateLicense(inputKey: String): ActivationResult = withContext(Dispatchers.IO) {
        val cleanKey = inputKey.trim().uppercase()
        if (cleanKey.isBlank()) {
            return@withContext ActivationResult.Error("Please enter a valid license key")
        }

        val deviceId = getDeviceId()

        try {
            val url = URL("$FIRESTORE_BASE_URL/$cleanKey?key=$API_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                conn.disconnect()
                return@withContext ActivationResult.Error("License key not found. Please verify your key with Prince.")
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext ActivationResult.Error("Unable to verify license (Server code: $responseCode)")
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
            val fields = json.optJSONObject("fields")
                ?: return@withContext ActivationResult.Error("Invalid license format on server")

            // 1. Check Project ID
            val licProjectId = fields.optJSONObject("projectId")?.optString("stringValue", "") ?: ""
            if (licProjectId.isNotBlank() && licProjectId != TARGET_APP_PROJECT) {
                return@withContext ActivationResult.Error("This license is for a different project ($licProjectId)")
            }

            // 2. Check Status
            val status = fields.optJSONObject("status")?.optString("stringValue", "ACTIVE") ?: "ACTIVE"
            if (status == "REVOKED") {
                saveLicenseRevoked(cleanKey)
                return@withContext ActivationResult.Error("This license has been revoked / suspended by admin")
            }

            // 3. Check Expiry
            val expiresAtObj = fields.optJSONObject("expiresAt")
            val expiresAt = if (expiresAtObj != null && expiresAtObj.has("stringValue")) {
                expiresAtObj.optString("stringValue", "")
            } else null
            val daysRemaining = expiresAt?.let { calculateDaysRemaining(it) }
            if (daysRemaining != null && daysRemaining < 0) {
                return@withContext ActivationResult.Error("This license has expired. Contact Prince to renew.")
            }

            // 4. Device Binding Check
            val boundDevicesArray = fields.optJSONObject("boundDevices")?.optJSONObject("arrayValue")?.optJSONArray("values")
            val existingDevices = mutableListOf<String>()
            if (boundDevicesArray != null) {
                for (i in 0 until boundDevicesArray.length()) {
                    val dVal = boundDevicesArray.getJSONObject(i).optString("stringValue", "")
                    if (dVal.isNotBlank()) existingDevices.add(dVal)
                }
            }

            val maxDevices = fields.optJSONObject("maxDevices")?.optString("integerValue", "1")?.toIntOrNull() ?: 1

            if (!existingDevices.contains(deviceId)) {
                if (existingDevices.size >= maxDevices) {
                    return@withContext ActivationResult.Error(
                        "Device limit exceeded! This license is already bound to device: ${existingDevices.firstOrNull()}"
                    )
                }

                // Bind device in Firestore
                existingDevices.add(deviceId)
                bindDeviceToFirestore(cleanKey, existingDevices)
            }

            val clientName = fields.optJSONObject("clientName")?.optString("stringValue", "Creator") ?: "Creator"

            // Save to local SharedPreferences
            saveLocalLicense(cleanKey, clientName, expiresAt)

            ActivationResult.Success(
                message = "Snap Loom Activated for $clientName!",
                daysRemaining = daysRemaining
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ActivationResult.Error("Connection error: ${e.localizedMessage ?: "Check your internet"}")
        }
    }

    private fun bindDeviceToFirestore(key: String, devices: List<String>) {
        try {
            val url = URL("$FIRESTORE_BASE_URL/$key?updateMask.fieldPaths=boundDevices&key=$API_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val valuesArr = JSONArray()
            for (d in devices) {
                valuesArr.put(JSONObject().put("stringValue", d))
            }
            val payload = JSONObject().put(
                "fields",
                JSONObject().put(
                    "boundDevices",
                    JSONObject().put("arrayValue", JSONObject().put("values", valuesArr))
                )
            )

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLocalLicense(key: String, clientName: String, expiresAt: String?) {
        prefs.edit()
            .putString("key", key)
            .putString("client_name", clientName)
            .putString("expires_at", expiresAt)
            .putBoolean("is_revoked", false)
            .putLong("activated_at", System.currentTimeMillis())
            .apply()
    }

    private fun saveLicenseRevoked(key: String) {
        prefs.edit()
            .putString("key", key)
            .putBoolean("is_revoked", true)
            .apply()
    }

    fun removeLicense() {
        prefs.edit().clear().apply()
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
