package com.careerops.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careerops.mobile.data.ApplicationPack
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.FormSuggestion
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.StructuredJobDraft
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.data.ProfileStore
import com.careerops.mobile.llm.HybridLlmEngine
import com.careerops.mobile.llm.LocalLlmEngine
import com.careerops.mobile.llm.StubLocalLlmEngine
import com.careerops.mobile.scoring.JobScoringEngine
import com.careerops.mobile.scoring.SalaryNormalizer
import com.careerops.mobile.web.FormSuggestionEngine
import com.careerops.mobile.web.JobPageExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class MainUiState(
    val profile: CandidateProfile = CandidateProfile(),
    val company: String = "",
    val role: String = "",
    val url: String = "",
    val jdText: String = "",
    val extractedPageText: String = "",
    val isGenerating: Boolean = false,
    val isAutoGenerating: Boolean = false,
    val pendingAutoGenerateAfterExtraction: Boolean = false,
    val statusMessage: String = "",
    val latestPackFolder: String = "",
    val detectedCompany: String = "",
    val detectedRole: String = "",
    val detectedSalaryHint: String = "",
    val fitScore: Double = 0.0,
    val recommendation: String = "Review",
    val recommendationReasons: List<String> = emptyList(),
    val generatedCoverLetter: String = "",
    val generatedResumeHighlights: String = "",
    val formSuggestions: List<FormSuggestion> = emptyList(),
    val autoFlowRequestedAtMs: Long = 0L,
    val isJobLinkDetected: Boolean = false,
    val shouldAutoStartFromShare: Boolean = false,
    val dictationFocusField: String = "",
    val voiceDraftNarrative: String = "",
    val isSummarizingMemory: Boolean = false,
    val knowledgePrompt: String = "Tell me your strongest project story with measurable impact.",
    val manualCaptureMode: Boolean = true,
    val pendingGenerateAfterManualCapture: Boolean = false,
    val apiTestStatus: String = "",
    val isTestingApiConnection: Boolean = false,
    val aiSetupModeDraft: String = "local",
    val aiSetupLocalModelPathDraft: String = "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf",
    val aiSetupApiBaseUrlDraft: String = "https://api.openai.com/v1",
    val aiSetupApiModelDraft: String = "gpt-4o-mini",
    val aiSetupApiKeyDraft: String = "",
    /** Raw concatenated JSON-LD script bodies from the in-app WebView (see JobWebViewScreen). */
    val lastJsonLdRaw: String = "",
    /** Optional per-job notes the user wants emphasized in prompts and scoring. */
    val jobExtraContext: String = "",
    val webViewLoadError: String = "",
    /** Derived whenever state is committed (onboarding / share / normal). */
    val appFlowPhase: AppFlowPhase = AppFlowPhase.FirstRun
)

