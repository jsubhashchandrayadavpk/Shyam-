package com.example.reminder

import com.example.data.ReminderSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReminderTimeCalculator {

    /**
     * Computes the next valid reminder timestamp in epoch millis strictly after [nowMillis],
     * honoring the minimum 5-minute interval, active start/end time window, and selected weekdays.
     * Returns 0L if reminders are disabled, paused, or no weekdays are selected.
     */
    fun computeNextReminderTimeMillis(
        settings: ReminderSettings,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        if (!settings.enabled || settings.paused) return 0L
        val activeWeekdays = settings.selectedWeekdays.ifEmpty { return 0L }
        val safeIntervalMin = settings.intervalMinutes.coerceAtLeast(ReminderSettings.MIN_INTERVAL_MINUTES)

        val candidateMillis = nowMillis + safeIntervalMin * 60_000L
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = candidateMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Preserve exact second/millisecond offset when candidate itself is valid
        val candidateRawCal = Calendar.getInstance(timeZone).apply { timeInMillis = candidateMillis }
        if (isWithinWindowAndWeekday(candidateRawCal, settings.startMinutesOfDay, settings.endMinutesOfDay, activeWeekdays)) {
            return candidateMillis
        }

        // Otherwise search forward up to 8 days for the next active window start
        for (dayOffset in 0..8) {
            val dayCal = Calendar.getInstance(timeZone).apply {
                timeInMillis = candidateMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, settings.startMinutesOfDay / 60)
                set(Calendar.MINUTE, settings.startMinutesOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekday = dayCal.get(Calendar.DAY_OF_WEEK)
            if (weekday in activeWeekdays && dayCal.timeInMillis >= candidateMillis) {
                return dayCal.timeInMillis
            }
        }

        // Fallback (should not happen when activeWeekdays is non-empty)
        return cal.timeInMillis
    }

    fun isWithinWindowAndWeekday(
        cal: Calendar,
        startMinutesOfDay: Int,
        endMinutesOfDay: Int,
        selectedWeekdays: Set<Int>
    ): Boolean {
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        if (weekday !in selectedWeekdays) return false

        val currentMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMinutesOfDay <= endMinutesOfDay) {
            currentMin in startMinutesOfDay..endMinutesOfDay
        } else {
            // Overnight window (e.g., 21:00 to 05:00)
            currentMin >= startMinutesOfDay || currentMin <= endMinutesOfDay
        }
    }

    fun formatMinutesOfDay(minutesOfDay: Int): String {
        val clamped = minutesOfDay.coerceIn(0, 1439)
        val hour24 = clamped / 60
        val minute = clamped % 60
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
    }

    fun formatIntervalHindi(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes मिनट"
            minutes % 60 == 0 -> {
                val hours = minutes / 60
                if (hours == 1) "1 घंटा" else "$hours घंटे"
            }
            else -> "${minutes / 60} घं ${minutes % 60} मि"
        }
    }

    fun formatNextReminderHindi(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (timestampMillis <= 0L) return "कोई रिमाइंडर सेट नहीं"
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val timeText = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestampMillis))

        val sameDay = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = tomorrowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            tomorrowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

        val dayLabel = when {
            sameDay -> "आज"
            isTomorrow -> "कल"
            else -> weekdayShortHindi(targetCal.get(Calendar.DAY_OF_WEEK))
        }
        return "$dayLabel, $timeText"
    }

    fun weekdayShortHindi(calendarDay: Int): String = when (calendarDay) {
        Calendar.MONDAY -> "सोम"
        Calendar.TUESDAY -> "मंगल"
        Calendar.WEDNESDAY -> "बुध"
        Calendar.THURSDAY -> "गुरु"
        Calendar.FRIDAY -> "शुक्र"
        Calendar.SATURDAY -> "शनि"
        Calendar.SUNDAY -> "रवि"
        else -> ""
    }
}
