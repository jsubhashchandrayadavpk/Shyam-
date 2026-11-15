package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class DownloadProgressState {
    data object Idle : DownloadProgressState()
    data class Downloading(
        val downloadId: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressFraction: Float,
        val statusTextHindi: String
    ) : DownloadProgressState()

    data class Completed(
        val file: File,
        val validation: ApkValidationResult.Valid
    ) : DownloadProgressState()

    data class Failed(val errorHindi: String) : DownloadProgressState()
}

class ShyamApkUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchRemoteVersion(versionJsonUrl: String): Result<RemoteVersionInfo> =
        withContext(Dispatchers.IO) {
            if (AppUpdateConfig.isPlaceholderUrl(versionJsonUrl)) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "अपडेट सर्वर URL अभी कॉन्फ़िगर नहीं है। कृपया AppUpdateConfig.kt या नीचे URL कॉन्फ़िगरेशन में वास्तविक version.json HTTPS लिंक दर्ज करें।"
                    )
                )
            }
            if (!AppUpdateConfig.isValidHttpsUrl(versionJsonUrl)) {
                return@withContext Result.failure(
                    IllegalArgumentException("कृपया एक मान्य HTTPS version.json URL दर्ज करें।")
                )
            }
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(
                    IllegalStateException("इंटरनेट कनेक्शन उपलब्ध नहीं है। कृपया नेटवर्क जांचें।")
                )
            }

            val resolvedUrl = GoogleDriveUrlResolver.resolveDirectDownloadUrl(versionJsonUrl)
            try {
                val request = Request.Builder()
                    .url(resolvedUrl)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("सर्वर त्रुटि (HTTP ${response.code})। कृपया कुछ समय बाद पुनः प्रयास करें।")
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("सर्वर से खाली उत्तर प्राप्त हुआ।")
                        )
                    }
                    UpdateMetadataParser.parse(body)
                }
            } catch (_: UnknownHostException) {
                Result.failure(IllegalStateException("सर्वर से संपर्क नहीं हो सका। कृपया URL और इंटरनेट जांचें।"))
            } catch (_: SocketTimeoutException) {
                Result.failure(IllegalStateException("सर्वर से प्रतिक्रिया में समय समाप्त हो गया। कृपया पुनः प्रयास करें।"))
            } catch (e: Throwable) {
                Result.failure(IllegalStateException("अपडेट जांच विफल रही: ${e.message ?: "अज्ञात त्रुटि"}"))
            }
        }

    fun getTargetApkFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "SHYAM-update.apk")
    }

    fun startApkDownload(apkUrl: String): Result<Pair<Long, File>> {
        if (AppUpdateConfig.isPlaceholderUrl(apkUrl)) {
            return Result.failure(
                IllegalStateException(
                    "कृपया ऊपर बॉक्स में अपना Google Drive या HTTPS .apk डाउनलोड लिंक दर्ज करें, या ऑफ़लाइन सेव बटन से APK प्राप्त करें।"
                )
            )
        }
        if (!AppUpdateConfig.isValidHttpsUrl(apkUrl)) {
            return Result.failure(
                IllegalArgumentException("अमान्य APK URL। केवल मान्य HTTPS लिंक स्वीकार किए जाते हैं।")
            )
        }
        if (!isNetworkAvailable()) {
            return Result.failure(
                IllegalStateException("इंटरनेट कनेक्शन नहीं है। डाउनलोड शुरू करने के लिए इंटरनेट चालू करें।")
            )
        }

        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return Result.failure(IllegalStateException("DownloadManager उपलब्ध नहीं है।"))

            val targetFile = getTargetApkFile()
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val resolvedApkUrl = GoogleDriveUrlResolver.resolveDirectDownloadUrl(apkUrl)
            val request = DownloadManager.Request(Uri.parse(resolvedApkUrl))
                .setTitle("SHYAM APK डाउनलोड")
                .setDescription("SHYAM नवीनतम Android ऐप पैकेज")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    targetFile.name
                )

            val downloadId = dm.enqueue(request)
            Result.success(downloadId to targetFile)
        } catch (e: Throwable) {
            Result.failure(IllegalStateException("डाउनलोड प्रारंभ नहीं हो सका: ${e.message ?: "त्रुटि"}"))
        }
    }

    fun queryDownloadState(downloadId: Long, expectedFile: File): DownloadProgressState {
        if (downloadId <= 0L) return DownloadProgressState.Idle
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return DownloadProgressState.Failed("DownloadManager उपलब्ध नहीं है।")

        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return DownloadProgressState.Failed("डाउनलोड रद्द या हटाया गया है।")
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

            return when (status) {
                DownloadManager.STATUS_PENDING -> DownloadProgressState.Downloading(
                    downloadId = downloadId,
                    bytesDownloaded = downloaded.coerceAtLeast(0L),
                    totalBytes = total.coerceAtLeast(0L),
                    progressFraction = 0f,
                    statusTextHindi = "डाउनलोड की प्रतीक्षा हो रही है..."
                )

                DownloadManager.STATUS_PAUSED -> DownloadProgressState.Downloading(
                    downloadId = downloadId,
                    bytesDownloaded = downloaded.coerceAtLeast(0L),
                    totalBytes = total.coerceAtLeast(0L),
                    progressFraction = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f,
                    statusTextHindi = "डाउनलोड रुका हुआ है..."
                )

                DownloadManager.STATUS_RUNNING -> {
                    val fraction = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    DownloadProgressState.Downloading(
                        downloadId = downloadId,
                        bytesDownloaded = downloaded.coerceAtLeast(0L),
                        totalBytes = total.coerceAtLeast(0L),
                        progressFraction = fraction,
                        statusTextHindi = if (total > 0L) {
                            "डाउनलोड हो रहा है: ${(fraction * 100).toInt()}%"
                        } else {
                            "डाउनलोड हो रहा है: ${formatBytes(downloaded)}"
                        }
                    )
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    when (val validation = ApkValidator.validateApkFile(context, expectedFile)) {
                        is ApkValidationResult.Valid -> DownloadProgressState.Completed(expectedFile, validation)
                        is ApkValidationResult.Invalid -> DownloadProgressState.Failed(validation.reasonHindi)
                    }
                }

                DownloadManager.STATUS_FAILED -> DownloadProgressState.Failed(mapFailureReason(reason))
                else -> DownloadProgressState.Idle
            }
        }
        return DownloadProgressState.Idle
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildUnknownSourcesPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun buildInstallApkIntent(apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun mapFailureReason(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "डिवाइस में पर्याप्त स्टोरेज स्थान नहीं है।"
        DownloadManager.ERROR_HTTP_DATA_ERROR,
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "सर्वर से APK फ़ाइल प्राप्त करने में HTTP त्रुटि हुई।"
        DownloadManager.ERROR_CANNOT_RESUME -> "डाउनलोड पुनः शुरू नहीं हो सका। कृपया पुनः प्रयास करें।"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "अत्यधिक रीडायरेक्ट के कारण डाउनलोड विफल हुआ।"
        404 -> "सर्वर पर APK फ़ाइल नहीं मिली (404)।"
        else -> "डाउनलोड विफल हुआ (कोड: $reason)। कृपया URL और इंटरनेट जांचें।"
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "अज्ञात"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
