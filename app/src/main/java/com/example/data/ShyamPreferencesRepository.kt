package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.shyamDataStore: DataStore<Preferences> by preferencesDataStore(name = "shyam_preferences")

class ShyamPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val dateProvider: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
) {
    constructor(context: Context) : this(context.shyamDataStore)

    private object Keys {
        val TOTAL_JAP_COUNT = longPreferencesKey("total_jap_count")
        val TODAY_JAP_COUNT = longPreferencesKey("today_jap_count")
        val LAST_JAP_DATE = stringPreferencesKey("last_jap_date")
        val MALA_COUNT = longPreferencesKey("mala_count")
        val CURRENT_MALA_BEADS = intPreferencesKey("current_mala_beads")
        val MALA_TARGET = intPreferencesKey("mala_target")
        val DAILY_TARGET = intPreferencesKey("daily_target")
        val SELECTED_MANTRA = stringPreferencesKey("selected_mantra")
        val CUSTOM_MANTRA = stringPreferencesKey("custom_mantra")
        val IS_CUSTOM_MANTRA = booleanPreferencesKey("is_custom_mantra")
        val COUNTER_SOUND = booleanPreferencesKey("counter_sound")
        val COUNTER_VIBRATION = booleanPreferencesKey("counter_vibration")
        val COUNTER_BG_THEME = stringPreferencesKey("counter_bg_theme")
        val COUNTER_DISPLAY_STYLE = stringPreferencesKey("counter_display_style")

        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_PAUSED = booleanPreferencesKey("reminder_paused")
        val REMINDER_INTERVAL_MIN = intPreferencesKey("reminder_interval_min")
        val REMINDER_SOUND = booleanPreferencesKey("reminder_sound")
        val REMINDER_VIBRATION = booleanPreferencesKey("reminder_vibration")
        val REMINDER_HINDI_VOICE = booleanPreferencesKey("reminder_hindi_voice")
        val REMINDER_MESSAGE = stringPreferencesKey("reminder_message")
        val REMINDER_START_MIN = intPreferencesKey("reminder_start_min")
        val REMINDER_END_MIN = intPreferencesKey("reminder_end_min")
        val REMINDER_WEEKDAYS = stringSetPreferencesKey("reminder_weekdays")
        val NEXT_REMINDER_TS = longPreferencesKey("next_reminder_ts")

        val CUSTOM_VERSION_JSON_URL = stringPreferencesKey("custom_version_json_url")
        val CUSTOM_APK_DOWNLOAD_URL = stringPreferencesKey("custom_apk_download_url")
        val LAST_DOWNLOAD_ID = longPreferencesKey("last_download_id")
        val LAST_DOWNLOADED_APK_PATH = stringPreferencesKey("last_downloaded_apk_path")
    }

    private val safePrefs: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    val counterPreferencesFlow: Flow<CounterPreferences> = safePrefs.map { prefs ->
        val today = dateProvider()
        val storedDate = prefs[Keys.LAST_JAP_DATE] ?: today
        val rawTodayCount = prefs[Keys.TODAY_JAP_COUNT] ?: 0L
        val effectiveTodayCount = if (storedDate == today) rawTodayCount else 0L

        val bgTheme = prefs[Keys.COUNTER_BG_THEME]?.let { name ->
            CounterBackgroundTheme.entries.firstOrNull { it.name == name }
        } ?: CounterBackgroundTheme.SAFFRON_CREAM

        val displayStyle = prefs[Keys.COUNTER_DISPLAY_STYLE]?.let { name ->
            CounterDisplayStyle.entries.firstOrNull { it.name == name }
        } ?: CounterDisplayStyle.MALA_RING

        val malaTarget = (prefs[Keys.MALA_TARGET] ?: 108).coerceAtLeast(1)
        val rawBeads = (prefs[Keys.CURRENT_MALA_BEADS] ?: 0).coerceIn(0, (malaTarget - 1).coerceAtLeast(0))

        CounterPreferences(
            totalJapCount = (prefs[Keys.TOTAL_JAP_COUNT] ?: 0L).coerceAtLeast(0L),
            todayJapCount = effectiveTodayCount.coerceAtLeast(0L),
            lastJapDate = storedDate,
            malaCount = (prefs[Keys.MALA_COUNT] ?: 0L).coerceAtLeast(0L),
            currentMalaBeads = rawBeads,
            malaTarget = malaTarget,
            dailyTarget = (prefs[Keys.DAILY_TARGET] ?: 1080).coerceAtLeast(1),
            selectedMantra = prefs[Keys.SELECTED_MANTRA] ?: "श्री राधा",
            customMantra = prefs[Keys.CUSTOM_MANTRA] ?: "",
            isCustomMantraActive = prefs[Keys.IS_CUSTOM_MANTRA] ?: false,
            soundEnabled = prefs[Keys.COUNTER_SOUND] ?: true,
            vibrationEnabled = prefs[Keys.COUNTER_VIBRATION] ?: true,
            backgroundTheme = bgTheme,
            displayStyle = displayStyle
        )
    }

    val reminderSettingsFlow: Flow<ReminderSettings> = safePrefs.map { prefs ->
        val storedWeekdays = prefs[Keys.REMINDER_WEEKDAYS]?.mapNotNull { it.toIntOrNull() }?.toSet()
        ReminderSettings(
            enabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            paused = prefs[Keys.REMINDER_PAUSED] ?: false,
            intervalMinutes = (prefs[Keys.REMINDER_INTERVAL_MIN] ?: 15)
                .coerceAtLeast(ReminderSettings.MIN_INTERVAL_MINUTES),
            soundEnabled = prefs[Keys.REMINDER_SOUND] ?: true,
            vibrationEnabled = prefs[Keys.REMINDER_VIBRATION] ?: true,
            hindiVoiceEnabled = prefs[Keys.REMINDER_HINDI_VOICE] ?: false,
            customMessage = prefs[Keys.REMINDER_MESSAGE]?.takeIf { it.isNotBlank() }
                ?: ReminderSettings.DEFAULT_MESSAGE,
            startMinutesOfDay = (prefs[Keys.REMINDER_START_MIN] ?: (5 * 60)).coerceIn(0, 1439),
            endMinutesOfDay = (prefs[Keys.REMINDER_END_MIN] ?: (22 * 60)).coerceIn(0, 1439),
            selectedWeekdays = if (storedWeekdays.isNullOrEmpty()) {
                ReminderSettings.ALL_WEEKDAYS
            } else {
                storedWeekdays
            },
            nextReminderTimestampMillis = prefs[Keys.NEXT_REMINDER_TS] ?: 0L
        )
    }

    val updateUrlOverridesFlow: Flow<UpdateUrlOverrides> = safePrefs.map { prefs ->
        UpdateUrlOverrides(
            customVersionJsonUrl = prefs[Keys.CUSTOM_VERSION_JSON_URL] ?: "",
            customApkDownloadUrl = prefs[Keys.CUSTOM_APK_DOWNLOAD_URL] ?: "",
            lastDownloadId = prefs[Keys.LAST_DOWNLOAD_ID] ?: -1L,
            lastDownloadedApkPath = prefs[Keys.LAST_DOWNLOADED_APK_PATH] ?: ""
        )
    }

    suspend fun getReminderSettingsOnce(): ReminderSettings = reminderSettingsFlow.first()

    /**
     * Increments Jap count by exactly 1.
     * Returns true if a Mala just completed on this tap.
     */
    suspend fun incrementJap(): Boolean {
        var malaCompleted = false
        val today = dateProvider()
        dataStore.edit { prefs ->
            val currentTotal = (prefs[Keys.TOTAL_JAP_COUNT] ?: 0L).coerceAtLeast(0L)
            val storedDate = prefs[Keys.LAST_JAP_DATE] ?: today
            val currentToday = if (storedDate == today) {
                (prefs[Keys.TODAY_JAP_COUNT] ?: 0L).coerceAtLeast(0L)
            } else {
                0L
            }
            val malaTarget = (prefs[Keys.MALA_TARGET] ?: 108).coerceAtLeast(1)
            val currentBeads = (prefs[Keys.CURRENT_MALA_BEADS] ?: 0).coerceAtLeast(0)
            val currentMalas = (prefs[Keys.MALA_COUNT] ?: 0L).coerceAtLeast(0L)

            val newBeads = currentBeads + 1
            if (newBeads >= malaTarget) {
                prefs[Keys.CURRENT_MALA_BEADS] = 0
                prefs[Keys.MALA_COUNT] = currentMalas + 1
                malaCompleted = true
            } else {
                prefs[Keys.CURRENT_MALA_BEADS] = newBeads
            }

            prefs[Keys.TOTAL_JAP_COUNT] = currentTotal + 1
            prefs[Keys.TODAY_JAP_COUNT] = currentToday + 1
            prefs[Keys.LAST_JAP_DATE] = today
        }
        return malaCompleted
    }

    /**
     * Undoes the last Jap increment safely without going below zero.
     */
    suspend fun undoLastJap() {
        val today = dateProvider()
        dataStore.edit { prefs ->
            val currentTotal = (prefs[Keys.TOTAL_JAP_COUNT] ?: 0L).coerceAtLeast(0L)
            if (currentTotal <= 0L) return@edit

            val storedDate = prefs[Keys.LAST_JAP_DATE] ?: today
            val currentToday = if (storedDate == today) {
                (prefs[Keys.TODAY_JAP_COUNT] ?: 0L).coerceAtLeast(0L)
            } else {
                0L
            }
            val malaTarget = (prefs[Keys.MALA_TARGET] ?: 108).coerceAtLeast(1)
            val currentBeads = (prefs[Keys.CURRENT_MALA_BEADS] ?: 0).coerceAtLeast(0)
            val currentMalas = (prefs[Keys.MALA_COUNT] ?: 0L).coerceAtLeast(0L)

            prefs[Keys.TOTAL_JAP_COUNT] = currentTotal - 1
            if (currentToday > 0L) {
                prefs[Keys.TODAY_JAP_COUNT] = currentToday - 1
            }
            if (currentBeads > 0) {
                prefs[Keys.CURRENT_MALA_BEADS] = currentBeads - 1
            } else if (currentMalas > 0L) {
                prefs[Keys.MALA_COUNT] = currentMalas - 1
                prefs[Keys.CURRENT_MALA_BEADS] = (malaTarget - 1).coerceAtLeast(0)
            }
        }
    }

    suspend fun resetCounter(resetTodayOnly: Boolean) {
        val today = dateProvider()
        dataStore.edit { prefs ->
            if (resetTodayOnly) {
                prefs[Keys.TODAY_JAP_COUNT] = 0L
                prefs[Keys.CURRENT_MALA_BEADS] = 0
                prefs[Keys.LAST_JAP_DATE] = today
            } else {
                prefs[Keys.TOTAL_JAP_COUNT] = 0L
                prefs[Keys.TODAY_JAP_COUNT] = 0L
                prefs[Keys.MALA_COUNT] = 0L
                prefs[Keys.CURRENT_MALA_BEADS] = 0
                prefs[Keys.LAST_JAP_DATE] = today
            }
        }
    }

    suspend fun updateCounterCustomization(
        selectedMantra: String? = null,
        customMantra: String? = null,
        isCustomMantraActive: Boolean? = null,
        malaTarget: Int? = null,
        dailyTarget: Int? = null,
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        backgroundTheme: CounterBackgroundTheme? = null,
        displayStyle: CounterDisplayStyle? = null
    ) {
        dataStore.edit { prefs ->
            selectedMantra?.let { prefs[Keys.SELECTED_MANTRA] = it }
            customMantra?.let { prefs[Keys.CUSTOM_MANTRA] = it }
            isCustomMantraActive?.let { prefs[Keys.IS_CUSTOM_MANTRA] = it }
            malaTarget?.let { target ->
                val valid = target.coerceAtLeast(1)
                prefs[Keys.MALA_TARGET] = valid
                val beads = prefs[Keys.CURRENT_MALA_BEADS] ?: 0
                if (beads >= valid) {
                    prefs[Keys.CURRENT_MALA_BEADS] = beads % valid
                }
            }
            dailyTarget?.let { prefs[Keys.DAILY_TARGET] = it.coerceAtLeast(1) }
            soundEnabled?.let { prefs[Keys.COUNTER_SOUND] = it }
            vibrationEnabled?.let { prefs[Keys.COUNTER_VIBRATION] = it }
            backgroundTheme?.let { prefs[Keys.COUNTER_BG_THEME] = it.name }
            displayStyle?.let { prefs[Keys.COUNTER_DISPLAY_STYLE] = it.name }
        }
    }

    suspend fun saveReminderSettings(settings: ReminderSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = settings.enabled
            prefs[Keys.REMINDER_PAUSED] = settings.paused
            prefs[Keys.REMINDER_INTERVAL_MIN] =
                settings.intervalMinutes.coerceAtLeast(ReminderSettings.MIN_INTERVAL_MINUTES)
            prefs[Keys.REMINDER_SOUND] = settings.soundEnabled
            prefs[Keys.REMINDER_VIBRATION] = settings.vibrationEnabled
            prefs[Keys.REMINDER_HINDI_VOICE] = settings.hindiVoiceEnabled
            prefs[Keys.REMINDER_MESSAGE] = settings.customMessage
            prefs[Keys.REMINDER_START_MIN] = settings.startMinutesOfDay.coerceIn(0, 1439)
            prefs[Keys.REMINDER_END_MIN] = settings.endMinutesOfDay.coerceIn(0, 1439)
            val safeDays = settings.selectedWeekdays.ifEmpty { ReminderSettings.ALL_WEEKDAYS }
            prefs[Keys.REMINDER_WEEKDAYS] = safeDays.map { it.toString() }.toSet()
            prefs[Keys.NEXT_REMINDER_TS] = settings.nextReminderTimestampMillis
        }
    }

    suspend fun updateNextReminderTimestamp(timestampMillis: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_REMINDER_TS] = timestampMillis
        }
    }

    suspend fun saveUpdateUrlOverrides(versionJsonUrl: String, apkDownloadUrl: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_VERSION_JSON_URL] = versionJsonUrl.trim()
            prefs[Keys.CUSTOM_APK_DOWNLOAD_URL] = apkDownloadUrl.trim()
        }
    }

    suspend fun saveLastDownloadInfo(downloadId: Long, apkPath: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_DOWNLOAD_ID] = downloadId
            prefs[Keys.LAST_DOWNLOADED_APK_PATH] = apkPath
        }
    }
}
