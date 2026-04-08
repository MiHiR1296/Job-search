# Weekly Workflow

## Daily (15-30 min)

1. Scan portals and company pages.
2. Add promising jobs to `data/applications.md` as `evaluated`.
3. Evaluate top 1-3 roles with `modes/evaluate-offer.md`.
4. Generate role-specific cover letter with `modes/cover-letter.md`.

## Twice per week (60-90 min)

1. Generate tailored resume draft for best-fit roles.
2. Apply manually on official portal.
3. Update status to `applied` and set `next_action`.

## Weekly review (30 min)

1. Run `modes/pipeline.md` on tracker.
2. Send follow-ups where due.
3. Close stale items to keep funnel clean.
4. Recalibrate role filters and compensation thresholds.
5. Run one discovery session (`modes/discovery-update.md`) to improve system memory.

## Recommended tracker discipline

- One row per role application.
- Always keep `status`, `next_action`, and `date_added` updated.
- Use short, objective notes (facts, not emotions).

## Learning loop (system gets better over time)

1. After each application, run `modes/post-application-learnings.md`.
2. Update:
   - `knowledge/application-learnings.md`
   - `knowledge/master-profile.md`
3. Every few applications, run `modes/discovery-update.md` and answer 6-8 focused questions.
4. Use updated knowledge when generating:
   - tailored resume drafts (`modes/tailored-cv.md`)
   - cover letters (`modes/cover-letter.md`)

## Mobile-first loop (OnePlus/Android friendly)

1. Share or paste job link.
2. Generate pack:
   - `node tools/mobile/generate-application-pack.mjs --company="..." --role="..." --url="..."`
3. Run `modes/mobile-pack.md` to complete:
   - `<slug>-cover-letter.md`
   - `<slug>-resume.md`
   - `<slug>-application-pack.md`
4. Open pack files on phone while filling forms.
5. Submit manually, then run `modes/post-application-learnings.md`.

## Native app loop (fastest daily usage)

1. From LinkedIn/WhatsApp/Instagram/browser, share job link to **Career Ops Mobile**.
2. If link matches job patterns, app auto-starts one-tap flow:
   - opens in-app page,
   - captures page text,
   - extracts role/company/salary hints,
   - generates output and opens Results.
3. Review recommendation + reasons in Results tab.
4. Use generated content and live suggestions while filling the application form.
5. Use Memory tab for voice updates:
   - dictate new project stories,
   - summarize into long-term memory,
   - reuse in future applications.

### Practical tips

- Keep API mode only if you want stronger generation quality and you trust your endpoint.
- Keep local mode as fallback for offline/privacy-first usage.
- Use one-tap flow first; use manual Job Input only when extraction is weak.
