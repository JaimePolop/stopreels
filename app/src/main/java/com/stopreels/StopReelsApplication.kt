package com.stopreels

import android.app.Application
import com.stopreels.data.PreferencesRepository

class StopReelsApplication : Application() {
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(this)
    }
}
