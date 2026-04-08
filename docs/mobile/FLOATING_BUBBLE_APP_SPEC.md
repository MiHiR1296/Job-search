# Floating Bubble Android App - MVP Specification

This document describes a realistic path to build your proposed Android companion app.

## Goal

Provide a seamless phone-first application assistant that:

1. accepts shared job links or reads on-screen job content,
2. generates a role-specific apply pack (cover letter + tailored resume hints + form answers),
3. stores files locally by job name,
4. helps with fast multi-portal form filling.

## Feasible on OnePlus 12R (8GB RAM)?

Yes, for an MVP and production-grade app.
Use cloud LLM calls for generation and lightweight on-device logic for UI + extraction.

## High-level Architecture

### Android app modules

1. **Share Target Receiver**
   - Receives job URLs from LinkedIn, browser, Naukri, etc.
   - Creates a new "job session" in local DB.

2. **Job Content Extractor**
   - Primary: URL fetch + readability extraction (server-side preferred).
   - Fallback: Accessibility text capture from visible app screen.
   - Optional: OCR from screenshot (ML Kit / Tesseract).

3. **LLM Orchestrator**
   - Sends JD + profile memory to backend endpoint.
   - Returns:
     - cover letter text,
     - tailored resume draft (or bullet updates),
     - form answer map.

4. **Floating Bubble + Overlay**
   - Android `SYSTEM_ALERT_WINDOW` overlay.
   - Quick actions:
     - Paste next answer
     - Copy cover letter
     - Open resume file
     - Mark field completed

5. **Accessibility Autofill Assistant**
   - Uses `AccessibilityService` to read active form labels and focused fields.
   - Suggests or inserts mapped values.
   - Final submit remains manual for safety.

6. **Local File Manager**
   - Saves per-job outputs:
     - `JobName-cover-letter.txt/.md`
     - `JobName-form-answers.md`
     - `JobName-resume.md/pdf` (if available)
   - Store in app-private + optional exported folder.

## Recommended MVP (Phase 1)

Do this first before full screen-reading automation:

1. Share URL into app.
2. App calls backend generation endpoint.
3. App creates per-job pack locally.
4. Floating bubble provides copy/paste snippets and quick-open files.

This gives most of the value with much lower complexity and fewer permission risks.

## Phase 2 (Advanced)

- Add on-screen parsing via Accessibility and optional OCR.
- Add semi-automatic field mapping suggestions.
- Add "next field" smart navigation.

## Security and Privacy

- Never auto-submit final applications.
- Encrypt local sensitive profile data.
- Allow "clear all data" one-tap reset.
- If backend used, avoid sending unnecessary PII.

## Backend API sketch

### POST `/generate/job-pack`

Input:

```json
{
  "jobUrl": "https://...",
  "jobText": "optional extracted jd",
  "candidateProfile": "...",
  "knowledge": "...",
  "preferences": {}
}
```

Output:

```json
{
  "company": "Framestore",
  "role": "Lead Texture Artist",
  "coverLetter": "...",
  "resumeTailoring": "...",
  "formAnswers": {
    "full_name": "Mihir Botle",
    "email": "..."
  }
}
```

## Suggested Tech Stack

- **Android:** Kotlin + Jetpack Compose
- **Storage:** Room DB + encrypted shared prefs
- **Overlay:** Foreground service + overlay window
- **Accessibility:** AccessibilityService
- **OCR (optional):** Google ML Kit text recognition
- **Backend:** Node.js/Express or Python/FastAPI
- **LLM:** API-based model (cloud)

## UX Flow

1. User taps "Share" on job URL -> chooses app.
2. App shows "Generate pack" progress.
3. Pack appears with:
   - cover letter,
   - tailored resume notes,
   - field-wise answers.
4. User opens portal, taps bubble, inserts/copies answers quickly.
5. User submits manually and marks status as Applied.

## Constraints and Risks

- Some apps block clipboard/autofill behavior.
- Accessibility permissions may be sensitive for Play Store compliance.
- OCR accuracy varies by UI theme and language.
- Portal custom widgets may still require manual selection.

## Practical Recommendation

Start with **Share URL + Generate Pack + Bubble Copy/Paste** MVP.
It is the fastest path to a seamless experience across many portals without plugin dependency.

