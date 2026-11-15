package com.example.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream

sealed class ApkValidationResult {
    data class Valid(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val fileSizeBytes: Long
    ) : ApkValidationResult()

    data class Invalid(val reasonHindi: String) : ApkValidationResult()
}

object ApkValidator {

    /**
     * Checks ZIP magic bytes (PK\x03\x04) and verifies Android PackageManager can parse
     * the archive's manifest so HTML/PDF/corrupted files are rejected cleanly.
     */
    fun validateApkFile(context: Context, file: File): ApkValidationResult {
        if (!file.exists() || !file.isFile) {
            return ApkValidationResult.Invalid("डाउनलोड की गई APK फ़ाइल नहीं मिली।")
        }
        val length = file.length()
        if (length < 1024L) {
            return ApkValidationResult.Invalid("डाउनलोड की गई फ़ाइल बहुत छोटी या अधूरी है।")
        }

        val header = ByteArray(4)
        val bytesRead = try {
            FileInputStream(file).use { it.read(header) }
        } catch (_: Throwable) {
            -1
        }
        val isZipHeader = bytesRead == 4 &&
            header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()

        if (!isZipHeader) {
            return ApkValidationResult.Invalid("यह वैध Android APK पैकेज नहीं है (संभवतः HTML या PDF फ़ाइल प्राप्त हुई)।")
        }

        val pm = context.packageManager
        val pkgInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, 0)
            }
        } catch (_: Throwable) {
            null
        }

        if (pkgInfo == null || pkgInfo.packageName.isNullOrBlank()) {
            return ApkValidationResult.Invalid("APK पैकेज मैनिफेस्ट सत्यापित नहीं हो सका। कृपया पुनः डाउनलोड करें।")
        }

        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }

        return ApkValidationResult.Valid(
            packageName = pkgInfo.packageName,
            versionName = pkgInfo.versionName ?: "अज्ञात",
            versionCode = vCode,
            fileSizeBytes = length
        )
    }
}
