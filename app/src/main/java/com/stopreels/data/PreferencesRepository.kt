package com.stopreels.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.stopReelsDataStore by preferencesDataStore(name = "stopreels_preferences")

data class AppPreferences(
    val hasSeenWelcome: Boolean = false,
    val languageTag: String = "en",
    val blockingEnabled: Boolean = true,
    val blockFeedReels: Boolean = true,
    val blockReelsTab: Boolean = true,
    val blockShortcuts: Boolean = false,
    val temporaryUnlockEnabled: Boolean = true,
    val unlockDurationSeconds: Int = 30,
    val unlockDelaySeconds: Int = 5,
    val blockStories: Boolean = false,
    val blockNormalVideos: Boolean = false,
    val allowPrivateMedia: Boolean = true,
    val diagnosticMode: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 22,
    val scheduleEndHour: Int = 8,
    val dailyLimitMinutes: Int = 0,
    val dailyUnlockedSeconds: Long = 0L,
    val dailyUsageDate: String = "",
    val blockedCount: Int = 0,
    val temporaryUnlockCount: Int = 0,
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val hasSeenWelcome = booleanPreferencesKey("has_seen_welcome")
        val languageTag = stringPreferencesKey("language_tag")
        val blockingEnabled = booleanPreferencesKey("blocking_enabled")
        val blockFeedReels = booleanPreferencesKey("block_feed_reels")
        val blockReelsTab = booleanPreferencesKey("block_reels_tab")
        val blockShortcuts = booleanPreferencesKey("block_shortcuts")
        val temporaryUnlockEnabled = booleanPreferencesKey("temporary_unlock_enabled")
        val unlockDurationSeconds = intPreferencesKey("unlock_duration_seconds")
        val unlockDelaySeconds = intPreferencesKey("unlock_delay_seconds")
        val blockStories = booleanPreferencesKey("block_stories")
        val blockNormalVideos = booleanPreferencesKey("block_normal_videos")
        val allowPrivateMedia = booleanPreferencesKey("allow_private_media")
        val diagnosticMode = booleanPreferencesKey("diagnostic_mode")
        val scheduleEnabled = booleanPreferencesKey("schedule_enabled")
        val scheduleStartHour = intPreferencesKey("schedule_start_hour")
        val scheduleEndHour = intPreferencesKey("schedule_end_hour")
        val dailyLimitMinutes = intPreferencesKey("daily_limit_minutes")
        val dailyUnlockedSeconds = longPreferencesKey("daily_unlocked_seconds")
        val dailyUsageDate = stringPreferencesKey("daily_usage_date")
        val blockedCount = intPreferencesKey("blocked_count")
        val temporaryUnlockCount = intPreferencesKey("temporary_unlock_count")
    }

    val preferences: Flow<AppPreferences> = context.stopReelsDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { values ->
            AppPreferences(
                hasSeenWelcome = values[Keys.hasSeenWelcome] ?: false,
                languageTag = values[Keys.languageTag] ?: "en",
                blockingEnabled = values[Keys.blockingEnabled] ?: true,
                blockFeedReels = values[Keys.blockFeedReels] ?: true,
                blockReelsTab = values[Keys.blockReelsTab] ?: true,
                blockShortcuts = values[Keys.blockShortcuts] ?: false,
                temporaryUnlockEnabled = values[Keys.temporaryUnlockEnabled] ?: true,
                unlockDurationSeconds = values[Keys.unlockDurationSeconds] ?: 30,
                unlockDelaySeconds = (values[Keys.unlockDelaySeconds] ?: 5).coerceIn(1, 30),
                blockStories = values[Keys.blockStories] ?: false,
                blockNormalVideos = values[Keys.blockNormalVideos] ?: false,
                allowPrivateMedia = values[Keys.allowPrivateMedia] ?: true,
                diagnosticMode = values[Keys.diagnosticMode] ?: false,
                scheduleEnabled = values[Keys.scheduleEnabled] ?: false,
                scheduleStartHour = (values[Keys.scheduleStartHour] ?: 22).coerceIn(0, 23),
                scheduleEndHour = (values[Keys.scheduleEndHour] ?: 8).coerceIn(0, 23),
                dailyLimitMinutes = (values[Keys.dailyLimitMinutes] ?: 0).coerceIn(0, 60),
                dailyUnlockedSeconds = values[Keys.dailyUnlockedSeconds] ?: 0L,
                dailyUsageDate = values[Keys.dailyUsageDate] ?: "",
                blockedCount = values[Keys.blockedCount] ?: 0,
                temporaryUnlockCount = values[Keys.temporaryUnlockCount] ?: 0,
            )
        }

    suspend fun markWelcomeSeen() = context.stopReelsDataStore.edit { it[Keys.hasSeenWelcome] = true }

    suspend fun setLanguageTag(tag: String) {
        context.getSharedPreferences("stopreels_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("language_tag", tag)
            .apply()
        context.stopReelsDataStore.edit { it[Keys.languageTag] = tag }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockingEnabled] = enabled
    }

    suspend fun setBlockFeedReels(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockFeedReels] = enabled
    }

    suspend fun setBlockReelsTab(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockReelsTab] = enabled
    }

    suspend fun setBlockShortcuts(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockShortcuts] = enabled
    }

    suspend fun setTemporaryUnlockEnabled(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.temporaryUnlockEnabled] = enabled
    }

    suspend fun setUnlockDurationSeconds(seconds: Int) = context.stopReelsDataStore.edit {
        it[Keys.unlockDurationSeconds] = seconds.coerceIn(15, 60)
    }

    suspend fun setUnlockDelaySeconds(seconds: Int) = context.stopReelsDataStore.edit {
        it[Keys.unlockDelaySeconds] = seconds.coerceIn(1, 30)
    }

    suspend fun setBlockStories(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockStories] = enabled
    }

    suspend fun setBlockNormalVideos(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.blockNormalVideos] = enabled
    }

    suspend fun setAllowPrivateMedia(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.allowPrivateMedia] = enabled
    }

    suspend fun setDiagnosticMode(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.diagnosticMode] = enabled
    }

    suspend fun setScheduleEnabled(enabled: Boolean) = context.stopReelsDataStore.edit {
        it[Keys.scheduleEnabled] = enabled
    }

    suspend fun setScheduleStartHour(hour: Int) = context.stopReelsDataStore.edit {
        it[Keys.scheduleStartHour] = hour.coerceIn(0, 23)
    }

    suspend fun setScheduleEndHour(hour: Int) = context.stopReelsDataStore.edit {
        it[Keys.scheduleEndHour] = hour.coerceIn(0, 23)
    }

    suspend fun setDailyLimitMinutes(minutes: Int) = context.stopReelsDataStore.edit {
        it[Keys.dailyLimitMinutes] = minutes.coerceIn(0, 60)
    }

    suspend fun addDailyUnlockedSeconds(seconds: Long, date: String) = context.stopReelsDataStore.edit {
        val previousDate = it[Keys.dailyUsageDate]
        if (previousDate != date) {
            it[Keys.dailyUsageDate] = date
            it[Keys.dailyUnlockedSeconds] = seconds.coerceAtLeast(0L)
        } else {
            it[Keys.dailyUnlockedSeconds] = (it[Keys.dailyUnlockedSeconds] ?: 0L) + seconds
        }
    }

    suspend fun incrementBlockedCount() = context.stopReelsDataStore.edit {
        it[Keys.blockedCount] = (it[Keys.blockedCount] ?: 0) + 1
    }

    suspend fun incrementTemporaryUnlockCount() = context.stopReelsDataStore.edit {
        it[Keys.temporaryUnlockCount] = (it[Keys.temporaryUnlockCount] ?: 0) + 1
    }

    suspend fun resetStatistics() = context.stopReelsDataStore.edit {
        it[Keys.blockedCount] = 0
        it[Keys.temporaryUnlockCount] = 0
    }
}
