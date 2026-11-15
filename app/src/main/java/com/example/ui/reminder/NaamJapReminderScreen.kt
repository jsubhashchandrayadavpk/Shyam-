package com.example.ui.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ReminderSettings
import com.example.reminder.ReminderNotificationHelper
import com.example.reminder.ReminderScheduler
import com.example.reminder.ReminderTimeCalculator
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutlineWarm
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronDeep
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TulsiContainer
import com.example.ui.theme.TulsiGreen
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NaamJapReminderScreen(
    settings: ReminderSettings,
    validationError: String?,
    statusNotice: String?,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onTogglePaused: () -> Unit,
    onSelectIntervalMinutes: (Int) -> Boolean,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetHindiVoiceEnabled: (Boolean) -> Unit,
    onUpdateMessage: (String) -> Unit,
    onUpdateStartTime: (Int, Int) -> Unit,
    onUpdateEndTime: (Int, Int) -> Unit,
    onToggleWeekday: (Int) -> Unit,
    onTestVoice: () -> Unit,
    onSendTestNotification: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotifPermission by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context))
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted && !settings.enabled) {
            onToggleEnabled(true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = ReminderNotificationHelper.hasNotificationPermission(context)
                hasExactAlarmPermission = ReminderScheduler.canScheduleExactAlarms(context)
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var customIntervalText by remember(settings.intervalMinutes) {
        mutableStateOf(settings.intervalMinutes.toString())
    }
    var messageDraft by remember(settings.customMessage) {
        mutableStateOf(settings.customMessage)
    }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CreamBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = InkPrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reminder_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "वापस जाएं"
                        )
                    }
                },
                title = {
                    Text(
                        text = "श्री राधा नाम जप रिमाइंडर",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error / Status Banner
                validationError?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AlertRedContainer,
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = AlertRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(14.dp)
                                .testTag("reminder_error_text")
                        )
                    }
                }
                statusNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TulsiContainer,
                        border = BorderStroke(1.dp, TulsiGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notice,
                            color = TulsiGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // 1. Master Enable / Pause / Next Time Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "रिमाइंडर स्थिति",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkPrimary
                                )
                                val statusLabel = when {
                                    !settings.enabled -> "बंद है"
                                    settings.paused -> "अस्थायी विराम पर (Paused)"
                                    else -> "सक्रिय • हर ${ReminderTimeCalculator.formatIntervalHindi(settings.intervalMinutes)}"
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (settings.enabled && !settings.paused) TulsiGreen else InkSecondary
                                )
                            }
                            Switch(
                                checked = settings.enabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        onToggleEnabled(true)
                                    } else {
                                        onToggleEnabled(enable)
                                    }
                                },
                                modifier = Modifier.testTag("reminder_enable_switch")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SaffronContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = "अगला रिमाइंडर समय",
                                    tint = SaffronDeep
                                )
                                Column {
                                    Text(
                                        text = "अगला रिमाइंडर समय",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary
                                    )
                                    val nextText = when {
                                        !settings.enabled -> "रिमाइंडर बंद है"
                                        settings.paused -> "विराम पर (पुनः आरंभ करें)"
                                        else -> ReminderTimeCalculator.formatNextReminderHindi(
                                            settings.nextReminderTimestampMillis
                                        )
                                    }
                                    Text(
                                        text = nextText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SaffronDeep,
                                        modifier = Modifier.testTag("next_reminder_time_text")
                                    )
                                }
                            }
                        }

                        if (settings.enabled) {
                            OutlinedButton(
                                onClick = onTogglePaused,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reminder_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (settings.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (settings.paused) "रिमाइंडर पुनः आरंभ करें (Resume)" else "अस्थायी विराम (Pause)"
                                )
                            }
                        }
                    }
                }

                // 2. Permissions & Honest Android 14 / TECNO Battery Reliability Card
                if (!hasNotifPermission || !hasExactAlarmPermission || settings.enabled) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryAlert,
                                    contentDescription = null,
                                    tint = SaffronDeep
                                )
                                Text(
                                    text = "बैकग्राउंड और बैटरी विश्वसनीयता",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary
                                )
                            }
                            Text(
                                text = "Android 14 (जैसे TECNO Camon 20 Premier / HiOS) में बैटरी ऑप्टिमाइज़ेशन या Doze मोड बैकग्राउंड अलार्म में देरी कर सकता है। सटीक समय पर रिमाइंडर के लिए सूचना, सटीक अलार्म व अनरिस्ट्रिक्टेड बैटरी अनुमति दें। ऐप बंद होने और स्क्रीन लॉक रहने पर भी अलार्म कार्य करेंगे, और फ़ोन रीस्टार्ट के बाद स्वतः बहाल होंगे।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )

                            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Button(
                                    onClick = {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("सूचना (Notification) अनुमति दें")
                                }
                            }

                            if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("सटीक अलार्म (Exact Alarm) अनुमति खोलें")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    val fallbackIntent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    runCatching { context.startActivity(batteryIntent) }
                                        .onFailure { runCatching { context.startActivity(fallbackIntent) } }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("बैटरी ऑप्टिमाइज़ेशन सेटिंग्स खोलें")
                            }
                        }
                    }
                }

                // 3. Interval Presets & Custom Interval (Min 5 minutes)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "रिमाइंडर अंतराल (न्यूनतम 5 मिनट)",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReminderSettings.PRESET_INTERVALS_MINUTES.forEach { minutes ->
                                FilterChip(
                                    selected = settings.intervalMinutes == minutes,
                                    onClick = {
                                        customIntervalText = minutes.toString()
                                        onSelectIntervalMinutes(minutes)
                                    },
                                    label = { Text(ReminderTimeCalculator.formatIntervalHindi(minutes)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronContainer,
                                        selectedLabelColor = SaffronDeep
                                    ),
                                    modifier = Modifier.testTag("interval_preset_$minutes")
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customIntervalText,
                                onValueChange = { input ->
                                    customIntervalText = input.filter { it.isDigit() }
                                },
                                label = { Text("कस्टम अंतराल (मिनट ≥ 5)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_interval_input")
                            )
                            Button(
                                onClick = {
                                    val parsed = customIntervalText.toIntOrNull() ?: 0
                                    onSelectIntervalMinutes(parsed)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("apply_custom_interval_button")
                            ) {
                                Text("सेट करें")
                            }
                        }
                    }
                }

                // 4. Sound, Vibration & Hindi Voice Reminder
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ध्वनि, कंपन और वॉइस",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        ReminderToggleRow(
                            label = "सूचना ध्वनि (Sound)",
                            checked = settings.soundEnabled,
                            onCheckedChange = onSetSoundEnabled,
                            testTag = "reminder_sound_switch"
                        )
                        ReminderToggleRow(
                            label = "कंपन (Vibration)",
                            checked = settings.vibrationEnabled,
                            onCheckedChange = onSetVibrationEnabled,
                            testTag = "reminder_vibration_switch"
                        )
                        ReminderToggleRow(
                            label = "हिंदी वॉइस रिमाइंडर (Text-to-Speech)",
                            checked = settings.hindiVoiceEnabled,
                            onCheckedChange = onSetHindiVoiceEnabled,
                            testTag = "reminder_voice_switch"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestVoice,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("test_hindi_voice_button")
                            ) {
                                Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("वॉइस सुनें")
                            }
                            OutlinedButton(
                                onClick = onSendTestNotification,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("test_notification_button")
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("टेस्ट सूचना")
                            }
                        }
                    }
                }

                // 5. Custom Reminder Message
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "कस्टम रिमाइंडर संदेश",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        OutlinedTextField(
                            value = messageDraft,
                            onValueChange = { messageDraft = it },
                            label = { Text("सूचना संदेश") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_reminder_message_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    messageDraft = ReminderSettings.DEFAULT_MESSAGE
                                    onUpdateMessage(ReminderSettings.DEFAULT_MESSAGE)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("डिफ़ॉल्ट संदेश")
                            }
                            Button(
                                onClick = { onUpdateMessage(messageDraft) },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_reminder_message_button")
                            ) {
                                Text("संदेश सहेजें")
                            }
                        }
                    }
                }

                // 6. Active Time Window & Weekdays
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "समय सीमा और सप्ताह के दिन",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showStartTimePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("start_time_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("आरंभ समय", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = ReminderTimeCalculator.formatMinutesOfDay(settings.startMinutesOfDay),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showEndTimePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("end_time_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("समाप्ति समय", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = ReminderTimeCalculator.formatMinutesOfDay(settings.endMinutesOfDay),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "सप्ताह के दिन चुनें",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkSecondary
                        )
                        val orderedDays = listOf(
                            Calendar.MONDAY,
                            Calendar.TUESDAY,
                            Calendar.WEDNESDAY,
                            Calendar.THURSDAY,
                            Calendar.FRIDAY,
                            Calendar.SATURDAY,
                            Calendar.SUNDAY
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            orderedDays.forEach { day ->
                                val selected = day in settings.selectedWeekdays
                                FilterChip(
                                    selected = selected,
                                    onClick = { onToggleWeekday(day) },
                                    label = { Text(ReminderTimeCalculator.weekdayShortHindi(day)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronContainer,
                                        selectedLabelColor = SaffronDeep
                                    ),
                                    modifier = Modifier.testTag("weekday_chip_$day")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogWrapper(
            title = "आरंभ समय चुनें",
            initialHour = settings.startMinutesOfDay / 60,
            initialMinute = settings.startMinutesOfDay % 60,
            onConfirm = { h, m ->
                onUpdateStartTime(h, m)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogWrapper(
            title = "समाप्ति समय चुनें",
            initialHour = settings.endMinutesOfDay / 60,
            initialMinute = settings.endMinutesOfDay % 60,
            onConfirm = { h, m ->
                onUpdateEndTime(h, m)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun ReminderToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = InkPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogWrapper(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(state = pickerState)
        },
        confirmButton = {
            Button(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text("चुनें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
