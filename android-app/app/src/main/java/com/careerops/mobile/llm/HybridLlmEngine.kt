package com.careerops.mobile.llm

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInput
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

    override suspend fun generateCoverLetter(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
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

                Keep it truthful, practical, and under 260 words.
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.generateCoverLetter(jobInput, profile)
    }

    override suspend fun generateResumeHighlights(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
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

                Return short bullets only.
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.isNotBlank()) return remote
        }
        return localEngine.generateResumeHighlights(jobInput, profile)
    }

    override suspend fun suggestApplyDecision(jobInput: JobInput, profile: CandidateProfile): String {
        if (shouldUseRemote(profile)) {
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
            """.trimIndent()
            val remote = callRemote(profile, prompt)
            if (remote.contains("Strong Apply", ignoreCase = true)) return "Strong Apply"
            if (remote.contains("Apply", ignoreCase = true)) return "Apply"
            if (remote.contains("Skip", ignoreCase = true)) return "Skip"
        }
        return localEngine.suggestApplyDecision(jobInput, profile)
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

            val req = Request.Builder()
                .url(profile.apiBaseUrl.trimEnd('/') + "/chat/completions")
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
}