class MainViewModel(
    private val repository: PackRepository,
    private val profileStore: ProfileStore,
    private val llmEngine: LocalLlmEngine = StubLocalLlmEngine(),
    private val formSuggestionEngine: FormSuggestionEngine = FormSuggestionEngine(),
    private val scoringEngine: JobScoringEngine = JobScoringEngine()
) : ViewModel() {
    private val knowledgePrompts = listOf(
        "Tell me your strongest project story with measurable impact.",
        "Describe a challenge you solved that others found difficult.",
        "What specific tools or pipelines are you strongest with?",
        "What kind of roles, teams, and work style suit you best?",
        "What compensation, location, or notice constraints matter most to you?"
    )
    private var knowledgePromptIndex = 0

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        push(_uiState.value)
        viewModelScope.launch {
            profileStore.profileFlow.collect { profile ->
                push(_uiState.value.copy(profile = profile))
                syncAiDraftsFromProfile(profile)
            }
        }
    }

    fun ingestSharedText(sharedText: String?) {
        val text = sharedText?.trim().orEmpty()
        if (text.isBlank()) return
        val maybeUrl = extractFirstUrl(text)
        val isLikelyJob = maybeUrl.isNotBlank() && JobPageExtractor.isLikelyJobUrl(maybeUrl)
        push(
            _uiState.value.copy(
                url = maybeUrl.ifBlank { _uiState.value.url },
                extractedPageText = if (maybeUrl.isNotBlank()) "" else _uiState.value.extractedPageText,
                jdText = if (maybeUrl.isBlank()) text else _uiState.value.jdText,
                detectedCompany = if (maybeUrl.isNotBlank()) "" else _uiState.value.detectedCompany,
                detectedRole = if (maybeUrl.isNotBlank()) "" else _uiState.value.detectedRole,
                detectedSalaryHint = if (maybeUrl.isNotBlank()) "" else _uiState.value.detectedSalaryHint,
                isJobLinkDetected = isLikelyJob,
                shouldAutoStartFromShare = isLikelyJob,
                statusMessage = if (isLikelyJob) {
                    "Job link detected from share. Starting one-tap flow."
                } else {
                    "Shared content imported."
                }
            )
        )
    }

    fun updateCompany(value: String) { push(_uiState.value.copy(company = value)) }
    fun updateRole(value: String) { push(_uiState.value.copy(role = value)) }
    fun updateUrl(value: String) {
        val current = _uiState.value
        push(
            current.copy(
                url = value,
                extractedPageText = if (current.url != value) "" else current.extractedPageText,
                detectedCompany = if (current.url != value) "" else current.detectedCompany,
                detectedRole = if (current.url != value) "" else current.detectedRole,
                detectedSalaryHint = if (current.url != value) "" else current.detectedSalaryHint,
                lastJsonLdRaw = if (current.url != value) "" else current.lastJsonLdRaw,
                webViewLoadError = if (current.url != value) "" else current.webViewLoadError
            )
        )
    }
    fun updateJdText(value: String) { push(_uiState.value.copy(jdText = value)) }

    fun updateJsonLdFromPage(jsonLd: String) {
        push(_uiState.value.copy(lastJsonLdRaw = jsonLd))
    }

    fun updateJobExtraContext(value: String) {
        push(_uiState.value.copy(jobExtraContext = value))
    }

    fun reportWebViewLoadError(message: String) {
        push(_uiState.value.copy(webViewLoadError = message))
    }

    fun clearWebViewLoadError() {
        push(_uiState.value.copy(webViewLoadError = ""))
    }

    fun updateExtractedPageText(value: String) {
        val state = _uiState.value
        val cleaned = JobPageExtractor.cleanExtractedText(value)
        val merged = if (state.lastJsonLdRaw.isNotBlank()) {
            JobPageExtractor.mergeJsonLdIntoPageText(cleaned, state.lastJsonLdRaw)
        } else {
            cleaned
        }
        val detectedCompany = JobPageExtractor.detectCompany(merged)
        val detectedRole = JobPageExtractor.detectRole(merged)
        val detectedSalary = JobPageExtractor.detectSalaryExpanded(merged)
        val next = state.copy(
            extractedPageText = merged,
            webViewLoadError = "",
            company = if (state.company.isBlank()) detectedCompany else state.company,
            role = if (state.role.isBlank()) detectedRole else state.role,
            detectedCompany = detectedCompany,
            detectedRole = detectedRole,
            detectedSalaryHint = detectedSalary
        )
        push(next)
        if (next.pendingAutoGenerateAfterExtraction || next.pendingGenerateAfterManualCapture) {
            push(
                next.copy(
                    pendingAutoGenerateAfterExtraction = false,
                    pendingGenerateAfterManualCapture = false,
                    statusMessage = if (cleaned.isBlank()) {
                        "Page had little readable text. Generating with available input."
                    } else {
                        next.statusMessage
                    }
                )
            )
            generatePack()
        }
    }

    fun refreshSuggestionsFromVisibleText(visibleText: String) {
        val suggestions = formSuggestionEngine.suggestFromVisibleText(
            visibleText = visibleText,
            profile = _uiState.value.profile
        )
        push(_uiState.value.copy(formSuggestions = suggestions))
    }

    fun requestGenerateAfterManualCapture() {
        val state = _uiState.value
        if (state.url.isBlank()) {
            push(state.copy(statusMessage = "Job URL is required."))
            return
        }
        push(
            state.copy(
                pendingGenerateAfterManualCapture = true,
                statusMessage = "Waiting for manual capture, then generating."
            )
        )
    }

    fun setManualCaptureMode(enabled: Boolean) {
        push(_uiState.value.copy(manualCaptureMode = enabled))
    }

    fun saveProfile(updated: CandidateProfile) {
        viewModelScope.launch {
            profileStore.saveProfile(updated)
            push(
                _uiState.value.copy(
                    profile = updated,
                    statusMessage = "Profile saved locally."
                )
            )
        }
    }

    fun applyImportedResumeProfile(updated: CandidateProfile, autoFilled: Boolean) {
        viewModelScope.launch {
            profileStore.saveProfile(updated)
            push(
                _uiState.value.copy(
                    profile = updated,
                    statusMessage = if (autoFilled) {
                        "Resume imported. Profile fields were auto-filled where possible."
                    } else {
                        "Resume imported. You can fill remaining fields manually."
                    }
                )
            )
        }
    }

    fun updateAiSetupModeDraft(value: String) {
        push(_uiState.value.copy(aiSetupModeDraft = value))
    }

    fun updateAiSetupLocalModelPathDraft(value: String) {
        push(_uiState.value.copy(aiSetupLocalModelPathDraft = value))
    }

    fun updateAiSetupApiBaseUrlDraft(value: String) {
        push(_uiState.value.copy(aiSetupApiBaseUrlDraft = value))
    }

    fun updateAiSetupApiModelDraft(value: String) {
        push(_uiState.value.copy(aiSetupApiModelDraft = value))
    }

    fun updateAiSetupApiKeyDraft(value: String) {
        push(_uiState.value.copy(aiSetupApiKeyDraft = value))
    }

    fun saveAiSetupDraftsToProfile() {
        val state = _uiState.value
        val updated = state.profile.copy(
            llmProviderMode = state.aiSetupModeDraft,
            localModelPath = state.aiSetupLocalModelPathDraft,
            apiBaseUrl = state.aiSetupApiBaseUrlDraft,
            apiModel = state.aiSetupApiModelDraft,
            apiKey = state.aiSetupApiKeyDraft
        )
        saveProfile(updated)
    }

    fun onSharedJobAutoStartHandled() {
        push(_uiState.value.copy(shouldAutoStartFromShare = false))
    }

    fun beginVoiceCapture(field: String) {
        push(_uiState.value.copy(dictationFocusField = field))
    }

    fun consumeVoiceField(): String {
        val field = _uiState.value.dictationFocusField
        push(_uiState.value.copy(dictationFocusField = ""))
        return field
    }

    fun applyVoiceInput(field: String, text: String) {
        val profile = _uiState.value.profile
        when (field) {
            "fullName" -> saveProfile(profile.copy(fullName = text))
            "currentTitle" -> saveProfile(profile.copy(currentTitle = text))
            "targetRole" -> saveProfile(profile.copy(targetRole = text))
            "resumeUri" -> saveProfile(profile.copy(resumeUri = text))
            "strengths" -> saveProfile(profile.copy(strengths = text))
            "achievements" -> saveProfile(profile.copy(achievements = text))
            "careerNarrative" -> {
                val current = _uiState.value.voiceDraftNarrative
                val merged = listOf(current, text).filter { it.isNotBlank() }.joinToString("\n\n")
                push(_uiState.value.copy(voiceDraftNarrative = merged))
            }
        }
    }

    fun onVoiceCaptureUnavailable(message: String = "Voice capture unavailable on this device. Please type your answer.") {
        push(
            _uiState.value.copy(
                dictationFocusField = "",
                statusMessage = message
            )
        )
    }

    fun onVoicePermissionDenied() {
        push(
            _uiState.value.copy(
                dictationFocusField = "",
                statusMessage = "Microphone permission denied. Enable it from app settings to use voice memory."
            )
        )
    }

    fun clearNarrativeDraft() {
        push(_uiState.value.copy(voiceDraftNarrative = ""))
    }

    fun nextKnowledgePrompt() {
        knowledgePromptIndex = (knowledgePromptIndex + 1) % knowledgePrompts.size
        push(_uiState.value.copy(knowledgePrompt = knowledgePrompts[knowledgePromptIndex]))
    }

    fun updateNarrativeDraft(value: String) {
        push(_uiState.value.copy(voiceDraftNarrative = value))
    }

    fun summarizeNarrativeIntoMemory() {
        val state = _uiState.value
        val narrative = state.voiceDraftNarrative.trim()
        if (narrative.isBlank()) {
            push(state.copy(statusMessage = "Please dictate your story first."))
            return
        }
        viewModelScope.launch {
            push(state.copy(isSummarizingMemory = true, statusMessage = "Summarizing your story..."))
            val summarized = withContext(Dispatchers.IO) {
                llmEngine.summarizeCareerMemory(state.profile.careerMemory, narrative, state.profile)
            }
            val updatedProfile = state.profile.copy(careerMemory = summarized)
            profileStore.saveProfile(updatedProfile)
            push(
                _uiState.value.copy(
                    profile = updatedProfile,
                    isSummarizingMemory = false,
                    voiceDraftNarrative = "",
                    statusMessage = "Career memory updated from your voice notes."
                )
            )
        }
    }

    fun startAutoGenerateFromUrlOnly() {
        val state = _uiState.value
        if (state.url.isBlank()) {
            push(state.copy(statusMessage = "Job URL is required."))
            return
        }
        if (state.manualCaptureMode) {
            push(
                state.copy(
                    isAutoGenerating = true,
                    pendingAutoGenerateAfterExtraction = false,
                    pendingGenerateAfterManualCapture = false,
                    autoFlowRequestedAtMs = System.currentTimeMillis(),
                    extractedPageText = "",
                    detectedCompany = "",
                    detectedRole = "",
                    detectedSalaryHint = "",
                    shouldAutoStartFromShare = false,
                    statusMessage = "Manual capture mode is ON. Open In-App Page and press Capture + Generate."
                )
            )
            return
        }
        push(
            state.copy(
                isAutoGenerating = true,
                pendingAutoGenerateAfterExtraction = true,
                autoFlowRequestedAtMs = System.currentTimeMillis(),
                extractedPageText = "",
                detectedCompany = "",
                detectedRole = "",
                detectedSalaryHint = "",
                shouldAutoStartFromShare = false,
                statusMessage = "Opening page and extracting JD. Generation will start automatically."
            )
        )
    }

    fun startAutoFlowFromShare() {
        val state = _uiState.value
        if (state.manualCaptureMode) {
            push(
                state.copy(
                    shouldAutoStartFromShare = false,
                    isAutoGenerating = true,
                    autoFlowRequestedAtMs = System.currentTimeMillis(),
                    extractedPageText = "",
                    detectedCompany = "",
                    detectedRole = "",
                    detectedSalaryHint = "",
                    statusMessage = "Job link detected. Manual capture mode is ON. Open In-App Page and tap Capture + Generate."
                )
            )
            return
        }
        startAutoGenerateFromUrlOnly()
    }

    fun generatePack() {
        val state = _uiState.value
        if (state.url.isBlank()) {
            push(state.copy(statusMessage = "Job URL is required."))
            return
        }

        viewModelScope.launch {
            push(state.copy(isGenerating = true, statusMessage = "Generating pack..."))

            val effectiveJd = when {
                state.extractedPageText.isNotBlank() -> state.extractedPageText
                state.jdText.isNotBlank() -> state.jdText
                else -> ""
            }

            val structuredLlm: StructuredJobDraft? = withContext(Dispatchers.IO) {
                if (effectiveJd.isBlank()) null
                else llmEngine.extractStructuredJobFromJd(effectiveJd, state.profile)
            }

            var detectedCompany = if (state.company.isBlank()) JobPageExtractor.detectCompany(effectiveJd) else state.company.trim()
            var detectedRole = if (state.role.isBlank()) JobPageExtractor.detectRole(effectiveJd) else state.role.trim()
            if (detectedCompany.isBlank()) detectedCompany = structuredLlm?.company?.trim().orEmpty()
            if (detectedRole.isBlank()) detectedRole = structuredLlm?.role?.trim().orEmpty()

            var detectedSalary = JobPageExtractor.detectSalaryExpanded(effectiveJd)
            if (detectedSalary.isBlank()) detectedSalary = structuredLlm?.salaryRaw?.trim().orEmpty()

            val salaryBlob = listOf(detectedSalary, structuredLlm?.salaryRaw.orEmpty())
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" | ")
            val lpaApprox = SalaryNormalizer.approximateAnnualLpa(
                salaryText = salaryBlob,
                payPeriodHint = structuredLlm?.payPeriodHint ?: "unknown",
                extraContext = effectiveJd.take(4000)
            )

            val resumeSummary = state.profile.resumeTextSnapshot.ifBlank { state.profile.strengths }.take(6000)

            val jobInput = JobInput(
                company = detectedCompany.ifBlank { "Unknown Company" },
                role = detectedRole.ifBlank { "Unknown Role" },
                url = state.url.trim(),
                jdText = effectiveJd,
                salaryHint = detectedSalary,
                salaryAnnualLpaApprox = lpaApprox,
                jobSpecificNotes = state.jobExtraContext.trim(),
                resumeSummaryForPrompt = resumeSummary
            )

            val insight = scoringEngine.score(jobInput, state.profile)

            val pack: ApplicationPack = withContext(Dispatchers.IO) {
                repository.generatePack(jobInput, state.profile)
            }
            val coverLetter = withContext(Dispatchers.IO) {
                llmEngine.generateCoverLetter(jobInput, state.profile)
            }
            val resumeHighlights = withContext(Dispatchers.IO) {
                llmEngine.generateResumeHighlights(jobInput, state.profile)
            }
            val applyDecision = withContext(Dispatchers.IO) {
                llmEngine.suggestApplyDecision(jobInput, state.profile)
            }

            withContext(Dispatchers.IO) {
                writeGeneratedOutputs(pack.folderName, jobInput, coverLetter, resumeHighlights)
            }

            push(
                _uiState.value.copy(
                    isGenerating = false,
                    isAutoGenerating = false,
                    pendingAutoGenerateAfterExtraction = false,
                    company = insight.detectedCompany.ifBlank { jobInput.company },
                    role = insight.detectedRole.ifBlank { jobInput.role },
                    latestPackFolder = pack.folderName,
                    detectedCompany = insight.detectedCompany,
                    detectedRole = insight.detectedRole,
                    detectedSalaryHint = insight.detectedSalaryText.ifBlank { detectedSalary },
                    fitScore = insight.score,
                    recommendation = if (applyDecision == "Strong Apply" && insight.recommendation == "Apply") "Strong Apply" else insight.recommendation,
                    recommendationReasons = insight.reasons,
                    generatedCoverLetter = coverLetter,
                    generatedResumeHighlights = resumeHighlights,
                    statusMessage = "Pack generated at ${pack.folderPath}"
                )
            )
        }
    }

    fun testApiConnection() {
        val state = _uiState.value
        viewModelScope.launch {
            push(state.copy(isTestingApiConnection = true, apiTestStatus = ""))
            val profile = state.profile.copy(
                llmProviderMode = state.aiSetupModeDraft,
                localModelPath = state.aiSetupLocalModelPathDraft,
                apiBaseUrl = state.aiSetupApiBaseUrlDraft,
                apiModel = state.aiSetupApiModelDraft,
                apiKey = state.aiSetupApiKeyDraft
            )
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (!profile.llmProviderMode.equals("api", ignoreCase = true)) {
                        return@runCatching "Switch to API key mode first."
                    }
                    if (profile.apiBaseUrl.isBlank() || profile.apiModel.isBlank() || profile.apiKey.isBlank()) {
                        return@runCatching "Fill API Base URL, model, and key."
                    }
                    if (llmEngine is HybridLlmEngine) {
                        llmEngine.pingApi(profile).getOrElse { err ->
                            "API test failed: ${err.message ?: "Unknown error"}"
                        }
                    } else {
                        val probe = JobInput(
                            company = "API Connectivity Check",
                            role = "Test",
                            url = "https://example.com",
                            jdText = "Test prompt for API connectivity.",
                            salaryHint = "",
                            jobSpecificNotes = "",
                            resumeSummaryForPrompt = ""
                        )
                        val response = llmEngine.suggestApplyDecision(probe, profile)
                        if (response.isBlank()) {
                            "No response received. Check endpoint, model, and key permissions."
                        } else {
                            "API connection looks OK."
                        }
                    }
                }.getOrElse { err ->
                    "API test failed: ${err.message ?: "Unknown error"}"
                }
            }
            push(
                _uiState.value.copy(
                    isTestingApiConnection = false,
                    apiTestStatus = result,
                    statusMessage = result
                )
            )
        }
    }

    private fun writeGeneratedOutputs(
        folderName: String,
        jobInput: JobInput,
        coverLetter: String,
        resumeHighlights: String
    ) {
        val slug = "${jobInput.company}-${jobInput.role}".lowercase(Locale.ENGLISH)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        val baseDir = File(repository.getPackBaseDir(), folderName)

        File(baseDir, "$slug-cover-letter.md").writeText(
            "# Cover Letter - ${jobInput.company} - ${jobInput.role}\n\n$coverLetter\n"
        )
        File(baseDir, "$slug-resume.md").writeText(
            "# Tailored Resume Notes - ${jobInput.company} - ${jobInput.role}\n\n$resumeHighlights\n"
        )
    }

    private fun extractFirstUrl(text: String): String {
        val match = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE).find(text)?.value.orEmpty()
        return match.trim().trimEnd('.', ',', ';', ')', ']', '}')
    }

    private fun syncAiDraftsFromProfile(profile: CandidateProfile) {
        push(
            _uiState.value.copy(
                aiSetupModeDraft = profile.llmProviderMode,
                aiSetupLocalModelPathDraft = profile.localModelPath,
                aiSetupApiBaseUrlDraft = profile.apiBaseUrl,
                aiSetupApiModelDraft = profile.apiModel,
                aiSetupApiKeyDraft = profile.apiKey
            )
        )
    }

    private fun deriveAppFlow(state: MainUiState): AppFlowPhase = when {
        !state.profile.onboardingCompleted -> AppFlowPhase.FirstRun
        state.shouldAutoStartFromShare || (state.isAutoGenerating && state.url.isNotBlank()) ->
            AppFlowPhase.JobFromShare
        else -> AppFlowPhase.Ready
    }

    private fun push(next: MainUiState) {
        _uiState.value = next.copy(appFlowPhase = deriveAppFlow(next))
    }
}

