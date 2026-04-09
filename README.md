# Career Ops India

AI-assisted job search operating system tailored for India-based opportunities.

This repository gives you a practical, customizable workflow to:

- discover relevant openings from India-heavy portals and company career pages,
- score opportunities with a consistent rubric (fit, compensation, growth, risk),
- generate role-specific resume variants and outreach drafts,
- track all applications in one place.

> This is a filter-first system, not a mass-apply bot. Use it to focus on high-quality roles.

---

## What You Get

- **India-specific portal configuration** (Naukri, Instahyre, Cutshort, LinkedIn India, Hirist, Wellfound, Foundit, IIMJobs)
- **Offer evaluation rubric in INR/LPA terms**
- **Profile template** for your goals, constraints, and story
- **Job tracker format** for applications and interview stages
- **Reusable AI modes/prompts** for evaluate, scan, CV tailoring, and pipeline updates
- **Learning loop memory system** that improves resume/cover letter quality after each application
- **Android-first application packs** for fast multi-portal phone applications

---

## Quick Start

1) Copy templates:

```bash
cp config/profile.example.yml config/profile.yml
cp templates/portals.india.example.yml portals.yml
cp examples/cv.example.md cv.md
```

2) Edit these files:

- `config/profile.yml` -> your background, target roles, compensation floor
- `portals.yml` -> portals, filters, and company watchlist
- `cv.md` -> master resume in markdown

3) Start using your AI assistant in this repo:

- Use prompts in `modes/` (or copy them into your chat).
- Save outputs into `reports/` and keep statuses updated in `data/applications.md`.

Setup guide: `docs/SETUP.md`

---

## Suggested Commands (copy/paste prompts)

### 1) Evaluate a job description

Use `modes/evaluate-offer.md` and score this JD using:
- `config/profile.yml`
- `cv.md`
- `templates/evaluation-rubric.india.yml`

Then write a report to:
- `reports/YYYY-MM-DD-company-role.md`

### 2) Scan for new India jobs

Use `modes/scan-india.md` and:
- read `portals.yml`
- return only jobs matching profile filters
- append qualified rows to `data/applications.md` with status `evaluated`

### 3) Tailor resume for one opening

Use `modes/tailored-cv.md` to produce:
- ATS keywords map
- revised summary
- bullet rewrites with measurable impact

### 4) Generate a role-specific cover letter

Use `modes/cover-letter.md` with:
- JD text/URL
- `cv.md`
- `config/profile.yml`
- `knowledge/master-profile.md`
- `knowledge/application-learnings.md`

Save to:
- `cover-letters/YYYY-MM-DD-company-role.md`

### 5) Assisted apply form filling

Use the autofill helper for external portal forms:

- Setup: `docs/AUTOFILL.md`
- Generate personal loader:
  - `node tools/autofill/generate-loader.mjs`
- In browser console on the application page:
  - run `tools/autofill/generated-loader.js`
  - run `tools/autofill/form-autofill.js`

This fills common fields but does **not** auto-submit.

### 6) Run discovery + learning updates

- Run `modes/discovery-update.md` to ask focused questions and enrich your profile memory.
- Run `modes/post-application-learnings.md` after each application/interview outcome.
- Keep updating:
  - `knowledge/master-profile.md`
  - `knowledge/application-learnings.md`

### 7) Android phone flow (no plugin)

Generate a phone-friendly pack per job:

```bash
node tools/mobile/generate-application-pack.mjs \
  --company="Framestore" \
  --role="Lead Texture Artist" \
  --url="https://www.linkedin.com/jobs/view/4375111638/"
```

Then run `modes/mobile-pack.md` to auto-complete:
- role-specific cover letter,
- tailored resume file,
- copy/paste form answers.

Use docs:
- `docs/mobile/ANDROID_WORKFLOW.md` (practical phone flow now)
- `docs/mobile/FLOATING_BUBBLE_APP_SPEC.md` (future floating-bubble app blueprint)

### 8) Native Android app (mobile-first, live)

