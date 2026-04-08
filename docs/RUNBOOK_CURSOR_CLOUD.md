# Cursor Cloud Runbook (Career Ops India)

This document is the single reference for:

- environment setup used in this repo,
- where Android/mobile artifacts are located,
- what has been implemented so far,
- exact commands to rebuild and continue quickly.

No secrets are stored here. Do not add API keys, tokens, or private credentials.

---

## 1) Repository and branch

- Repo root: `/workspace`
- Git remote (`origin`): `https://github.com/MiHiR1296/Job-search`
- Active development branch: `cursor/career-ops-india-78d5`
- Base branch: `main`
- Current PR: `https://github.com/MiHiR1296/Job-search/pull/1`

---

## 2) Runtime environment used (Cursor Cloud VM)

- OS: Linux `6.12.58+`
- Shell: `bash`
- Java: OpenJDK `21.0.10`
- Gradle (wrapper runtime): `8.8`
- Android project: `/workspace/android-app`
- Android SDK root used here: `/workspace/android-sdk`
- Android SDK compile platform: `android-34`
- Android Build Tools: `34.0.0`
- Android local properties file:
  - `/workspace/android-app/local.properties`
  - contains: `sdk.dir=/workspace/android-sdk`

---

## 3) Key paths and artifacts

### Android app source

- App root: `/workspace/android-app`
- Manifest: `/workspace/android-app/app/src/main/AndroidManifest.xml`
- Main activity: `/workspace/android-app/app/src/main/java/com/careerops/mobile/MainActivity.kt`
- Main UI: `/workspace/android-app/app/src/main/java/com/careerops/mobile/ui/MainScreen.kt`
- ViewModel: `/workspace/android-app/app/src/main/java/com/careerops/mobile/ui/MainViewModel.kt`
- Profile persistence: `/workspace/android-app/app/src/main/java/com/careerops/mobile/data/ProfileStore.kt`
- Data models: `/workspace/android-app/app/src/main/java/com/careerops/mobile/data/Models.kt`
- LLM routing:
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/llm/LocalLlmEngine.kt`
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/llm/HybridLlmEngine.kt`
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/llm/LlamaCppLocalLlmEngine.kt`
- API key encryption:
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/security/ApiKeyCrypto.kt`
- Voice dictation helper:
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/voice/VoiceDictationManager.kt`
- Web extraction helpers:
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/web/JobPageExtractor.kt`
  - `/workspace/android-app/app/src/main/java/com/careerops/mobile/web/FormSuggestionEngine.kt`

### Built APK

- Build output (Gradle):  
  `/workspace/android-app/app/build/outputs/apk/debug/app-debug.apk`
- Repository-shared APK path:  
  `/workspace/apk/CareerOpsMobile-debug.apk`

---

## 4) What is implemented so far

### Core workflow

1. Share or open job link in app.
2. App can detect likely job links and auto-start one-tap flow.
3. In-app WebView captures JD/page text.
4. Company/role/salary hints are extracted.
5. Fit score and recommendation generated.
6. Cover letter + resume highlights generated.
7. Form suggestions shown from visible page text.
8. Outputs saved in app storage and exposed in results UI.

### LLM modes

- Local mode:
  - Attempts on-device llama.cpp runtime (via `org.codeshipping:llama-kotlin-android`).
  - Default GGUF path target in code:  
    `/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf`
  - Falls back to stub local generator if model is unavailable.
- API mode:
  - OpenAI-compatible `/chat/completions` endpoint.
  - Uses user-provided base URL, model, API key.

### Security and privacy

- API key encrypted at rest using Android Keystore (AES-GCM).
- Profile data stored locally in DataStore.
- Backward-compat read for legacy plaintext key was added, and migration to encrypted key on save.
- No server sync layer in app.

### Voice-first additions

- Microphone permission added.
- Speech-to-text dictation flow added.
- Dictation shortcuts in onboarding fields.
- Memory tab to capture long spoken narratives.
- AI summarization into persistent long-term career memory.

### Share intent additions

- Handles:
  - `ACTION_SEND` for text links,
  - `ACTION_VIEW` for deep links (`http/https`),
  - `ACTION_SEND` PDF (`application/pdf`) for resume URI capture.

---

## 5) Important URLs and references

- Repository: `https://github.com/MiHiR1296/Job-search`
- PR: `https://github.com/MiHiR1296/Job-search/pull/1`
- Sample job URL used during development:
  - `https://www.linkedin.com/jobs/view/4375111638/`
- Local runtime dependency used:
  - `org.codeshipping:llama-kotlin-android:0.1.0`
- MLC Android docs reference (research):
  - `https://llm.mlc.ai/docs/deploy/android.html`

---

## 6) Rebuild and release commands (from this environment)

Run from repo root unless stated otherwise.

### Build APK

```bash
cd /workspace/android-app
./gradlew assembleDebug
```

### Copy APK to shared repo artifact path

```bash
cp -f /workspace/android-app/app/build/outputs/apk/debug/app-debug.apk /workspace/apk/CareerOpsMobile-debug.apk
```

### Git flow used

```bash
cd /workspace
git add <files>
git commit -m "Your message"
git push -u origin cursor/career-ops-india-78d5
```

---

## 7) Recent implementation milestones (commit references)

- `409e67c` - reproducible Android APK builds with Gradle wrapper
- `d39b2f0` - first debug APK artifact added
- `fa32f7d` - onboarding + in-app JD extraction + live suggestions
- `e87d01b` - one-link extraction/scoring/autofill guidance
- `dccbad9` - hybrid API-key + local provider modes
- `1e3a67b` - URL-only auto flow + local runtime + encrypted key storage
- `4456aab` - shared-link autopilot + voice dictation + career memory

---

## 8) Operational notes for future sessions

- Do not commit secrets or raw API keys.
- If build fails with Android SDK path error:
  - ensure `/workspace/android-app/local.properties` has:
    - `sdk.dir=/workspace/android-sdk`
- If APK gets large warnings on push:
  - current workflow still pushes `apk/CareerOpsMobile-debug.apk`,
  - GitHub warns >50MB but accepts under hard limit.
- If local model unavailable on device:
  - app should still function via fallback or API mode.

---

## 9) Documentation map

- Root usage: `/workspace/README.md`
- Setup: `/workspace/docs/SETUP.md`
- Workflow: `/workspace/docs/WORKFLOW.md`
- Customization: `/workspace/docs/CUSTOMIZATION.md`
- Autofill helper: `/workspace/docs/AUTOFILL.md`
- Android build and app behavior: `/workspace/docs/mobile/ANDROID_APP_BUILD.md`
- Android practical workflow: `/workspace/docs/mobile/ANDROID_WORKFLOW.md`
- Floating bubble spec: `/workspace/docs/mobile/FLOATING_BUBBLE_APP_SPEC.md`

