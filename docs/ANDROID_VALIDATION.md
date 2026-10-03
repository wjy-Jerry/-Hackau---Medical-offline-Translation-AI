# Android validation record (2026-10-03)

## Verified in this workspace

- `:app:assembleDebug` succeeded with Gradle 8.9, Android Gradle Plugin 8.7.3, API 35, and JDK 21. `:app:lintDebug` succeeded after an API compatibility fix. `:app:testDebugUnitTest` reported **NO-SOURCE**; no native JVM unit tests exist yet.
- The debug APK is 73,414,450 bytes. SHA-256: `C5FF203D07ED142B484D3A167B68E45702584FBCD69EDEB7DA97A02436FCB5AF`. APK v2 signature verification passed. `aapt` reports package `org.fieldtalk.backup`, min API 31, target API 35, and launcher `MainActivity`.
- The APK contains the four shared ambulance phrase JSON files and all 21 generated EN/ZH/RU question WAV files. Python asset integrity tests passed, including WAV frame and sample-width checks.
- Source inspection found no WebView, laptop backend URL, or `/process_audio` call in the Android app. The only declared network use is the explicit ML Kit model provisioning action. Free Conversation checks an on-device recognizer, downloaded translation models, and a voice flagged as not requiring network.

## Not verified

`adb devices -l` returned **no attached devices**. No Android emulator or system image is installed in the isolated SDK. Therefore install, launch, navigation, microphone permission, actual question playback, offline ML Kit translation, and embedded TTS are **not runtime verified**. A physical Android device has not been tested. Do not describe this APK as phone-validated.

The guaranteed offline design portion is bundled Quick Questions, Yes / No, and an in-memory Handoff Card; their runtime behavior still needs a device test. Free Conversation remains conditional on device capabilities and a connected one-time translation model setup. Android structured information extraction is not implemented; patient original text and translation can still be added to Handoff. Russian wording remains pending native-speaker validation.

## Manual device check

Use an Android 12+ phone. Connect it by USB with USB debugging enabled, then from the repository root:

```powershell
$adb='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\android-sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb install -r .\dist\FieldTalk-debug.apk
& $adb shell am start -n org.fieldtalk.backup/.MainActivity
```

Check, in order: (1) launch/home status, (2) language selection, (3) seven Quick Questions display and Repeat audio in each available language, (4) large Yes / No controls, (5) manual patient statement → Handoff → Clear, and (6) microphone permission and honest unavailable state if the device lacks on-device recognition. For Free Conversation, while connected explicitly prepare both translation models, confirm an embedded voice, then disconnect network and test a short utterance → recognized text → local translation → local speech. Record the phone model, Android version, installed recognizer/voices, exact result, and any failure. Do not use `SpeechRecognizer.createSpeechRecognizer` or online TTS as a fallback.
