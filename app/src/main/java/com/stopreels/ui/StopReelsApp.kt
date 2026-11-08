package com.stopreels.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stopreels.R
import com.stopreels.StopReelsViewModel
import com.stopreels.data.AppPreferences
import com.stopreels.data.DailyStat
import com.stopreels.service.StopReelsAccessibilityService
import kotlinx.coroutines.delay
import java.time.LocalDate

private enum class AppSection(val labelRes: Int) {
    HOME(R.string.home),
    SETTINGS(R.string.settings),
    HELP(R.string.help),
}

private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
    "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StopReelsApp(viewModel: StopReelsViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var accessibilityEnabled by rememberAccessibilityEnabled(context)
    var section by rememberSaveable { mutableStateOf(AppSection.HOME) }
    val xiaomiFamily = remember { isXiaomiFamily() }
    var showStartup by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(650L)
        showStartup = false
    }

    if (showStartup) {
        StartupScreen()
        return
    }

    if (!preferences.hasSeenWelcome) {
        WelcomeScreen(
            onOpenAccessibility = {
                context.openAccessibilitySettings()
            },
            onContinue = viewModel::markWelcomeSeen,
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(section.labelRes)) },
                actions = {
                    IconButton(onClick = {
                        context.openAccessibilitySettings()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.AccessibilityNew,
                            contentDescription = stringResource(R.string.accessibility_settings),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                AppSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppSection.HOME -> Icons.Outlined.Home
                                    AppSection.SETTINGS -> Icons.Outlined.Settings
                                    AppSection.HELP -> Icons.Outlined.HelpOutline
                                },
                                contentDescription = stringResource(item.labelRes),
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        AnimatedContent(
            targetState = section,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 8 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { -it / 8 })
            },
            label = "app_section_transition",
        ) { activeSection ->
            when (activeSection) {
                AppSection.HOME -> HomeScreen(
                    modifier = Modifier.padding(paddingValues),
                    preferences = preferences,
                    accessibilityEnabled = accessibilityEnabled,
                    xiaomiFamily = xiaomiFamily,
                    onBlockingChanged = viewModel::setBlockingEnabled,
                    onOpenAccessibility = {
                        context.openAccessibilitySettings()
                    },
                )

                AppSection.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(paddingValues),
                    preferences = preferences,
                    onBlockingChanged = viewModel::setBlockingEnabled,
                    onFeedReelsChanged = viewModel::setBlockFeedReels,
                    onReelsTabChanged = viewModel::setBlockReelsTab,
                    onShortcutsChanged = viewModel::setBlockShortcuts,
                    onTemporaryChanged = viewModel::setTemporaryUnlockEnabled,
                    onStrictModeChanged = viewModel::setStrictMode,
                    onDurationChanged = viewModel::setUnlockDurationSeconds,
                    onUnlockDelayChanged = viewModel::setUnlockDelaySeconds,
                    onStoriesChanged = viewModel::setBlockStories,
                    onNormalVideosChanged = viewModel::setBlockNormalVideos,
                    onDiagnosticChanged = viewModel::setDiagnosticMode,
                    onScheduleChanged = viewModel::setScheduleEnabled,
                    onScheduleStartChanged = viewModel::setScheduleStartHour,
                    onScheduleEndChanged = viewModel::setScheduleEndHour,
                    onDailyLimitChanged = viewModel::setDailyLimitMinutes,
                    languageTag = preferences.languageTag,
                    onLanguageChanged = { tag ->
                        viewModel.setLanguageTag(tag)
                        (context as? android.app.Activity)?.recreate()
                    },
                    onResetStatistics = viewModel::resetStatistics,
                    onOpenAccessibility = {
                        context.openAccessibilitySettings()
                    },
                )

                AppSection.HELP -> HelpScreen(
                    modifier = Modifier.padding(paddingValues),
                    xiaomiFamily = xiaomiFamily,
                )
            }
        }
    }
}

