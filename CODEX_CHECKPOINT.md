# FieldTalk work checkpoint

Updated: 2026-10-03 (Asia/Shanghai)

- Current branch: `backup/codex-full-stack`
- Origin: `https://github.com/wjy-Jerry/-Hackau---Medical-offline-Translation-AI.git`
- Latest successful commit: the commit containing this checkpoint; run `git rev-parse HEAD` for its hash.
- Current phase: new 12-phase autonomous plan, Phase 1 complete; Phase 2 is next.

## Completed tasks

- New plan Phase 0: verified clean personal-fork branch, 101 passed/2 skipped tests, and frontend production build.
- New plan Phase 1: modular local ambulance phrase pack now drives frontend and backend Quick Questions. Seven EN/ZH/RU phrases retain their prior wording and API. Russian is explicitly pending native-speaker validation; EN/ZH team review has not been claimed. See `docs/PHRASE_REVIEW.md`. Checks: 103 passed/2 skipped and frontend build passed.

- The emergency frontend redesign and its audio cache fix are committed and pushed.
- Phase 0: 66 tests passed, 2 skipped; frontend production build passed; `/health` returned ready for the installed EN/ZH ASR, translation, and TTS components.
- Phase 0: an actual locally generated English WAV passed through `/process_audio` to Chinese text, patient-stated allergy extraction, and a local audio URL. No cloud service was used for inference.
- Local Whisper base, EN/ZH Argos packages, and EN/ZH Piper voices are present in Git-ignored `models_local/`.
- Russian ASR uses that same multilingual Whisper base model. Six distinct locally synthesized Russian utterances were recognized; some words were misspelled or inflected differently, so native review is still required.
- Official Argos `ru→en` and `en→ru` packages are locally installed. `ru↔zh` uses an English pivot. The Russian Argos packages' Stanza resource metadata is incompatible with the installed Stanza version; their short utterances use local punctuation splitting instead. Relevant RU translation tests and existing pipeline regressions passed. All four Argos packages remain Git ignored.
- The Russian Piper `ru_RU-irina-medium` voice is installed and the unchanged `text_to_speech(text, language)` interface now generates real Russian WAV files. The TTS contract, Russian ASR, baseline pipeline, and `/health` checks passed.
- Seven English/Chinese/Russian Quick Questions are present, each Russian wording marked `pending_native_speaker_validation`. `data/russian_review_set.json` records six actual synthetic speech ASR and translation results with empty human-review fields. The generator is `python -m scripts.build_russian_review_set`. Phrase, review-data, quick-question, and frontend-build checks passed.
- The API now accepts all six different-language pairs among EN/ZH/RU with the same `/process_audio` schema. Quick Question audio accepts Russian. The existing frontend language selector and Yes/No controls include Russian and retain the emergency visual layout. A neutral reminder marks Russian wording as pending native-speaker review. API, phrase, translation, and frontend-build checks passed.
- Real locally synthesized Russian speech completed RU→EN and RU→ZH `/process_audio` requests with local WAV retrieval. The RU→EN chest-pain sample returned “У меня болит грудь.” → “My chest hurts.” with ASR 761.7 ms, translation 2124.8 ms, TTS 1314.5 ms, total 4202.8 ms. The RU→ZH allergy sample completed in 3016.8 ms total. Actual browser checks at 1200/768/390px showed no horizontal overflow and exercised Quick Questions, Russian Yes/No, and RU→EN Free Conversation recording/result/audio presence.
- Phase 2 real synthetic-audio samples: EN→ZH allergy plus chest pain returned both `allergy: Penicillin` and `symptom: Chest pain` in 5228.5 ms total. ZH→EN chest pain plus breathing difficulty returned readable English and WAV in 854.5 ms; ASR added an extra `有` before `疼`, initially causing a chest-pain highlight miss. RU→EN chest pain returned readable English and WAV in 779.0 ms. The conservative Chinese pain pattern now handles that observed ASR variant. Emergency information extraction exceptions now leave the translation usable with `{}` highlights. Relevant tests, including empty audio, unsupported languages, ASR/translation/TTS failures, and low-confidence warning simulation, passed.
- Browser follow-up verified Russian Quick Question audio reached Ready and Repeat advanced actual playback, Yes/No Russian buttons were large and selected correctly, Free Conversation Record Again reset the microphone view, and the backend-unavailable message was clear after a frontend fix. EN→ZH and ZH→EN browser recordings loaded and played translated WAVs. The fake microphone loops source WAVs if held too long, so its duplicate tails are a fixture artifact.
- Runtime review found no external frontend URLs or download calls in model adapters. Local Whisper, four Argos packages, and three Piper voices are present. A real RU→EN request and Russian Quick Question succeeded while non-loopback Python socket connections were blocked. The combined setup command reported every existing model present and downloaded nothing.
- Detailed evidence and tomorrow's manual demo steps are in `docs/DEMO_VALIDATION.md`.
- Final checks after all code changes: `python -m pytest -q` reported 101 passed, 2 skipped, 1 dependency deprecation warning; `npm run build` passed; `git diff --check` found no whitespace errors. The skipped tests need optional manually recorded WAV fixtures.

