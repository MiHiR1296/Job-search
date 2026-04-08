# Android App Build Guide (MVP)

This guide is for the new Android app under `android-app/`.

## What is implemented now

- Kotlin + Jetpack Compose app scaffold
- Share-intent intake for job links/text
- Local per-job pack generation in app storage
- Local LLM interface abstraction + stub engine
- Foreground bubble service skeleton
- Accessibility service skeleton for future label parsing
- One-link job flow with in-app page text capture + auto detect company/role/salary
- Hybrid LLM mode:
  - local fallback
  - optional API-key provider mode (user-provided key stored on-device)

## Privacy and data safety

Current defaults are privacy-focused:

- Personal data is stored locally on-device in app private storage.
- Generated packs stay in private app storage (`files/mobile-packs/...`) unless you explicitly export/share.
- No server sync is implemented in the current Android app code.
- If you choose API mode and provide an API key, prompt content is sent to your configured API endpoint.

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
4. (Optional) Open In-App Page tab and capture page text.
5. Tap "Generate full output".
6. App creates files in internal storage:
   - `files/mobile-packs/<date-slug>/...`
7. Generates:
   - fit score + recommendation
   - cover letter
   - resume highlights
   - form suggestions

## Provider modes

- **Local mode**: no API key required, uses local fallback generator.
- **API key mode**: user provides:
  - API Base URL
  - API model
  - API key
  and app calls `/chat/completions` style endpoint.

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

1. Start with Qwen2.5 1.5B quantized.
2. Measure:
   - first-token latency,
   - total generation time for ~220-word cover letter,
   - thermal behavior over 5 consecutive generations.
3. Move to larger model only if experience remains responsive.

## Next implementation steps

1. Replace `StubLocalLlmEngine` with real MLC/llama.cpp engine wrapper.
2. Add profile editor + persisted storage (DataStore).
3. Implement real bubble overlay UI (WindowManager).
4. Add accessibility label extraction and field suggestion mapper.
5. Add export/share of generated pack files to user-visible storage.

## Notes

- Final form submit remains manual by design.
- Accessibility and overlay permissions should be user-initiated with clear rationale.
