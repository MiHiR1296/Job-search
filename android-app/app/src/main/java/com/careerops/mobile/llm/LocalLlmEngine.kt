package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput

interface LocalLlmEngine {
    suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String
    suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String
}

class StubLocalLlmEngine : LocalLlmEngine {
    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        return """
            Dear Hiring Team,

            I am excited to apply for ${jobInput.role} at ${jobInput.company}.
            I bring practical experience relevant to this role and a strong track record of delivery.

            (Replace this with local model output.)

            Sincerely,
            ${profile.fullName}
        """.trimIndent()
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        return """
            - Role match highlights for ${jobInput.role}
            - Tooling/workflow alignment from your profile
            - Evidence-backed impact bullets
            (Replace this with local model output.)
        """.trimIndent()
    }
}

