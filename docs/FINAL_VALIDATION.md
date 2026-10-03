# FieldTalk fallback final validation (2026-10-03)

## Windows web fallback

- Full Python suite: **118 passed, 2 skipped, 1 dependency deprecation warning**. The skipped tests require optional manually recorded WAV fixtures. Frontend Node tests: **3 passed**. Vite production build passed.
- Four actual local synthetic WAV requests completed through ASR → translation → TTS with playable local WAV responses: EN→ZH (6,036.3 ms total), ZH→EN (2,320.6 ms), RU→EN (768.8 ms), and EN→RU (2,540.5 ms). EN input explicitly stated penicillin allergy and chest pain; the EN-source extractor returned those two fields. RU-source structured highlights remained empty, as designed. These are single-machine timings with warmed/cold components mixed, not benchmarks.
- `/quick_question/breathing/audio` returned local WAVs for EN, ZH, and RU. Browser checks at 1200, 768, 390, and 320 px showed no horizontal overflow. Quick Question displayed and played its patient-language phrase; Yes / No selection worked; language swap changed the displayed question. The Handoff browser flow was previously checked at 390 px with a real RU→EN API result injected after headless microphone capture failed: it displayed the original Russian and English translation, with unknown structured fields. This was a UI check using a controlled transport, **not** a real microphone test.
- The web fallback and Android project contain no cloud inference call in their app paths. Earlier non-loopback socket denial checks covered the Windows local pipeline. Physical Airplane Mode, live patient speech, and Russian native-speaker quality remain unverified.

## Standalone Android APK

- `:app:assembleDebug` and `:app:lintDebug` passed. `:app:testDebugUnitTest` had **NO-SOURCE**. Python Android asset tests passed. The APK is signed with the debug key and APK Signature Scheme v2 verification passed.
- Package `org.fieldtalk.backup`, min API 31, target API 35. The APK includes four phrase JSON files and 21 local question WAV files. APK size: 73,414,450 bytes. SHA-256: `C5FF203D07ED142B484D3A167B68E45702584FBCD69EDEB7DA97A02436FCB5AF`.
- `adb devices -l` found no phone, and no emulator/system image is installed. Installation, launch, touch navigation, microphone permission, audio playback, and the conditional on-device Free Conversation pipeline are **not runtime verified**. Free Conversation requires an available on-device recognizer, pre-downloaded ML Kit language models, and an embedded voice that does not require network access. It must report unavailable if any requirement is absent. Android structured extraction is not implemented; the Handoff Card can retain manually added patient words and translations in memory.

The APK is at `dist/FieldTalk-debug.apk` in the repo and in the projectless `outputs/FieldTalk-debug.apk` delivery folder. The APK/build directories, downloaded SDK, Gradle cache, and models are not committed. See `docs/ANDROID_VALIDATION.md` for exact install commands and manual phone checks.
