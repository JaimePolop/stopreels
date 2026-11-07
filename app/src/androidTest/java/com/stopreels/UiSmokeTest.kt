package com.stopreels

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Basic device smoke test. Accessibility service behavior is covered by pure JVM tests. */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun activityLaunches() {
        check(!activityRule.activity.isFinishing)
    }
}
