# Career Ops Mobile Android App (MVP)

This is the Android companion app for mobile-first job applications.

## Implemented in this milestone

- Compose-based app shell
- Share-intent intake for job links/text
- Per-job application pack generation in app private storage
- Local LLM interface abstraction (`LocalLlmEngine`) with a stub implementation
- Floating bubble service skeleton
- Accessibility service skeleton with basic label-to-value autofill mapping

## Folder structure

```text
android-app/
  app/
    src/main/java/com/careerops/mobile/
      data/
      llm/
      service/
      ui/
```

## Build

Open `android-app/` in Android Studio and run on a device (Android 10+).

Detailed setup: `../docs/mobile/ANDROID_APP_BUILD.md`

## Storage path

Generated packs are written to:

`/data/data/com.careerops.mobile/files/mobile-packs/<date-slug>/`

Use Android Studio Device File Explorer or add export/share flow in next milestone.

## Next steps

1. Persist editable candidate profile via DataStore.
2. Replace stub LLM engine with on-device model runtime (MLC LLM or llama.cpp JNI).
3. Implement actual overlay UI with actionable buttons.
4. Improve accessibility autofill with better context extraction.
5. Add export to user-visible Documents folder.
