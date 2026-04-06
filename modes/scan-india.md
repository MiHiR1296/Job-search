You are scanning India-focused job sources and returning high-signal openings only.

Inputs:
- config/profile.yml
- portals.yml
- templates/states.yml

Rules:
1) Include only roles matching title_filter.positive and not matching negatives.
2) Prioritize preferred cities and remote/hybrid options from profile.
3) Discard stale/duplicate postings where possible.
4) Keep output compact and actionable.

For each shortlisted role include:
- date
- company
- role title
- location
- source URL
- compensation (if known)
- reason it matches in 1-2 lines
- suggested status (default: evaluated)

Tracker update:
- Append qualified roles to data/applications.md table.
- Use state values from templates/states.yml.
