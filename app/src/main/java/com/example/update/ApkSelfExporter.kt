package com.example.update

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ExportedApkResult(
    val internalCopy: File,
    val validation: ApkValidationResult.Valid,
    val savedLocationHindi: String
)

object ApkSelfExporter {

    suspend fun exportInstalledApk(context: Context): Result<ExportedApkResult> =
        withContext(Dispatchers.IO) {
            try {
                val appInfo = context.applicationInfo
                val sourcePath = appInfo.publicSourceDir ?: appInfo.sourceDir
                if (sourcePath.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("ऐप का स्रोत APK पथ उपलब्ध नहीं है।")
                    )
                }

                val sourceFile = File(sourcePath)
                if (!sourceFile.exists() || !sourceFile.canRead()) {
                    return@withContext Result.failure(
                        IllegalStateException("डिवाइस पर स्थापित APK पढ़ा नहीं जा सका।")
                    )
                }

                val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                if (!exportDir.exists()) exportDir.mkdirs()

                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val vName = (pkgInfo.versionName ?: "1.0").replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val fileName = "SHYAM-v$vName.apk"
                val destFile = File(exportDir, fileName)

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val validation = ApkValidator.validateApkFile(context, destFile)
                if (validation !is ApkValidationResult.Valid) {
                    val reason = (validation as? ApkValidationResult.Invalid)?.reasonHindi
                        ?: "निर्यातित APK सत्यापित नहीं हो सका।"
                    return@withContext Result.failure(IllegalStateException(reason))
                }

                val savedLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStoreDownloads(context, destFile, fileName)
                } else {
                    "Downloads/$fileName"
                }

                Result.success(
                    ExportedApkResult(
                        internalCopy = destFile,
                        validation = validation,
                        savedLocationHindi = savedLocation
                    )
                )
            } catch (e: Throwable) {
                Result.failure(
                    IllegalStateException("APK सेव करने में त्रुटि: ${e.message ?: "अज्ञात कारण"}")
                )
            }
        }

    private fun saveToMediaStoreDownloads(
        context: Context,
        sourceApk: File,
        fileName: String
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Downloads/$fileName"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values)
            ?: return "${sourceApk.parentFile?.name ?: "Files"}/$fileName"

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceApk).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            return "Downloads/$fileName"
        } catch (_: Throwable) {
            runCatching { resolver.delete(itemUri, null, null) }
            return "${sourceApk.parentFile?.name ?: "Files"}/$fileName"
        }
    }

    fun buildShareApkIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SHYAM (श्याम) Android App APK")
            putExtra(Intent.EXTRA_TEXT, "SHYAM (श्याम) श्री राधा नाम जप Android ऐप (.apk)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "SHYAM APK शेयर करें").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
