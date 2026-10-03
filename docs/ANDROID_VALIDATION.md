# Android validation record (2026-10-03)

## New Whisper APK and remaining physical validation

- New Android package: `dist/FieldTalk-debug-whisper.apk`, package `org.fieldtalk.backup`, versionCode 2 / versionName 0.2.0, min Android API 31, arm64-v8a only. Size: **88,080,726 bytes**; SHA-256: `56FD0E52481624B1B9DA3A5B0FD55B99E986A284E0D87A84946C611A826DB093`.
- This APK contains the 59,707,625-byte multilingual `ggml-base-q5_1.bin` model, checksum `422F1AE452ADE6F30A004D7E5C6A43195E4433BC370BF23FAC9CC591F01A8898`, bundled question audio, and an arm64 `libfieldtalk_asr.so` with all three expected JNI functions. The app no longer calls Android `SpeechRecognizer`. ASR is designed for fully local inference with a chosen English, Chinese, or Russian source language. The model is bundled, so no phone-side ASR model download is required.
- `:app:testDebugUnitTest` passed **6 tests, 0 failures**; `:app:assembleDebug` and `:app:lintDebug` passed. APK v2 signature verification passed. The new and previous APKs have the same debug signing certificate (`142753f5d9c4cb8be6979837b621745fd0e47e5b69047618668f1dc6da1c38d0`), so an update install is possible. The previous APK remains untouched at `dist/FieldTalk-debug.apk`, size 73,414,450 bytes and SHA-256 `C5FF203D07ED142B484D3A167B68E45702584FBCD69EDEB7DA97A02436FCB5AF`.
- Status UX is now one compact in-place panel with ASR, Translation, and TTS rows and one current diagnostic line. Diagnostics do not append to the transcript or translation. If translation fails, the original transcript is still visible; if TTS fails, the translated text stays visible.
- Conservative EN/ZH/RU explicit-only extraction highlights allergy, medication, pain location, breathing difficulty, bleeding, and loss of consciousness. It skips questions, negation, uncertainty, and other-person statements. Handoff changes only after a responder taps **Add to Handoff** on a patient recording; unknown fields remain `Unknown / Not stated`. Responder speech cannot be added as patient facts through Free Conversation.
- **No ADB-connected phone was available for this new APK.** The already-installed previous APK supplied the user's reported failure. English, Chinese, and Russian microphone transcription, different-audio discrimination, translation, TTS playback, latency, and Airplane Mode operation have **not** been physically verified for this new APK. Do not claim full offline Android success until the steps below pass on the phone. ML Kit translation still needs an explicit connected model preparation for each selected pair, and TTS needs a suitable installed embedded voice.

### Exact phone test, including Airplane Mode

1. Confirm the phone is Android 12+ and arm64. Copy `dist/FieldTalk-debug-whisper.apk` to the phone and install it as an update, or enable USB debugging and run the PowerShell commands below. Keep the old APK file as a rollback copy.
2. While online, open **Free Conversation**. Wait for `ASR Ready` (the bundled model is copied and verified on first use). Choose a responder language and patient language; tap **Prepare Translation Models** on Wi-Fi for each pair used. Confirm `Translation Ready` and `TTS Ready` for the selected output language. If TTS says Error, install a device-provided offline voice for that language before testing speech output.
3. Enable Airplane Mode, turn Wi-Fi off, fully force-stop FieldTalk, then reopen it. Do not turn connectivity back on during the test. Wait for all three rows to say Ready for the current pair.
4. Test **English → Chinese**: set Responder Language to Chinese and Patient Language to English; leave Free Conversation in **Patient → Responder**. Record `I am allergic to penicillin.` for at least one second, tap **Stop Recording**, check the original and different Chinese translation, and tap **Play Translation**. Check that `Allergy: Penicillin` appears. Tap **Add to Handoff** and verify it appears in Handoff.
5. Test **Chinese → English**: set Responder Language to English and Patient Language to Chinese; record `我胸口疼，而且呼吸困难。` and verify Chinese original, English translation, English voice, and explicit chest pain/breathing highlights. Test **Russian → English** with Patient Language Russian and `У меня болит грудь.`; verify a distinct Russian transcript and English translation/voice. For these tests, use the patient mode; **Switch Speaker / Direction** is for responder speech and must not add patient facts.
6. Record three distinct English sentences while selected source is English: `I am allergic to penicillin.`, `My chest hurts.`, and `I cannot breathe normally.` Confirm different original transcripts and different highlights. Test silence or unclear speech: the app should say `Speech could not be recognized. Please repeat.` and must not invent a transcript or continue to translation.
7. Still offline, use Quick Questions and Repeat Audio, then inspect Handoff. Confirm only statements explicitly added by the responder are present, and all other fields say `Unknown / Not stated`. Clear the session and confirm facts disappear.
8. Record device model, Android version, each status row, actual transcript/translation, whether audio played, and measured wait time for each direction. If any stage fails, capture Android logcat and keep the previous stable APK available.

```powershell
$adb='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\android-sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb install -r .\dist\FieldTalk-debug-whisper.apk
& $adb shell am force-stop org.fieldtalk.backup
& $adb shell am start -n org.fieldtalk.backup/.MainActivity
```

The earlier validation record below describes the previous pre-Whisper APK only.

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
