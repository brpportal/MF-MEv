package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class RegistrationStatus {
    CHECKING,
    NOT_LINKED,
    WAITING_CONFIRMATION,
    LINKED,
    ACTIVATED,
    ACCESS_REVOKED,
    ERROR
}

class LinkingRepository(private val context: Context? = null) {
    private val PREFS_NAME = "mf_device_linking_prefs"
    private val KEY_ACTIVATED = "is_device_activated"
    private val KEY_ACTIVATED_USER = "activated_user_id"
    private val KEY_PAIRING_CODE = "last_pairing_code"

    fun isActivated(): Boolean {
        if (context == null) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    private fun isLocallyActivated(userId: String): Boolean {
        if (context == null) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isActivated = prefs.getBoolean(KEY_ACTIVATED, false)
        val savedUid = prefs.getString(KEY_ACTIVATED_USER, null)
        return isActivated && (savedUid == null || savedUid.isBlank() || savedUid == userId || savedUid.contains(userId) || userId.contains(savedUid))
    }

    fun setLocallyActivated(userId: String, code: String = "") {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, true)
            .putString(KEY_ACTIVATED_USER, userId)
            .putString(KEY_PAIRING_CODE, code)
            .apply()
    }

    fun clearActivation() {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun activateLocally(userId: String): Boolean {
        setLocallyActivated(userId, "OFFLINE_TEST")
        return true
    }

    /**
     * Observes real-time registration / activation status from Supabase PostgREST for the user.
     */
    fun getRegistrationStatus(userId: String): Flow<RegistrationStatus> = flow {
        emit(RegistrationStatus.CHECKING)

        if (userId.isBlank()) {
            emit(RegistrationStatus.NOT_LINKED)
            return@flow
        }

        if (context == null) {
            emit(RegistrationStatus.ACTIVATED)
            return@flow
        }

        // If previously activated on this device, immediately emit ACTIVATED
        if (isLocallyActivated(userId)) {
            emit(RegistrationStatus.ACTIVATED)
        }

        // Periodically verify with Supabase PostgREST table app_registrations
        var attempts = 0
        while (true) {
            try {
                val remoteStatus = SupabaseClient.getRegistrationStatus(context, userId)
                if (remoteStatus == RegistrationStatus.ACTIVATED) {
                    setLocallyActivated(userId)
                    emit(RegistrationStatus.ACTIVATED)
                } else if (remoteStatus == RegistrationStatus.ACCESS_REVOKED) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().remove(KEY_ACTIVATED).apply()
                    emit(RegistrationStatus.ACCESS_REVOKED)
                } else {
                    if (isLocallyActivated(userId)) {
                        emit(RegistrationStatus.ACTIVATED)
                    } else {
                        emit(remoteStatus)
                    }
                }
            } catch (e: Exception) {
                Log.w("LinkingRepository", "Supabase status query failed: ${e.message}")
                if (isLocallyActivated(userId)) {
                    emit(RegistrationStatus.ACTIVATED)
                } else {
                    emit(RegistrationStatus.NOT_LINKED)
                }
            }

            delay(10000) // Poll Supabase every 10 seconds for real-time dashboard linking
            attempts++
        }
    }

    /**
     * Atomically verifies the 6-digit pairing code with Supabase PostgREST backend.
     */
    suspend fun pairWithCode(userId: String, pairingCode: String): Result<Unit> {
        val cleanCode = pairingCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return Result.failure(Exception("Pairing code must be exactly 6 digits."))
        }

        if (context == null) {
            return Result.success(Unit)
        }

        val result = SupabaseClient.verify6DigitPairingCode(context, userId, cleanCode)
        if (result.isSuccess) {
            setLocallyActivated(userId, cleanCode)
            return Result.success(Unit)
        } else {
            return result
        }
    }

    suspend fun pairWithEmailAndCode(email: String, pairingCode: String): Result<EvidenceUser> {
        val cleanEmail = email.trim()
        val cleanCode = pairingCode.trim()

        if (cleanEmail.isBlank()) {
            return Result.failure(Exception("Email cannot be empty."))
        }
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return Result.failure(Exception("Pairing code must be exactly 6 digits."))
        }

        if (context == null) {
            val user = EvidenceUser(
                uid = "USR-DEV",
                email = cleanEmail,
                displayName = cleanEmail.substringBefore("@")
            )
            return Result.success(user)
        }

        val result = SupabaseClient.verifyPairingWithEmail(context, cleanEmail, cleanCode)
        if (result.isSuccess) {
            val user = result.getOrThrow()
            setLocallyActivated(user.uid, cleanCode)
            return Result.success(user)
        }
        return result
    }
}
