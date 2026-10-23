package com.example.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionStatus {
    LOGGED_IN,
    NOT_LOGGED_IN,
    EXPIRED
}

data class SupportedPlatform(
    val id: String,
    val name: String,
    val loginUrl: String,
    val domain: String,
    val sessionCookieNames: List<String>,
    val status: SessionStatus = SessionStatus.NOT_LOGGED_IN,
    val lastCheckedTimestamp: Long? = null
)

class PlatformSessionManager(private val context: Context) {

    private val cookieManager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }

    private val _platforms = MutableStateFlow(
        listOf(
            SupportedPlatform(
                id = "instagram",
                name = "Instagram",
                loginUrl = "https://www.instagram.com/accounts/login/",
                domain = "instagram.com",
                sessionCookieNames = listOf("sessionid", "ds_user_id", "csrftoken")
            ),
            SupportedPlatform(
                id = "facebook",
                name = "Facebook",
                loginUrl = "https://www.facebook.com/login/",
                domain = "facebook.com",
                sessionCookieNames = listOf("c_user", "xs")
            ),
            SupportedPlatform(
                id = "tiktok",
                name = "TikTok",
                loginUrl = "https://www.tiktok.com/login",
                domain = "tiktok.com",
                sessionCookieNames = listOf("sessionid", "sid_guard", "passport_csrf_token")
            ),
            SupportedPlatform(
                id = "youtube",
                name = "YouTube",
                loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube",
                domain = "youtube.com",
                sessionCookieNames = listOf("LOGIN_INFO", "SAPISID", "SSID", "SID")
            ),
            SupportedPlatform(
                id = "x",
                name = "X (Twitter)",
                loginUrl = "https://x.com/i/flow/login",
                domain = "x.com",
                sessionCookieNames = listOf("auth_token", "twid", "ct0")
            ),
            SupportedPlatform(
                id = "generic",
                name = "Other HTTPS",
                loginUrl = "https://google.com",
                domain = "google.com",
                sessionCookieNames = emptyList()
            )
        )
    )

    val platforms: StateFlow<List<SupportedPlatform>> = _platforms.asStateFlow()

    init {
        refreshAllSessionStatuses()
    }

    fun refreshAllSessionStatuses() {
        val current = _platforms.value.map { platform ->
            val status = checkSessionStatus(platform)
            platform.copy(
                status = status,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
        }
        _platforms.value = current
    }

    fun checkSessionStatus(platform: SupportedPlatform): SessionStatus {
        val cookies = cookieManager.getCookie("https://${platform.domain}") ?: ""
        if (cookies.isBlank()) return SessionStatus.NOT_LOGGED_IN

        if (platform.sessionCookieNames.isEmpty()) {
            return if (cookies.isNotBlank()) SessionStatus.LOGGED_IN else SessionStatus.NOT_LOGGED_IN
        }

        // Check if any of the session cookie indicators exist
        val hasSessionCookie = platform.sessionCookieNames.any { cookieName ->
            cookies.contains("$cookieName=")
        }

        return if (hasSessionCookie) SessionStatus.LOGGED_IN else SessionStatus.NOT_LOGGED_IN
    }

    fun logoutPlatform(platformId: String, onComplete: () -> Unit = {}) {
        val platform = _platforms.value.find { it.id == platformId } ?: return

        val url = "https://${platform.domain}"
        val cookies = cookieManager.getCookie(url)
        if (!cookies.isNullOrBlank()) {
            val cookiePairs = cookies.split(";")
            for (cookie in cookiePairs) {
                val parts = cookie.split("=")
                if (parts.isNotEmpty()) {
                    val name = parts[0].trim()
                    cookieManager.setCookie(url, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                }
            }
        }
        cookieManager.flush()
        refreshAllSessionStatuses()
        onComplete()
    }

    fun clearAllBrowserSessions(onComplete: () -> Unit = {}) {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
            refreshAllSessionStatuses()
            onComplete()
        }
    }
}
