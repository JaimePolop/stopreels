package com.stopreels

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.stopreels.ui.StopReelsApp
import com.stopreels.ui.theme.StopReelsTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: StopReelsViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val languageTag = newBase
            .getSharedPreferences("stopreels_settings", Context.MODE_PRIVATE)
            .getString("language_tag", "en")
            .orEmpty()
            .ifBlank { "en" }
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StopReelsTheme {
                StopReelsApp(viewModel = viewModel)
            }
        }
    }
}
