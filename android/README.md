# FieldTalk Android prototype

This independent native project does not use the laptop backend. The bundled ambulance phrase pack and 21 pre-generated WAV files make Quick Questions and Yes / No work without a network connection. The Handoff Card is in memory only. Free Conversation records 16 kHz mono PCM and transcribes it through bundled whisper.cpp 1.8.0 with the multilingual `ggml-base-q5_1` model. Translation uses already-downloaded ML Kit language models; speech output requires an installed TTS voice that does not need a network connection. Translation model download is an explicit connected setup action, never an inference fallback.

Regenerate audio only after the phrase pack changes: `..\.venv\Scripts\python.exe -m scripts.build_android_phrase_audio` from the repository root. No new model download is needed; this uses the already installed Piper voices. See `docs/ANDROID_ARCHITECTURE.md` for limitations and source links.

From the repository root, run `powershell -ExecutionPolicy Bypass -File .\scripts\prepare_android_asr_model.ps1` once while connected. The model is 59,707,625 bytes and is verified by SHA-256. It is Git ignored, but `:app:preBuild` fails if it is missing or corrupt, ensuring that a build cannot silently omit ASR. The pinned whisper.cpp 1.8.0 source and its MIT license are under `android/third_party/whisper.cpp`.

Build (after installing Android SDK, NDK 27.0.12077973, CMake 3.22.1, and Gradle wrapper): `cd android; .\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`. The APK currently targets arm64 Android 12+. Do not commit SDK, model binary, build output, or debug APK. Hardware validation must be reported separately. ML Kit translation models and suitable embedded TTS voices still need preparation on the phone.
