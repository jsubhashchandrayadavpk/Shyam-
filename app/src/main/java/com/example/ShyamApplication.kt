package com.example

import android.app.Application
import android.content.Context
import com.example.data.ShyamPreferencesRepository
import com.example.reminder.ReminderNotificationHelper

class ShyamApplication : Application() {

    val repository: ShyamPreferencesRepository by lazy {
        ShyamPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        ReminderNotificationHelper.ensureChannelsCreated(this)
    }

    companion object {
        fun getRepository(context: Context): ShyamPreferencesRepository {
            val app = context.applicationContext as? ShyamApplication
            return app?.repository ?: ShyamPreferencesRepository(context.applicationContext)
        }
    }
}
