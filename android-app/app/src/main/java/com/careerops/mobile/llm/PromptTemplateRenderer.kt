package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput

object PromptTemplateRenderer {
    /**
     * Very small, predictable templating: replaces {{key}} and {key} with string values.
     * Unknown keys are left untouched.
     */
    fun render(template: String, vars: Map<String, String>): String {
        var out = template
        vars.forEach { (k, v) ->
            out = out.replace("{{${k}}}", v)
            out = out.replace("{${k}}", v)
        }
        return out
    }

    fun vars(
        jobInput: JobInput,
        profile: CandidateProfile,
        jdTruncated: String,
        contextBlock: String
    ): Map<String, String> = mapOf(
        "company" to jobInput.company,
        "role" to jobInput.role,
        "url" to jobInput.url,
        "jd" to jdTruncated,
        "salary_hint" to jobInput.salaryHint,
        "job_notes" to jobInput.jobSpecificNotes,
        "resume_excerpts" to jobInput.resumeSummaryForPrompt,
        "context" to contextBlock,
        "full_name" to profile.fullName,
        "email" to profile.email,
        "phone" to profile.phone,
        "location" to profile.location,
        "linkedin" to profile.linkedin,
        "github" to profile.github,
        "portfolio" to profile.portfolio,
        "current_title" to profile.currentTitle,
        "target_role" to profile.targetRole,
        "strengths" to profile.strengths,
        "achievements" to profile.achievements,
        "career_memory" to profile.careerMemory
    )
}

