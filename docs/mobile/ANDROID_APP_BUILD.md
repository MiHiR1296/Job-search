# Android App Build Guide (MVP)

This guide is for the new Android app under `android-app/`.

## What is implemented now

- Kotlin + Jetpack Compose app scaffold
- Share-intent intake for job links/text
- Local per-job pack generation in app storage
- Local LLM interface with real on-device runtime path + fallback stub
- Foreground bubble service skeleton
- Accessibility service skeleton for future label parsing
- One-link job flow with in-app page text capture + auto detect company/role/salary
- One-tap URL-only flow:
  - app auto-switches to In-App Page to capture JD,
  - then auto-generates score + recommendations + outputs,
  - and auto-switches to Results when done
- Smart capture controls:
  - dedicated **AI Setup** tab for provider configuration (local/API),
  - API connection test button,
  - manual capture mode switch to avoid early login/ads-page misreads,
  - "Capture + Generate" button in WebView for user-controlled extraction timing
- Share-intent autopilot:
  - when you share a likely job link from other apps (LinkedIn/WhatsApp/Instagram/browser),
  - app auto-detects it as a job link and starts one-tap flow immediately
- Resume intake via share:
  - share a PDF resume to Career Ops Mobile and app stores its URI in profile
- Voice-first profile capture:
  - speech-to-text dictation buttons for onboarding fields
  - dedicated Memory tab to dictate long-form career stories
  - AI summarizes dictated stories into reusable long-term career memory
- Hybrid LLM mode:
  - local fallback
  - optional API-key provider mode (user-provided key stored on-device)
- API key is encrypted at rest with Android Keystore (AES-GCM)

## Privacy and data safety

Current defaults are privacy-focused:

- Personal data is stored locally on-device in app private storage.
- Generated packs stay in private app storage (`files/mobile-packs/...`) unless you explicitly export/share.
- No server sync is implemented in the current Android app code.
- If you choose API mode and provide an API key, prompt content is sent to your configured API endpoint.
- API key persistence is encrypted using Android Keystore before being written to DataStore.

If you later add cloud APIs, keep them optional and off by default.

## Project path

`android-app/`

## Open in Android Studio

1. Open Android Studio (latest stable).
2. Select **Open** and choose the `android-app` folder.
3. Let Gradle sync complete.
4. Run app on OnePlus 12R (USB debugging) or emulator.

## Minimum requirements

- Android Studio Hedgehog or newer
- Android SDK 34
- JDK 17 (bundled with Android Studio recommended)

## Current app flow

1. Share a job URL/text to "Career Ops Mobile" (or open app directly).
2. Fill profile once in Onboarding tab (including provider mode).
3. Paste/share job URL.
4. If the shared URL looks like a job link, app auto-starts one-tap flow without extra taps.
5. If page initially shows login/ads, keep Manual Capture Mode ON and press "Capture + Generate" only on real job content.
6. Dictate your profile and stories in Onboarding/Memory tabs; app summarizes durable insights.
7. App creates files in internal storage:
   - `files/mobile-packs/<date-slug>/...`
8. Generates:
   - fit score + recommendation
   - cover letter
   - resume highlights
   - form suggestions

## Provider modes

- **Local mode**: no API key required.
  - First tries on-device llama.cpp (`org.codeshipping:llama-kotlin-android`) with the GGUF path from **AI Setup**.
  - If the model is missing, fails to load, or returns **empty text**, the app falls back to a short **stub** draft (you will see an explicit note in the cover letter). Empty generations are often a **chat-template mismatch** (Qwen2.5 uses ChatML `im_start` / `im_end`; Llama 3.x uses `begin_of_text` + `start_header_id` / `eot_id`). The app picks the Llama 3 wrapper when the file path contains `llama-3`, `llama3`, `meta-llama-3`, etc.; otherwise it uses the Qwen-style wrapper.
  - Defaults use **4096** context and **768** max new tokens; very long JDs are truncated before prompting.
