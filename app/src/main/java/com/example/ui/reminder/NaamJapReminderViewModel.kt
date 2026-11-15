package com.example.ui.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShyamApplication
import com.example.data.ReminderSettings
import com.example.data.ShyamPreferencesRepository
import com.example.reminder.HindiVoiceSpeaker
import com.example.reminder.ReminderNotificationHelper
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NaamJapReminderViewModel(
    application: Application,
    private val repository: ShyamPreferencesRepository
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        repository = ShyamApplication.getRepository(application)
    )

    val settings: StateFlow<ReminderSettings> = repository.reminderSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReminderSettings()
        )

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    fun clearNotices() {
        _validationError.value = null
        _statusNotice.value = null
    }

    fun setReminderEnabled(enabled: Boolean) {
        val updated = settings.value.copy(
            enabled = enabled,
            paused = if (!enabled) false else settings.value.paused
        )
        persistAndReschedule(updated)
    }

    fun togglePaused() {
        val current = settings.value
        if (!current.enabled) return
        val updated = current.copy(paused = !current.paused)
        persistAndReschedule(updated)
    }

    fun applyIntervalMinutes(minutes: Int): Boolean {
        if (minutes < ReminderSettings.MIN_INTERVAL_MINUTES) {
            _validationError.value = "न्यूनतम रिमाइंडर अंतराल ${ReminderSettings.MIN_INTERVAL_MINUTES} मिनट होना आवश्यक है।"
            return false
        }
        _validationError.value = null
        val updated = settings.value.copy(intervalMinutes = minutes)
        persistAndReschedule(updated)
        _statusNotice.value = "रिमाइंडर अंतराल सेट किया गया।"
        return true
    }

    fun setSoundEnabled(enabled: Boolean) {
        persistAndReschedule(settings.value.copy(soundEnabled = enabled))
    }

    fun setVibrationEnabled(enabled: Boolean) {
        persistAndReschedule(settings.value.copy(vibrationEnabled = enabled))
    }

    fun setHindiVoiceEnabled(enabled: Boolean) {
        persistAndReschedule(settings.value.copy(hindiVoiceEnabled = enabled))
    }

    fun updateCustomMessage(message: String) {
        val safeMsg = message.ifBlank { ReminderSettings.DEFAULT_MESSAGE }
        persistAndReschedule(settings.value.copy(customMessage = safeMsg))
        _statusNotice.value = "रिमाइंडर संदेश सहेजा गया।"
    }

    fun updateStartTime(hourOfDay: Int, minute: Int) {
        val startMin = (hourOfDay * 60 + minute).coerceIn(0, 1439)
        persistAndReschedule(settings.value.copy(startMinutesOfDay = startMin))
    }

    fun updateEndTime(hourOfDay: Int, minute: Int) {
        val endMin = (hourOfDay * 60 + minute).coerceIn(0, 1439)
        persistAndReschedule(settings.value.copy(endMinutesOfDay = endMin))
    }

    fun toggleWeekday(calendarDay: Int) {
        val currentDays = settings.value.selectedWeekdays
        val updatedDays = if (calendarDay in currentDays) {
            if (currentDays.size <= 1) {
                _validationError.value = "कम से कम एक सप्ताह का दिन चुना होना चाहिए।"
                return
            }
            currentDays - calendarDay
        } else {
            currentDays + calendarDay
        }
        _validationError.value = null
        persistAndReschedule(settings.value.copy(selectedWeekdays = updatedDays))
    }

    fun testHindiVoice() {
        val ctx = getApplication<Application>()
        HindiVoiceSpeaker(ctx).speakHindi(settings.value.customMessage)
        _statusNotice.value = "हिंदी वॉइस रिमाइंडर चलाया जा रहा है..."
    }

    fun sendTestNotificationNow() {
        val ctx = getApplication<Application>()
        val s = settings.value
        if (!ReminderNotificationHelper.hasNotificationPermission(ctx)) {
            _validationError.value = "कृपया पहले सूचना (Notification) की अनुमति दें।"
            return
        }
        ReminderNotificationHelper.showReminderNotification(
            context = ctx,
            message = s.customMessage,
            soundEnabled = s.soundEnabled,
            vibrationEnabled = s.vibrationEnabled
        )
        if (s.hindiVoiceEnabled) {
            HindiVoiceSpeaker(ctx).speakHindi(s.customMessage)
        }
        _statusNotice.value = "परीक्षण रिमाइंडर सूचना भेजी गई।"
    }

    fun refreshScheduleOnPermissionChange() {
        val current = settings.value
        if (current.enabled && !current.paused) {
            persistAndReschedule(current)
        }
    }

    private fun persistAndReschedule(updated: ReminderSettings) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val nextMillis = ReminderScheduler.scheduleOrCancel(ctx, updated)
            repository.saveReminderSettings(updated.copy(nextReminderTimestampMillis = nextMillis))
        }
    }
}
