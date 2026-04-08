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

### 4) Assisted apply form filling

Use the autofill helper for external portal forms:

- Setup: `docs/AUTOFILL.md`
- Generate personal loader:
  - `node tools/autofill/generate-loader.mjs`
- In browser console on the application page:
  - run `tools/autofill/generated-loader.js`
  - run `tools/autofill/form-autofill.js`

This fills common fields but does **not** auto-submit.

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
│   ├── SETUP.md
│   └── WORKFLOW.md
├── examples/
│   ├── cv.example.md
│   └── job-evaluation.example.md
├── modes/
│   ├── evaluate-offer.md
│   ├── pipeline.md
│   ├── scan-india.md
│   └── tailored-cv.md
├── tools/
│   └── autofill/
│       ├── form-autofill.js
│       ├── generate-loader.mjs
│       ├── load-profile-and-fill.js
│       └── profile.autofill.example.json
├── templates/
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
