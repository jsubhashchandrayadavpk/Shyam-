package com.example.update

import org.json.JSONObject

data class RemoteVersionInfo(
    val latestVersionName: String,
    val versionCode: Long,
    val apkDownloadUrl: String,
    val apkFileSizeBytes: Long,
    val apkFileSizeLabel: String,
    val releaseNotes: String
)

object UpdateMetadataParser {

    fun parse(jsonString: String): Result<RemoteVersionInfo> {
        return try {
            val root = JSONObject(jsonString)

            val versionName = root.optString("latestVersionName").ifBlank {
                root.optString("latest_version_name").ifBlank {
                    root.optString("versionName")
                }
            }.trim()

            val versionCode = when {
                root.has("versionCode") -> root.getLong("versionCode")
                root.has("version_code") -> root.getLong("version_code")
                else -> -1L
            }

            val apkUrl = root.optString("apkDownloadUrl").ifBlank {
                root.optString("apk_download_url").ifBlank {
                    root.optString("apkUrl")
                }
            }.trim()

            val sizeBytes = when {
                root.has("apkFileSizeBytes") -> root.optLong("apkFileSizeBytes", 0L)
                root.has("apk_file_size_bytes") -> root.optLong("apk_file_size_bytes", 0L)
                root.has("fileSizeBytes") -> root.optLong("fileSizeBytes", 0L)
                else -> 0L
            }.coerceAtLeast(0L)

            val sizeLabel = root.optString("apkFileSize").ifBlank {
                root.optString("apk_file_size")
            }.trim()

            val releaseNotes = root.optString("releaseNotes").ifBlank {
                root.optString("release_notes")
            }.trim()

            if (versionName.isBlank() || versionCode <= 0L) {
                return Result.failure(IllegalArgumentException("version.json में संस्करण जानकारी अधूरी है।"))
            }
            if (apkUrl.isBlank()) {
                return Result.failure(IllegalArgumentException("version.json में APK डाउनलोड URL उपलब्ध नहीं है।"))
            }

            Result.success(
                RemoteVersionInfo(
                    latestVersionName = versionName,
                    versionCode = versionCode,
                    apkDownloadUrl = apkUrl,
                    apkFileSizeBytes = sizeBytes,
                    apkFileSizeLabel = sizeLabel,
                    releaseNotes = releaseNotes
                )
            )
        } catch (e: Throwable) {
            Result.failure(IllegalArgumentException("अमान्य JSON फ़ाइल प्रारूप: ${e.message ?: "त्रुटि"}"))
        }
    }
}
