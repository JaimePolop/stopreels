package com.stopreels.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.stopreels.R
import com.stopreels.StopReelsApplication
import com.stopreels.data.AppPreferences
import com.stopreels.domain.AccessibilityNodeSnapshot
import com.stopreels.domain.BlockTarget
import com.stopreels.domain.BlockTargetKind
import com.stopreels.domain.DetectionPreferences
import com.stopreels.domain.INSTAGRAM_PACKAGE
import com.stopreels.domain.ReelDetectionEngine
import com.stopreels.domain.ScreenRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Local-only Instagram filter.
 *
 * The service reads the active accessibility tree only while Instagram is foregrounded.
 * It does not request screenshots, key events, gestures, or network permissions.
 */
class StopReelsAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var preferences = AppPreferences()
    private var lastPackage: String? = null
    private var lastOverlayKey: String? = null
    private var lastCountedKey: String? = null
    private var unlockedUntilUptime: Long = 0L
    private var currentOverlays = mutableListOf<Pair<View, WindowManager.LayoutParams>>()
    private var nodeBudget = 0
    private var lastPositiveDetectionUptime: Long = 0L

    private companion object {
        const val DETECTION_GRACE_MS = 3_000L
        const val TAG = "StopReelsService"
    }

    private val processRunnable = Runnable { processActiveWindow() }
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (lastPackage == INSTAGRAM_PACKAGE) {
                processActiveWindow()
                mainHandler.postDelayed(this, 700L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 120
            flags = flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceScope.launch {
            (application as StopReelsApplication).preferencesRepository.preferences.collectLatest {
                preferences = it
                if (!it.blockingEnabled) clearOverlays()
                else scheduleProcess()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
        // The blocking overlay belongs to StopReels and can emit its own
        // accessibility events. Those events must not be treated as leaving Instagram.
        if (packageName == application.packageName) return
        if (packageName != INSTAGRAM_PACKAGE) {
            // System UI, the keyboard, and navigation controls can emit events while
            // Instagram remains the active app. The heartbeat verifies the real root
            // window instead of tearing down the Reel block on every such event.
            return
        }

        if (lastPackage != packageName) {
            clearOverlays()
            lastCountedKey = null
        }
        // Instagram can switch between several internal windows while opening
        // the Reel viewer. Do not remove a valid cover merely because the
        // accessibility window id changed; processActiveWindow verifies the
        // package and the detector decides when the cover is no longer needed.
        lastPackage = packageName
        scheduleProcess()
        mainHandler.removeCallbacks(heartbeatRunnable)
        mainHandler.postDelayed(heartbeatRunnable, 700L)
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(heartbeatRunnable)
        clearOverlays()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(processRunnable)
        mainHandler.removeCallbacks(heartbeatRunnable)
        clearOverlays()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun scheduleProcess() {
        mainHandler.removeCallbacks(processRunnable)
        mainHandler.postDelayed(processRunnable, 120L)
    }

    private fun processActiveWindow() {
        try {
            processActiveWindowSafely()
        } catch (error: RuntimeException) {
            // A broken node or OEM WindowManager transition must not kill the
            // accessibility service. The next heartbeat will retry detection.
            Log.e(TAG, "Recoverable error while processing Instagram", error)
            clearOverlays()
        }
    }

    private fun processActiveWindowSafely() {
        if (!::windowManager.isInitialized || lastPackage != INSTAGRAM_PACKAGE) return
        if (!preferences.blockingEnabled || SystemClock.uptimeMillis() < unlockedUntilUptime) {
            clearOverlays()
            return
        }
        if (!isWithinSchedule()) {
            clearOverlays()
            return
        }

        val root = rootInActiveWindow ?: run {
            clearIfDetectionExpired()
            return
        }
        val rootPackage = root.packageName?.toString()
        if (rootPackage != INSTAGRAM_PACKAGE) {
            if (rootPackage != application.packageName) {
                clearOverlays()
                lastPackage = rootPackage
                mainHandler.removeCallbacks(heartbeatRunnable)
            }
            return
        }
        nodeBudget = 0
        val snapshot = try {
            snapshotOf(root, depth = 0)
        } catch (_: RuntimeException) {
            clearIfDetectionExpired()
            return
        }

        val result = ReelDetectionEngine.detect(
            snapshot,
            DetectionPreferences(
                blockingEnabled = preferences.blockingEnabled,
                blockFeedReels = preferences.blockFeedReels,
                blockReelsTab = preferences.blockReelsTab,
                blockShortcuts = preferences.blockShortcuts,
                blockStories = preferences.blockStories,
                blockNormalVideos = preferences.blockNormalVideos,
                diagnosticMode = preferences.diagnosticMode,
            ),
        )

        if (!result.shouldBlock) {
            // A Direct/chat result is authoritative. Keeping the old overlay for
            // the normal anti-flicker grace period would briefly cover a Reel or
            // photo that a friend just sent in a conversation.
            if (result.isPrivateContext) clearOverlays() else clearIfDetectionExpired()
            return
        }

        lastPositiveDetectionUptime = SystemClock.uptimeMillis()
        val newKey = result.stableKey
        // A vendor can detach an accessibility overlay during a window
        // transition without sending us a matching event. Only reuse the key
        // while a real attached view still exists; otherwise recreate it.
        if (newKey == lastOverlayKey && currentOverlays.isNotEmpty() && currentOverlays.all { it.first.isAttachedToWindow }) {
            return
        }
        clearOverlays()
        result.targets.forEach { target -> addBlockingOverlay(target, preferences) }
        lastOverlayKey = newKey

        if (newKey != lastCountedKey && result.targets.any { it.kind != BlockTargetKind.STORY }) {
            lastCountedKey = newKey
            serviceScope.launch {
                (application as StopReelsApplication).preferencesRepository.incrementBlockedCount()
            }
        }
    }

    private fun addBlockingOverlay(target: BlockTarget, currentPreferences: AppPreferences) {
        val view = BlockOverlayView(
            context = localizedOverlayContext(),
            target = target,
            durationSeconds = currentPreferences.unlockDurationSeconds,
            delaySeconds = currentPreferences.unlockDelaySeconds,
            canUnlock = currentPreferences.temporaryUnlockEnabled && !currentPreferences.strictMode,
            diagnosticMode = currentPreferences.diagnosticMode,
            onUnlock = { requestTemporaryUnlock(currentPreferences.unlockDurationSeconds) },
            onExit = { performGlobalAction(GLOBAL_ACTION_HOME) },
        )
        val screen = overlayBounds(target.bounds)
        val params = WindowManager.LayoutParams(
            screen.width.coerceAtLeast(if (target.kind.name == "SHORTCUT") 96 else 180),
            screen.height.coerceAtLeast(if (target.kind.name == "SHORTCUT") 72 else 120),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screen.left
            y = screen.top
        }
        try {
            windowManager.addView(view, params)
            currentOverlays += view to params
        } catch (_: WindowManager.BadTokenException) {
            // The accessibility service may be stopping while the event is delivered.
        } catch (_: IllegalStateException) {
            // Avoid bringing down the service if Android invalidates a window during rotation.
        } catch (_: SecurityException) {
            // Some OEM builds can revoke overlay access while the service is being disabled.
        }
    }

    private fun localizedOverlayContext(): Context {
        val languageTag = preferences.languageTag.ifBlank { "en" }
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return createConfigurationContext(configuration)
    }

    private fun overlayBounds(bounds: ScreenRect): ScreenRect {
        val displayHeight = resources.displayMetrics.heightPixels
        val isFullScreenTarget = bounds.height >= (displayHeight * 0.8f).toInt()
        if (!isFullScreenTarget) return bounds

        val statusBarHeight = systemBarHeight("status_bar_height")
        val navigationBarHeight = systemBarHeight("navigation_bar_height")
        return ScreenRect(
            left = bounds.left,
            top = (bounds.top + statusBarHeight).coerceAtMost(bounds.bottom - 1),
            right = bounds.right,
            bottom = (bounds.bottom - navigationBarHeight).coerceAtLeast(bounds.top + statusBarHeight + 1),
        )
    }

    private fun systemBarHeight(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId == 0) 0 else resources.getDimensionPixelSize(resourceId)
    }

    private fun requestTemporaryUnlock(durationSeconds: Int) {
        if (!preferences.temporaryUnlockEnabled || preferences.strictMode) return
        val today = LocalDate.now().toString()
        val usedToday = if (preferences.dailyUsageDate == today) preferences.dailyUnlockedSeconds else 0L
        val dailyLimitSeconds = preferences.dailyLimitMinutes * 60L
        val availableSeconds = if (dailyLimitSeconds <= 0L) {
            durationSeconds.toLong()
        } else {
            (dailyLimitSeconds - usedToday).coerceAtLeast(0L).coerceAtMost(durationSeconds.toLong())
        }
        if (availableSeconds <= 0L) return
        unlockedUntilUptime = SystemClock.uptimeMillis() + availableSeconds * 1_000L
        serviceScope.launch {
            (application as StopReelsApplication).preferencesRepository.incrementTemporaryUnlockCount()
            if (preferences.dailyLimitMinutes > 0) {
                (application as StopReelsApplication).preferencesRepository.addDailyUnlockedSeconds(
                    seconds = availableSeconds,
                    date = today,
                )
            }
        }
        clearOverlays()
        mainHandler.removeCallbacks(processRunnable)
        mainHandler.postDelayed(processRunnable, availableSeconds * 1_000L)
    }

    private fun isWithinSchedule(): Boolean {
        if (!preferences.scheduleEnabled) return true
        val currentHour = LocalTime.now().hour
        val start = preferences.scheduleStartHour
        val end = preferences.scheduleEndHour
        if (start == end) return true
        return if (start < end) {
            currentHour in start until end
        } else {
            currentHour >= start || currentHour < end
        }
    }

    private fun clearOverlays() {
        if (!::windowManager.isInitialized) return
        currentOverlays.forEach { (view, _) ->
            try {
                windowManager.removeView(view)
            } catch (_: RuntimeException) {
                // The view may already be detached by Android or an OEM during
                // rotation/window transitions. Never let that kill the service.
            }
        }
        currentOverlays.clear()
        lastOverlayKey = null
    }

    private fun clearIfDetectionExpired() {
        val now = SystemClock.uptimeMillis()
        if (currentOverlays.isNotEmpty() && now - lastPositiveDetectionUptime < DETECTION_GRACE_MS) return
        clearOverlays()
    }

    private fun snapshotOf(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeSnapshot {
        nodeBudget++
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val children = if (depth < 18 && nodeBudget < 2_000) {
            buildList {
                for (index in 0 until node.childCount) {
                    val child = node.getChild(index) ?: continue
                    try {
                        add(snapshotOf(child, depth + 1))
                    } finally {
                        child.recycle()
                    }
                    if (nodeBudget >= 2_000) break
                }
            }
        } else {
            emptyList()
        }
        return AccessibilityNodeSnapshot(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            viewId = node.viewIdResourceName,
            className = node.className?.toString(),
            bounds = ScreenRect(bounds.left, bounds.top, bounds.right, bounds.bottom),
            visibleToUser = node.isVisibleToUser,
            children = children,
            hintText = node.hintText?.toString(),
        )
    }
}

private class BlockOverlayView(
    context: android.content.Context,
    private val target: BlockTarget,
    private val durationSeconds: Int,
    private val delaySeconds: Int,
    private val canUnlock: Boolean,
    private val diagnosticMode: Boolean,
    private val onUnlock: () -> Unit,
    private val onExit: () -> Unit,
) : FrameLayout(context) {
    private var unlockCountdown: CountDownTimer? = null
    private val overlayTextColor = if (
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    ) {
        Color.WHITE
    } else {
        ContextCompat.getColor(context, R.color.stopreels_navy)
    }

    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(18, 12, 18, 12)
    }

    init {
        setBackgroundColor(
            if (overlayTextColor == Color.WHITE) Color.rgb(20, 32, 43) else Color.WHITE,
        )
        isClickable = true
        isFocusable = false
        // Consume touches at the accessibility overlay window so Instagram cannot
        // receive swipes/taps underneath the block.
        setOnTouchListener { _, _ -> true }
        elevation = 18f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.blocked_overlay_title)
        addView(content, LayoutParams(-1, -1))
        showBlockedState()
    }

    private fun showBlockedState() {
        unlockCountdown?.cancel()
        unlockCountdown = null
        content.removeAllViews()
        val title = TextView(context).apply {
            text = context.getString(R.string.blocked_overlay_title)
            textSize = if (target.kind == BlockTargetKind.SHORTCUT) 14f else 18f
            setTextColor(overlayTextColor)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        content.addView(title, LinearLayout.LayoutParams(-1, -2))

        TextView(context).also { reason ->
            reason.text = context.getString(R.string.blocked_overlay_reason)
            reason.textSize = 13f
            reason.setTextColor(overlayTextColor)
            reason.gravity = Gravity.CENTER
            content.addView(reason, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = 8
            })
        }

        if (diagnosticMode) {
            TextView(context).also { diagnostic ->
                diagnostic.text = context.getString(R.string.diagnostic_overlay, target.kind.name)
                diagnostic.textSize = 11f
                diagnostic.setTextColor(overlayTextColor)
                diagnostic.gravity = Gravity.CENTER
                content.addView(diagnostic, LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = 4
                })
            }
        }

        if (canUnlock && target.kind != BlockTargetKind.SHORTCUT) {
            val unlockButton = Button(context).apply {
                text = context.getString(R.string.temporary_unlock, durationSeconds)
                contentDescription = text
                setOnClickListener { showConfirmationState() }
            }
            content.addView(unlockButton, LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = 8
            })
        }

        val navigationActions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val exitButton = Button(context).apply {
                text = context.getString(R.string.overlay_exit)
                contentDescription = text
                setOnClickListener { onExit() }
            }
            addView(exitButton)
        }
        content.addView(navigationActions, LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = 8
        })
    }

    private fun showConfirmationState() {
        unlockCountdown?.cancel()
        content.removeAllViews()
        TextView(context).also { title ->
            title.text = context.getString(R.string.confirm_unlock_title)
            title.textSize = 18f
            title.setTextColor(overlayTextColor)
            title.gravity = Gravity.CENTER
            title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
            content.addView(title, LinearLayout.LayoutParams(-1, -2))
        }
        TextView(context).also { body ->
            body.text = context.getString(R.string.confirm_unlock_body, durationSeconds, delaySeconds)
            body.gravity = Gravity.CENTER
            body.setTextColor(overlayTextColor)
            content.addView(body, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
        }
        LinearLayout(context).also { actions ->
            actions.gravity = Gravity.CENTER
            val cancel = Button(context).apply {
                text = context.getString(R.string.cancel)
                setOnClickListener { showBlockedState() }
            }
            val confirm = Button(context).apply {
                isEnabled = delaySeconds <= 0
                text = if (isEnabled) {
                    context.getString(R.string.confirm)
                } else {
                    context.getString(R.string.confirm_waiting, delaySeconds)
                }
                setOnClickListener { onUnlock() }
            }
            actions.addView(cancel)
            actions.addView(confirm)
            content.addView(actions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = 8 })

            if (delaySeconds > 0) {
                unlockCountdown = object : CountDownTimer(delaySeconds * 1_000L, 1_000L) {
                    override fun onTick(millisUntilFinished: Long) {
                        val remaining = ((millisUntilFinished + 999L) / 1_000L).toInt()
                        confirm.text = context.getString(R.string.confirm_waiting, remaining)
                    }

                    override fun onFinish() {
                        confirm.isEnabled = true
                        confirm.text = context.getString(R.string.confirm)
                        unlockCountdown = null
                    }
                }.start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        unlockCountdown?.cancel()
        unlockCountdown = null
        super.onDetachedFromWindow()
    }
}