@Composable
private fun StartupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.startup_body), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun WelcomeScreen(onOpenAccessibility: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        InfoCard(
            title = stringResource(R.string.what_service_reads_title),
            body = stringResource(R.string.what_service_reads_body),
            icon = Icons.Outlined.AccessibilityNew,
        )
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = stringResource(R.string.privacy_title),
            body = stringResource(R.string.privacy_short),
            icon = Icons.Outlined.Lock,
        )
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = stringResource(R.string.permission_info_title),
            body = stringResource(R.string.permission_info_body),
            icon = Icons.Outlined.AccessibilityNew,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onOpenAccessibility,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.enable_accessibility))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.continue_without_service))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    preferences: AppPreferences,
    accessibilityEnabled: Boolean,
    xiaomiFamily: Boolean,
    onBlockingChanged: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge)
        ProtectionHeroCard(accessibilityEnabled = accessibilityEnabled, blockingEnabled = preferences.blockingEnabled)
        ReelsWarningCard()
        StatisticsCard(preferences.dailyStats)
        ServiceStatusCard(accessibilityEnabled, onOpenAccessibility)
        SetupChecklistCard(
            accessibilityEnabled = accessibilityEnabled,
            xiaomiFamily = xiaomiFamily,
            onOpenAccessibility = onOpenAccessibility,
        )
        BackgroundSetupCard(xiaomiFamily)
        ProtectedBlockingSwitch(
            checked = preferences.blockingEnabled,
            onCheckedChange = onBlockingChanged,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Lock,
                value = preferences.blockedCount,
                label = stringResource(R.string.blocked_count),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
                value = preferences.temporaryUnlockCount,
                label = stringResource(R.string.unlocked_count),
            )
        }
    }
}

