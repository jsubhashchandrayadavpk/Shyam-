package com.example.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ReminderNotificationHelper {

    private const val CHANNEL_SOUND_VIB = "shyam_reminder_sv_v1"
    private const val CHANNEL_SOUND_ONLY = "shyam_reminder_s_v1"
    private const val CHANNEL_VIB_ONLY = "shyam_reminder_v_v1"
    private const val CHANNEL_SILENT = "shyam_reminder_silent_v1"
    private const val NOTIFICATION_ID = 2008

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibPattern = longArrayOf(0L, 180L, 120L, 220L)

        val channels = listOf(
            NotificationChannel(
                CHANNEL_SOUND_VIB,
                "श्री राधा नाम जप रिमाइंडर (ध्वनि व कंपन)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "नियमित श्री राधा नाम स्मरण सूचनाएं (ध्वनि व कंपन सहित)"
                setSound(defaultSound, audioAttrs)
                enableVibration(true)
                vibrationPattern = vibPattern
            },
            NotificationChannel(
                CHANNEL_SOUND_ONLY,
                "श्री राधा नाम जप रिमाइंडर (केवल ध्वनि)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "केवल ध्वनि सहित नाम स्मरण सूचनाएं"
                setSound(defaultSound, audioAttrs)
                enableVibration(false)
            },
            NotificationChannel(
                CHANNEL_VIB_ONLY,
                "श्री राधा नाम जप रिमाइंडर (केवल कंपन)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "केवल कंपन सहित नाम स्मरण सूचनाएं"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = vibPattern
            },
            NotificationChannel(
                CHANNEL_SILENT,
                "श्री राधा नाम जप रिमाइंडर (शांत)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "मौन नाम स्मरण सूचनाएं"
                setSound(null, null)
                enableVibration(false)
            }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun channelIdFor(soundEnabled: Boolean, vibrationEnabled: Boolean): String = when {
        soundEnabled && vibrationEnabled -> CHANNEL_SOUND_VIB
        soundEnabled && !vibrationEnabled -> CHANNEL_SOUND_ONLY
        !soundEnabled && vibrationEnabled -> CHANNEL_VIB_ONLY
        else -> CHANNEL_SILENT
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(
        context: Context,
        message: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannelsCreated(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_route", "counter")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = channelIdFor(soundEnabled, vibrationEnabled)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SHYAM • श्री राधा नाम जप")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (!soundEnabled) {
            builder.setSilent(!vibrationEnabled)
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L, 180L, 120L, 220L))
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
