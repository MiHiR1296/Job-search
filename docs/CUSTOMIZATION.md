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
