# SHYAM (श्याम) — APK डाउनलोड, Google Drive होस्टिंग और अपडेट गाइड

## 1. GitHub के बिना Google Drive पर APK कैसे होस्ट करें (सबसे आसान तरीका)

1. Google Drive में अपना `SHYAM` APK अपलोड करें।
2. फ़ाइल पर दायाँ क्लिक / 3-बिंदु मेनू दबाएं → **Share (शेयर करें)** → **General access** को **Anyone with the link (Viewer)** चुनें।
3. **Copy link** दबाएं (जैसे `https://drive.google.com/file/d/<FILE_ID>/view?usp=sharing`)।
4. SHYAM ऐप में **SHYAM APK डाउनलोड करें → होस्टिंग एवं URL कॉन्फ़िगरेशन** खोलें और `APK_DOWNLOAD_URL` में वही लिंक पेस्ट करके **URL सहेजें** दबाएं (या स्थायी रूप से `AppUpdateConfig.kt` में डालें)।
   - ऐप Google Drive के शेयर लिंक को स्वतः डायरेक्ट बाइनरी डाउनलोड लिंक (`drive.usercontent.google.com`) में बदल देता है।

---

## 2. भविष्य के अपडेट के लिए `version.json` सेटअप

1. `distribution/version.json` को अपने Google Drive (Anyone with the link) या किसी भी HTTPS सर्वर पर अपलोड करें:
   ```json
   {
     "latestVersionName": "1.1.0",
     "versionCode": 2,
     "apkDownloadUrl": "https://drive.google.com/file/d/YOUR_NEW_APK_FILE_ID/view?usp=sharing",
     "apkFileSizeBytes": 12450816,
     "apkFileSize": "11.87 MB",
     "releaseNotes": "• नया अपडेट विवरण"
   }
   ```
2. `VERSION_CHECK_JSON_URL` में उस `version.json` का लिंक दर्ज करें।
3. भविष्य में नया संस्करण आने पर उपयोगकर्ताओं को **"नया अपडेट उपलब्ध है"** और **"अपडेट डाउनलोड करें"** दिखेगा।
