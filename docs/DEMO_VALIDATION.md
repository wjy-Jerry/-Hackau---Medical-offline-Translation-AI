# FieldTalk fallback demo validation

Checked on 2026-10-03 (Asia/Shanghai) on the local Windows development machine. All spoken samples below were synthesized by locally installed Piper voices. They verify the software path, not performance on actual patient speech. Russian text and medical phrasing remain **pending native-speaker validation**.

Final project checks: `python -m pytest -q` reported **101 passed, 2 skipped, 1 dependency deprecation warning**; `npm run build` succeeded. The skipped tests require optional manually recorded English/Chinese WAV fixtures. No model files, generated audio, browser profiles, or secrets are committed.

## Real audio through the backend

| Direction and spoken sample | Actual ASR and translation | Patient-stated highlights | ASR / translation / TTS / total |
| --- | --- | --- | --- |
| EN→ZH: “I am allergic to penicillin and my chest hurts.” | Same English transcript; “我对青霉素过敏,胸口痛” | Allergy: Penicillin; symptom: Chest pain | 801.6 / 3065.6 / 1359.3 / 5228.5 ms |
| ZH→EN: “我胸口疼，而且我无法正常呼吸。” | ASR: “我胸口有疼而且我无法正常呼吸”; translation: “My chest hurts and I can't breathe properly.” | Initially breathing difficulty only; a regression fix recognizes this transcript's “胸口有疼” wording as chest pain | 531.0 / 231.1 / 90.5 / 854.5 ms |
| RU→EN: “У меня болит грудь.” | ASR: “у меня болит грудь.”; translation: “My chest hurts.” | `{}` because the conservative extractor currently covers English and Chinese source statements | 491.5 / 225.8 / 60.4 / 779.0 ms |

Each response returned a local `/audio/{id}.wav` URL. Fetching each URL returned HTTP 200, `audio/wav`, and nonempty WAV bytes. Translation timing includes emergency NLP; a separate repeated NLP call for the EN sample measured 0.046 ms. These timings mix cold and warm model states and are not benchmark averages.

A second ZH→EN synthesis of the same input produced a different ASR error, “胸口偶疼”, while the English translation still conveyed chest pain and breathing difficulty. The extractor initially missed chest pain again; the observed variant now has a targeted regression case. This is evidence that the synthetic Chinese ASR output varies and that highlighting can still miss patient details. The original transcript and translation must remain visible for human confirmation.

## Frontend checks

- Inspected the running app at 1200px desktop, 768px tablet, and 390px mobile widths; no horizontal overflow was observed.
- Russian responder / English patient: Quick Question text appeared; local audio played. Russian→English microphone recording displayed the actual ASR text and translation with a playable local audio element.
- English responder / Russian patient: Yes / No displayed the Russian question and large ДА / НЕТ controls with English labels beneath.
- Russian patient Quick Question Repeat: audio reached “Ready to repeat”; pressing Repeat changed state to “Playing for patient”, with the audio element advancing and not paused.
- Free Conversation Record Again returned to the microphone view.
- English→Chinese and Chinese→English browser recordings displayed real backend results and a loaded translated WAV. Pressing Play Translation advanced each audio element (0.98 s and 0.41 s observed) with playback active. The headless fake microphone loops its source WAV, so long recordings sometimes repeated a sentence or captured a partial second loop; the backend samples above use the complete original WAV once.
- With the backend deliberately stopped, a recorded upload displayed “Local backend unavailable. Check the device connection.” The backend was restarted afterwards.

## Failure and offline checks

- Backend tests cover empty audio, unsupported language, ASR failure, translation failure, TTS failure with visible text preserved, extraction failure with translation preserved, and a simulated low-confidence warning. Whisper currently returns `confidence: null`, so a real low-confidence threshold event was not observed.
- `frontend/index.html`, the React source, and Vite proxy use local files and `127.0.0.1`. Runtime model adapters load Whisper, Argos packages, and Piper voices from `models_local/`. Download calls are confined to explicit setup scripts.
- A real RU→EN pipeline request and a Russian Quick Question request both passed while the Python process rejected non-loopback socket connections. Local loopback sockets were allowed for the ASGI test harness.
- **Offline architecture verified; physical Airplane Mode test still requires manual confirmation.** No operating-system network setting was changed.

## Tomorrow's manual demo

1. In PowerShell, from `C:\Users\19573\Documents\Codex\FieldTalk-Backup`, run `& '.\.venv\Scripts\python.exe' -m uvicorn backend.main:app --host 127.0.0.1 --port 8000`.
2. In a second PowerShell window, set Node on PATH if necessary: `$env:PATH='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\node-v22.23.3-win-x64;' + $env:PATH`; then `Set-Location 'C:\Users\19573\Documents\Codex\FieldTalk-Backup\frontend'; npm run dev`.
3. Open `http://127.0.0.1:5173/`; confirm **OFFLINE READY**. Choose responder English and patient Chinese. Open Quick Questions, tap “Where does it hurt?”, watch the Chinese text and audio state, then press Repeat.
4. Return home and open Yes / No. Choose “Can you breathe normally?”, show the large Chinese response buttons, and tap an answer.
5. Return home, open Free Conversation, tap the microphone to start and again to stop. Say “I am allergic to penicillin and my chest hurts.” Show Original, Translation, patient-stated Important Information, and Play Translation. Use Record Again.
6. Return home, select responder Russian and patient English. Open Quick Questions or Free Conversation. Have the Russian teammate speak a short phrase such as “У меня болит грудь.” Compare the actual transcript and translation; record any dangerous meaning change in `data/russian_review_set.json` rather than assuming the synthetic results establish quality.
7. After all local models are installed, optionally repeat the demo in real Airplane Mode to confirm the physical offline claim. Keep both local servers running.

## Highest-priority human checks

1. Have the Russian teammate review all seven predefined phrases and six review cases, especially the translated bleeding question and Russian↔Chinese wording.
2. Try a real microphone in the intended noisy setting and confirm that allergy, pain, and breathing terms survive ASR and translation.
3. Run both servers with Wi-Fi disabled or Airplane Mode enabled and repeat one spoken round trip plus Quick Question audio.
