# Setup Guide

## 1) Initialize your personal files

From repository root:

```bash
cp config/profile.example.yml config/profile.yml
cp templates/portals.india.example.yml portals.yml
cp examples/cv.example.md cv.md
```

Edit:
- `config/profile.yml`
- `portals.yml`
- `cv.md`

## 2) Decide your thresholds

Open `templates/evaluation-rubric.india.yml` and set:
- `apply_threshold`
- `strong_apply_threshold`
- compensation assumptions for your profile

## 3) Use AI modes

- `modes/scan-india.md` to discover roles
- `modes/evaluate-offer.md` to score one role
- `modes/tailored-cv.md` to generate role-specific resume draft
- `modes/pipeline.md` for weekly maintenance

## 4) Keep data organized

- Add/maintain opportunities in `data/applications.md`
- Save detailed evaluations in `reports/`
- Save tailored CV drafts in `output/`

## 5) Weekly cadence

1. Scan and shortlist
2. Evaluate and decide
3. Tailor CV + apply
4. Update tracker and follow-ups

## 6) Assisted application form filling

To speed up repetitive portal forms while keeping control:

1. Open `docs/AUTOFILL.md`
2. Create `tools/autofill/profile.autofill.json` from the example
3. Run:

```bash
node tools/autofill/generate-loader.mjs
```

4. In browser devtools console on an application form:
   - run `generated-loader.js`
   - run `form-autofill.js`
5. Review everything, then submit manually

## 7) Enable the learning loop (recommended)

Initialize and keep these files updated:

- `knowledge/master-profile.md` (your durable hiring signals)
- `knowledge/application-learnings.md` (per-application outcomes)
- `knowledge/question-bank.md` (question prompts to deepen personalization)

Use these modes regularly:

- `modes/cover-letter.md` for role-specific cover letters
- `modes/discovery-update.md` for targeted Q&A sessions
- `modes/post-application-learnings.md` after each application outcome

## 8) Mobile-first (Android) setup

If you apply mostly from phone:

1. Open `docs/mobile/ANDROID_WORKFLOW.md`
2. For each job link, generate a mobile pack:

```bash
node tools/mobile/generate-application-pack.mjs \
  --company="Company Name" \
  --role="Role Name" \
  --url="https://job-link"
```

3. Run `modes/mobile-pack.md` to fill the generated pack with:
   - role-specific cover letter,
   - tailored resume highlights,
   - finalized form answers.
4. Sync/open the `mobile/` folder on phone and apply quickly across portals.

## 9) Native Android app setup (recommended)

Use this when you want a low-friction mobile app flow (share link -> auto process).

1. Open:
   - `docs/mobile/ANDROID_APP_BUILD.md`
2. Build app:

```bash
cd android-app
./gradlew assembleDebug
```

3. Install APK from:
   - `apk/CareerOpsMobile-debug.apk`
4. In app Onboarding:
   - fill profile once,
   - set LLM mode:
     - Local mode (on-device model path),
     - API mode (your API base/model/key).
5. Use share from LinkedIn/WhatsApp/browser to send job links directly to app.

## 10) Documentation/runbook reference

For full environment paths, commands, and implementation history:

- `docs/RUNBOOK_CURSOR_CLOUD.md`

Use this file for quick recovery when starting a new session in Cursor Cloud.
