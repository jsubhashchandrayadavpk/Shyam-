package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ShyamApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != null && intent.action != ReminderScheduler.ACTION_NAAM_JAP_REMINDER) {
            return
        }

        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext
        val repository = ShyamApplication.getRepository(appContext)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = repository.getReminderSettingsOnce()
                if (!settings.enabled || settings.paused) {
                    ReminderScheduler.cancel(appContext)
                    repository.updateNextReminderTimestamp(0L)
                    return@launch
                }

                val nowCal = Calendar.getInstance()
                val shouldNotifyNow = ReminderTimeCalculator.isWithinWindowAndWeekday(
                    cal = nowCal,
                    startMinutesOfDay = settings.startMinutesOfDay,
                    endMinutesOfDay = settings.endMinutesOfDay,
                    selectedWeekdays = settings.selectedWeekdays
                )

                if (shouldNotifyNow) {
                    ReminderNotificationHelper.showReminderNotification(
                        context = appContext,
                        message = settings.customMessage,
                        soundEnabled = settings.soundEnabled,
                        vibrationEnabled = settings.vibrationEnabled
                    )
                    if (settings.hindiVoiceEnabled) {
                        HindiVoiceSpeaker(appContext).speakHindi(settings.customMessage)
                    }
                }

                val nextMillis = ReminderScheduler.scheduleOrCancel(
                    context = appContext,
                    settings = settings,
                    nowMillis = System.currentTimeMillis()
                )
                repository.updateNextReminderTimestamp(nextMillis)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
