package com.example.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ShyamApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in supportedActions) return

        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext
        val repository = ShyamApplication.getRepository(appContext)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = repository.getReminderSettingsOnce()
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
