# FieldTalk work checkpoint

Updated: 2026-10-03 (Asia/Shanghai)

- Current branch: `backup/codex-full-stack`
- Origin: `https://github.com/wjy-Jerry/-Hackau---Medical-offline-Translation-AI.git`
- Latest successful application commit before this checkpoint update: `042787d5b6b0837c044614bae1d58bb53ef2a4fc`
- Current phase: Phase 1 integration validation; Russian API and UI integration is complete after this checkpoint commit.

## Completed tasks

- The emergency frontend redesign and its audio cache fix are committed and pushed.
- Phase 0: 66 tests passed, 2 skipped; frontend production build passed; `/health` returned ready for the installed EN/ZH ASR, translation, and TTS components.
- Phase 0: an actual locally generated English WAV passed through `/process_audio` to Chinese text, patient-stated allergy extraction, and a local audio URL. No cloud service was used for inference.
- Local Whisper base, EN/ZH Argos packages, and EN/ZH Piper voices are present in Git-ignored `models_local/`.
- Russian ASR uses that same multilingual Whisper base model. Six distinct locally synthesized Russian utterances were recognized; some words were misspelled or inflected differently, so native review is still required.
- Official Argos `ru→en` and `en→ru` packages are locally installed. `ru↔zh` uses an English pivot. The Russian Argos packages' Stanza resource metadata is incompatible with the installed Stanza version; their short utterances use local punctuation splitting instead. Relevant RU translation tests and existing pipeline regressions passed. All four Argos packages remain Git ignored.
- The Russian Piper `ru_RU-irina-medium` voice is installed and the unchanged `text_to_speech(text, language)` interface now generates real Russian WAV files. The TTS contract, Russian ASR, baseline pipeline, and `/health` checks passed.
- Seven English/Chinese/Russian Quick Questions are present, each Russian wording marked `pending_native_speaker_validation`. `data/russian_review_set.json` records six actual synthetic speech ASR and translation results with empty human-review fields. The generator is `python -m scripts.build_russian_review_set`. Phrase, review-data, quick-question, and frontend-build checks passed.
- The API now accepts all six different-language pairs among EN/ZH/RU with the same `/process_audio` schema. Quick Question audio accepts Russian. The existing frontend language selector and Yes/No controls include Russian and retain the emergency visual layout. A neutral reminder marks Russian wording as pending native-speaker review. API, phrase, translation, and frontend-build checks passed.

## Unfinished task and exact next action

Run real synthetic Russian speech → local ASR → English translation → English TTS through `/process_audio`, and inspect the actual frontend at desktop/tablet/mobile widths including Russian Quick Questions and Yes/No. Record timing and fix any integration bugs. Then begin Phase 2 final validation: EN→ZH, ZH→EN, RU→EN, Quick Questions, Yes/No, Free Conversation, failure states, offline-code inspection, and practical tests. Commit/push each stable validation or fix milestone.

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

Do not rerun the baseline tests above unless subsequent changes require them. Do not redownload the existing models. For a stable subphase, stage only source/checkpoint files, commit, and push with:

```powershell
& 'C:\Program Files\Git\cmd\git.exe' push origin backup/codex-full-stack
```

## Known errors and blockers

- Real RU end-to-end API and browser checks remain unfinished after the UI/API integration milestone.
- Russian ASR testing so far used synthetic Piper speech, not a native speaker recording. Native-speaker quality validation remains pending.
- EN→RU model translated “Are you bleeding?” as wording closer to “Do you have blood?” in one check. Use provisional reviewed phrase-pack wording for Quick Questions and flag free-response Russian for human confirmation.
- RU→ZH pivot produced repeated wording for one bleeding phrase. Treat RU↔ZH as experimental until native-speaker review.
- Git's global URL rewrite breaks GitHub access in this environment; use `$env:GIT_CONFIG_GLOBAL='NUL'` for all Git commands.
- Git author identity is not configured in this environment. For a commit, use the prior commit identity as one-command options: `-c user.name='Jerry Wang' -c user.email='72400309@cityu-dg.edu.cn'`.
- Two optional real-ASR fixture tests were skipped because `FIELDTALK_ASR_TEST_AUDIO_DIR` was not set. The separate real EN/ZH pipeline request passed.

## Services and processes to restart

- No backend or frontend server is currently running. Start backend with `& '.\.venv\Scripts\python.exe' -m uvicorn backend.main:app --host 127.0.0.1 --port 8000` from the repository root.
- Start frontend in another PowerShell terminal after adding the portable Node directory above to `PATH`: `Set-Location frontend; npm run dev`.
