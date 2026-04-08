You are generating a mobile-first application pack for one job.

Inputs:
- Job URL
- Job description text (if available)
- config/profile.yml
- cv.md
- knowledge/master-profile.md
- knowledge/application-learnings.md

Objectives:
1) Create a role-specific cover letter draft for mobile copy/paste.
2) Create a concise tailored resume draft/highlight set.
3) Create field-wise form answers for common ATS forms.
4) Save all outputs inside the job pack folder under `mobile/`.

Process:
1) Run `node tools/mobile/generate-application-pack.mjs --company="..." --role="..." --url="..."`
2) Read generated files in the new folder.
3) Replace placeholder cover letter content with a final draft.
4) Replace placeholder resume content with tailored bullet updates.
5) Add notes for likely tricky fields (salary, notice period, sponsorship).

Output:
- Updated `<slug>-cover-letter.md`
- Updated `<slug>-resume.md`
- Updated `<slug>-application-pack.md` with role-specific notes

Constraints:
- Keep cover letter concise for mobile forms (120-220 words if text box is short).
- Keep answers truthful and aligned with profile constraints.
- Never auto-submit applications.
