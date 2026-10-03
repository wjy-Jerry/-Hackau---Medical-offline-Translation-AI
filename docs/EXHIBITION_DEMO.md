# FieldTalk exhibition demo (60–90 seconds)

FieldTalk is an offline emergency communication system for a conscious patient and responder. The first screen should show **Quick Questions** first. Use the prepared local laptop, installed models, and a real microphone. Run one full rehearsal on that exact device before the exhibition; physical Airplane Mode and live microphone performance remain manual checks.

## Before judges arrive

1. From the repository root, start the backend: `.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000`.
2. In a second terminal, run `cd frontend; npm run dev`. Open `http://127.0.0.1:5173` in a browser and allow microphone access.
3. Check that the screen says **OFFLINE READY**. This checks local model presence, not network isolation. If demonstrating Airplane Mode, turn it on and confirm the local page, Quick Question audio, and one conversation still work.
4. Set responder and patient languages before recording. For a patient response, make the **source** language the patient's language, using the swap control if needed.

## Live sequence

| Time | Action | What the judge sees |
| --- | --- | --- |
| 0–15 s | Show the disabled internet connection and **OFFLINE READY**; select languages. | Local device, clear emergency choices. |
| 15–35 s | Open **QUICK QUESTIONS** and tap “Can you breathe normally?” | Large patient-language wording, local spoken question, Repeat. |
| 35–65 s | Open **FREE CONVERSATION**. Ask the judge to speak one short patient statement; tap to start and tap to finish. | Original speech text, translation, local playback, any explicitly extracted fact. |
| 65–90 s | If the judge spoke as the patient, tap **ADD PATIENT STATEMENT**, then **VIEW HANDOFF**. | Patient-stated original/translation, structured facts where supported, and Unknown / Not stated for missing fields. |

Suggested short statements: “I am allergic to penicillin.”, “My chest hurts.”, “I cannot breathe normally.” For Chinese, try “我对青霉素过敏。” or “我的胸口疼。” Keep one statement per recording. Russian short speech can be demonstrated, but Russian wording awaits native-speaker validation and its structured extraction may be empty; the original and translation still appear.

## If the microphone or live recognition fails

- State plainly that the live microphone step failed. Use **QUICK QUESTIONS** and **YES / NO** to show communication without patient speech. Written patient-language prompts remain usable if speech playback fails.
- If the backend is unavailable, restart the two local processes and wait for **OFFLINE READY**. Do not present cached screenshots, prerecorded outputs, or a network connection as a live result.
- If a translation is unclear, show the original text and repeat/confirm with the patient. The handoff card stores only statements the responder explicitly adds.

Do not spend the 90 seconds on model file sizes, settings, general chat, benchmark numbers, or unsupported clinical claims. Do not call the phrase packs clinically validated. End with the communication handoff, not a diagnosis.
