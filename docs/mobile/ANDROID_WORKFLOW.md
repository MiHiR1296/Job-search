# Android-First Workflow (No Browser Plugin)

This workflow is built for applying from phone across many portals quickly.

## What you do

1. Share/paste a job URL to your assistant.
2. Generate a mobile application pack for that role.
3. Open the pack files on phone while filling forms.
4. Copy/paste answers, cover letter, and upload the tailored resume.

## Generate one pack

From repo root:

```bash
node tools/mobile/generate-application-pack.mjs \
  --company="Framestore" \
  --role="Lead Texture Artist" \
  --url="https://www.linkedin.com/jobs/view/4375111638/"
```

This creates:

```text
mobile/YYYY-MM-DD-company-role/
  - <slug>-application-pack.md
  - <slug>-form-answers.md
  - <slug>-cover-letter.md
  - <slug>-resume.md
```

## Make it practical on OnePlus 12R

### Option A: Sync folder to phone

- Keep repo synced to phone via Google Drive / Syncthing / Git client.
- Open `mobile/<job-folder>/` files while applying in Chrome/LinkedIn app.

### Option B: Use split-screen

- Left: job portal app/browser
- Right: notes app with `form-answers.md` and `cover-letter.md`
- Copy-paste field values quickly.

### Option C: Use Android clipboard manager

- Keep commonly reused items pinned (email, phone, LinkedIn, portfolio).
- Use pack file for role-specific content only.

## Fast cycle per application

1. Generate pack
2. Generate tailored resume (`modes/tailored-cv.md`) -> save into pack resume file
3. Generate cover letter (`modes/cover-letter.md`) -> save into pack cover file
4. Apply on portal from phone
5. Update tracker and post-application learnings

## Why this works without plugin

- No portal-specific dependency.
- Works across LinkedIn, Naukri, company ATS pages, etc.
- Keeps data portable and reusable even when switching apps.

## Native Android app progress

- Android app scaffold now exists in `android-app/`.
- Build/run guide: `docs/mobile/ANDROID_APP_BUILD.md`
- Current app supports:
  - shared-link autopilot (detect likely job links and auto-start one-tap flow),
  - deep-link intake (`ACTION_VIEW`) and shared PDF resume URI capture,
  - one-tap URL-only JD capture + scoring + generation,
  - local on-device LLM runtime path + API-key mode fallback,
  - voice-first onboarding and long-term career memory summarization,
  - floating bubble + accessibility service skeletons.
