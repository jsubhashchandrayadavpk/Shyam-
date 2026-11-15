package com.example.ui.download

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShyamApplication
import com.example.data.ShyamPreferencesRepository
import com.example.update.ApkSelfExporter
import com.example.update.ApkValidationResult
import com.example.update.ApkValidator
import com.example.update.AppUpdateConfig
import com.example.update.DownloadProgressState
import com.example.update.RemoteVersionInfo
import com.example.update.ShyamApkUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpdateAvailable(val info: RemoteVersionInfo) : UpdateCheckState()
    data class UpToDate(val info: RemoteVersionInfo?) : UpdateCheckState()
    data class Error(val messageHindi: String) : UpdateCheckState()
}

sealed class InstallLaunchResult {
    data class NeedsUnknownSourcesPermission(val settingsIntent: Intent) : InstallLaunchResult()
    data class LaunchInstaller(val installIntent: Intent) : InstallLaunchResult()
    data class Error(val messageHindi: String) : InstallLaunchResult()
}

data class ApkDownloadUiState(
    val installedVersionName: String = "1.0",
    val installedVersionCode: Long = 1L,
    val effectiveVersionJsonUrl: String = AppUpdateConfig.VERSION_CHECK_JSON_URL,
    val effectiveApkDownloadUrl: String = AppUpdateConfig.APK_DOWNLOAD_URL,
    val isApkUrlConfigured: Boolean = false,
    val isVersionJsonConfigured: Boolean = false,
    val updateCheckState: UpdateCheckState = UpdateCheckState.Idle,
    val downloadState: DownloadProgressState = DownloadProgressState.Idle,
    val canInstallUnknownApps: Boolean = true,
    val isExportingLocalApk: Boolean = false,
    val exportedApkMessageHindi: String? = null,
    val exportedApkFilePath: String? = null,
    val actionFeedbackHindi: String? = null
) {
    val isPlaceholderMode: Boolean
        get() = !isApkUrlConfigured && !isVersionJsonConfigured
}

