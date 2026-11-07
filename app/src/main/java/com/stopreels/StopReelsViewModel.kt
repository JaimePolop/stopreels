package com.stopreels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stopreels.data.AppPreferences
import com.stopreels.data.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StopReelsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PreferencesRepository =
        (application as StopReelsApplication).preferencesRepository

    val preferences: StateFlow<AppPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences(),
    )

    fun markWelcomeSeen() {
        viewModelScope.launch { repository.markWelcomeSeen() }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch { repository.setLanguageTag(tag) }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockingEnabled(enabled) }
    }

    fun setBlockFeedReels(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockFeedReels(enabled) }
    }

    fun setBlockReelsTab(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockReelsTab(enabled) }
    }

    fun setBlockShortcuts(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockShortcuts(enabled) }
    }

    fun setTemporaryUnlockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTemporaryUnlockEnabled(enabled) }
    }

    fun setUnlockDurationSeconds(seconds: Int) {
        viewModelScope.launch { repository.setUnlockDurationSeconds(seconds) }
    }

    fun setBlockStories(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockStories(enabled) }
    }

    fun setBlockNormalVideos(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockNormalVideos(enabled) }
    }

    fun setAllowPrivateMedia(enabled: Boolean) {
        viewModelScope.launch { repository.setAllowPrivateMedia(enabled) }
    }

    fun setDiagnosticMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDiagnosticMode(enabled) }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setScheduleEnabled(enabled) }
    }

    fun setScheduleStartHour(hour: Int) {
        viewModelScope.launch { repository.setScheduleStartHour(hour) }
    }

    fun setScheduleEndHour(hour: Int) {
        viewModelScope.launch { repository.setScheduleEndHour(hour) }
    }

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDailyLimitMinutes(minutes) }
    }

    fun resetStatistics() {
        viewModelScope.launch { repository.resetStatistics() }
    }
}