The app project now lives in:

- `android-app/`

Current app in code:
- share-intent intake for job links/text,
- deep-link intake (`ACTION_VIEW`) and PDF share intake for resume URI capture,
- per-job pack generation in app storage,
- one-tap URL-only flow (auto-opens in-app page, captures text, auto-generates outputs),
- share-link autopilot (shared likely job URLs auto-start one-tap flow),
- dedicated AI Setup tab (provider mode, local model path, API test),
- manual capture mode to avoid login/ads/skip-content misreads before generation,
- local on-device LLM runtime path (llama.cpp Android binding; Qwen2.5-1.5B GGUF target),
- hybrid LLM mode with remote API fallback,
- voice dictation onboarding/memory capture with long-term memory summarization,
- floating bubble service stub,
- accessibility service stub.

Build guide:
- `docs/mobile/ANDROID_APP_BUILD.md`
- full runbook (env, paths, rebuild commands): `docs/RUNBOOK_CURSOR_CLOUD.md`

### 9) Android app quick usage

1. Install APK from `apk/CareerOpsMobile-debug.apk`.
2. Open app -> Onboarding tab -> fill details once and save.
3. Optional but recommended:
   - Local mode: place GGUF at `/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf`
   - API mode: enter your own API base/model/key (key is encrypted locally).
4. Share a job link from LinkedIn/WhatsApp/browser to Career Ops Mobile.
5. App auto-detects likely job links and starts one-tap pipeline.
6. Review output in Results tab:
   - fit score + recommendation,
   - generated cover letter,
   - resume highlights,
   - field suggestions for forms.
7. Use Memory tab to dictate your story and save long-term career memory.

### 10) Practical tips

- Keep strengths/achievements specific (metrics, tools, outcomes).
- If extraction is weak on a page, use In-App Page -> capture/refresh manually.
- Keep API mode optional and only when you want richer reasoning.
- Never store API keys in repo files; keep only in app onboarding.
- Use the tracker (`data/applications.md`) after every application to keep momentum.

---

## Project Structure

```text
.
├── CLAUDE.md
├── config/
│   └── profile.example.yml
├── data/
│   └── applications.md
├── docs/
│   ├── AUTOFILL.md
│   ├── CUSTOMIZATION.md
│   ├── mobile/
│   │   ├── ANDROID_APP_BUILD.md
│   │   ├── ANDROID_WORKFLOW.md
│   │   └── FLOATING_BUBBLE_APP_SPEC.md
│   ├── SETUP.md
│   └── WORKFLOW.md
├── cover-letters/
├── examples/
│   ├── cover-letter.example.md
│   ├── cv.example.md
│   └── job-evaluation.example.md
├── knowledge/
│   ├── application-learnings.md
│   ├── master-profile.md
│   └── question-bank.md
├── modes/
│   ├── cover-letter.md
│   ├── discovery-update.md
│   ├── evaluate-offer.md
│   ├── mobile-pack.md
│   ├── pipeline.md
│   ├── post-application-learnings.md
│   ├── scan-india.md
│   └── tailored-cv.md
├── mobile/
├── android-app/
├── tools/
│   └── autofill/
│       ├── form-autofill.js
│       ├── generate-loader.mjs
│       ├── load-profile-and-fill.js
│       └── profile.autofill.example.json
│   └── mobile/
│       └── generate-application-pack.mjs
├── templates/
│   └── mobile/
│       └── application-pack-template.md
│   ├── evaluation-rubric.india.yml
│   ├── portals.india.example.yml
│   └── states.yml
└── reports/
```

---

## Notes

- Compensation is tracked in **LPA** by default.
- Keep sensitive data out of git (see `.gitignore`).
- You always decide where to apply; this workflow only helps you reason faster and better.
- Privacy default: generated personal artifacts are local-first (`config/profile.yml`, `cv.md`, `portals.yml`, `mobile/`).
- Android app profile data is local-only; API key is stored encrypted (Android Keystore-backed AES-GCM).
