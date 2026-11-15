package com.example.update

import java.net.URI

/**
 * Resolves direct HTTPS links as well as Google Drive sharing URLs into direct download URLs.
 *
 * Supported Google Drive formats:
 * - https://drive.google.com/file/d/<FILE_ID>/view?usp=sharing
 * - https://drive.google.com/open?id=<FILE_ID>
 * - https://drive.google.com/uc?id=<FILE_ID>&export=download
 * - https://drive.usercontent.google.com/download?id=<FILE_ID>&export=download
 */
object GoogleDriveUrlResolver {

    private val fileIdFromPathRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
    private val idQueryRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")

    fun resolveDirectDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return trimmed

        val driveId = extractGoogleDriveFileId(trimmed) ?: return trimmed
        return "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t"
    }

    fun isGoogleDriveUrl(rawUrl: String): Boolean {
        val lower = rawUrl.trim().lowercase()
        return lower.contains("drive.google.com") || lower.contains("drive.usercontent.google.com")
    }

    fun extractGoogleDriveFileId(rawUrl: String): String? {
        if (!isGoogleDriveUrl(rawUrl)) return null
        val uriHost = try {
            URI(rawUrl.trim()).host?.lowercase().orEmpty()
        } catch (_: Throwable) {
            ""
        }
        if (uriHost != "drive.google.com" && uriHost != "drive.usercontent.google.com") {
            return null
        }

        fileIdFromPathRegex.find(rawUrl)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.isNotBlank()) return id
        }
        idQueryRegex.find(rawUrl)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.isNotBlank()) return id
        }
        return null
    }
}