@Composable
private fun ProtectionHeroCard(accessibilityEnabled: Boolean, blockingEnabled: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(
                        if (blockingEnabled) R.string.protection_active else R.string.protection_paused,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (accessibilityEnabled) R.string.protection_ready else R.string.protection_needs_accessibility,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReelsWarningCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val references = listOf(
        R.string.reels_reference_surgeon_general to
            "https://www.hhs.gov/surgeongeneral/reports-and-publications/youth-mental-health/social-media/index.html",
        R.string.reels_reference_apa to
            "https://www.apa.org/topics/social-media-internet/health-advisory-adolescent-social-media-use",
        R.string.reels_reference_attention to
            "https://pubmed.ncbi.nlm.nih.gov/38993329/",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reels_warning_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        stringResource(
                            if (expanded) R.string.reels_warning_hide else R.string.reels_warning_show,
                        ),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Text(
                        text = stringResource(R.string.reels_warning_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.reels_references_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    references.forEach { (labelRes, url) ->
                        TextButton(
                            onClick = { context.openReferenceUrl(url) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(labelRes), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupChecklistCard(
    accessibilityEnabled: Boolean,
    xiaomiFamily: Boolean,
    onOpenAccessibility: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.setup_body), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            SetupInstructionRow("1", stringResource(R.string.setup_accessibility_step))
            SetupInstructionRow("2", stringResource(R.string.setup_app_step))
            if (xiaomiFamily) {
                SetupInstructionRow("3", stringResource(R.string.setup_xiaomi_step))
            }
            Spacer(Modifier.height(10.dp))
            if (accessibilityEnabled) {
                Text(
                    text = stringResource(R.string.setup_accessibility_done),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Button(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.enable_accessibility))
                }
            }
        }
    }
}

@Composable
private fun SetupInstructionRow(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("$number.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProtectedBlockingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var showDisableDialog by rememberSaveable { mutableStateOf(false) }
    var secondsRemaining by rememberSaveable { mutableStateOf(10) }

    SettingSwitchRow(
        title = stringResource(R.string.block_reels),
        summary = stringResource(R.string.block_reels_summary),
        checked = checked,
        onCheckedChange = { enabled ->
            if (enabled) {
                onCheckedChange(true)
            } else {
                secondsRemaining = 10
                showDisableDialog = true
            }
        },
    )

    if (showDisableDialog) {
        LaunchedEffect(Unit) {
            while (secondsRemaining > 0) {
                delay(1_000L)
                secondsRemaining -= 1
            }
        }
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text(stringResource(R.string.disable_blocking_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.disable_blocking_body,
                        secondsRemaining,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = secondsRemaining == 0,
                    onClick = {
                        onCheckedChange(false)
                        showDisableDialog = false
                    },
                ) {
                    Text(stringResource(R.string.disable_blocking_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun StatisticsCard(stats: List<DailyStat>) {
    val today = LocalDate.now()
    val lastSevenDays = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        stats.firstOrNull { it.date == date.toString() } ?: DailyStat(date.toString())
    }
    val weekBlocked = lastSevenDays.sumOf { it.blocked }
    val weekSavedSeconds = lastSevenDays.sumOf { it.savedSeconds }
    val streak = protectedStreak(stats, today)
    val maximum = lastSevenDays.maxOfOrNull { it.blocked }?.coerceAtLeast(1) ?: 1
    val chartDescription = stringResource(R.string.statistics_chart_description)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatisticMetric(
                    modifier = Modifier.weight(1f),
                    value = weekBlocked.toString(),
                    label = stringResource(R.string.statistics_reels_blocked),
                )
                StatisticMetric(
                    modifier = Modifier.weight(1f),
                    value = formatSavedTime(weekSavedSeconds),
                    label = stringResource(R.string.statistics_time_saved),
                )
                StatisticMetric(
                    modifier = Modifier.weight(1f),
                    value = streak.toString(),
                    label = stringResource(R.string.statistics_streak),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = chartDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                lastSevenDays.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((20 + (day.blocked.toFloat() / maximum * 56)).dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp),
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(day.date.takeLast(2), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticMetric(modifier: Modifier, value: String, label: String) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun protectedStreak(stats: List<DailyStat>, today: LocalDate): Int {
    val byDate = stats.associateBy { it.date }
    var date = today
    var streak = 0
    while ((byDate[date.toString()]?.blocked ?: 0) > 0) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

private fun formatSavedTime(seconds: Long): String {
    val minutes = seconds / 60
    return if (minutes > 0) "${minutes}m" else "${seconds}s"
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    preferences: AppPreferences,
    onBlockingChanged: (Boolean) -> Unit,
    onFeedReelsChanged: (Boolean) -> Unit,
    onReelsTabChanged: (Boolean) -> Unit,
    onShortcutsChanged: (Boolean) -> Unit,
    onTemporaryChanged: (Boolean) -> Unit,
    onStrictModeChanged: (Boolean) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onUnlockDelayChanged: (Int) -> Unit,
    onStoriesChanged: (Boolean) -> Unit,
    onNormalVideosChanged: (Boolean) -> Unit,
    onDiagnosticChanged: (Boolean) -> Unit,
    onScheduleChanged: (Boolean) -> Unit,
    onScheduleStartChanged: (Int) -> Unit,
    onScheduleEndChanged: (Int) -> Unit,
    onDailyLimitChanged: (Int) -> Unit,
    languageTag: String,
    onLanguageChanged: (String) -> Unit,
    onResetStatistics: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var resetWasClicked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        LanguageSelector(languageTag = languageTag, onLanguageChanged = onLanguageChanged)
        ProtectedBlockingSwitch(
            checked = preferences.blockingEnabled,
            onCheckedChange = onBlockingChanged,
        )
        SettingSwitchRow(stringResource(R.string.feed_reels_title), stringResource(R.string.feed_reels_summary), preferences.blockFeedReels, onFeedReelsChanged)
        SettingSwitchRow(stringResource(R.string.reels_tab_title), stringResource(R.string.reels_tab_summary), preferences.blockReelsTab, onReelsTabChanged)
        SettingSwitchRow(stringResource(R.string.shortcuts_title), stringResource(R.string.shortcuts_summary), preferences.blockShortcuts, onShortcutsChanged)
        SettingSwitchRow(stringResource(R.string.temporary_title), stringResource(R.string.temporary_summary), preferences.temporaryUnlockEnabled, onTemporaryChanged)
        SettingSwitchRow(stringResource(R.string.strict_mode_title), stringResource(R.string.strict_mode_summary), preferences.strictMode, onStrictModeChanged)
        SettingSwitchRow(stringResource(R.string.stories_title), stringResource(R.string.stories_summary), preferences.blockStories, onStoriesChanged)
        SettingSwitchRow(stringResource(R.string.normal_videos_title), stringResource(R.string.normal_videos_summary), preferences.blockNormalVideos, onNormalVideosChanged)
        SettingSwitchRow(stringResource(R.string.diagnostic_title), stringResource(R.string.diagnostic_summary), preferences.diagnosticMode, onDiagnosticChanged)
        SettingSwitchRow(stringResource(R.string.schedule_title), stringResource(R.string.schedule_summary), preferences.scheduleEnabled, onScheduleChanged)
        if (preferences.scheduleEnabled) {
            HourSlider(stringResource(R.string.schedule_start), preferences.scheduleStartHour, onScheduleStartChanged)
            HourSlider(stringResource(R.string.schedule_end), preferences.scheduleEndHour, onScheduleEndChanged)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.duration_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(15, 30, 60).forEachIndexed { index, seconds ->
                SegmentedButton(
                    selected = preferences.unlockDurationSeconds == seconds,
                    onClick = { onDurationChanged(seconds) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 3),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.seconds, seconds))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.unlock_delay_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.unlock_delay_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(3, 5, 10, 15).forEachIndexed { index, seconds ->
                SegmentedButton(
                    selected = preferences.unlockDelaySeconds == seconds,
                    onClick = { onUnlockDelayChanged(seconds) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 4),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.seconds, seconds))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.daily_limit_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.daily_limit_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(0, 5, 10, 20, 30).forEachIndexed { index, minutes ->
                SegmentedButton(
                    selected = preferences.dailyLimitMinutes == minutes,
                    onClick = { onDailyLimitChanged(minutes) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 5),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (minutes == 0) stringResource(R.string.off) else "$minutes")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AccessibilityNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.enable_accessibility))
        }
        OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_statistics))
        }
        if (resetWasClicked) {
            Text(
                text = stringResource(R.string.statistics_reset),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_statistics)) },
            text = { Text(stringResource(R.string.reset_statistics_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetStatistics()
                    resetWasClicked = true
                    showResetDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private data class LanguageOption(val tag: String, val label: String)

@Composable
private fun LanguageSelector(languageTag: String, onLanguageChanged: (String) -> Unit) {
    val languages = listOf(
        LanguageOption("en", "English"),
        LanguageOption("es", "Español"),
        LanguageOption("fr", "Français"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("it", "Italiano"),
        LanguageOption("pt-BR", "Português (Brasil)"),
        LanguageOption("ca", "Català"),
    )
    var expanded by remember { mutableStateOf(false) }
    val current = languages.firstOrNull { it.tag == languageTag } ?: languages.first()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.language_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(current.label)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onLanguageChanged(option.tag)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HourSlider(label: String, hour: Int, onHourChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hour_value, label, hour), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = hour.toFloat(),
            onValueChange = { onHourChanged(it.toInt()) },
            valueRange = 0f..23f,
            steps = 22,
        )
    }
}

@Composable
private fun HelpScreen(modifier: Modifier, xiaomiFamily: Boolean) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.help_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoCard(stringResource(R.string.how_it_works_title), stringResource(R.string.how_it_works_body), Icons.Outlined.AccessibilityNew)
        InfoCard(stringResource(R.string.permission_info_title), stringResource(R.string.permission_info_body), Icons.Outlined.Lock)
        InfoCard(
            title = stringResource(if (xiaomiFamily) R.string.xiaomi_background_title else R.string.background_title),
            body = stringResource(if (xiaomiFamily) R.string.xiaomi_background_body else R.string.background_generic_body),
            icon = Icons.Outlined.Settings,
        )
        InfoCard(stringResource(R.string.direct_title), stringResource(R.string.direct_body), Icons.Outlined.CheckCircle)
        InfoCard(stringResource(R.string.limitations_title), stringResource(R.string.limitations_body), Icons.Outlined.HelpOutline)
        InfoCard(stringResource(R.string.disable_title), stringResource(R.string.disable_body), Icons.Outlined.Settings)
        InfoCard(stringResource(R.string.privacy_title), stringResource(R.string.privacy_body), Icons.Outlined.Shield)
        InfoCard(stringResource(R.string.copyright_title), stringResource(R.string.copyright_body), Icons.Outlined.Shield)
    }
}

@Composable
private fun ServiceStatusCard(accessibilityEnabled: Boolean, onOpenAccessibility: () -> Unit) {
    val statusColor by animateColorAsState(
        targetValue = if (accessibilityEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "service_status_color",
    )
    Card(colors = CardDefaults.cardColors(containerColor = statusColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = accessibilityEnabled,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "service_status_icon",
            ) { enabled ->
                Icon(
                    imageVector = if (enabled) Icons.Outlined.CheckCircle else Icons.Outlined.AccessibilityNew,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (accessibilityEnabled) R.string.service_enabled else R.string.service_disabled),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(if (accessibilityEnabled) R.string.service_enabled_description else R.string.service_disabled_description),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!accessibilityEnabled) {
                TextButton(onClick = onOpenAccessibility) { Text(stringResource(R.string.enable_accessibility)) }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title. $summary" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int, label: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(10.dp))
            AnimatedContent(targetState = value, label = "statistic_value") { displayedValue ->
                Text(displayedValue.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BackgroundSetupCard(xiaomiFamily: Boolean) {
    val context = LocalContext.current
    val detailsIntent = remember(context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }
    val batteryIntent = remember {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(if (xiaomiFamily) R.string.xiaomi_background_title else R.string.background_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (xiaomiFamily) R.string.xiaomi_background_body else R.string.background_generic_body,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { context.startSettingsIntent(detailsIntent) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.open_app_settings))
                }
                OutlinedButton(onClick = { context.startSettingsIntent(batteryIntent, detailsIntent) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.open_battery_settings))
                }
            }
        }
    }
}

@Composable
private fun rememberAccessibilityEnabled(context: Context): MutableState<Boolean> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled = remember { mutableStateOf(isStopReelsAccessibilityEnabled(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled.value = isStopReelsAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { enabled.value = isStopReelsAccessibilityEnabled(context) }
    return enabled
}

private fun isStopReelsAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
    return manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any { info ->
            info.resolveInfo?.serviceInfo?.packageName == context.packageName &&
                info.resolveInfo?.serviceInfo?.name == StopReelsAccessibilityService::class.java.name
        } == true
}

private fun isXiaomiFamily(): Boolean {
    val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
    val brand = Build.BRAND.orEmpty().lowercase()
    val model = Build.MODEL.orEmpty().lowercase()
    return manufacturer.contains("xiaomi") ||
        manufacturer.contains("redmi") ||
        manufacturer.contains("poco") ||
        brand.contains("xiaomi") ||
        brand.contains("redmi") ||
        brand.contains("poco") ||
        model.contains("x8 pro")
}

private fun Context.startSettingsIntent(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        fallback?.let { fallbackIntent -> startActivity(fallbackIntent) }
    }
}

private fun Context.openReferenceUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // The app remains usable even if this device has no browser installed.
    }
}

private fun Context.openAccessibilitySettings() {
    val serviceComponent = ComponentName(this, StopReelsAccessibilityService::class.java)
    val detailIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
        putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
    }
    try {
        startActivity(detailIntent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (_: SecurityException) {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
