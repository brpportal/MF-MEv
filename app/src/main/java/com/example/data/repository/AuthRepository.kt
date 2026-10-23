package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.data.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EvidenceUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val teamName: String = "Evidence Team Alpha",
    val role: String = "Evidence Specialist",
    val isAuthorized: Boolean = true
)

class AuthRepository(private val context: Context) {

    private val _currentUser = MutableStateFlow<EvidenceUser?>(null)
    val currentUser: StateFlow<EvidenceUser?> = _currentUser.asStateFlow()

    fun isConfigured(): Boolean = true

    fun attemptSilentSignIn(scope: CoroutineScope) {
        scope.launch {
            try {
                val saved = SupabaseClient.getSavedSession(context)
                if (saved != null) {
                    _currentUser.value = saved
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Session restoration error: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (EvidenceUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        // Quick sign in for development/testing if user taps Google sign in
        scope.launch {
            val user = EvidenceUser(
                uid = "USR-GOOG-${System.currentTimeMillis() % 10000}",
                email = "officer.alpha@evidence.mfmpanel.com",
                displayName = "Evidence Officer",
                teamName = "Evidence Team Alpha",
                role = "Evidence Specialist",
                isAuthorized = true
            )
            SupabaseClient.saveSession(context, user, "google-oauth-token")
            _currentUser.value = user
            onSuccess(user)
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        scope: CoroutineScope,
        onSuccess: (EvidenceUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Please enter both email and password.")
            return
        }

        scope.launch {
            val result = SupabaseClient.signInWithEmail(context, email, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _currentUser.value = user
                onSuccess(user)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Invalid email or password."
                onError(err)
            }
        }
    }

    fun sendPasswordReset(
        email: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Please enter your registered officer email address.")
            return
        }

        scope.launch {
            val result = SupabaseClient.sendPasswordReset(context, email)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset link.")
            }
        }
    }

    fun verifyAndAccessWithPairingCode(
        email: String,
        code: String,
        scope: CoroutineScope,
        onSuccess: (EvidenceUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanCode = code.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            onError("Please enter a valid officer email address.")
            return
        }
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            onError("Pairing code must be exactly 6 digits.")
            return
        }

        scope.launch {
            val result = SupabaseClient.verifyPairingWithEmail(context, cleanEmail, cleanCode)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                LinkingRepository(context).setLocallyActivated(user.uid, cleanCode)
                _currentUser.value = user
                onSuccess(user)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Pairing verification failed."
                onError(err)
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch {
            SupabaseClient.clearSession(context)
            LinkingRepository(context).clearActivation()
            _currentUser.value = null
            onComplete()
        }
    }
}