## Unfinished task and exact next action

Start Phase 2 of the user's new 12-phase plan: add clear fail-safe states for unclear speech, processing, translation failure, audio unavailable, unsupported language, and no audio captured. Preserve translated text on TTS failure and do not invent emergency facts. Run relevant tests, commit `feat: add fail-safe emergency communication states`, and push only `origin backup/codex-full-stack`. Then continue in strict phase order. Human review and physical Airplane Mode checks remain pending.

## Continue commands (PowerShell)

```powershell
Set-Location 'C:\Users\19573\Documents\Codex\FieldTalk-Backup'
$env:GIT_CONFIG_GLOBAL='NUL'
& 'C:\Program Files\Git\cmd\git.exe' status --short --branch
& 'C:\Program Files\Git\cmd\git.exe' branch --show-current
& 'C:\Program Files\Git\cmd\git.exe' remote -v
Get-Content CODEX_CHECKPOINT.md -Raw -Encoding UTF8
& '.\.venv\Scripts\python.exe' -m pytest -q
$env:PATH='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\node-v22.23.3-win-x64;' + $env:PATH
Push-Location frontend; npm run build; Pop-Location
```

Do not rerun successful tests unless subsequent changes require them. Do not redownload the existing models. For a later stable fix, stage only source/checkpoint files, commit, and push with:

```powershell
& 'C:\Program Files\Git\cmd\git.exe' push origin backup/codex-full-stack
```

## Known errors and blockers

- Russian speech evaluation used Piper synthetic audio only; real native-speaker speech quality has not been measured.
- EN→RU model translated “Are you bleeding?” as wording closer to “Do you have blood?” in one check. Use provisional reviewed phrase-pack wording for Quick Questions and flag free-response Russian for human confirmation.
- RU→ZH pivot produced repeated wording for one bleeding phrase. Treat RU↔ZH as experimental until native-speaker review.
- Chinese synthetic ASR changed “胸口疼” into variants including “胸口有疼” and “胸口偶疼”; the extractor has targeted coverage, but further speech variations can still be missed. The original and translation remain visible for confirmation.
- Offline architecture was verified with non-loopback socket denial. **Physical Airplane Mode has not been tested.**
- Git's global URL rewrite breaks GitHub access in this environment; use `$env:GIT_CONFIG_GLOBAL='NUL'` for all Git commands.
- Git author identity is not configured in this environment. For a commit, use the prior commit identity as one-command options: `-c user.name='Jerry Wang' -c user.email='72400309@cityu-dg.edu.cn'`.
- Two optional real-ASR fixture tests were skipped because `FIELDTALK_ASR_TEST_AUDIO_DIR` was not set. The separate real EN/ZH pipeline request passed.

## Services and processes to restart

- No backend or frontend server is currently running. Start backend with `& '.\.venv\Scripts\python.exe' -m uvicorn backend.main:app --host 127.0.0.1 --port 8000` from the repository root.
- Start frontend in another PowerShell terminal after adding the portable Node directory above to `PATH`: `Set-Location frontend; npm run dev`.
