package com.example.ui.download

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
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
import com.example.update.AppUpdateConfig
import com.example.update.DownloadProgressState
import com.example.update.ShyamApkUpdateManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkDownloadScreen(
    uiState: ApkDownloadUiState,
    onBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onStartDownload: () -> Unit,
    onDownloadWithUrl: (String) -> Unit,
    onExportInstalledApk: () -> Unit,
    onGetShareApkIntent: () -> android.content.Intent?,
    onPrepareInstall: () -> InstallLaunchResult,
    onSaveCustomUrls: (String, String) -> Unit,
    onResetDefaultUrls: () -> Unit,
    onRefreshInstallPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var localInstallError by remember { mutableStateOf<String?>(null) }
    var showAdvancedJsonConfig by remember { mutableStateOf(false) }

    var apkUrlInput by remember(uiState.effectiveApkDownloadUrl) {
        mutableStateOf(
            if (AppUpdateConfig.isPlaceholderUrl(uiState.effectiveApkDownloadUrl)) ""
            else uiState.effectiveApkDownloadUrl
        )
    }
    var jsonUrlInput by remember(uiState.effectiveVersionJsonUrl) {
        mutableStateOf(
            if (AppUpdateConfig.isPlaceholderUrl(uiState.effectiveVersionJsonUrl)) ""
            else uiState.effectiveVersionJsonUrl
        )
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshInstallPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshInstallPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                        modifier = Modifier.testTag("download_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "वापस जाएं"
                        )
                    }
                },
                title = {
                    Text(
                        text = "SHYAM APK डाउनलोड करें",
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
                // 1. App Logo & Version Metadata Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(2.dp, GoldAccent),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.shyam_launcher_fg),
                                    contentDescription = "SHYAM ऐप लोगो",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "SHYAM",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "श्याम • श्री राधा नाम जप ऐप",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                        }

                        val remoteInfo = when (val u = uiState.updateCheckState) {
                            is UpdateCheckState.UpdateAvailable -> u.info
                            is UpdateCheckState.UpToDate -> u.info
                            else -> null
                        }

                        val latestVersionText = remoteInfo?.let {
                            "v${it.latestVersionName} (कोड ${it.versionCode})"
                        } ?: "वर्तमान बिल्ड: v${uiState.installedVersionName}"

                        val fileSizeText = when {
                            remoteInfo != null && remoteInfo.apkFileSizeLabel.isNotBlank() ->
                                remoteInfo.apkFileSizeLabel
                            remoteInfo != null && remoteInfo.apkFileSizeBytes > 0L ->
                                ShyamApkUpdateManager.formatBytes(remoteInfo.apkFileSizeBytes)
                            uiState.downloadState is DownloadProgressState.Completed ->
                                ShyamApkUpdateManager.formatBytes(uiState.downloadState.validation.fileSizeBytes)
                            else -> "डाउनलोड / सेव करने पर दिखेगा"
                        }

                        VersionRow("वर्तमान स्थापित संस्करण", "v${uiState.installedVersionName} (कोड ${uiState.installedVersionCode})")
                        VersionRow("उपलब्ध संस्करण", latestVersionText)
                        VersionRow("APK फ़ाइल आकार", fileSizeText)
                    }
                }

                // 2. Direct Local APK Export & Share (Works offline, zero GitHub)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamCard),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SHYAM APK सेव व शेयर करें (ऑफ़लाइन)",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = "बिना किसी वेबसाइट या GitHub के सीधे इस ऐप की असली .apk फ़ाइल अपने फ़ोन के Downloads फ़ोल्डर में सेव करें या WhatsApp / Google Drive / Bluetooth से भेजें।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )

                        uiState.exportedApkMessageHindi?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TulsiContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TulsiGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onExportInstalledApk,
                                enabled = !uiState.isExportingLocalApk,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("export_local_apk_button")
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isExportingLocalApk) "सेव हो रहा..." else "APK सेव करें",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (uiState.downloadState is DownloadProgressState.Completed || uiState.exportedApkFilePath != null) {
                                OutlinedButton(
                                    onClick = {
                                        onGetShareApkIntent()?.let { shareIntent ->
                                            runCatching { context.startActivity(shareIntent) }
                                                .onFailure { localInstallError = "शेयर मेनू खुल नहीं सका।" }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("share_apk_button")
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("शेयर करें")
                                }
                            }
                        }
                    }
                }

                // 3. Online APK Download (Google Drive + Direct HTTPS) & Install Card
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
                            text = "ऑनलाइन APK डाउनलोड एवं इंस्टॉल",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = "Google Drive शेयर लिंक (Anyone with the link) या कोई भी सीधा HTTPS .apk लिंक पेस्ट करें। ऐप लिंक को स्वतः डायरेक्ट डाउनलोड में बदलकर वास्तविक प्रगति के साथ इंस्टॉल के लिए तैयार करता है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )

                        OutlinedTextField(
                            value = apkUrlInput,
                            onValueChange = { apkUrlInput = it },
                            label = { Text("APK लिंक (Google Drive या HTTPS .apk)") },
                            placeholder = { Text("https://drive.google.com/file/d/.../view") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_apk_url_input")
                        )

                        when (val dState = uiState.downloadState) {
                            is DownloadProgressState.Idle -> Unit
                            is DownloadProgressState.Downloading -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = dState.statusTextHindi,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SaffronDeep,
                                        modifier = Modifier.testTag("download_progress_text")
                                    )
                                    if (dState.totalBytes > 0L) {
                                        LinearProgressIndicator(
                                            progress = { dState.progressFraction },
                                            color = SaffronPrimary,
                                            trackColor = SaffronContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                        )
                                        Text(
                                            text = "${ShyamApkUpdateManager.formatBytes(dState.bytesDownloaded)} / ${ShyamApkUpdateManager.formatBytes(dState.totalBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkSecondary
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            color = SaffronPrimary,
                                            trackColor = SaffronContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                        )
                                    }
                                }
                            }
                            is DownloadProgressState.Completed -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = TulsiContainer,
                                    border = BorderStroke(1.dp, TulsiGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = TulsiGreen
                                        )
                                        Column {
                                            Text(
                                                text = "वैध SHYAM APK तैयार और सत्यापित है",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TulsiGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("download_completed_text")
                                            )
                                            Text(
                                                text = "पैकेज: ${dState.validation.packageName} • v${dState.validation.versionName} (${ShyamApkUpdateManager.formatBytes(dState.validation.fileSizeBytes)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            is DownloadProgressState.Failed -> {
                                ErrorBanner(
                                    message = dState.errorHindi,
                                    testTag = "download_error_text"
                                )
                            }
                        }

                        localInstallError?.let { err ->
                            ErrorBanner(message = err, testTag = "install_error_text")
                        }

                        if (!uiState.canInstallUnknownApps) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = GoldContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, tint = SaffronDeep)
                                    Text(
                                        text = "APK इंस्टॉल करने के लिए Android की \"अज्ञात ऐप्स इंस्टॉल करें (Install unknown apps)\" अनुमति आवश्यक है। नीचे \"APK इंस्टॉल करें\" दबाने पर सेटिंग्स खुलेंगी।",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }

                        val isUpdateAvailable = uiState.updateCheckState is UpdateCheckState.UpdateAvailable
                        val downloadButtonLabel = if (isUpdateAvailable) {
                            stringResource(R.string.download_update_cta)
                        } else {
                            "SHYAM APK डाउनलोड करें"
                        }

                        Button(
                            onClick = {
                                localInstallError = null
                                val trimmedInput = apkUrlInput.trim()
                                if (trimmedInput.isNotBlank()) {
                                    onDownloadWithUrl(trimmedInput)
                                } else {
                                    onStartDownload()
                                }
                            },
                            enabled = uiState.downloadState !is DownloadProgressState.Downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("download_apk_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(downloadButtonLabel, fontWeight = FontWeight.Bold)
                        }

                        if (uiState.downloadState is DownloadProgressState.Completed) {
                            Button(
                                onClick = {
                                    localInstallError = null
                                    when (val action = onPrepareInstall()) {
                                        is InstallLaunchResult.NeedsUnknownSourcesPermission -> {
                                            runCatching {
                                                unknownSourcesLauncher.launch(action.settingsIntent)
                                            }.onFailure {
                                                localInstallError = "इंस्टॉल अनुमति सेटिंग्स खुल नहीं सकीं।"
                                            }
                                        }
                                        is InstallLaunchResult.LaunchInstaller -> {
                                            runCatching {
                                                context.startActivity(action.installIntent)
                                            }.onFailure {
                                                localInstallError = "Android पैकेज इंस्टॉलर शुरू नहीं हो सका।"
                                            }
                                        }
                                        is InstallLaunchResult.Error -> {
                                            localInstallError = action.messageHindi
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TulsiGreen),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("install_apk_button")
                            ) {
                                Icon(Icons.Rounded.InstallMobile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("APK इंस्टॉल करें", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. Remote Version Update Checker Card
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
                            Text(
                                text = "नया अपडेट जांचें (version.json)",
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showAdvancedJsonConfig = !showAdvancedJsonConfig },
                                modifier = Modifier.testTag("toggle_url_config_button")
                            ) {
                                Icon(
                                    imageVector = if (showAdvancedJsonConfig) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = "version.json URL सेटिंग्स"
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAdvancedJsonConfig || !uiState.isVersionJsonConfigured) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = jsonUrlInput,
                                    onValueChange = { jsonUrlInput = it },
                                    label = { Text("version.json लिंक (Google Drive / HTTPS)") },
                                    placeholder = { Text("https://example.com/version.json") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_json_url_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            jsonUrlInput = ""
                                            apkUrlInput = ""
                                            onResetDefaultUrls()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("रीसेट")
                                    }
                                    Button(
                                        onClick = { onSaveCustomUrls(jsonUrlInput, apkUrlInput) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("save_custom_urls_button")
                                    ) {
                                        Text("लिंक सहेजें")
                                    }
                                }
                            }
                        }

                        when (val checkState = uiState.updateCheckState) {
                            is UpdateCheckState.Idle -> {
                                Text(
                                    text = "Google Drive या HTTPS पर होस्ट की गई version.json से नया संस्करण जांचें।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                            is UpdateCheckState.Checking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = SaffronPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text("नवीनतम संस्करण की जांच हो रही है...")
                                }
                            }
                            is UpdateCheckState.UpdateAvailable -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SaffronContainer,
                                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_available_title) +
                                                " (v${checkState.info.latestVersionName})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SaffronDeep,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("update_available_text")
                                        )
                                        if (checkState.info.releaseNotes.isNotBlank()) {
                                            Text(
                                                text = checkState.info.releaseNotes,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = InkPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            is UpdateCheckState.UpToDate -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = TulsiContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = TulsiGreen
                                        )
                                        Text(
                                            text = stringResource(R.string.up_to_date_message),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TulsiGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.testTag("up_to_date_text")
                                        )
                                    }
                                }
                            }
                            is UpdateCheckState.Error -> {
                                ErrorBanner(
                                    message = checkState.messageHindi,
                                    testTag = "update_error_text"
                                )
                            }
                        }

                        uiState.actionFeedbackHindi?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TulsiGreen
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (jsonUrlInput.isNotBlank()) {
                                    onSaveCustomUrls(jsonUrlInput, apkUrlInput)
                                }
                                onCheckForUpdates()
                            },
                            enabled = uiState.updateCheckState !is UpdateCheckState.Checking,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_updates_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("अपडेट जांचें (Check for Updates)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun VersionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = InkPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorBanner(message: String, testTag: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AlertRedContainer,
        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = AlertRed)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AlertRed,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
