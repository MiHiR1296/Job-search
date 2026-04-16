# Customization Guide

## Tune role targeting

Edit `config/profile.yml`:
- `target_roles.primary` and `secondary`
- `preferences.preferred_cities`
- `experience.top_skills`

## Tune search precision

Edit `portals.yml`:
- add/remove `title_filter.positive`
- expand `title_filter.negative` to remove noise
- disable sources that produce low-quality roles

## Tune scoring behavior

Edit `templates/evaluation-rubric.india.yml`:
- increase `role_fit` and `skills_match` if you want stricter relevance
- increase `compensation_fit` if your floor is non-negotiable
- reduce `brand_stability` if you are startup-first

## Add your company watchlist

In `portals.yml > tracked_companies`, add:
- company name
- careers URL
- optional web search query

Tip: keep a balanced mix of high-growth startups and stable product orgs.

## Android app customization (mobile)

In the app Onboarding tab, tune:

- LLM provider mode:
  - `local` for on-device first
  - `api` for remote model via your own API key
- API fields:
  - base URL
  - model name
  - API key (encrypted locally on device)
- Candidate profile details:
  - strengths, achievements, compensation expectations
  - notice period, relocation, sponsorship constraints

### Local model path

The current default target path for local GGUF model is:

- `/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf`

If file is unavailable, app falls back to local stub behavior (or API mode if configured).

### Voice-first usage

- Use dictation buttons in Onboarding for key fields.
- Use the Memory tab to dictate long narratives and summarize into reusable career memory.
- Keep memory factual and measurable (projects, impact, ownership, outcomes).
