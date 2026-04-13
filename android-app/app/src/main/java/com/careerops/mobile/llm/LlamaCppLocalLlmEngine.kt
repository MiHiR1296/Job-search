package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft
import com.careerops.mobile.web.JobPageExtractor
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
    /**
     * llama.cpp / JNI is not safe for concurrent generate() on one context.
     * Guard load + generate together (not just load).
     */
    private val inferenceMutex = Mutex()

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        val system = """
            You write tailored, truthful cover letters for job applications in India and globally.
            Rules:
            - Use the exact Company and Role strings provided in the user message (never write "Unknown Company" or placeholders if real names are given).
            - Never paste internal markers, dashed metadata blocks, JSON-LD, or lines starting with "---".
            - Tie at least one concrete phrase from the candidate achievements or resume excerpts to a JD theme when possible.
            - Plain professional English, under 240 words, no salary negotiation, no invented employers or degrees.
        """.trimIndent()

        val user = buildString {
            appendLine("Company: ${jobInput.company}")
            appendLine("Role: ${jobInput.role}")
            appendLine("URL: ${jobInput.url}")
            appendLine("Job description (truncated):")
            appendLine(jobInput.jdText.take(JD_CHARS_COVER))
            appendLine()
            appendLine("Candidate:")
            appendLine("Name: ${profile.fullName}")
            appendLine("Current title: ${profile.currentTitle}")
            appendLine("Target role: ${profile.targetRole}")
            appendLine("Strengths: ${profile.strengths}")
            appendLine("Achievements: ${profile.achievements}")
            append(contextBlock(jobInput, profile))
        }.toString()

        val local = generateWithLocalModel(system, user, profile)
        return if (local.isNotBlank()) local else fallbackEngine.generateCoverLetter(jobInput, profile)
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        val system = """
            Produce ATS-friendly bullet points only for the listed role and company.
            Never include metadata markers or "---" sections. Map bullets to JD themes where evidence exists in the profile.
        """.trimIndent()

        val user = buildString {
            appendLine("Job role: ${jobInput.role}")
            appendLine("Company: ${jobInput.company}")
            appendLine("JD:")
            appendLine(jobInput.jdText.take(JD_CHARS_RESUME))
            appendLine()
            appendLine("Candidate profile:")
            appendLine("Current title: ${profile.currentTitle}")
            appendLine("Experience (years): ${profile.yearsExperience}")
            appendLine("Strengths: ${profile.strengths}")
            appendLine("Achievements: ${profile.achievements}")
            append(contextBlock(jobInput, profile))
            appendLine()
            appendLine("Return 6-10 bullets, each short and action-focused.")
        }.toString()

        val local = generateWithLocalModel(system, user, profile)
        return if (local.isNotBlank()) local else fallbackEngine.generateResumeHighlights(jobInput, profile)
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        val system = "Classify job fit using one label only from the allowed set."
        val user = buildString {
            appendLine("Allowed labels: Strong Apply, Apply, Review, Skip.")
            appendLine("Job role: ${jobInput.role}")
            appendLine("Company: ${jobInput.company}")
            appendLine("JD:")
            appendLine(jobInput.jdText.take(JD_CHARS_DECISION))
            appendLine()
            appendLine("Candidate target role: ${profile.targetRole}")
            appendLine("Candidate strengths: ${profile.strengths}")
            appendLine("Expected CTC LPA: ${profile.expectedCtcLpa}")
            appendLine("Minimum acceptable LPA: ${profile.minimumAcceptableLpa}")
            append(contextBlock(jobInput, profile))
            appendLine()
            appendLine("Respond with one label only.")
        }.toString()

        val local = normalizeDecision(generateWithLocalModel(system, user, profile))
        return if (local == "Review") fallbackEngine.suggestApplyDecision(jobInput, profile) else local
    }

    override suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? {
        val system = """
            Extract job fields from noisy job page text. Reply with a single JSON object only, keys exactly:
            {"company":"","role":"","location":"","salary_raw":"","pay_period":"","responsibilities":""}
            pay_period must be one of: monthly, yearly, lpa, hourly, unknown. No markdown, no prose.
        """.trimIndent().trim()
        // Keep well under context window in *tokens* (chars >> tokens for noisy page text).
        val user = rawJd.take(6000)
        val raw = generateWithLocalModel(system, user, profile, sanitize = false)
        val parsed = LlmStructuredJobParser.parseStructuredJobJson(sanitizeExtractJson(raw))
        return parsed ?: fallbackEngine.extractStructuredJobFromJd(rawJd, profile)
    }

    override suspend fun summarizeCareerMemory(
        existingMemory: String,
        latestNarrative: String,
        profile: CandidateProfile
    ): String {
        val system = """
            Convert candidate narrative into durable career memory.
            Keep only reusable, high-signal details for future applications.
        """.trimIndent()

        val user = buildString {
            appendLine("Existing memory:")
            appendLine(existingMemory.take(8000))
            appendLine()
            appendLine("New narrative:")
            appendLine(latestNarrative.take(8000))
            appendLine()
            appendLine("Return markdown with sections:")
            appendLine("- Core strengths")
            appendLine("- Evidence-based achievements")
            appendLine("- Preferences and constraints")
            appendLine("- Strong story snippets")
            appendLine("- Open questions")
        }.toString()

        val local = generateWithLocalModel(system, user, profile)
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

    private suspend fun generateWithLocalModel(
        system: String,
        user: String,
        profile: CandidateProfile,
        sanitize: Boolean = true
    ): String = inferenceMutex.withLock {
        val modelPath = profile.localModelPath.trim().ifBlank {
            "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        }
        if (modelPath.isBlank()) return@withLock ""
        val engine = loadModelIfNeededLocked(modelPath) ?: return@withLock ""
        val prompt = wrapChat(system, user, modelPath)
        var result = runCatching { engine.generate(prompt).trim() }.getOrDefault("")
        if (sanitize) result = sanitizeModelOutput(result)
        if (result.isBlank() && user.length > 3000) {
            val shortPrompt = wrapChat(system, user.take(2800), modelPath)
            result = runCatching { engine.generate(shortPrompt).trim() }.getOrDefault("")
            if (sanitize) result = sanitizeModelOutput(result)
        }
        result
    }

    /** Must only be called while holding [inferenceMutex]. */
    private suspend fun loadModelIfNeededLocked(modelPath: String): LlamaModel? {
        val current = model
        if (current != null && loadedModelPath == modelPath) {
            return current
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
        return loaded
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

    private fun sanitizeModelOutput(text: String): String {
        var t = JobPageExtractor.stripInjectedStructuredMetadata(text)
        val leakStart = "<|" + "im_start" + "|>"
        if (t.contains(leakStart)) {
            val lastAsst = t.lastIndexOf(leakStart + "assistant")
            if (lastAsst >= 0) {
                val after = t.substring(lastAsst).substringAfter("\n", missingDelimiterValue = t.substring(lastAsst))
                val trimmed = after.removePrefix(leakStart + "assistant").trim().removePrefix("\n")
                if (trimmed.isNotBlank()) t = trimmed
            }
        }
        return t.trim()
    }

    private fun sanitizeExtractJson(raw: String): String {
        var t = raw.trim()
        val fence = Regex("""```(?:json)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
        val m = fence.find(t)
        if (m != null) t = m.groupValues[1].trim()
        return t
    }

    private fun wrapChat(system: String, user: String, modelPath: String): String =
        if (useLlama3StyleChat(modelPath)) buildLlama3InstructPrompt(system, user)
        else buildQwen25ChatPrompt(system, user)

    private fun useLlama3StyleChat(modelPath: String): Boolean {
        val p = modelPath.lowercase()
        if (p.contains("qwen") || p.contains("gemma") || p.contains("mistral") || p.contains("phi-")) return false
        if (!p.contains("llama")) return false
        return p.contains("llama-3") || p.contains("llama3") || p.contains("llama_3") ||
            p.contains("meta-llama-3") || p.contains("meta_llama_3")
    }

    /** Qwen2.5 chatML-style markers (split so tooling does not alter tokens). */
    private fun buildQwen25ChatPrompt(system: String, user: String): String {
        val imStart = "<|" + "im_start" + "|>"
        val imEnd = "<|" + "im_end" + "|>"
        return buildString {
            append(imStart).append("system\n")
            append(system.trim()).append("\n")
            append(imEnd).append("\n")
            append(imStart).append("user\n")
            append(user.trim()).append("\n")
            append(imEnd).append("\n")
            append(imStart).append("assistant\n")
        }
    }

    /** Llama 3.x Instruct style (matches common GGUF chat templates). */
    private fun buildLlama3InstructPrompt(system: String, user: String): String {
        val begin = "<|" + "begin_of_text" + "|>"
        val sysHdr = "<|" + "start_header_id" + "|>system<|" + "end_header_id" + "|>\n\n"
        val usrHdr = "<|" + "start_header_id" + "|>user<|" + "end_header_id" + "|>\n\n"
        val asstHdr = "<|" + "start_header_id" + "|>assistant<|" + "end_header_id" + "|>\n\n"
        val eot = "<|" + "eot_id" + "|>"
        return begin + sysHdr + system.trim() + eot + usrHdr + user.trim() + eot + asstHdr
    }

    companion object {
        /** Conservative caps: token count is lower than char count, esp. with chat templates. */
        private const val JD_CHARS_COVER = 3200
        private const val JD_CHARS_RESUME = 3200
        private const val JD_CHARS_DECISION = 2800
    }
}
