package com.example

import com.example.data.ReminderSettings
import com.example.reminder.ReminderTimeCalculator
import com.example.update.GoogleDriveUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ShyamCoreUnitTest {

    @Test
    fun `reminder calculator computes 5 minute, 10 minute and custom intervals inside window`() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val baseCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 14, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseMillis = baseCal.timeInMillis

        val s5 = ReminderSettings(enabled = true, intervalMinutes = 5, startMinutesOfDay = 5 * 60, endMinutesOfDay = 22 * 60)
        assertEquals(baseMillis + 5 * 60_000L, ReminderTimeCalculator.computeNextReminderTimeMillis(s5, baseMillis, tz))

        val s10 = s5.copy(intervalMinutes = 10)
        assertEquals(baseMillis + 10 * 60_000L, ReminderTimeCalculator.computeNextReminderTimeMillis(s10, baseMillis, tz))

        val sCustom = s5.copy(intervalMinutes = 42)
        assertEquals(baseMillis + 42 * 60_000L, ReminderTimeCalculator.computeNextReminderTimeMillis(sCustom, baseMillis, tz))
    }

    @Test
    fun `reminder calculator enforces minimum 5 minutes and skips outside active window`() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val lateCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 14, 22, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 2,
            startMinutesOfDay = 5 * 60,
            endMinutesOfDay = 22 * 60
        )
        val next = ReminderTimeCalculator.computeNextReminderTimeMillis(settings, lateCal.timeInMillis, tz)
        val nextCal = Calendar.getInstance(tz).apply { timeInMillis = next }
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun `reminder calculator returns 0 when disabled or paused`() {
        val now = System.currentTimeMillis()
        assertEquals(0L, ReminderTimeCalculator.computeNextReminderTimeMillis(ReminderSettings(enabled = false), now))
        assertEquals(0L, ReminderTimeCalculator.computeNextReminderTimeMillis(ReminderSettings(enabled = true, paused = true), now))
    }

    @Test
    fun `google drive share url resolves to direct download url while direct https url stays unchanged`() {
        val shareView = "https://drive.google.com/file/d/1AbCDefGhIjKlMnOpQrStUvWxYz/view?usp=sharing"
        assertEquals(
            "https://drive.usercontent.google.com/download?id=1AbCDefGhIjKlMnOpQrStUvWxYz&export=download&confirm=t",
            GoogleDriveUrlResolver.resolveDirectDownloadUrl(shareView)
        )

        val openUrl = "https://drive.google.com/open?id=987654321xyz"
        assertEquals(
            "https://drive.usercontent.google.com/download?id=987654321xyz&export=download&confirm=t",
            GoogleDriveUrlResolver.resolveDirectDownloadUrl(openUrl)
        )

        val directHttps = "https://example.com/releases/SHYAM-v1.0.apk"
        assertEquals(directHttps, GoogleDriveUrlResolver.resolveDirectDownloadUrl(directHttps))
    }
}
