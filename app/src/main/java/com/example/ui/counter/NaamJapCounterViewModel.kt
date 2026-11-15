package com.example.ui.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShyamApplication
import com.example.data.CounterBackgroundTheme
import com.example.data.CounterDisplayStyle
import com.example.data.CounterPreferences
import com.example.data.ShyamPreferencesRepository
import com.example.util.DevotionalFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NaamJapCounterViewModel(
    application: Application,
    private val repository: ShyamPreferencesRepository,
    private val feedbackHelper: DevotionalFeedbackHelper
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        repository = ShyamApplication.getRepository(application),
        feedbackHelper = DevotionalFeedbackHelper(application)
    )

    val counterState: StateFlow<CounterPreferences> = repository.counterPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CounterPreferences()
        )

    private val _malaCompletedSignal = MutableStateFlow(false)
    val malaCompletedSignal: StateFlow<Boolean> = _malaCompletedSignal.asStateFlow()

    fun onTapJap() {
        val current = counterState.value
        viewModelScope.launch {
            val malaCompleted = repository.incrementJap()
            if (malaCompleted) {
                feedbackHelper.triggerMalaCompletedFeedback(
                    soundEnabled = current.soundEnabled,
                    vibrationEnabled = current.vibrationEnabled
                )
                _malaCompletedSignal.value = true
                delay(2400L)
                _malaCompletedSignal.value = false
            } else {
                feedbackHelper.triggerTapFeedback(
                    soundEnabled = current.soundEnabled,
                    vibrationEnabled = current.vibrationEnabled
                )
            }
        }
    }

    fun onUndoJap() {
        viewModelScope.launch {
            repository.undoLastJap()
        }
    }

    fun onResetCounter(resetTodayOnly: Boolean) {
        viewModelScope.launch {
            repository.resetCounter(resetTodayOnly = resetTodayOnly)
        }
    }

    fun selectPresetMantra(mantra: String) {
        viewModelScope.launch {
            repository.updateCounterCustomization(
                selectedMantra = mantra,
                isCustomMantraActive = false
            )
        }
    }

    fun setCustomMantra(text: String) {
        viewModelScope.launch {
            repository.updateCounterCustomization(
                customMantra = text,
                isCustomMantraActive = text.isNotBlank()
            )
        }
    }

    fun setMalaTarget(target: Int) {
        if (target <= 0) return
        viewModelScope.launch {
            repository.updateCounterCustomization(malaTarget = target)
        }
    }

    fun setDailyTarget(target: Int) {
        if (target <= 0) return
        viewModelScope.launch {
            repository.updateCounterCustomization(dailyTarget = target)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCounterCustomization(soundEnabled = enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCounterCustomization(vibrationEnabled = enabled)
        }
    }

    fun setBackgroundTheme(theme: CounterBackgroundTheme) {
        viewModelScope.launch {
            repository.updateCounterCustomization(backgroundTheme = theme)
        }
    }

    fun setDisplayStyle(style: CounterDisplayStyle) {
        viewModelScope.launch {
            repository.updateCounterCustomization(displayStyle = style)
        }
    }
}
