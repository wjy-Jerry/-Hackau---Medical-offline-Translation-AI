# FieldTalk Android prototype

This independent native project does not use the laptop backend. The bundled ambulance phrase pack and 21 pre-generated WAV files make Quick Questions and Yes / No work without a network connection. The Handoff Card is in memory only. Free Conversation uses only Android's on-device recognizer, already-downloaded ML Kit translation models, and an installed embedded TTS voice; it reports unavailable if any is missing. Translation model download is an explicit connected setup action, never an inference fallback.

Regenerate audio only after the phrase pack changes: `..\.venv\Scripts\python.exe -m scripts.build_android_phrase_audio` from the repository root. No new model download is needed; this uses the already installed Piper voices. See `docs/ANDROID_ARCHITECTURE.md` for limitations and source links.

Build (after installing Android SDK and Gradle wrapper): `cd android; .\gradlew.bat :app:assembleDebug`. Do not commit SDK, build output, or debug APK. Hardware and emulator validation must be reported separately.
