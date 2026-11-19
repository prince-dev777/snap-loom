# Snap Loom 🎥 (स्क्रीन & रिएक्शन स्टूडियो)

YouTube Reaction और Gaming Videos बनाने के लिए Android ऐप। इसमें Screen Recording के साथ Floating Facecam (Reaction Camera), Live Brush Tool, Shake to Stop और Voice Commentary का पूरा सपोर्ट है।

---

## 🌟 मुख्य फीचर्स (Features)

1. **Screen Recording with Facecam (Floating Camera Overlay)**:
   - स्क्रीन रिकॉर्डिंग के साथ-साथ फ्रंट या बैक कैमरा का फ्लोटिंग विंडो (Facecam) स्क्रीन के ऊपर चलता है।
   - Facecam को स्क्रीन पर कहीं भी Touch & Drag करके मूव कर सकते हैं।
   - **Front & Rear Camera Switch**: एक टैप में फ्रंट या बैक कैमरा में स्विच करें।
   - **Shape, Size & Border Glow**: सर्कल (Circle) या राउंडेड रेक्टेंगल (Rounded Rectangle), 3 साइज़ (Small, Medium, Large) और 5 नियॉन बॉर्डर कलर्स (Neon Red, Cyan, Gold, Green, Purple)।
   - Facecam को रिकॉर्डिंग के दौरान कभी भी On/Off कर सकते हैं।

2. **Screen Brush / Marker Tool (स्क्रीन पर लाइव ड्रा करना)**:
   - फ्लोटिंग बार में नया **Brush (Pen) बटन**: रिकॉर्डिंग के दौरान स्क्रीन पर कुछ भी हाइलाइट करें, सर्कल बनाएं या लिखें।
   - कलर्स (Neon Red, Cyan, Yellow, Green), Undo और Clear बटन के साथ।

3. **Shake to Stop (फोन हिलाते ही रिकॉर्डिंग बंद)**:
   - रिकॉर्डिंग बंद करने के लिए नोटिफिकेशन बार नीचे खींचने की ज़रूरत नहीं—फोन को एक बार शेक (हिलाएं) और रिकॉर्डिंग तुरंत साफ़ तरीके से सेव हो जाएगी।

4. **Built-in Lossless Video Trimmer (वीडियो कटर)**:
   - रिकॉर्डिंग्स टैब में नया **Trim बटन**: बिना क्वालिटी खोए 1 सेकंड में वीडियो का शुरू या आखरी हिस्सा काटें और डायरेक्ट YouTube पर अपलोड करें।

5. **Screen Orientation Lock**:
   - YouTube Shorts/Reels के लिए **Portrait** या सामान्य YouTube वीडियो और गेमिंग के लिए **Landscape** लॉक करने का विकल्प।

6. **Audio Source Selection**:
   - **Microphone**: लाइव कमेंट्री और वॉयस रिएक्शन के लिए।
   - **Internal Sound**: गेम या वीडियो की अंदरूनी आवाज़ रिकॉर्ड करने के लिए।
   - **Mute**: साइलेंट रिकॉर्डिंग के लिए।

7. **Custom Video Quality, FPS & Bitrate**:
   - **Resolution**: 1080p Full HD, 720p HD, 480p SD, या Screen Native।
   - **Frame Rate (FPS)**: 60 FPS (Ultra Smooth), 30 FPS (Standard), 24 FPS (Cinematic)।
   - **Bitrate**: 16 Mbps (Ultra), 10 Mbps (High), 6 Mbps (Balanced), 3 Mbps (Compact)।

8. **Floating Quick Controls Bar**:
   - स्क्रीन पर फ्लोटिंग विजेट: लाइव टाइमर, Pause / Resume, Stop, Facecam On/Off, और Brush Tool।

9. **Gallery & Direct Sharing**:
   - `Movies/ReactionScreenRecordings` में ऑटो-सेव।
   - इन-ऐप वीडियो प्लेबैक, ट्रिमिंग, और सीधे YouTube, WhatsApp, Instagram पर शेयर।

6. **Modern Android Architecture**:
   - 100% Kotlin + Jetpack Compose + Material 3.
   - Android CameraX API.
   - Android 14+ Compliant Foreground Service (`mediaProjection`, `camera`, `microphone`).

---

## 📱 APK फ़ाइल कहाँ मिलेगी?

प्रोजेक्ट पहले से कंपाइल और बिल्ड हो चुका है:
- **Debug APK Location**:
  `app/build/outputs/apk/debug/app-debug.apk`

### अपने फोन में इंस्टॉल करने का तरीका:
1. अपने Android फोन को USB केबल से कनेक्ट करें (USB Debugging ऑन रखें) और कमांड चलाएं:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
2. या फिर `app-debug.apk` फ़ाइल को सीधे अपने फोन में WhatsApp / Google Drive / Bluetooth से भेजकर इंस्टॉल कर लें।

---

## 🛠️ प्रोजेक्ट कैसे बिल्ड करें (Building from Source)

```powershell
# Gradle से APK बिल्ड करने के लिए:
.\gradlew assembleDebug
```