- **Job text to the model**: JSON-LD metadata is merged into captured text for scoring and field fill, then the `---STRUCTURED_JOB_METADATA---` block is **stripped** before cover-letter prompts so the model does not echo it. Employer/title from that block are parsed into company/role even when generic line heuristics miss.
- **API key mode**: user provides:
  - API Base URL
  - API model
  - API key
  and app calls `/chat/completions` style endpoint.

Default local model path in code:

- `/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf`

You can place the Qwen2.5 1.5B Q4_K_M GGUF there for immediate on-device generation.

**Repo `android-app/models/`**: contains `manifest.json` (suggested Hugging Face repos) and `README.md` for on-device vs **Git LFS** workflows. GGUF binaries are not committed as plain Git objects; use LFS if the team needs weights in git (see models README).

## Local LLM options on Android (recommended path)

For true on-device model inference, use one of:

- **MLC LLM** (good Android support; practical for 1.5B–3B quantized)
- **llama.cpp via JNI** (more custom work)

Suggested first model size for OnePlus 12R (8GB):

- Qwen2.5 1.5B Instruct, 4-bit quantized
- Phi-3.5 mini (quantized) can be evaluated if memory permits

Keep prompts concise and context trimmed to avoid latency spikes.

### Suggested on-device model shortlist (2026 mobile-practical)

- Qwen2.5 1.5B Instruct (4-bit)
- Qwen2.5 3B Instruct (4-bit) if memory/latency acceptable
- Phi-3.5 Mini Instruct (quantized), benchmark before committing

Recommendation for first production attempt on OnePlus 12R:

1. Use **Qwen2.5 7B–8B instruct Q4_K_M** on device when you want stronger reasoning; keep **1.5B** only for smoke tests.
2. Measure:
   - first-token latency,
   - total generation time for ~220-word cover letter,
   - thermal behavior over 5 consecutive generations.
3. If the phone cannot hold a 7B model in RAM, use **local heuristics + JSON-LD extraction** for job fields and switch **AI Setup → API mode** for cover-letter quality when needed.

## Debug install upgrades (no uninstall loop)

Debug APKs are signed with the **shared** keystore under `android-app/keystore/career-ops-debug.jks` (see that folder’s README). That way **CI builds and local Android Studio builds** use the **same signing key**, so you can install a new debug APK over the old one like a normal update.

`versionCode` is set from **`git rev-list --count HEAD`**, so each new commit produces a higher version and Android accepts upgrades. CI checks out with **full git history** (`fetch-depth: 0`) so the count is not stuck at `1`.

**One-time migration:** If you already have the app installed from an **older** build that used the default debug key or a different machine key, Android will refuse the update. **Uninstall once**, then install a build from this setup; after that, in-place upgrades should work.

## Manual verification checklist (post UX overhaul)

- Cold install: Onboarding wizard appears; resume upload auto-fills; finishing onboarding lands on **Job** tab.
- Relaunch: app opens on **Job** tab (onboarding not repeated).
- Share a LinkedIn job URL: one-tap flow fills company/role from page; Results shows fit score.
- Indeed (or heavy login): **Open job in Custom Tab** loads the listing; return to app, use **In-App Page** or paste JD override if needed.
- Monthly pay line (e.g. `$X/mo`): salary hint appears in Job Input “Detected”; scoring adds a note when pay is not LPA.
- Results: add **Notes for this job** and **Regenerate with these notes**; cover letter should reflect those notes.
- JSON-LD: job pages that embed `JobPosting` schema should surface employer/title/pay in extracted text block.

## Next implementation steps

1. Add onboarding UI to choose local GGUF model path and inference knobs.
2. Implement real bubble overlay UI (WindowManager).
3. Add accessibility label extraction and field suggestion mapper.
4. Add export/share of generated pack files to user-visible storage.
5. Add optional Google Sign-In and user profile bootstrap (still keep core flow usable without login).
6. Add robust resume parser pipeline to extract structured profile from PDF content.

## Notes

- Final form submit remains manual by design.
- Accessibility and overlay permissions should be user-initiated with clear rationale.
