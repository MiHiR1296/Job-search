package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class HybridLlmEngine(
    private val localEngine: LocalLlmEngine = StubLocalLlmEngine(),
    private val httpClient: OkHttpClient = OkHttpClient()
) : LocalLlmEngine {

    private fun profileJobContext(jobInput: JobInput, profile: CandidateProfile): String = buildString {
        if (profile.careerMemory.isNotBlank()) {
            appendLine("Long-term career memory:")
            appendLine(profile.careerMemory.take(6000))
        }
        if (jobInput.jobSpecificNotes.isNotBlank()) {
            appendLine("Per-job notes (emphasize for this application):")
            appendLine(jobInput.jobSpecificNotes.take(2000))
        }
        if (jobInput.resumeSummaryForPrompt.isNotBlank()) {
            appendLine("Resume excerpts:")
            appendLine(jobInput.resumeSummaryForPrompt.take(4500))
        }
    }.toString().trim()

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
            val ctx = profileJobContext(jobInput, profile)
            val prompt = """
                Create a concise, role-specific cover letter for:
                Company: ${jobInput.company}
                Role: ${jobInput.role}
                JD:
                ${jobInput.jdText.take(12000)}

                Candidate:
                Name: ${profile.fullName}
                Current title: ${profile.currentTitle}
                Target role: ${profile.targetRole}
                Strengths: ${profile.strengths}
                Achievements: ${profile.achievements}
                ${if (ctx.isNotBlank()) "\n$ctx" else ""}

                Keep it truthful, practical, and under 260 words.
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.generateCoverLetter(jobInput, profile)
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
            val ctx = profileJobContext(jobInput, profile)
            val prompt = """
                Generate ATS-oriented resume highlights for:
                Company: ${jobInput.company}
                Role: ${jobInput.role}
                JD:
                ${jobInput.jdText.take(12000)}

                Candidate:
                Current title: ${profile.currentTitle}
                Target role: ${profile.targetRole}
                Strengths: ${profile.strengths}
                Achievements: ${profile.achievements}
                Experience: ${profile.yearsExperience}
                ${if (ctx.isNotBlank()) "\n$ctx" else ""}

                Return short bullets only.
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.generateResumeHighlights(jobInput, profile)
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
            val ctx = profileJobContext(jobInput, profile)
            val prompt = """
                Decide one of: Strong Apply, Apply, Review, Skip.
                Job role: ${jobInput.role}
                Company: ${jobInput.company}
                JD:
                ${jobInput.jdText.take(12000)}

                Candidate target role: ${profile.targetRole}
                Candidate strengths: ${profile.strengths}
                Expected CTC: ${profile.expectedCtcLpa}
                Minimum acceptable CTC: ${profile.minimumAcceptableLpa}
                ${if (ctx.isNotBlank()) "\n$ctx" else ""}
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.contains("Strong Apply", ignoreCase = true)) return "Strong Apply"
            if (remote.contains("Apply", ignoreCase = true)) return "Apply"
            if (remote.contains("Skip", ignoreCase = true)) return "Skip"
        }
        return localEngine.suggestApplyDecision(jobInput, profile)
    }

    override suspend fun extractStructuredJobFromJd(rawJd: String, profile: CandidateProfile): StructuredJobDraft? {
        val local = localEngine.extractStructuredJobFromJd(rawJd, profile)
        if (local != null && (local.company.isNotBlank() || local.role.isNotBlank() || local.salaryRaw.isNotBlank())) {
            return local
        }
        if (!shouldUseRemote(profile)) return local
        val prompt = """
            Extract structured job fields from noisy web page text. Reply with a single JSON object only (no markdown fences), keys exactly:
            {"company":"","role":"","location":"","salary_raw":"","pay_period":"","responsibilities":""}
            pay_period must be one of: monthly, yearly, lpa, hourly, unknown.

            Page text:
            ${rawJd.take(14000)}
        """.trimIndent()
        val remote = callRemote(profile, prompt)
        return LlmStructuredJobParser.parseStructuredJobJson(remote) ?: local
    }

    override suspend fun summarizeCareerMemory(
        existingMemory: String,
        latestNarrative: String,
        profile: CandidateProfile
    ): String {
        if (shouldUseRemote(profile)) {
            val prompt = """
                Convert spoken candidate narrative into durable career memory.
                Keep only reusable details for future job applications.

                Existing memory:
                ${existingMemory.take(12000)}

                New narrative:
                ${latestNarrative.take(12000)}

                Return concise markdown with sections:
                - Core strengths
                - Evidence-based achievements
                - Preferences and constraints
                - Good portfolio/resume stories
                - Open questions
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.summarizeCareerMemory(existingMemory, latestNarrative, profile)
    }

    override suspend fun chat(prompt: String, profile: CandidateProfile): String {
        val p = prompt.trim()
        if (p.isBlank()) return ""
        if (shouldUseRemote(profile)) {
            val remote = callRemote(profile, p)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.chat(p, profile)
    }

    suspend fun pingApi(profile: CandidateProfile): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!shouldUseRemote(profile)) {
                return@runCatching "API mode is not fully configured."
            }
            val endpoint = resolveChatCompletionsEndpoint(profile.apiBaseUrl)
            val payload = JSONObject()
                .put("model", profile.apiModel)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", "You are a connectivity probe."))
                        .put(JSONObject().put("role", "user").put("content", "Reply with: ok"))
                )
                .put("temperature", 0.0)
                .toString()

            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${profile.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty().take(1200)
                if (!res.isSuccessful) {
                    val bodyPreview = if (body.isBlank()) "(empty response body)" else body
                    return@runCatching "API test failed (${res.code})\nEndpoint: $endpoint\n$bodyPreview"
                }
                val content = runCatching {
                    val json = JSONObject(body)
                    val choices = json.optJSONArray("choices")
                    if (choices == null || choices.length() == 0) "" else {
                        choices.getJSONObject(0)
                            .optJSONObject("message")
                            ?.optString("content", "")
                            .orEmpty()
                            .trim()
                    }
                }.getOrDefault("")
                if (content.isNotBlank()) {
                    "API connection looks OK.\nReply: ${content.take(120)}"
                } else {
                    "API connection looks OK.\nEndpoint: $endpoint"
                }
            }
        }
    }

    private fun shouldUseRemote(profile: CandidateProfile): Boolean {
        return profile.llmProviderMode.equals("api", ignoreCase = true) &&
            profile.apiKey.isNotBlank() &&
            profile.apiBaseUrl.isNotBlank() &&
            profile.apiModel.isNotBlank()
    }

    private suspend fun callRemote(profile: CandidateProfile, prompt: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("model", profile.apiModel)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", "You are a precise career assistant."))
                        .put(JSONObject().put("role", "user").put("content", prompt))
                )
                .put("temperature", 0.3)
                .toString()

            val endpoint = resolveChatCompletionsEndpoint(profile.apiBaseUrl)
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${profile.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@runCatching ""
                val body = res.body?.string().orEmpty()
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: return@runCatching ""
                if (choices.length() == 0) return@runCatching ""
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message") ?: return@runCatching ""
                message.optString("content", "").trim()
            }
        }.getOrDefault("")
    }

    private fun resolveChatCompletionsEndpoint(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions", ignoreCase = true)) {
            normalized
        } else {
            "$normalized/chat/completions"
        }
    }
}

