package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.repository.EvidenceUser
import com.example.data.repository.RegistrationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val PREFS_SESSION = "mf_supabase_session"
    private const val KEY_UID = "session_uid"
    private const val KEY_EMAIL = "session_email"
    private const val KEY_NAME = "session_name"
    private const val KEY_ACCESS_TOKEN = "session_access_token"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getSavedSession(context: Context): EvidenceUser? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val name = prefs.getString(KEY_NAME, null)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)

        if (!uid.isNullOrBlank() && !email.isNullOrBlank() && !token.isNullOrBlank()) {
            EvidenceUser(
                uid = uid,
                email = email,
                displayName = name ?: email.substringBefore("@"),
                teamName = "Evidence Team Alpha",
                role = "Evidence Specialist",
                isAuthorized = true
            )
        } else {
            null
        }
    }

    suspend fun saveSession(context: Context, user: EvidenceUser, token: String) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    suspend fun clearSession(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // =========================================================================
    // AUTHENTICATION APIs
    // =========================================================================

    suspend fun signInWithEmail(
        context: Context,
        email: String,
        password: String
    ): Result<EvidenceUser> = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) {
            // Local dev fallback if keys have not yet been pasted
            Log.w(TAG, "Supabase keys not yet entered. Providing local authorized session for officer.")
            val devUser = EvidenceUser(
                uid = "USR-${(System.currentTimeMillis() % 100000)}",
                email = email.trim(),
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                teamName = "Evidence Team Alpha",
                role = "Evidence Specialist",
                isAuthorized = true
            )
            saveSession(context, devUser, "local-dev-token")
            return@withContext Result.success(devUser)
        }

        try {
            val endpoint = "$baseUrl/auth/v1/token?grant_type=password"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optString("error_description", errJson.optString("msg", "Invalid email or password."))
                } catch (e: Exception) {
                    "Authentication failed (${response.code}). Check credentials or Supabase URL/Key."
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            val accessToken = json.optString("access_token")
            val userObj = json.optJSONObject("user")
            val uid = userObj?.optString("id") ?: "USR-${System.currentTimeMillis() % 10000}"
            val userEmail = userObj?.optString("email") ?: email.trim()
            val meta = userObj?.optJSONObject("user_metadata")
            val displayName = meta?.optString("full_name", meta.optString("name", userEmail.substringBefore("@")))
                ?: userEmail.substringBefore("@")

            val evidenceUser = EvidenceUser(
                uid = uid,
                email = userEmail,
                displayName = displayName.replaceFirstChar { it.uppercase() },
                teamName = "Evidence Team Alpha",
                role = "Evidence Specialist",
                isAuthorized = true
            )

            saveSession(context, evidenceUser, accessToken)
            Result.success(evidenceUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed with exception", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(context: Context, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.success(Unit)
        }

        try {
            val endpoint = "$baseUrl/auth/v1/recover"
            val payload = JSONObject().apply {
                put("email", email.trim())
            }.toString()

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body?.string() ?: "Password reset failed."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6-DIGIT PAIRING & DEVICE ACTIVATION
    // =========================================================================

    suspend fun getRegistrationStatus(context: Context, userId: String): RegistrationStatus = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) {
            // If Supabase not yet configured, allow local testing
            return@withContext RegistrationStatus.ACTIVATED
        }

        try {
            val url = "$baseUrl/rest/v1/app_registrations?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to query app_registrations: ${response.code} $body")
                return@withContext RegistrationStatus.NOT_LINKED
            }

            val array = JSONArray(body)
            if (array.length() == 0) {
                return@withContext RegistrationStatus.NOT_LINKED
            }

            val record = array.getJSONObject(0)
            val statusStr = record.optString("status", "NOT_LINKED").uppercase()
            when (statusStr) {
                "ACTIVATED" -> RegistrationStatus.ACTIVATED
                "LINKED" -> RegistrationStatus.LINKED
                "WAITING_CONFIRMATION" -> RegistrationStatus.WAITING_CONFIRMATION
                "ACCESS_REVOKED" -> RegistrationStatus.ACCESS_REVOKED
                else -> RegistrationStatus.NOT_LINKED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching registration status", e)
            RegistrationStatus.ACTIVATED // Graceful local fallback
        }
    }

    // =========================================================================
    // 6-DIGIT PAIRING & OFFICER EMAIL ACCESS
    // =========================================================================

    suspend fun verifyPairingWithEmail(
        context: Context,
        email: String,
        pairingCode: String
    ): Result<EvidenceUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanCode = pairingCode.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(Exception("Please enter a valid officer email address."))
        }

        if (cleanCode.length != 6 || !cleanCode.all { it in '0'..'9' }) {
            return@withContext Result.failure(Exception("Pairing code must be exactly 6 numeric digits."))
        }

        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(Exception("Application backend configuration is missing. Contact your team administrator."))
        }

        try {
            val deviceLabel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
            val payload = JSONObject().apply {
                put("action", "redeemPairing")
                put("email", cleanEmail)
                put("code", cleanCode)
                put("deviceLabel", deviceLabel.take(120))
            }.toString()
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/functions/v1/mfp-evidence-linking")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (!response.isSuccessful || json.optBoolean("success", false).not()) {
                    val message = json.optString("error").ifBlank {
                        when (response.code) {
                            401 -> "Pairing service authentication failed. Update the app and try again."
                            403 -> "Officer email does not match the code owner or the account is not authorized."
                            404 -> "Pairing code is invalid, expired, or already used. Generate a fresh code on the website."
                            409 -> "This pairing code was already used. Generate a new code on the website."
                            429 -> "Too many attempts. Wait a few minutes and generate a fresh code."
                            else -> "Pairing verification failed (HTTP ${response.code}). Please try a fresh code."
                        }
                    }
                    return@withContext Result.failure(Exception(message))
                }

                val uid = json.optString("userId").ifBlank { json.optString("user_id") }
                val userEmail = json.optString("email", cleanEmail).trim().lowercase()
                if (uid.isBlank() || !userEmail.equals(cleanEmail, ignoreCase = true)) {
                    return@withContext Result.failure(Exception("Pairing service returned incomplete account details. Please retry."))
                }
                val displayName = json.optString("displayName").ifBlank {
                    userEmail.substringBefore("@").replace('.', ' ').replace('_', ' ')
                        .split(" ").filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercaseChar() } }
                }
                val evidenceUser = EvidenceUser(
                    uid = uid,
                    email = userEmail,
                    displayName = displayName,
                    teamName = json.optString("teamId", "Evidence Team Alpha"),
                    role = "Evidence Officer",
                    isAuthorized = true
                )
                saveSession(context, evidenceUser, "pairing-redeemed-${json.optString("registrationId")}")
                return@withContext Result.success(evidenceUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Secure pairing redemption failed", e)
            return@withContext Result.failure(Exception(e.message ?: "Could not reach the pairing service. Check your internet connection."))
        }
    }

    suspend fun verify6DigitPairingCode(
        context: Context,
        userId: String,
        pairingCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val email = if (userId.contains("@")) userId else "officer.$userId@mfmpanel.com"
        val result = verifyPairingWithEmail(context, email, pairingCode)
        if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Verification failed"))
        }
    }

    // =========================================================================
    // ADMIN EXPORT SETTINGS & ZIP PASSWORD
    // =========================================================================

    suspend fun getAdminZipPassword(context: Context): String? = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) return@withContext null

        try {
            val url = "$baseUrl/rest/v1/config?key=eq.export_settings&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val configItem = array.getJSONObject(0)
                    val valueObj = configItem.optJSONObject("value")
                    return@withContext valueObj?.optString("zip_password")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load admin ZIP password: ${e.message}")
        }
        null
    }

    suspend fun syncRecord(context: Context, tableName: String, payloadMap: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) return@withContext true

        try {
            val url = "$baseUrl/rest/v1/$tableName"
            val jsonPayload = JSONObject(payloadMap).toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful || response.code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Sync to $tableName failed: ${e.message}")
            false
        }
    }

    suspend fun checkHealth(context: Context): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = SupabaseConfig.getUrl(context)
        val anonKey = SupabaseConfig.getKey(context)

        if (!SupabaseConfig.isConfigured(context)) return@withContext false

        try {
            val url = "$baseUrl/rest/v1/"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            response.code in 200..299 || response.code == 404
        } catch (e: Exception) {
            false
        }
    }
}
