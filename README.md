# FieldTalk

FieldTalk is a 48-hour hackathon prototype for offline speech translation between a first responder and a conscious patient. It translates communication; it does not diagnose, recommend treatment, or make medical decisions. Confirm critical details with the patient. **Current stage: local ASR and translation are real; TTS is mocked.**

## Current scope

English ↔ Chinese speech translation. The first milestone is microphone → speech recognition → translation → speech synthesis → playback. Quick questions, Russian, medical NLP, and other modes are future work.

## Architecture and stable interfaces

```text
React/Vite microphone + result page
                ↓ multipart POST /process_audio
FastAPI orchestrator (backend/main.py)
                ↓
ASR → Translation (+ empty NLP hook) → TTS
                ↓
JSON result + local /audio/{id}.wav → browser playback
```

Keep these public interfaces and response keys stable when replacing a model:

| Module | Function | Return |
| --- | --- | --- |
| ASR | `speech_to_text(audio_path, language=None)` | `{text: str, language: str, confidence: float or null}` |
| Translation | `translate_and_extract(text, source_language, target_language)` | `{translation: str, key_information: object}` |
| TTS | `text_to_speech(text, language)` | Path to a local WAV file |
| API | `POST /process_audio` multipart fields `audio`, `source_language`, `target_language` | `{original_text, translation, confidence, key_information, audio_url, warning, timings_ms, mode}` |

Language codes are `en` and `zh`. Confidence is `null` when the ASR model has no calibrated utterance confidence. The current emergency NLP hook returns `{}`. Do not infer an allergy, symptom, or confidence value from model output without a reviewed method.

## Repository structure

```text
backend/                 FastAPI, contracts, configuration, model adapters
backend/models/          asr.py, translation.py, tts.py, emergency_nlp.py
frontend/                One-page React/Vite microphone UI
data/                    Placeholder quick-question content, not in the UI
scripts/download_asr_model.py  One-time ASR model setup
scripts/download_translation_models.py  One-time translation model setup
tests/                   API contract and optional real-ASR audio tests
models_local/             Downloaded ASR and translation models (Git ignored)
generated_audio/          Synthesized WAV files (Git ignored)
```

## Setup real ASR and translation with mock TTS (Windows PowerShell)

Run these commands from the repository root. Python 3.10+ and Node.js 20+ are needed.

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt -r requirements-translation.txt
.\.venv\Scripts\python.exe -m scripts.download_asr_model
.\.venv\Scripts\python.exe -m scripts.download_translation_models
cd frontend
npm ci
cd ..
$env:FIELDTALK_MODE = 'mock'
.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
```

In a second terminal:

```powershell
cd frontend
npm run dev
```

Open `http://127.0.0.1:5173`. Choose English → Chinese, record a short phrase, and stop. Recognition uses local faster-whisper and translation uses local Argos Translate. TTS generates a **tone**, not speech. `FIELDTALK_MODE=mock` selects the mock TTS; ASR and translation always use local models.

## Local ASR model (internet needed once)

From the repository root, with the Python virtual environment created:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-asr.txt
.\.venv\Scripts\python.exe -m scripts.download_asr_model
```

The script downloads the multilingual faster-whisper `base` model to `models_local/whisper-base`. Runtime loading uses that directory with `local_files_only=True`; no speech API or model download is used during inference. The ASR dependency pins PyAV below 19 because faster-whisper 1.2.1 calls an argument [removed in PyAV 19](https://github.com/PyAV-Org/PyAV/blob/main/CHANGELOG.rst). Do not commit the model assets.

The [faster-whisper project](https://github.com/SYSTRAN/faster-whisper) documents local model loading. Do not run `scripts.download_models` for this milestone.

## Local translation models (internet needed once)

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-translation.txt
.\.venv\Scripts\python.exe -m scripts.download_translation_models
```

This installs direct Argos Translate English → Chinese and Chinese → English packages under `models_local/argos`. Both model directories are Git ignored. Translation runs from those local packages after setup; the adapter disables Stanza's runtime resource update check. The packages together occupy about 166 MiB on disk; Argos Translate also installs sizable Python dependencies. The first request for each direction loads its model and may take several seconds. Subsequent short requests are faster. Translation can alter medical nuance, so confirm critical wording with the patient.

Start the current mixed backend:

```powershell
$env:FIELDTALK_MODE = 'mock'
.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
```

Start the frontend with `cd frontend; npm run dev` in another terminal. `GET http://127.0.0.1:8000/health` should show `asr: ready`, `translation: ready`, and `tts: mock`.

## Test the complete pipeline

Run contract, translation, and ASR tests:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
cd frontend
npm run build
```

For the real ASR test, record **“I am allergic to penicillin.”**, **“My chest hurts.”**, and **“I cannot breathe normally.”** as three separate WAV files. Set `FIELDTALK_ASR_TEST_AUDIO_DIR` to their directory and name them `allergy.wav`, `chest.wav`, and `breathing.wav`, then run `pytest -q`. The real-ASR test is skipped when the audio directory is unset. In the browser, say the same phrases and confirm that `Original` and `Translation` change. The audio remains a mock tone. Select Chinese as source to exercise both reverse models. Backend logs report ASR model load and translation inference milliseconds; the API reports pipeline stage timings. Speech accuracy and translation quality depend on voice, noise, and terminology.

## Verify without internet

1. Finish dependency and model downloads while online.
2. Start both servers with `FIELDTALK_MODE=mock` and confirm `/health` reports `{"mode":"mock","ready":true,"components":{"asr":"ready","translation":"ready","tts":"mock"}}`.
3. Disable Wi-Fi and unplug Ethernet or enable Airplane Mode.
4. Reload `http://127.0.0.1:5173` and run both directions again. The page should still load because Vite and FastAPI are local processes.
5. If you need strict proof of no network calls, monitor the processes with an OS network monitor while recording and processing.

The runtime ASR and translation models are local; speech synthesis is still a mock tone.

## Error handling and limitations

The UI reports microphone permission, empty recording, backend errors, and model failures. The API rejects unsupported pairs and empty/oversized audio. ASR returns `confidence: null`; Whisper's segment statistics are not a calibrated utterance confidence, so no confidence warning is generated. English test sentences were recognized, but a synthetic Chinese medical phrase was misrecognized; critical terms require human confirmation. Argos may paraphrase or distort symptoms and is not medically validated. Real TTS, physical-microphone accuracy, and a network-disabled device run remain unverified. Generated mock WAV files remain on disk until manually removed. The backend is intended for a single local demo user.

Team members can work within `backend/models/asr.py`, `translation.py`, `tts.py`, `emergency_nlp.py`, or `frontend/` independently. Keep the tabled function signatures, language codes, and API response keys stable; coordinate any needed contract change before merging branches.
