package com.careerops.mobile.data

data class CandidateProfile(
    val fullName: String = "Your Name",
    val email: String = "you@example.com",
    val phone: String = "+91XXXXXXXXXX",
    val location: String = "Mumbai, India",
    val linkedin: String = "https://linkedin.com/in/your-handle",
    val github: String = "https://github.com/your-handle",
    val portfolio: String = "https://your-portfolio.example.com",
    val currentTitle: String = "",
    val targetRole: String = "",
    val resumeUri: String = "",
    /** Truncated resume text for LLM/scoring context (filled after resume import). */
    val resumeTextSnapshot: String = "",
    val strengths: String = "",
    val achievements: String = "",
    val careerMemory: String = "",
    val yearsExperience: String = "",
    val noticePeriodDays: String = "",
    val currentCtcLpa: String = "",
    val expectedCtcLpa: String = "",
    val minimumAcceptableLpa: String = "",
    val requiresSponsorship: String = "No",
    val willingToRelocate: String = "Yes",
    val onboardingCompleted: Boolean = false,
    val onboardingVersion: Int = 1,
    val llmProviderMode: String = "local",
    val localModelPath: String = "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf",
    val apiBaseUrl: String = "https://api.openai.com/v1",
    val apiModel: String = "gpt-4o-mini",
    val apiKey: String = "",

    /**
     * Developer overrides for prompts (optional). If blank, app defaults are used.
     * These are intended for debugging/stability tuning on-device.
     */
    val promptApplyDecisionSystem: String = "",
    val promptApplyDecisionUser: String = "",
    val promptResumeHighlightsSystem: String = "",
    val promptResumeHighlightsUser: String = "",
    val promptCoverLetterSystem: String = "",
    val promptCoverLetterUser: String = "",

    /** Safety valve: cap streamed output chars collected from local models. */
    val maxOutputChars: Int = 1800
)

/** Parsed job fields from JSON-LD and/or LLM (best-effort). */
data class StructuredJobDraft(
    val company: String = "",
    val role: String = "",
    val location: String = "",
    val salaryRaw: String = "",
    /** monthly, yearly, lpa, unknown */
    val payPeriodHint: String = "unknown",
    val responsibilitiesSnippet: String = ""
)

data class JobInput(
    val company: String,
    val role: String,
    val url: String,
    val jdText: String = "",
    val salaryHint: String = "",
    /**
     * Heuristic annual CTC in LPA when pay is monthly/hourly or non-Indian wording;
     * scoring prefers this when explicit "X LPA" is missing.
     */
    val salaryAnnualLpaApprox: Double? = null,
    /** Per-job notes the user adds before generating (memory for this application). */
    val jobSpecificNotes: String = "",
    /** Snippets from resume for fit scoring and prompts. */
    val resumeSummaryForPrompt: String = ""
)

data class ApplicationPack(
    val folderName: String,
    val folderPath: String,
    val packMarkdown: String,
    val formAnswersMarkdown: String,
    val coverLetterMarkdown: String,
    val resumeMarkdown: String
)

data class FormSuggestion(
    val label: String,
    val suggestedValue: String
)

data class JobInsight(
    val detectedCompany: String = "",
    val detectedRole: String = "",
    val detectedSalaryText: String = "",
    val score: Double = 0.0,
    val recommendation: String = "Review",
    val reasons: List<String> = emptyList()
)
