# Assisted Job Application Autofill

This adds a practical "apply assistant" workflow for external job portals.

## Important

- This tool fills form fields for you.
- It **does not click final submit**.
- You always review before submitting.

## Files

- `tools/autofill/profile.autofill.example.json` - template of your application data
- `tools/autofill/load-profile-and-fill.js` - manual profile loader template
- `tools/autofill/form-autofill.js` - fills visible form fields using label matching
- `tools/autofill/generate-loader.mjs` - generates personal loader from local profile JSON

## Setup

1. Copy the profile template:

```bash
cp tools/autofill/profile.autofill.example.json tools/autofill/profile.autofill.json
```

2. Edit `tools/autofill/profile.autofill.json` with your latest details.

3. Generate your personal loader script:

```bash
node tools/autofill/generate-loader.mjs
```

4. Open a job application page in your browser.

## Usage (Console workflow)

1. Open browser Developer Tools -> Console.
2. Paste `tools/autofill/generated-loader.js` content and run it.
3. Paste `tools/autofill/form-autofill.js` content and run it.
4. Review all fields and manually upload files if needed.
5. Manually click submit.

## Mobile app alternative (recommended first)

If you use Android and switch across portals a lot, prefer the native app flow first:

1. Share job link directly to Career Ops Mobile.
2. Let one-tap URL flow generate outputs.
3. Use in-app suggestions and generated answers while filling forms.
4. Submit manually.

Use browser-console autofill only where it fits your workflow best.

## Multi-step forms

Repeat step 3 on each step/page after navigating next.

## Current limitations

- File upload fields (resume/portfolio files) are not auto-uploaded.
- Some portals use custom widgets that may require manual selection.
- Captcha and verification steps are always manual.

## Best practice

- Keep one browser tab for application form.
- Keep `cv.md` and tailored CV open for copy/paste.
- Run autofill, then manually quality-check:
  - salary expectations
  - notice period
  - sponsorship/work authorization
  - portfolio links
