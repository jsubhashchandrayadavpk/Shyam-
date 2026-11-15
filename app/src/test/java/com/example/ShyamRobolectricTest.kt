package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReminderSettings
import com.example.reminder.BootCompletedReceiver
import com.example.reminder.ReminderScheduler
import com.example.update.ApkValidationResult
import com.example.update.ApkValidator
import com.example.update.ShyamApkUpdateManager
import com.example.update.UpdateMetadataParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShyamRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `ui single tap equals one count, fast consecutive taps, 108 mala and undo work via real button clicks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ShyamApplication.getRepository(context)
        runBlocking {
            repo.resetCounter(resetTodayOnly = false)
            repo.updateCounterCustomization(soundEnabled = false, vibrationEnabled = false, malaTarget = 108)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_card_naam_jap").performScrollTo().performClick()
        composeRule.waitForIdle()

        val tapButton = composeRule.onNodeWithTag("counter_tap_button").performScrollTo()

        fun awaitCount(expected: Long) {
            val deadline = System.currentTimeMillis() + 4_000L
            var actual = -1L
            while (System.currentTimeMillis() < deadline) {
                composeRule.waitForIdle()
                actual = runBlocking { repo.counterPreferencesFlow.first().totalJapCount }
                if (actual == expected) return
                Thread.sleep(30L)
            }
            assertEquals("Expected totalJapCount=$expected but was $actual", expected, actual)
        }

        // TEST 1: 1 tap = 1 count
        tapButton.performClick()
        awaitCount(1L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("1")
        composeRule.onNodeWithTag("central_beads_value", useUnmergedTree = true).assertTextEquals("1")

        // 2nd tap = 2 counts
        tapButton.performClick()
        awaitCount(2L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("2")

        // 3 more rapid consecutive taps -> 5 counts
        repeat(3) { tapButton.performClick() }
        awaitCount(5L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("5")

        // TEST 2: 5 more rapid taps -> 10 taps = 10 counts
        repeat(5) { tapButton.performClick() }
        awaitCount(10L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("10")
        composeRule.onNodeWithTag("mala_count_value").assertTextEquals("0")

        // TEST 3: Remaining 98 taps -> 108 taps = 108 counts and exactly 1 completed Mala
        repeat(98) { tapButton.performClick() }
        awaitCount(108L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("108")
        composeRule.onNodeWithTag("mala_count_value").assertTextEquals("1")
        composeRule.onNodeWithTag("central_beads_value", useUnmergedTree = true).assertTextEquals("0")

        // TEST 4: Undo reverses exactly 1 count (108 -> 107, Mala 1 -> 0)
        composeRule.onNodeWithTag("counter_undo_button").performScrollTo().performClick()
        awaitCount(107L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_jap_value").assertTextEquals("107")
        composeRule.onNodeWithTag("mala_count_value").assertTextEquals("0")
        composeRule.onNodeWithTag("central_beads_value", useUnmergedTree = true).assertTextEquals("107")
    }

    @Test
    fun `reminder schedules without duplicates, cancels on disable, and restores on boot`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ShyamApplication.getRepository(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = shadowOf(alarmManager)

        val settings = ReminderSettings(
            enabled = true,
            paused = false,
            intervalMinutes = 5,
            startMinutesOfDay = 0,
            endMinutesOfDay = 1439
        )
        repo.saveReminderSettings(settings)

        // Schedule twice -> still only 1 alarm (no duplicates)
        ReminderScheduler.scheduleOrCancel(context, settings)
        ReminderScheduler.scheduleOrCancel(context, settings.copy(intervalMinutes = 10))
        assertEquals(1, shadowAlarm.scheduledAlarms.size)

        // Disable stops future alarms
        ReminderScheduler.scheduleOrCancel(context, settings.copy(enabled = false))
        assertEquals(0, shadowAlarm.scheduledAlarms.size)

        // BootCompletedReceiver restores alarm when enabled
        repo.saveReminderSettings(settings.copy(enabled = true, intervalMinutes = 15))
        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        Thread.sleep(250)
        assertTrue(shadowAlarm.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun `update metadata parser, apk validator and invalid url checks work`() {
        val validJson = """
            {
              "latestVersionName": "1.2.0",
              "versionCode": 2,
              "apkDownloadUrl": "https://cdn.example.com/shyam-1.2.0.apk",
              "apkFileSizeBytes": 8450120,
              "releaseNotes": "बग सुधार"
            }
        """.trimIndent()
        val parsed = UpdateMetadataParser.parse(validJson)
        assertTrue(parsed.isSuccess)
        val info = parsed.getOrThrow()
        assertEquals("1.2.0", info.latestVersionName)
        assertEquals(2L, info.versionCode)
        assertTrue(info.versionCode > 1L)
        assertTrue(UpdateMetadataParser.parse("<html>Not Found</html>").isFailure)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakePdf = File(context.cacheDir, "fake.apk").apply {
            writeText("%PDF-1.4 fake document not an apk")
        }
        val validation = ApkValidator.validateApkFile(context, fakePdf)
        assertTrue(validation is ApkValidationResult.Invalid)
        fakePdf.delete()

        val manager = ShyamApkUpdateManager(context)
        val badUrlResult = manager.startApkDownload("http://not-https-or-invalid")
        assertTrue(badUrlResult.isFailure)
    }

    @Test
    fun `home screen opens all three main features cleanly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_card_naam_jap").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("counter_tap_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("counter_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_card_reminder").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reminder_enable_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_card_apk_download").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("export_local_apk_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("download_apk_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("check_updates_button").performScrollTo().assertIsDisplayed()
    }
}
