package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * =============================================================================
 * 🔑 SUPABASE CONFIGURATION LOCATION
 * =============================================================================
 * File: /app/src/main/java/com/example/data/SupabaseConfig.kt
 *
 * You can edit your Supabase Project URL and Anon/Publishable Key directly below,
 * OR you can configure/change them at runtime from the app's Settings screen!
 * =============================================================================
 */
object SupabaseConfig {

    // Live Supabase configuration provided by user
    const val DEFAULT_URL = "https://nvzvyzgantmtwjfnrqdo.supabase.co"
    const val DEFAULT_ANON_KEY = "sb_publishable_ifbjck-YJ2Ms2LyxBZbCDA_nJLcCL95"

    private const val PREFS_NAME = "mf_supabase_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_KEY = "custom_supabase_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUrl(context: Context): String {
        val saved = getPrefs(context).getString(KEY_CUSTOM_URL, null)
        return if (!saved.isNullOrBlank() && !saved.contains("your-project-id")) {
            saved.trim().trimEnd('/')
        } else {
            DEFAULT_URL.trim().trimEnd('/')
        }
    }

    fun getKey(context: Context): String {
        val saved = getPrefs(context).getString(KEY_CUSTOM_KEY, null)
        return if (!saved.isNullOrBlank() && !saved.contains("your-anon-publishable-key")) {
            saved.trim()
        } else {
            DEFAULT_ANON_KEY.trim()
        }
    }

    fun saveConfig(context: Context, url: String, anonKey: String) {
        getPrefs(context).edit()
            .putString(KEY_CUSTOM_URL, url.trim().trimEnd('/'))
            .putString(KEY_CUSTOM_KEY, anonKey.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getUrl(context)
        val key = getKey(context)
        return url.isNotBlank() &&
                !url.contains("your-project-id") &&
                key.isNotBlank() &&
                !key.contains("your-anon-publishable-key")
    }
}
