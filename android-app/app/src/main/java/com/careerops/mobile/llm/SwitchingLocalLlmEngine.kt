package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft

/**
 * Chooses the local engine at call time based on [CandidateProfile.llmProviderMode].
 *
 * Modes:
 * - "local": llama.cpp (GGUF)
 * - "litert": LiteRT-LM (Gemma .litertlm)
 */
class SwitchingLocalLlmEngine(
    private val llamaEngine: LocalLlmEngine = LlamaCppLocalLlmEngine(),
    private val gemmaEngine: LocalLlmEngine = GemmaLiteRtLocalLlmEngine(),
    private val fallbackEngine: LocalLlmEngine = StubLocalLlmEngine()
) : LocalLlmEngine {

    private fun pick(profile: CandidateProfile): LocalLlmEngine {
        return when (profile.llmProviderMode.lowercase()) {
            "litert", "gemma" -> gemmaEngine
            "local", "llama" -> llamaEngine
            else -> fallbackEngine
        }
    }

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String =
        pick(profile).generateCoverLetter(jobInput, profile)

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String =
        pick(profile).generateResumeHighlights(jobInput, profile)

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String =
        pick(profile).suggestApplyDecision(jobInput, profile)

    override suspend fun summarizeCareerMemory(
        existingMemory: String,
        latestNarrative: String,
        profile: CandidateProfile
    ): String = pick(profile).summarizeCareerMemory(existingMemory, latestNarrative, profile)

    override suspend fun chat(prompt: String, profile: CandidateProfile): String =
        pick(profile).chat(prompt, profile)

    override suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? =
        pick(profile).extractStructuredJobFromJd(rawJd, profile)
}

