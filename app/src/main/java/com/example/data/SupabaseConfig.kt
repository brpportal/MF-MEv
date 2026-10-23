package com.example.data

import android.content.Context

/**
 * =============================================================================
 * 🔑 SUPABASE PRODUCTION CONFIGURATION
 * =============================================================================
 * File: /app/src/main/java/com/example/data/SupabaseConfig.kt
 * Hardcoded to production Supabase project and key. No user-side configuration exposed.
 * =============================================================================
 */
object SupabaseConfig {

    const val DEFAULT_URL = "https://nvzvyzgantmtwjfnrqdo.supabase.co"
    const val DEFAULT_ANON_KEY = "sb_publishable_ifbjck-YJ2Ms2LyxBZbCDA_nJLcCL95"

    fun getUrl(context: Context? = null): String {
        return DEFAULT_URL.trim().trimEnd('/')
    }

    fun getKey(context: Context? = null): String {
        return DEFAULT_ANON_KEY.trim()
    }

    fun isConfigured(context: Context? = null): Boolean {
        return true
    }
}
