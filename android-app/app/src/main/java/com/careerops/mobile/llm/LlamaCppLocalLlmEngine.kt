package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.codeshipping.llamakotlin.LlamaModel

class LlamaCppLocalLlmEngine(
    private val config: LocalLlmConfig = LocalLlmConfig(),
    private val fallbackEngine: LocalLlmEngine = StubLocalLlmEngine()
) : LocalLlmEngine {

    @Volatile
    private var model: LlamaModel? = null
    private var loadedModelPath: String? = null
    private val modelLoadMutex = Mutex()

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        val prompt = """
            <|im_start|>system
            You are a practical job application assistant. Write a concise, truthful cover letter.
            <|im_end|>
            <|im_start|>user
            Company: ${jobInput.company}
            Role: ${jobInput.role}
            URL: ${jobInput.url}
            JD:
            ${jobInput.jdText.take(8000)}

            Candidate:
            Name: ${profile.fullName}
            Current title: ${profile.currentTitle}
            Target role: ${profile.targetRole}
            Strengths: ${profile.strengths}
            Achievements: ${profile.achievements}
            ${contextBlock(jobInput, profile)}

            Constraints:
            - under 220 words
            - plain professional English
            - no fake claims
            <|im_end|>
            <|im_start|>assistant
        """.trimIndent()
        val local = generateWithLocalModel(prompt, profile)
        return if (local.isNotBlank()) local else fallbackEngine.generateCoverLetter(jobInput, profile)
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        val prompt = """
            <|im_start|>system
            Produce ATS-friendly bullet points only.
            <|im_end|>
            <|im_start|>user
            Job role: ${jobInput.role}
            Company: ${jobInput.company}
            JD:
            ${jobInput.jdText.take(8000)}

            Candidate profile:
            Current title: ${profile.currentTitle}
            Experience: ${profile.yearsExperience}
            Strengths: ${profile.strengths}
            Achievements: ${profile.achievements}
            ${contextBlock(jobInput, profile)}

            Return 6-10 bullets, each short and action-focused.
            <|im_end|>
            <|im_start|>assistant
        """.trimIndent()
        val local = generateWithLocalModel(prompt, profile)
        return if (local.isNotBlank()) local else fallbackEngine.generateResumeHighlights(jobInput, profile)
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        val prompt = """
            <|im_start|>system
            Classify job fit using one label only.
            <|im_end|>
            <|im_start|>user
            Allowed labels: Strong Apply, Apply, Review, Skip.
            Job role: ${jobInput.role}
            Company: ${jobInput.company}
            JD:
            ${jobInput.jdText.take(6000)}

            Candidate target role: ${profile.targetRole}
            Candidate strengths: ${profile.strengths}
            Expected CTC LPA: ${profile.expectedCtcLpa}
            Minimum acceptable LPA: ${profile.minimumAcceptableLpa}
            ${contextBlock(jobInput, profile)}

            Respond with one label only.
            <|im_end|>
            <|im_start|>assistant
        """.trimIndent()
        val local = normalizeDecision(generateWithLocalModel(prompt, profile))
        return if (local == "Review") fallbackEngine.suggestApplyDecision(jobInput, profile) else local
    }

    override suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? {
        val prompt = """
             <|im_start|>system
            Extract job fields from noisy job page text. Reply with a single JSON object only, keys exactly:
            {"company":"","role":"","location":"","salary_raw":"","pay_period":"","responsibilities":""}
            pay_period must be one of: monthly, yearly, lpa, hourly, unknown. No markdown, no prose.
             <|im_end|>
             <|im_start|>user
            ${rawJd.take(12000)}
             <|im_end|>
             <|im_start|>assistant
        """.trimIndent()
        val raw = generateWithLocalModel(prompt, profile)
        val parsed = LlmStructuredJobParser.parseStructuredJobJson(raw)
        return parsed ?: fallbackEngine.extractStructuredJobFromJd(rawJd, profile)
    }

    override suspend fun summarizeCareerMemory(
        existingMemory: String,
        latestNarrative: String,
        profile: CandidateProfile
    ): String {
        val prompt = """
            <|im_start|>system
            Convert candidate narrative into durable career memory.
            Keep only reusable, high-signal details for future applications.
            <|im_end|>
            <|im_start|>user
            Existing memory:
            ${existingMemory.take(8000)}

            New narrative:
            ${latestNarrative.take(8000)}

            Return markdown with sections:
            - Core strengths
            - Evidence-based achievements
            - Preferences and constraints
            - Strong story snippets
            - Open questions
            <|im_end|>
            <|im_start|>assistant
        """.trimIndent()
        val local = generateWithLocalModel(prompt, profile)
        return if (local.isNotBlank()) {
            local
        } else {
            fallbackEngine.summarizeCareerMemory(existingMemory, latestNarrative, profile)
        }
    }

    private fun contextBlock(jobInput: JobInput, profile: CandidateProfile): String = buildString {
        if (profile.careerMemory.isNotBlank()) {
            appendLine("Long-term career memory:")
            appendLine(profile.careerMemory.take(5000))
        }
        if (jobInput.jobSpecificNotes.isNotBlank()) {
            appendLine("Per-job notes (candidate wants to emphasize for this role):")
            appendLine(jobInput.jobSpecificNotes.take(2000))
        }
        if (jobInput.resumeSummaryForPrompt.isNotBlank()) {
            appendLine("Resume excerpts:")
            appendLine(jobInput.resumeSummaryForPrompt.take(4500))
        }
    }.toString().trim().let { if (it.isNotBlank()) "\n$it" else "" }

    private suspend fun generateWithLocalModel(prompt: String, profile: CandidateProfile): String {
        val modelPath = profile.localModelPath.trim().ifBlank {
            "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        }
        if (modelPath.isBlank()) return ""
        val engine = loadModelIfNeeded(modelPath) ?: return ""
        return runCatching { engine.generate(prompt).trim() }.getOrDefault("")
    }

    private suspend fun loadModelIfNeeded(modelPath: String): LlamaModel? {
        return modelLoadMutex.withLock {
            val current = model
            if (current != null && loadedModelPath == modelPath) {
                return@withLock current
            }
            runCatching { current?.close() }
            val loaded = runCatching {
                LlamaModel.load(modelPath) {
                    contextSize = config.contextSize
                    maxTokens = config.maxTokens
                    temperature = config.temperature
                    topP = config.topP
                    topK = config.topK
                    repeatPenalty = config.repeatPenalty
                    threads = config.threads
                    threadsBatch = config.threadsBatch
                }
            }.getOrNull()
            model = loaded
            loadedModelPath = if (loaded != null) modelPath else null
            loaded
        }
    }

    private fun normalizeDecision(raw: String): String {
        val text = raw.lowercase()
        return when {
            "strong apply" in text -> "Strong Apply"
            "apply" in text -> "Apply"
            "skip" in text -> "Skip"
            else -> "Review"
        }
    }

}

