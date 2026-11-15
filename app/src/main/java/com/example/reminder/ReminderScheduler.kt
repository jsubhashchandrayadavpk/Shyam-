package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.ReminderSettings

object ReminderScheduler {

    private const val REQUEST_CODE_REMINDER = 1008
    const val ACTION_NAAM_JAP_REMINDER = "com.example.action.NAAM_JAP_REMINDER"

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules the next reminder alarm cleanly without duplicates, returning the scheduled epoch millis (or 0L if cancelled).
     */
    fun scheduleOrCancel(
        context: Context,
        settings: ReminderSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        val pendingIntent = buildPendingIntent(context)

        // Always cancel existing alarm first to guarantee zero duplicates
        alarmManager.cancel(pendingIntent)

        if (!settings.enabled || settings.paused) {
            return 0L
        }

        val nextMillis = ReminderTimeCalculator.computeNextReminderTimeMillis(settings, nowMillis)
        if (nextMillis <= 0L) {
            return 0L
        }

        val canExact = canScheduleExactAlarms(context)
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMillis,
                pendingIntent
            )
        }
        return nextMillis
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    internal fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_NAAM_JAP_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
