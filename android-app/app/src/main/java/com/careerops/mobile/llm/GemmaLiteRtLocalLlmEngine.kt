package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

/**
 * Local engine using Google's LiteRT-LM (.litertlm models, e.g. Gemma).
 *
 * Note: Native crashes are still possible, but LiteRT-LM is a more "mobile-native"
 * runtime than llama.cpp on some devices.
 */
class GemmaLiteRtLocalLlmEngine(
    private val fallbackEngine: LocalLlmEngine = StubLocalLlmEngine()
) : LocalLlmEngine {

    private val inferenceMutex = Mutex()
    private class OutputLimitReached : RuntimeException()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var loadedModelPath: String? = null

    private suspend fun ensureEngineLocked(modelPath: String, cacheDir: String?): Engine? {
        val current = engine
        if (current != null && loadedModelPath == modelPath) return current
        runCatching { current?.close() }
        engine = null
        loadedModelPath = null

        val cfg = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            cacheDir = cacheDir
        )
        val e = Engine(cfg)
        // initialize can take seconds; do on background dispatcher
        withContext(Dispatchers.Default) { e.initialize() }
        engine = e
        loadedModelPath = modelPath
        return e
    }

    private suspend fun chatOnce(system: String, user: String, profile: CandidateProfile): String =
        inferenceMutex.withLock {
            val modelPath = profile.localModelPath.trim()
            // We will store LiteRT model path in localModelPath when mode=litert to avoid schema churn.
            if (modelPath.isBlank()) return@withLock ""

            val e = ensureEngineLocked(modelPath, cacheDir = null) ?: return@withLock ""
            val sampler = SamplerConfig(
                topK = 40,
                topP = 0.9,
                temperature = 0.3
            )
            val conversation = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(system),
                    samplerConfig = sampler
                )
            )
            conversation.use { c ->
                val sb = StringBuilder()
                val limit = profile.maxOutputChars.coerceIn(200, 20_000)
                // streaming is safest for long generations; we collect into a string
                try {
                    c.sendMessageAsync(user)
                        .catch { /* swallow, caller handles fallback */ }
                        .collect { msg ->
                            sb.append(msg.toString())
                            if (sb.length >= limit) throw OutputLimitReached()
                        }
                } catch (_: OutputLimitReached) {
                    // return partial output
                }
                sb.toString().take(limit).trim()
            }
        }

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        val defaultSystem = "Write a short truthful cover letter. Under 200 words."
        val defaultUser = """
            Company: ${jobInput.company}
            Role: ${jobInput.role}
            JD:
            ${jobInput.jdText.take(1800)}
        """.trimIndent()

        val systemTemplate = profile.promptCoverLetterSystem.ifBlank { defaultSystem }
        val userTemplate = profile.promptCoverLetterUser.ifBlank { defaultUser }
        val context = buildString {
            if (profile.careerMemory.isNotBlank()) appendLine(profile.careerMemory.take(5000))
            if (jobInput.jobSpecificNotes.isNotBlank()) appendLine(jobInput.jobSpecificNotes.take(2000))
            if (jobInput.resumeSummaryForPrompt.isNotBlank()) appendLine(jobInput.resumeSummaryForPrompt.take(4500))
        }.toString().trim()
        val vars = PromptTemplateRenderer.vars(
            jobInput = jobInput,
            profile = profile,
            jdTruncated = jobInput.jdText.take(1800),
            contextBlock = context
        )
        val system = PromptTemplateRenderer.render(systemTemplate, vars).trim()
        val user = PromptTemplateRenderer.render(userTemplate, vars).trim()

        val out = runCatching { chatOnce(system, user, profile) }.getOrDefault("")
        return if (out.isNotBlank()) out else fallbackEngine.generateCoverLetter(jobInput, profile)
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        val defaultSystem = "Return only 6-10 concise resume bullets."
        val defaultUser = """
            Role: ${jobInput.role}
            Company: ${jobInput.company}
            JD:
            ${jobInput.jdText.take(2200)}
        """.trimIndent()

        val systemTemplate = profile.promptResumeHighlightsSystem.ifBlank { defaultSystem }
        val userTemplate = profile.promptResumeHighlightsUser.ifBlank { defaultUser }
        val context = buildString {
            if (profile.careerMemory.isNotBlank()) appendLine(profile.careerMemory.take(5000))
            if (jobInput.jobSpecificNotes.isNotBlank()) appendLine(jobInput.jobSpecificNotes.take(2000))
            if (jobInput.resumeSummaryForPrompt.isNotBlank()) appendLine(jobInput.resumeSummaryForPrompt.take(4500))
        }.toString().trim()
        val vars = PromptTemplateRenderer.vars(
            jobInput = jobInput,
            profile = profile,
            jdTruncated = jobInput.jdText.take(2200),
            contextBlock = context
        )
        val system = PromptTemplateRenderer.render(systemTemplate, vars).trim()
        val user = PromptTemplateRenderer.render(userTemplate, vars).trim()

        val out = runCatching { chatOnce(system, user, profile) }.getOrDefault("")
        return if (out.isNotBlank()) out else fallbackEngine.generateResumeHighlights(jobInput, profile)
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        val defaultSystem = "Choose one label only: Strong Apply, Apply, Review, Skip."
        val defaultUser = """
            Allowed labels: Strong Apply, Apply, Review, Skip.
            Role: ${jobInput.role}
            Company: ${jobInput.company}
            JD:
            ${jobInput.jdText.take(1800)}
            Candidate target role: ${profile.targetRole}
            Candidate strengths: ${profile.strengths}
            Respond with one label only.
        """.trimIndent()

        val systemTemplate = profile.promptApplyDecisionSystem.ifBlank { defaultSystem }
        val userTemplate = profile.promptApplyDecisionUser.ifBlank { defaultUser }
        val context = buildString {
            if (profile.careerMemory.isNotBlank()) appendLine(profile.careerMemory.take(3500))
            if (jobInput.jobSpecificNotes.isNotBlank()) appendLine(jobInput.jobSpecificNotes.take(1200))
            if (jobInput.resumeSummaryForPrompt.isNotBlank()) appendLine(jobInput.resumeSummaryForPrompt.take(2500))
        }.toString().trim()
        val vars = PromptTemplateRenderer.vars(
            jobInput = jobInput,
            profile = profile,
            jdTruncated = jobInput.jdText.take(1400),
            contextBlock = context
        )
        val system = PromptTemplateRenderer.render(systemTemplate, vars).trim()
        val user = PromptTemplateRenderer.render(userTemplate, vars).trim()

        val out = runCatching { chatOnce(system, user, profile) }.getOrDefault("").trim()
        if (out.contains("strong apply", ignoreCase = true)) return "Strong Apply"
        if (out.contains("apply", ignoreCase = true)) return "Apply"
        if (out.contains("skip", ignoreCase = true)) return "Skip"
        if (out.contains("review", ignoreCase = true)) return "Review"
        return fallbackEngine.suggestApplyDecision(jobInput, profile)
    }

    override suspend fun summarizeCareerMemory(existingMemory: String, latestNarrative: String, profile: CandidateProfile): String {
        val system = "Summarize into reusable career memory bullets."
        val user = """
            Existing:
            ${existingMemory.take(6000)}
            New:
            ${latestNarrative.take(6000)}
        """.trimIndent()
        val out = runCatching { chatOnce(system, user, profile) }.getOrDefault("")
        return if (out.isNotBlank()) out else fallbackEngine.summarizeCareerMemory(existingMemory, latestNarrative, profile)
    }

    override suspend fun chat(prompt: String, profile: CandidateProfile): String {
        val out = runCatching { chatOnce("You are a helpful assistant.", prompt.take(1800), profile) }.getOrDefault("")
        return if (out.isNotBlank()) out else fallbackEngine.chat(prompt, profile)
    }

    override suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? = null
}

