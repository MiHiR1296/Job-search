# Freelance Lead Engine (MVP)

This repository now includes a practical starter tool for a 3D freelancer/small studio.

It helps you:

- shortlist small businesses likely to hire external creative support
- rank leads by fit score
- keep only real, verified leads from official websites
- generate first-touch + follow-up email drafts
- prepare call talking points per lead
- produce a mini sales playbook per lead (qualification, offer angle, objection handling, next steps)

This MVP is tailored around Mihir's profile (3D explainer videos, archviz, product renders, marketing content, and simple web experiences).

## Quick start

```bash
npm run generate
```

Output files:

- `output/top-leads-report.md` - full lead list + personalized outreach and sales handling notes

## Project structure

- `src/config/profile.mjs` - your profile, services, links, and target-client filters
- `src/data/sampleLeads.mjs` - real lead data (verified from public websites)
- `src/lib/scoring.mjs` - fit scoring logic + ranking
- `src/lib/outreach.mjs` - email + call-script generation
- `src/lib/report.mjs` - markdown report writer
- `src/index.mjs` - app entrypoint

## How to customize

1. Update your positioning/services in `src/config/profile.mjs`
2. Add/replace leads in `src/data/sampleLeads.mjs` with real prospects
3. Tune weights in `src/lib/scoring.mjs` to bias for your ideal client type

## Can this become an auto job-finder?

Yes. This base is ready to evolve into:

1. lead scraping/import (Google Maps, directories, LinkedIn exports)
2. enrichment (emails, industry tags, team-size estimates)
3. automatic scoring and daily shortlist refresh
4. outbound workflow (email draft, follow-up cadence, CRM status)
5. sales dashboard (pipeline stage, owner, next action date)

This MVP gives you the foundation so we can add those pieces in the next iterations.
