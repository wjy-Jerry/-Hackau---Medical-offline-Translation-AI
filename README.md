# FieldTalk

FieldTalk is a 48-hour hackathon prototype for offline emergency communication between a first responder and a conscious patient. It translates communication; it does not diagnose, recommend treatment, or make medical decisions. Confirm critical details with the patient. **Current Windows web fallback uses real local ASR, translation, and TTS.** A separate Android debug APK has been built, with phone testing still pending.

## Current scope

English ↔ Chinese, Russian ↔ English, and Russian ↔ Chinese emergency communication is available in the Windows web interface. Russian ↔ Chinese uses English as a local translation pivot and needs extra human review. The interface provides Quick Questions, Yes / No, Free Conversation, and an in-memory Emergency Handoff Card.

Russian support is present on the fallback branch. The seven Russian Quick Question phrases in `data/phrase_packs/ambulance/ru.json` are **pending native-speaker validation**; `data/emergency_questions.json` is a compatibility snapshot. `data/russian_review_set.json` stores actual outputs from synthetic local Russian speech for a teammate to rate as correct and natural, understandable but awkward, incorrect, or potentially dangerous meaning change. All human-review fields are intentionally empty. Synthetic speech and machine translation do not establish patient-facing language quality.

On 2026-10-03, locally synthesized Russian speech passed through the real `/process_audio` pipeline to English and Chinese with local playable WAV responses. The Russian → English chest-pain run reported 761.7 ms ASR, 2124.8 ms translation (cold model load), 1314.5 ms TTS, and 4202.8 ms total. A Russian → Chinese allergy run reported 460.7 ms ASR, 1192.8 ms translation, 1361.9 ms TTS, and 3016.8 ms total. These are single-machine technical samples, not human language-quality evaluations. Browser checks covered desktop (1200px), tablet (768px), and mobile (390px), including a Russian → English recorded conversation result.

