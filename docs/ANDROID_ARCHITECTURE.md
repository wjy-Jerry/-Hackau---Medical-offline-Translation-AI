# Android feasibility and architecture (2026-10-03)

**2026-10-03 implementation update:** The phone exposed the limitation described below: its local system recognizer was unavailable. The current Android source now uses bundled, Git-ignored multilingual Whisper `base-q5_1` (59,707,625 bytes) through pinned whisper.cpp 1.8.0, JNI, and 16 kHz PCM capture. It no longer calls Android `SpeechRecognizer` or a cloud speech service. The model is included in locally built APKs only after a checksum-checked setup; Gradle refuses a build without it. The installed phone has not yet been tested with this new APK. The rest of this document records the earlier feasibility decision and should be read as historical context.

## 1. Current desktop architecture

The recoverable Windows fallback is React/Vite calling local FastAPI. `/process_audio` runs multilingual faster-whisper `base`, Argos translation, conservative English/Chinese extraction, and Piper speech; Quick Questions use a local phrase pack and Piper. The downloaded desktop assets measured on this workspace are about **141 MB** for the faster-whisper model, **538 MB** for four Argos package directories (including tokenizers), and **181 MB** for three Piper ONNX voices. These are measured local files, not expected Android package sizes. The Python/CTranslate2/Argos/Piper stack is not an Android app and cannot be made standalone by putting the website in a WebView.

## 2. Android architecture and delivery order

Create a separate native Android project under `android/`; keep the web project and API untouched. The stable first APK should bundle the EN/ZH/RU ambulance phrase JSON and pre-generated, local question audio. Home, Quick Questions, Yes / No, and an in-memory Handoff Card can then work with no backend and no network permission. The Android card must distinguish patient-stated text from missing fields and clear on request. Generate audio before packaging and verify that its wording matches the phrase pack.

Free Conversation is a separate, capability-checked pipeline: microphone → **on-device** ASR → **downloaded local** translation model → **embedded** TTS voice. Each stage must report unavailable when its local capability/model is absent. Never silently invoke a general recognizer or online voice. Phrase workflows remain usable if conversation is unavailable.

## 3. Library changes and realistic local options

| Function | Desktop implementation | Android candidate and boundary |
| --- | --- | --- |
| UI and session | React/Vite, browser memory | Native Activity and in-memory state; no laptop server or account. |
| ASR | faster-whisper/CTranslate2, 141 MB local model | Android [`SpeechRecognizer.createOnDeviceSpeechRecognizer`](https://developer.android.com/reference/android/speech/SpeechRecognizer) only after checking `isOnDeviceRecognitionAvailable`. Availability and language coverage depend on the installed device recognizer. A bundled [`whisper.cpp` Android example](https://github.com/ggml-org/whisper.cpp/blob/master/examples/whisper.android/README.md) is a more portable but larger future path and needs native integration/performance testing. `EXTRA_PREFER_OFFLINE` alone is only a preference, so it is insufficient for a strict offline claim. |
| Translation | Argos Python/CTranslate2, 538 MB installed | [ML Kit on-device translation](https://developers.google.com/ml-kit/language/translation/android) runs locally **after** downloading models (~30 MB per language). Explicitly check/download required models during connected setup and refuse conversation translation offline if absent. This makes Free Conversation conditional; the bundled phrase pack works immediately. ML Kit warns that non-English pairs pivot through English and translation quality varies. |
| TTS | Piper ONNX voices, ~60 MB each | Prefer an Android [`TextToSpeech` voice whose `isNetworkConnectionRequired()` is false](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine); check installed voice for the selected language before enabling. Bundle pre-generated question audio for guaranteed phrase playback. Arbitrary text speech is unavailable if no suitable embedded voice is installed. |
| Critical facts | Regex over source EN/ZH transcript | Port only explicit and reviewed patterns if Free Conversation becomes available; otherwise store manually confirmed patient text without invented structured facts. |

## 4. Models, device needs, and build environment

- Guaranteed APK phrase content: seven questions × three languages; estimated audio size should be measured after generation. This does not require a model at runtime.
- Optional `whisper.cpp` multilingual tiny/base model files are roughly 75/142 MB according to the project's [model-size notes](https://github.com/ggml-org/whisper.cpp/discussions/467); actual RAM and latency vary by device. Bundling one would increase APK size and requires NDK/JNI work. No compatible model is installed for Android yet.
- Optional ML Kit language models are about 30 MB each and require a connected pre-download; the APK by itself cannot guarantee these are present. Check local state before claiming offline Free Conversation.
- Recommend an arm64 phone with enough free storage for app, audio, and optional language models; CPU/RAM and speech latency must be measured on a real device. No minimum performance claim is established.
- This workspace has JDK 21 but no detected Android SDK, Android Studio, Gradle, adb, or emulator. The [Android command-line build guide](https://developer.android.com/build/building-cmdline) supports debug APK builds with Gradle; [SDK command-line tools](https://developer.android.com/tools/sdkmanager) install platforms and build tools. Installation is a separate potentially long step after this document is committed. The chosen Gradle/Android Gradle Plugin versions must be pinned together and checked against [official compatibility notes](https://developer.android.com/build/releases/agp-8-7-0-release-notes).

## 5. Major risks and acceptance criteria

1. **Offline ASR coverage:** an on-device recognizer may be absent or lack RU/ZH. The app must say so and keep Quick Questions usable. `whisper.cpp` needs separate native work and phone benchmarks.
2. **Model provisioning:** ML Kit translation requires a pre-download. Airplane Mode works only after the required local models and voices are installed and verified; the APK must not claim otherwise.
3. **Clinical language quality:** machine translation and Russian phrases are not human-validated. Do not turn extracted text into a diagnosis or treatment advice.
4. **Build/toolchain:** SDK and Gradle are absent in this environment. APK generation is unverified until an actual build completes. A physical phone or emulator is also absent so far.
5. **Acceptance:** installable debug APK; standalone Quick Questions, Yes / No, and Handoff; local audio without laptop; explicit capability/error states for any incomplete Free Conversation stage; no network inference path; honest build/emulator/physical-device results.
