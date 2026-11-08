package com.stopreels.service

import android.content.Context

data class ServiceRuntimeState(
    val heartbeatAtMillis: Long = 0L,
    val lastError: String? = null,
    val activePackage: String? = null,
    val screen: String? = null,
    val targetKind: String? = null,
    val confidence: Int = 0,
    val reason: String? = null,
)

/**
 * Small local runtime status store. It contains only service health and enum
 * names; it never stores accessibility text, usernames, URLs, or media data.
 */
object ServiceRuntimeStatusStore {
    private const val PREFS_NAME = "stopreels_runtime_status"
    private const val HEARTBEAT = "heartbeat_at_millis"
    private const val LAST_ERROR = "last_error"
    private const val ACTIVE_PACKAGE = "active_package"
    private const val SCREEN = "screen"
    private const val TARGET_KIND = "target_kind"
    private const val CONFIDENCE = "confidence"
    private const val REASON = "reason"

    fun read(context: Context): ServiceRuntimeState {
        val values = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ServiceRuntimeState(
            heartbeatAtMillis = values.getLong(HEARTBEAT, 0L),
            lastError = values.getString(LAST_ERROR, null),
            activePackage = values.getString(ACTIVE_PACKAGE, null),
            screen = values.getString(SCREEN, null),
            targetKind = values.getString(TARGET_KIND, null),
            confidence = values.getInt(CONFIDENCE, 0),
            reason = values.getString(REASON, null),
        )
    }

    fun markConnected(context: Context) {
        context.runtimePreferences().edit()
            .putLong(HEARTBEAT, System.currentTimeMillis())
            .remove(LAST_ERROR)
            .apply()
    }

    fun markHeartbeat(context: Context) {
        context.runtimePreferences().edit()
            .putLong(HEARTBEAT, System.currentTimeMillis())
            .apply()
    }

    fun markStopped(context: Context) {
        context.runtimePreferences().edit().putLong(HEARTBEAT, 0L).apply()
    }

    fun recordError(context: Context, error: RuntimeException) {
        context.runtimePreferences().edit()
            .putString(LAST_ERROR, error::class.java.simpleName.take(80))
            .apply()
    }

    fun recordDetection(
        context: Context,
        activePackage: String?,
        screen: String,
        targetKind: String?,
        confidence: Int,
        reason: String,
    ) {
        context.runtimePreferences().edit()
            .putString(ACTIVE_PACKAGE, activePackage)
            .putString(SCREEN, screen)
            .putString(TARGET_KIND, targetKind)
            .putInt(CONFIDENCE, confidence.coerceIn(0, 100))
            .putString(REASON, reason)
            .apply()
    }

    private fun Context.runtimePreferences() =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
