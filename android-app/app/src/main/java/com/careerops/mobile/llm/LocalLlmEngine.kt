package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft

interface LocalLlmEngine {
    suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun summarizeCareerMemory(existingMemory: String, latestNarrative: String, profile: CandidateProfile): String

    /**
     * Small interactive prompt (used by Developer Tools).
     * Default implementation returns empty to signal "unsupported".
     */
    suspend fun chat(prompt: String, profile: CandidateProfile): String = ""

    /** Optional structured extraction from noisy JD text; default unsupported. */
    suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? = null
}

class StubLocalLlmEngine : LocalLlmEngine {
    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        val jdSignals = jobInput.jdText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.contains("STRUCTURED_JOB_METADATA", ignoreCase = true) }
            .take(5)
            .joinToString("; ")

        val strengths = profile.strengths
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(", ")

        val modelNote = """
            Note: The on-device model returned no text (check GGUF path, free RAM, and chat template: Qwen vs Llama 3 in docs).
            This placeholder uses the real job title and company from the page when available.
        """.trimIndent()

        return """
            Dear Hiring Team,

            I am excited to apply for ${jobInput.role} at ${jobInput.company}.
            I bring practical experience relevant to this role and a strong track record of delivery.
            ${if (strengths.isNotBlank()) "Relevant strengths include: $strengths." else ""}

            ${if (jdSignals.isNotBlank()) "From the job description, priorities appear to include: $jdSignals" else ""}

            $modelNote

            Sincerely,
            ${profile.fullName}
        """.trimIndent()
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        val jdSignals = jobInput.jdText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.contains("STRUCTURED_JOB_METADATA", ignoreCase = true) }
            .take(8)
            .joinToString("\n- ", prefix = "- ")

        return """
            - Role match highlights for ${jobInput.role}
            - Current title: ${profile.currentTitle}
            - Target role: ${profile.targetRole}
            - Experience: ${profile.yearsExperience} years
            - Core strengths: ${profile.strengths.ifBlank { "Add strengths in onboarding tab" }}
            - Key achievements: ${profile.achievements.ifBlank { "Add achievements in onboarding tab" }}
            - JD signals captured from page:
            ${if (jdSignals.isNotBlank()) jdSignals else "- Not captured yet. Open job in In-App Page tab first."}
            - Refine manually after full local model integration.
        """.trimIndent()
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        val strengths = profile.strengths.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val text = jobInput.jdText.lowercase()
        val hits = strengths.count { text.contains(it.lowercase()) }
        return when {
            hits >= 3 -> "Strong Apply"
            hits >= 1 -> "Apply"
            else -> "Review manually"
        }
    }

    override suspend fun summarizeCareerMemory(
        existingMemory: String,
        latestNarrative: String,
        profile: CandidateProfile
    ): String {
        val sentences = latestNarrative
            .split(Regex("[\\n\\r\\.]+"))
            .map { it.trim() }
            .filter { it.length > 20 }
            .take(6)

        val condensed = sentences.joinToString("\n- ", prefix = "- ")
        val prior = existingMemory.trim()
        return buildString {
            if (prior.isNotBlank()) {
                append(prior)
                append("\n\n")
            }
            append("Latest useful points:\n")
            append(
                if (condensed.isBlank()) {
                    "- (No clear points captured. Please dictate with specific achievements.)"
                } else {
                    ""
                }
            )
            if (condensed.isNotBlank()) append(condensed)
        }.trim()
    }

    override suspend fun chat(prompt: String, profile: CandidateProfile): String {
        if (prompt.isBlank()) return ""
        return "Local model not available (check AI Setup → Local GGUF path)."
    }
}