class ApkDownloadViewModel(
    application: Application,
    private val repository: ShyamPreferencesRepository,
    private val updateManager: ShyamApkUpdateManager
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        repository = ShyamApplication.getRepository(application),
        updateManager = ShyamApkUpdateManager(application)
    )

    private val _uiState = MutableStateFlow(ApkDownloadUiState())
    val uiState: StateFlow<ApkDownloadUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadInstalledPackageInfo()
        observeConfigAndExistingApk()
    }

    private fun loadInstalledPackageInfo() {
        val ctx = getApplication<Application>()
        try {
            val pkgInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            _uiState.update {
                it.copy(
                    installedVersionName = pkgInfo.versionName ?: "1.0",
                    installedVersionCode = vCode,
                    canInstallUnknownApps = updateManager.canRequestPackageInstalls()
                )
            }
        } catch (_: Throwable) {
            // Keep default 1.0 / 1
        }
    }

    private fun observeConfigAndExistingApk() {
        viewModelScope.launch {
            repository.updateUrlOverridesFlow.collect { overrides ->
                val jsonUrl = overrides.customVersionJsonUrl.ifBlank { AppUpdateConfig.VERSION_CHECK_JSON_URL }
                val apkUrl = overrides.customApkDownloadUrl.ifBlank { AppUpdateConfig.APK_DOWNLOAD_URL }

                _uiState.update {
                    it.copy(
                        effectiveVersionJsonUrl = jsonUrl,
                        effectiveApkDownloadUrl = apkUrl,
                        isApkUrlConfigured = AppUpdateConfig.isValidHttpsUrl(apkUrl),
                        isVersionJsonConfigured = AppUpdateConfig.isValidHttpsUrl(jsonUrl)
                    )
                }

                if (overrides.lastDownloadId > 0L && pollingJob == null) {
                    val targetFile = updateManager.getTargetApkFile()
                    val currentProgress = updateManager.queryDownloadState(overrides.lastDownloadId, targetFile)
                    if (currentProgress is DownloadProgressState.Downloading) {
                        startPolling(overrides.lastDownloadId, targetFile)
                    } else if (currentProgress is DownloadProgressState.Completed) {
                        _uiState.update { it.copy(downloadState = currentProgress) }
                    }
                }
            }
        }

        viewModelScope.launch {
            val file = updateManager.getTargetApkFile()
            if (file.exists()) {
                val result = ApkValidator.validateApkFile(getApplication(), file)
                if (result is ApkValidationResult.Valid) {
                    _uiState.update {
                        it.copy(downloadState = DownloadProgressState.Completed(file, result))
                    }
                }
            }
        }
    }

    fun refreshInstallPermissionState() {
        _uiState.update {
            it.copy(canInstallUnknownApps = updateManager.canRequestPackageInstalls())
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(actionFeedbackHindi = null) }
    }

    fun saveCustomUrls(versionJsonUrl: String, apkDownloadUrl: String) {
        viewModelScope.launch {
            repository.saveUpdateUrlOverrides(versionJsonUrl, apkDownloadUrl)
            _uiState.update {
                it.copy(
                    actionFeedbackHindi = "HTTPS / Google Drive लिंक सहेजे गए।",
                    updateCheckState = UpdateCheckState.Idle,
                    downloadState = if (it.downloadState is DownloadProgressState.Failed) {
                        DownloadProgressState.Idle
                    } else {
                        it.downloadState
                    }
                )
            }
        }
    }

    fun saveApkUrlAndStartDownload(apkUrlInput: String) {
        val cleaned = apkUrlInput.trim()
        if (!AppUpdateConfig.isValidHttpsUrl(cleaned)) {
            _uiState.update {
                it.copy(
                    downloadState = DownloadProgressState.Failed(
                        "कृपया मान्य HTTPS या Google Drive लिंक दर्ज करें (https:// से शुरू)।"
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            val currentJson = if (_uiState.value.isVersionJsonConfigured) {
                _uiState.value.effectiveVersionJsonUrl
            } else {
                ""
            }
            repository.saveUpdateUrlOverrides(currentJson, cleaned)
            startDownloadWithUrl(cleaned)
        }
    }

    fun resetUrlsToDefault() {
        viewModelScope.launch {
            repository.saveUpdateUrlOverrides("", "")
            _uiState.update {
                it.copy(
                    actionFeedbackHindi = "URL डिफ़ॉल्ट पर रीसेट किए गए।",
                    updateCheckState = UpdateCheckState.Idle,
                    downloadState = DownloadProgressState.Idle
                )
            }
        }
    }

    fun checkForUpdates() {
        val currentState = _uiState.value
        val jsonUrl = currentState.effectiveVersionJsonUrl
        if (!AppUpdateConfig.isValidHttpsUrl(jsonUrl)) {
            _uiState.update {
                it.copy(
                    updateCheckState = UpdateCheckState.Error(
                        "कृपया नीचे VERSION_CHECK_JSON_URL में अपनी version.json फ़ाइल का HTTPS या Google Drive लिंक दर्ज करें।"
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(updateCheckState = UpdateCheckState.Checking, actionFeedbackHindi = null)
        }

        viewModelScope.launch {
            val result = updateManager.fetchRemoteVersion(jsonUrl)
            result.fold(
                onSuccess = { remoteInfo ->
                    val installedCode = _uiState.value.installedVersionCode
                    if (remoteInfo.versionCode > installedCode) {
                        _uiState.update {
                            it.copy(
                                updateCheckState = UpdateCheckState.UpdateAvailable(remoteInfo),
                                effectiveApkDownloadUrl = remoteInfo.apkDownloadUrl.ifBlank { it.effectiveApkDownloadUrl },
                                isApkUrlConfigured = AppUpdateConfig.isValidHttpsUrl(
                                    remoteInfo.apkDownloadUrl.ifBlank { it.effectiveApkDownloadUrl }
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(updateCheckState = UpdateCheckState.UpToDate(remoteInfo))
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updateCheckState = UpdateCheckState.Error(
                                error.message ?: "अपडेट जांचने में त्रुटि हुई।"
                            )
                        )
                    }
                }
            )
        }
    }

    fun startApkDownload() {
        val state = _uiState.value
        val remoteApkUrl = (state.updateCheckState as? UpdateCheckState.UpdateAvailable)
            ?.info?.apkDownloadUrl?.takeIf { AppUpdateConfig.isValidHttpsUrl(it) }
        val urlToDownload = remoteApkUrl ?: state.effectiveApkDownloadUrl
        startDownloadWithUrl(urlToDownload)
    }

    private fun startDownloadWithUrl(urlToDownload: String) {
        val startResult = updateManager.startApkDownload(urlToDownload)
        startResult.fold(
            onSuccess = { (downloadId, targetFile) ->
                viewModelScope.launch {
                    repository.saveLastDownloadInfo(downloadId, targetFile.absolutePath)
                }
                startPolling(downloadId, targetFile)
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        downloadState = DownloadProgressState.Failed(
                            err.message ?: "APK डाउनलोड शुरू नहीं हो सका।"
                        )
                    )
                }
            }
        )
    }

    fun exportInstalledApkToDevice() {
        if (_uiState.value.isExportingLocalApk) return
        _uiState.update {
            it.copy(isExportingLocalApk = true, exportedApkMessageHindi = null)
        }
        viewModelScope.launch {
            val result = ApkSelfExporter.exportInstalledApk(getApplication())
            result.fold(
                onSuccess = { exported ->
                    _uiState.update {
                        it.copy(
                            isExportingLocalApk = false,
                            exportedApkFilePath = exported.internalCopy.absolutePath,
                            exportedApkMessageHindi = "SHYAM APK सफलतापूर्वक सेव हुआ: ${exported.savedLocationHindi} (${ShyamApkUpdateManager.formatBytes(exported.validation.fileSizeBytes)})",
                            downloadState = DownloadProgressState.Completed(
                                exported.internalCopy,
                                exported.validation
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isExportingLocalApk = false,
                            exportedApkMessageHindi = err.message ?: "APK निर्यात विफल रहा।"
                        )
                    }
                }
            )
        }
    }

    fun prepareShareApkIntent(): Intent? {
        val path = _uiState.value.exportedApkFilePath
            ?: (_uiState.value.downloadState as? DownloadProgressState.Completed)?.file?.absolutePath
            ?: return null
        val file = java.io.File(path)
        if (!file.exists()) return null
        return ApkSelfExporter.buildShareApkIntent(getApplication(), file)
    }

    private fun startPolling(downloadId: Long, targetFile: java.io.File) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                val progress = updateManager.queryDownloadState(downloadId, targetFile)
                _uiState.update { it.copy(downloadState = progress) }
                if (progress is DownloadProgressState.Completed || progress is DownloadProgressState.Failed || progress is DownloadProgressState.Idle) {
                    if (progress is DownloadProgressState.Completed) {
                        repository.saveLastDownloadInfo(-1L, targetFile.absolutePath)
                    } else if (progress is DownloadProgressState.Failed) {
                        repository.saveLastDownloadInfo(-1L, "")
                    }
                    break
                }
                delay(450L)
            }
        }
    }

    fun prepareInstallApk(): InstallLaunchResult {
        val completed = _uiState.value.downloadState as? DownloadProgressState.Completed
            ?: return InstallLaunchResult.Error("इंस्टॉल करने से पहले कृपया वैध SHYAM APK डाउनलोड या सेव करें।")

        val validation = ApkValidator.validateApkFile(getApplication(), completed.file)
        if (validation !is ApkValidationResult.Valid) {
            val reason = (validation as? ApkValidationResult.Invalid)?.reasonHindi
                ?: "APK फ़ाइल अमान्य है।"
            _uiState.update { it.copy(downloadState = DownloadProgressState.Failed(reason)) }
            return InstallLaunchResult.Error(reason)
        }

        if (!updateManager.canRequestPackageInstalls()) {
            _uiState.update { it.copy(canInstallUnknownApps = false) }
            return InstallLaunchResult.NeedsUnknownSourcesPermission(
                updateManager.buildUnknownSourcesPermissionIntent()
            )
        }

        return try {
            InstallLaunchResult.LaunchInstaller(updateManager.buildInstallApkIntent(completed.file))
        } catch (e: Throwable) {
            InstallLaunchResult.Error("Android पैकेज इंस्टॉलर खुल नहीं सका: ${e.message ?: "त्रुटि"}")
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
