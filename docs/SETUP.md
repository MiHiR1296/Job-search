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
