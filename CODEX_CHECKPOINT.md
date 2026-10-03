# FieldTalk work checkpoint

Updated: 2026-10-03 (Asia/Shanghai)

- Current branch: `backup/codex-full-stack`
- Origin: `https://github.com/wjy-Jerry/-Hackau---Medical-offline-Translation-AI.git`
- Latest successful commit: the commit containing this checkpoint; run `git rev-parse HEAD` for its hash.
- Current phase: new 12-phase autonomous plan, Phase 12 final validation complete; remaining checks require a real device, microphone, and human language review.

## Completed tasks

- New plan Phase 0: verified clean personal-fork branch, 101 passed/2 skipped tests, and frontend production build.
- New plan Phase 1: modular local ambulance phrase pack now drives frontend and backend Quick Questions. Seven EN/ZH/RU phrases retain their prior wording and API. Russian is explicitly pending native-speaker validation; EN/ZH team review has not been claimed. See `docs/PHRASE_REVIEW.md`. Checks: 103 passed/2 skipped and frontend build passed.
- New plan Phase 2: frontend now names unclear speech, missing audio, unavailable translation, unsupported pairs, and missing local backend with a safe next action. TTS failure leaves translated text visible. Processing shows one honest status instead of timed pseudo-stages. Checks: 1 frontend unit test, 52 relevant API tests, and frontend build passed.
- New plan Phase 3: conservative English/Chinese extraction now highlights explicitly stated dizziness, nausea, and fever as `other_symptom`, in addition to the existing six target fields. Added positive, negated, and combined-statement cases. Checks: 102 relevant Python tests, 1 frontend unit test, and frontend build passed. Russian extraction is still limited; no inferred fields are inserted.
- New plan Phase 4: in-memory Emergency Handoff Card stores only manually added patient statements and whitelisted extracted facts; missing fields say `Unknown / Not stated`. The card has View Handoff and confirmed clear session actions. No personal data is persisted or sent to a new service. Checks: 3 frontend unit tests, 102 relevant Python tests, and frontend build passed. Browser/hardware confirmation remains pending.
- New plan Phase 5: home copy emphasizes critical offline communication; Quick Questions remain the visually primary action, Handoff is visible on home. Actual Chrome screenshots at 1200/768/390 px had no horizontal overflow. Browser handoff flow used a real RU→EN local API response from the Russian WAV fixture with the microphone request substituted in Chrome because headless microphone capture returned no speech. The card now displays the original Russian and English translation even when conservative extraction finds no structured RU fields. Checks: 3 frontend tests, 50 API tests, frontend build, and browser layout/flow passed. The simulated browser transport is not a real microphone validation.
- New plan Phase 6: `docs/EXHIBITION_DEMO.md` provides a 60–90 second live flow, suggested sentences, and honest microphone/backend fallback. Documentation commands match the current repo; no app code changed.
- New plan Phase 7: `docs/PITCH_POSITIONING.md` describes the ambulance MVP, verified prototype boundaries, comparative positioning without named-competitor claims, and candidate future domain packs. No app code changed.
- New plan Phase 8: `docs/ANDROID_ARCHITECTURE.md` compares the desktop pipeline with a standalone native Android app, documents measured desktop model sizes, candidate on-device recognizer/ML Kit translation/embedded TTS, conditional offline capability, build requirements, and risks using official sources. No Android code or SDK has been installed yet.
- New plan Phase 9 source milestone: separate native Android Java/Gradle project includes shared ambulance phrase assets, 21 pre-generated local Piper WAVs (1.4 MB total), home/Quick Questions/Yes-No/in-memory Handoff. Free Conversation only uses `createOnDeviceSpeechRecognizer`, already-downloaded ML Kit models, and a non-network embedded TTS voice; model provisioning is an explicit connected setup. It reports missing capabilities and keeps text visible. Android structured extraction is intentionally unavailable; no facts are invented. **Android source has not yet compiled because SDK/Gradle are absent.** Tests before commit: 118 Python passed/2 skipped, 3 frontend passed, frontend build passed. This commit is a recoverable source milestone, not an APK validation.
- New plan Phase 10: official Android command-line tools SHA-256 verified, API 35/build-tools/adb installed under projectless `work/android-sdk`; official Gradle 8.9 SHA-256 verified under `work/gradle-dist`. The Gradle wrapper JAR checksum also matched Gradle's published checksum. Debug APK compiled and Android lint passed after replacing an API-33-only read call. `:app:testDebugUnitTest` had NO-SOURCE; Python Android asset tests passed in the 118-test run. APK is 73,414,450 bytes at repo `dist/FieldTalk-debug.apk` and projectless `outputs/FieldTalk-debug.apk`, SHA-256 `C5FF203D07ED142B484D3A167B68E45702584FBCD69EDEB7DA97A02436FCB5AF`. Signature verification passed with APK v2; archive contains 4 phrase JSON and 21 WAV assets. No emulator or physical phone verification yet.
- New plan Phase 11: `adb devices -l` found no attached device. Isolated SDK has no emulator/system image. `aapt` confirmed APK package, min/target API, launcher, and two permissions plus ML Kit's merged dependencies. Android source has no WebView or laptop backend call. `docs/ANDROID_VALIDATION.md` records exact build/static results, unverified runtime behavior, and device-install/manual-check commands.
- New plan Phase 12: `docs/FINAL_VALIDATION.md` records final results. Full Python suite 118 passed/2 skipped, frontend 3 passed/build passed, four real local synthetic WAV API directions plus EN/ZH/RU Quick Question WAV passed, browser Quick/Yes-No/responsive checks passed, prior controlled-result Handoff browser check passed, Android assemble/lint/signature/hash passed. No Android emulator/phone, physical Airplane Mode, or real-microphone validation was possible.

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

