# Android App Build Guide (MVP)

This guide is for the new Android app under `android-app/`.

## What is implemented now

- Kotlin + Jetpack Compose app scaffold
- Share-intent intake for job links/text
- Local per-job pack generation in app storage
- Local LLM interface abstraction + stub engine
- Foreground bubble service skeleton
- Accessibility service skeleton for future label parsing

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
2. Fill company + role + URL.
3. Tap "Generate Mobile Application Pack".
4. App creates files in internal storage:
   - `files/mobile-packs/<date-slug>/...`
5. Stub local LLM generates initial cover-letter/resume text.

## Local LLM options on Android (recommended path)

For on-device inference, use one of:

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
