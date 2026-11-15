package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.counter.NaamJapCounterScreen
import com.example.ui.counter.NaamJapCounterViewModel
import com.example.ui.download.ApkDownloadScreen
import com.example.ui.download.ApkDownloadViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.reminder.NaamJapReminderScreen
import com.example.ui.reminder.NaamJapReminderViewModel
import com.example.ui.theme.ShyamTheme

object ShyamRoutes {
    const val HOME = "home"
    const val COUNTER = "counter"
    const val REMINDER = "reminder"
    const val APK_DOWNLOAD = "apk_download"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialOpenRoute = intent?.getStringExtra("open_route")
        setContent {
            ShyamTheme {
                ShyamAppRoot(initialOpenRoute = initialOpenRoute)
            }
        }
    }
}

@Composable
fun ShyamAppRoot(initialOpenRoute: String? = null) {
    val navController = rememberNavController()
    val counterViewModel: NaamJapCounterViewModel = viewModel()
    val reminderViewModel: NaamJapReminderViewModel = viewModel()
    val downloadViewModel: ApkDownloadViewModel = viewModel()

    val counterPrefs by counterViewModel.counterState.collectAsStateWithLifecycle()
    val malaCompletedSignal by counterViewModel.malaCompletedSignal.collectAsStateWithLifecycle()
    val reminderSettings by reminderViewModel.settings.collectAsStateWithLifecycle()
    val reminderError by reminderViewModel.validationError.collectAsStateWithLifecycle()
    val reminderNotice by reminderViewModel.statusNotice.collectAsStateWithLifecycle()
    val downloadUiState by downloadViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialOpenRoute) {
        if (initialOpenRoute == ShyamRoutes.COUNTER) {
            navController.navigate(ShyamRoutes.COUNTER) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ShyamRoutes.HOME
    ) {
        composable(ShyamRoutes.HOME) {
            HomeScreen(
                counterPrefs = counterPrefs,
                reminderSettings = reminderSettings,
                onOpenNaamJap = {
                    navController.navigate(ShyamRoutes.COUNTER) { launchSingleTop = true }
                },
                onOpenReminder = {
                    navController.navigate(ShyamRoutes.REMINDER) { launchSingleTop = true }
                },
                onOpenApkDownload = {
                    navController.navigate(ShyamRoutes.APK_DOWNLOAD) { launchSingleTop = true }
                }
            )
        }

        composable(ShyamRoutes.COUNTER) {
            NaamJapCounterScreen(
                state = counterPrefs,
                malaCompletedSignal = malaCompletedSignal,
                onBack = { navController.popBackStack() },
                onTapJap = counterViewModel::onTapJap,
                onUndoJap = counterViewModel::onUndoJap,
                onResetCounter = counterViewModel::onResetCounter,
                onSelectPresetMantra = counterViewModel::selectPresetMantra,
                onSetCustomMantra = counterViewModel::setCustomMantra,
                onSetMalaTarget = counterViewModel::setMalaTarget,
                onSetDailyTarget = counterViewModel::setDailyTarget,
                onSetSoundEnabled = counterViewModel::setSoundEnabled,
                onSetVibrationEnabled = counterViewModel::setVibrationEnabled,
                onSetBackgroundTheme = counterViewModel::setBackgroundTheme,
                onSetDisplayStyle = counterViewModel::setDisplayStyle
            )
        }

        composable(ShyamRoutes.REMINDER) {
            NaamJapReminderScreen(
                settings = reminderSettings,
                validationError = reminderError,
                statusNotice = reminderNotice,
                onBack = {
                    reminderViewModel.clearNotices()
                    navController.popBackStack()
                },
                onToggleEnabled = reminderViewModel::setReminderEnabled,
                onTogglePaused = reminderViewModel::togglePaused,
                onSelectIntervalMinutes = reminderViewModel::applyIntervalMinutes,
                onSetSoundEnabled = reminderViewModel::setSoundEnabled,
                onSetVibrationEnabled = reminderViewModel::setVibrationEnabled,
                onSetHindiVoiceEnabled = reminderViewModel::setHindiVoiceEnabled,
                onUpdateMessage = reminderViewModel::updateCustomMessage,
                onUpdateStartTime = reminderViewModel::updateStartTime,
                onUpdateEndTime = reminderViewModel::updateEndTime,
                onToggleWeekday = reminderViewModel::toggleWeekday,
                onTestVoice = reminderViewModel::testHindiVoice,
                onSendTestNotification = reminderViewModel::sendTestNotificationNow,
                onRefreshPermissions = reminderViewModel::refreshScheduleOnPermissionChange
            )
        }

        composable(ShyamRoutes.APK_DOWNLOAD) {
            ApkDownloadScreen(
                uiState = downloadUiState,
                onBack = {
                    downloadViewModel.clearFeedback()
                    navController.popBackStack()
                },
                onCheckForUpdates = downloadViewModel::checkForUpdates,
                onStartDownload = downloadViewModel::startApkDownload,
                onDownloadWithUrl = downloadViewModel::saveApkUrlAndStartDownload,
                onExportInstalledApk = downloadViewModel::exportInstalledApkToDevice,
                onGetShareApkIntent = downloadViewModel::prepareShareApkIntent,
                onPrepareInstall = downloadViewModel::prepareInstallApk,
                onSaveCustomUrls = downloadViewModel::saveCustomUrls,
                onResetDefaultUrls = downloadViewModel::resetUrlsToDefault,
                onRefreshInstallPermission = downloadViewModel::refreshInstallPermissionState
            )
        }
    }
}
