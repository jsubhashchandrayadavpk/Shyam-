package com.example.data

import java.util.Calendar

enum class CounterBackgroundTheme(val displayNameHindi: String) {
    SAFFRON_CREAM("वृंदावन प्रभात"),
    SHYAM_TWILIGHT("श्याम संध्या"),
    LOTUS_IVORY("पद्म कमल"),
    SACRED_SANDALWOOD("चंदन कुंज")
}

enum class CounterDisplayStyle(val displayNameHindi: String) {
    MALA_RING("माला चक्र"),
    LOTUS_MANDALA("कमल मंडल"),
    MINIMAL_CLASSIC("सरल दिव्य")
}

object MantraPresets {
    val options = listOf("श्री राधा", "राधे राधे", "राधा रानी")
    const val CUSTOM_KEY = "CUSTOM"
}

data class CounterPreferences(
    val totalJapCount: Long = 0L,
    val todayJapCount: Long = 0L,
    val lastJapDate: String = "",
    val malaCount: Long = 0L,
    val currentMalaBeads: Int = 0,
    val malaTarget: Int = 108,
    val dailyTarget: Int = 1080,
    val selectedMantra: String = "श्री राधा",
    val customMantra: String = "",
    val isCustomMantraActive: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val backgroundTheme: CounterBackgroundTheme = CounterBackgroundTheme.SAFFRON_CREAM,
    val displayStyle: CounterDisplayStyle = CounterDisplayStyle.MALA_RING
) {
    val activeMantraText: String
        get() = if (isCustomMantraActive && customMantra.isNotBlank()) {
            customMantra.trim()
        } else {
            selectedMantra
        }
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val paused: Boolean = false,
    val intervalMinutes: Int = 15,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val hindiVoiceEnabled: Boolean = false,
    val customMessage: String = DEFAULT_MESSAGE,
    val startMinutesOfDay: Int = 5 * 60, // 05:00 AM
    val endMinutesOfDay: Int = 22 * 60,  // 10:00 PM
    val selectedWeekdays: Set<Int> = ALL_WEEKDAYS,
    val nextReminderTimestampMillis: Long = 0L
) {
    companion object {
        const val MIN_INTERVAL_MINUTES = 5
        const val DEFAULT_MESSAGE = "राधे राधे 🙏 अब श्री राधा नाम का स्मरण करें।"
        val ALL_WEEKDAYS: Set<Int> = setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
        val PRESET_INTERVALS_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 120, 180, 240)
    }
}

data class UpdateUrlOverrides(
    val customVersionJsonUrl: String = "",
    val customApkDownloadUrl: String = "",
    val lastDownloadId: Long = -1L,
    val lastDownloadedApkPath: String = ""
)