See [demo validation and tomorrow's walkthrough](docs/DEMO_VALIDATION.md) for actual multilingual results, offline-test limits, failure checks, and the manual demo sequence.

The [Android architecture](docs/ANDROID_ARCHITECTURE.md), [Android validation record](docs/ANDROID_VALIDATION.md), and [final validation record](docs/FINAL_VALIDATION.md) describe the separate APK. Its bundled Quick Questions, Yes / No, and Handoff source/build are complete, but no Android device or emulator was available for runtime testing. Android Free Conversation is conditional on an on-device recognizer, previously downloaded ML Kit models, and an embedded offline voice; Android structured extraction is not implemented.

## Architecture and stable interfaces

```text
React/Vite responder and patient views
                ↓ multipart POST /process_audio
FastAPI orchestrator (backend/main.py)
                ↓
ASR → Translation (+ patient-stated information) → TTS
                ↓
JSON result + local /audio/{id}.wav → browser playback

Quick Questions / Yes-No → bundled EN/ZH/RU prompt → GET /quick_question/{id}/audio
                                              ↓ local Piper voice → browser playback
```

Keep these public interfaces and response keys stable when replacing a model:

| Module | Function | Return |
| --- | --- | --- |
| ASR | `speech_to_text(audio_path, language=None)` | `{text: str, language: str, confidence: float or null}` |
| Translation | `translate_and_extract(text, source_language, target_language)` | `{translation: str, key_information: object}` |
| TTS | `text_to_speech(text, language)` | Path to a local WAV file |
| API | `POST /process_audio` multipart fields `audio`, `source_language`, `target_language` | `{original_text, translation, confidence, key_information, audio_url, warning, timings_ms, mode}` |
| Quick-question audio | `GET /quick_question/{id}/audio?target_language=en\|zh\|ru` | Local WAV for one predefined prompt |

Language codes are `en`, `zh`, and `ru`. Confidence is `null` when the ASR model has no calibrated utterance confidence. The information extractor currently highlights only explicit English or Chinese source phrases; Russian source highlights may be empty. It does not diagnose, recommend medication or treatment, or make decisions.

## Repository structure

```text
backend/                 FastAPI, contracts, configuration, model adapters
backend/models/          asr.py, translation.py, tts.py, emergency_nlp.py
frontend/                React/Vite home, question, yes/no, and conversation views
data/phrase_packs/       Shared EN/ZH/RU local question pack used by frontend, backend, and Android
android/                 Separate native Android source and bundled question audio
scripts/download_asr_model.py  One-time ASR model setup
scripts/download_translation_models.py  One-time translation model setup
scripts/download_tts_voices.py  One-time English/Chinese/Russian voice setup
tests/                   API contract and optional real-ASR audio tests
models_local/             Downloaded ASR, translation, and voice models (Git ignored)
generated_audio/          Synthesized WAV files (Git ignored)
```

## Setup offline ASR, translation, and TTS (Windows PowerShell)

Run these commands from the repository root. Python 3.10+ and Node.js 20+ are needed.

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt -r requirements-tts.txt
.\.venv\Scripts\python.exe -m scripts.download_asr_model
.\.venv\Scripts\python.exe -m scripts.download_translation_models
.\.venv\Scripts\python.exe -m scripts.download_tts_voices
cd frontend
npm ci
cd ..
.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
```

In a second terminal:

```powershell
cd frontend
npm run dev
```

Open `http://127.0.0.1:5173`. Set the responder and patient languages, then choose a mode. Quick Questions and Yes / No display patient-language prompts immediately from the local phrase pack and request only local speech from the backend. No human review is recorded for the current pack; Russian wording is explicitly pending native-speaker validation. Free Conversation records speech for the existing `/process_audio` pipeline. If speech generation fails, the written question or translated text remains visible. Yes / No selections stay on the current screen. Add only confirmed patient statements to the in-memory Handoff Card; missing facts remain Unknown / Not stated.

## Local ASR model (internet needed once)

From the repository root, with the Python virtual environment created:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-asr.txt
.\.venv\Scripts\python.exe -m scripts.download_asr_model
```

The script downloads the multilingual faster-whisper `base` model to `models_local/whisper-base`. Runtime loading uses that directory with `local_files_only=True`; no speech API or model download is used during inference. The ASR dependency pins PyAV below 19 because faster-whisper 1.2.1 calls an argument [removed in PyAV 19](https://github.com/PyAV-Org/PyAV/blob/main/CHANGELOG.rst). Do not commit the model assets.

The [faster-whisper project](https://github.com/SYSTRAN/faster-whisper) documents local model loading. Use the individual download scripts above to install each component.

## Local translation models (internet needed once)

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-translation.txt
.\.venv\Scripts\python.exe -m scripts.download_translation_models
```

This installs Argos Translate English ↔ Chinese and English ↔ Russian packages under `models_local/argos`. Model directories are Git ignored. Russian ↔ Chinese runs through English locally. The adapter disables Stanza's runtime resource update check; the Russian packages use local punctuation splitting because their bundled Stanza metadata is incompatible with the installed version. The first request for each direction loads its model and may take several seconds. Subsequent short requests are faster. Translation can alter medical nuance, so confirm critical wording with the patient.

## Patient-stated information highlighting

`translate_and_extract(text, source_language, target_language)` still returns `translation` and `key_information`. The extractor reads the **source transcript** and recognizes a short English/Chinese phrase list for allergies, medication being taken, pain location, breathing difficulty, bleeding, and reported fainting or loss of consciousness. Pain location uses the `symptom` key to match the existing example:

```json
{"allergy": "Penicillin", "symptom": "Chest pain"}
```

That object comes from “I am allergic to penicillin and my chest hurts.” Unsupported, negated, hypothetical, question, or other-person statements yield `{}` unless a separate supported patient statement is present. The rules run locally with no extra model or cloud API. They can miss wording or be wrong when ASR mishears a phrase; the original transcript remains visible for confirmation. These fields are communication highlights only, not diagnoses or medical instructions.

## Local TTS voices (internet needed once)

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-tts.txt
.\.venv\Scripts\python.exe -m scripts.download_tts_voices
```

The script installs `en_US-lessac-medium`, `zh_CN-huayan-medium`, and `ru_RU-irina-medium` under `models_local/voices`. [Piper's Python API](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/API_PYTHON.md) runs synthesis locally; FieldTalk caches each loaded voice and saves a WAV file under `generated_audio/`. The first synthesis for each language includes voice loading. No cloud speech API is called.

Start the backend:

```powershell
.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
```

Start the frontend with `cd frontend; npm run dev` in another terminal. `GET http://127.0.0.1:8000/health` should show `mode: local` and `asr`, `translation`, and `tts` all `ready`.

## Test the complete pipeline

Run contract, translation, and ASR tests:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
cd frontend
npm run build
```

For the real English ASR test, record **“I am allergic to penicillin.”**, **“My chest hurts.”**, and **“I cannot breathe normally.”** as three separate WAV files. Set `FIELDTALK_ASR_TEST_AUDIO_DIR` to their directory and name them `allergy.wav`, `chest.wav`, and `breathing.wav`, then run `pytest -q`. Those real-speaker fixture tests are skipped when the audio directory is unset. The separate Russian ASR test uses local synthetic speech. In the browser, test English → Chinese, Chinese → English, and Russian → English; confirm that `Original` and `Translation` change and translated speech plays. The API reports ASR, translation, TTS, and total timing in milliseconds. Speech accuracy and translation quality depend on voice, noise, and terminology.

## Verify without internet

1. Finish dependency and model downloads while online.
2. Start both servers and confirm `/health` reports `{"mode":"local","ready":true,"components":{"asr":"ready","translation":"ready","tts":"ready"}}`.
3. Disable Wi-Fi and unplug Ethernet or enable Airplane Mode.
4. Reload `http://127.0.0.1:5173` and run EN→ZH, ZH→EN, and RU→EN again. The page should still load because Vite and FastAPI are local processes.
5. If you need strict proof of no network calls, monitor the processes with an OS network monitor while recording and processing.

All three runtime models are local. The voice and translation model files are ignored by Git.

## Error handling and limitations

The UI reports microphone permission, empty recording, backend errors, and model failures. The API rejects unsupported pairs and empty/oversized audio. ASR returns `confidence: null`; Whisper's segment statistics are not a calibrated utterance confidence, so no confidence warning is generated by the current model. English test sentences were recognized, but some synthetic Chinese medical phrases were misrecognized; critical terms require human confirmation. Argos may paraphrase or distort symptoms and is not medically validated. Russian ↔ Chinese uses two model steps and one bleeding example repeated the translated wording. The information extractor covers English and Chinese source statements, and may omit or miss information; extraction failure now preserves the translated text with empty highlights. If TTS fails, `/process_audio` returns the recognized and translated text with `audio_url: ""` and a warning; the frontend leaves the text visible. Physical-microphone accuracy and a physical Airplane Mode run remain unverified. Generated WAV files remain on disk until manually removed. The backend is intended for a single local demo user.

Team members can work within `backend/models/asr.py`, `translation.py`, `tts.py`, `emergency_nlp.py`, or `frontend/` independently. Keep the tabled function signatures, language codes, and API response keys stable; coordinate any needed contract change before merging branches.