No automated phase remains. The next action for the team is to install `dist/FieldTalk-debug.apk` on an Android 12+ phone using the commands in `docs/ANDROID_VALIDATION.md`, run its five manual workflow checks and the conditional offline conversation test, then review Russian wording with a native speaker. Also rehearse the Windows web fallback with a real microphone in physical Airplane Mode. Record any found defects in follow-up commits; do not mark human review complete without an actual reviewer.

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

Android build/validation setup from the repo root:

```powershell
$env:JAVA_HOME='D:\java'
$env:ANDROID_HOME='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\android-sdk'
$env:GRADLE_USER_HOME='C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\gradle-cache'
$env:PATH='C:\Windows\System32;D:\java\bin;' + $env:PATH
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
& 'C:\Users\19573\Documents\Codex\2026-10-02\https-github-com-rrrrrrl-hackau-medical\work\gradle-dist\gradle-8.9\bin\gradle.bat' -p .\android :app:assembleDebug :app:lintDebug --no-daemon --console=plain
```

## Known errors and blockers

- Russian speech evaluation used Piper synthetic audio only; real native-speaker speech quality has not been measured.
- EN→RU model translated “Are you bleeding?” as wording closer to “Do you have blood?” in one check. Use the local phrase-pack wording for Quick Questions and flag free-response Russian for human confirmation. The phrase pack has not been marked human-reviewed.
- RU→ZH pivot produced repeated wording for one bleeding phrase. Treat RU↔ZH as experimental until native-speaker review.
- Chinese synthetic ASR changed “胸口疼” into variants including “胸口有疼” and “胸口偶疼”; the extractor has targeted coverage, but further speech variations can still be missed. The original and translation remain visible for confirmation.
- Offline architecture was verified with non-loopback socket denial. **Physical Airplane Mode has not been tested.**
- Git's global URL rewrite breaks GitHub access in this environment; use `$env:GIT_CONFIG_GLOBAL='NUL'` for all Git commands.
- Git author identity is not configured in this environment. For a commit, use the prior commit identity as one-command options: `-c user.name='Jerry Wang' -c user.email='72400309@cityu-dg.edu.cn'`.
- Two optional real-ASR fixture tests were skipped because `FIELDTALK_ASR_TEST_AUDIO_DIR` was not set. The separate real EN/ZH pipeline request passed.
- Android Free Conversation requires device on-device ASR, a connected one-time ML Kit model setup, and an embedded target-language voice. None of those device capabilities have been measured here; the APK's bundled phrase workflows are the guaranteed offline portion. Android structured extraction is unavailable, so Handoff records original/translation only.

## Services and processes to restart

- No backend or frontend server is currently running. Start backend with `& '.\.venv\Scripts\python.exe' -m uvicorn backend.main:app --host 127.0.0.1 --port 8000` from the repository root.
- Start frontend in another PowerShell terminal after adding the portable Node directory above to `PATH`: `Set-Location frontend; npm run dev`.
