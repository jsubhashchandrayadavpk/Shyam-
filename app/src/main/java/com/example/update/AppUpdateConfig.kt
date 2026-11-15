package com.example.update

/**
 * SHYAM APK डाउनलोड और अपडेट कॉन्फ़िगरेशन।
 *
 * महत्वपूर्ण निर्देश (बिना GitHub खाते के):
 * 1. अपना SHYAM APK (Debug या Release) Google Drive या किसी भी HTTPS सर्वर पर अपलोड करें।
 * 2. Google Drive पर फ़ाइल की शेयरिंग "Anyone with the link (Viewer)" सेट करें और उसका लिंक कॉपी करें।
 * 3. नीचे APK_DOWNLOAD_URL (और यदि अपडेट चेकर उपयोग करें तो VERSION_CHECK_JSON_URL) में अपना लिंक डालें।
 *    ऐप Google Drive शेयर लिंक को स्वतः डायरेक्ट डाउनलोड URL में बदल देता है।
 */
object AppUpdateConfig {

    /**
     * होस्ट की गई वास्तविक SHYAM APK फ़ाइल का सीधा HTTPS या Google Drive शेयर लिंक।
     */
    const val APK_DOWNLOAD_URL: String =
        "https://YOUR_HOSTING_DOMAIN_HERE/shyam/SHYAM-release.apk"

    /**
     * रिमोट version.json फ़ाइल का सीधा HTTPS या Google Drive शेयर लिंक।
     */
    const val VERSION_CHECK_JSON_URL: String =
        "https://YOUR_HOSTING_DOMAIN_HERE/shyam/version.json"

    fun isPlaceholderUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.isEmpty() ||
            trimmed.contains("YOUR_HOSTING_DOMAIN_HERE", ignoreCase = true) ||
            trimmed.contains("YOUR_DOMAIN", ignoreCase = true)
    }

    fun isValidHttpsUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (isPlaceholderUrl(trimmed)) return false
        return try {
            val uri = android.net.Uri.parse(trimmed)
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        } catch (_: Throwable) {
            false
        }
    }
}
