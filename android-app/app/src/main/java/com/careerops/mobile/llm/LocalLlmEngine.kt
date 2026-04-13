package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft

interface LocalLlmEngine {
    suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun summarizeCareerMemory(existingMemory: String, latestNarrative: String, profile: CandidateProfile): String

    /** Optional structured extraction from noisy JD text; default unsupported. */
    suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? = null
}

class StubLocalLlmEngine : LocalLlmEngine {
    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        val jdSignals = jobInput.jdText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString("; ")

        val strengths = profile.strengths
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(", ")

        return """
            Dear Hiring Team,

            I am excited to apply for ${jobInput.role} at ${jobInput.company}.
            I bring practical experience relevant to this role and a strong track record of delivery.
            ${if (strengths.isNotBlank()) "My strongest areas for this role are: $strengths." else ""}

            ${if (jdSignals.isNotBlank()) "From the job page, key priorities appear to be: $jdSignals" else ""}

            This draft was generated locally on-device and should be refined once a full local model is connected.

            Sincerely,
            ${profile.fullName}
        """.trimIndent()
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        val jdSignals = jobInput.jdText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
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
}

