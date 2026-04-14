package com.careerops.mobile.ui

import android.content.Context
import android.os.Debug
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
import com.careerops.mobile.diagnostics.CrashLogWriter
import com.careerops.mobile.diagnostics.AppLogger
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
import java.util.concurrent.atomic.AtomicBoolean

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
    /** True while a picked GGUF is being copied into app-private storage. */
    val isImportingLocalModel: Boolean = false,
    /** Raw concatenated JSON-LD script bodies from the in-app WebView (see JobWebViewScreen). */
    val lastJsonLdRaw: String = "",
    /** Optional per-job notes the user wants emphasized in prompts and scoring. */
    val jobExtraContext: String = "",
    val webViewLoadError: String = "",
    val liveLoggingEnabled: Boolean = false,
    val devChatPrompt: String = "",
    val devChatOutput: String = "",
    val isDevChatRunning: Boolean = false,
    /** Derived whenever state is committed (onboarding / share / normal). */
    val appFlowPhase: AppFlowPhase = AppFlowPhase.FirstRun
)

class MainViewModel(
    private val appContext: Context,
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
    private val generatePackInFlight = AtomicBoolean(false)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        push(_uiState.value.copy(liveLoggingEnabled = AppLogger.isEnabled()))
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
        AppLogger.log(appContext, "share", "ingestSharedText len=${text.length}")
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
        AppLogger.log(appContext, "job", "updateUrl url=${value.take(200)}")
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
        AppLogger.log(appContext, "capture", "updateExtractedPageText cleanedLen=${cleaned.length}")
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
        AppLogger.log(appContext, "generate", "requestGenerateAfterManualCapture url=${state.url.take(200)}")
        push(
            state.copy(
                pendingGenerateAfterManualCapture = true,
                statusMessage = "Waiting for manual capture, then generating."
            )
        )
    }

    fun setLiveLoggingEnabled(enabled: Boolean) {
        if (enabled) {
            val path = AppLogger.start(appContext)
            push(_uiState.value.copy(liveLoggingEnabled = true, statusMessage = "Live logging ON."))
            AppLogger.log(appContext, "logger", "path=$path")
        } else {
            AppLogger.stop(appContext)
            push(_uiState.value.copy(liveLoggingEnabled = false, statusMessage = "Live logging OFF."))
        }
    }

    fun exportLatestLogsToDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val crash = CrashLogWriter.exportLatestCrashToDownloads(appContext)
            val session = CrashLogWriter.exportLatestSessionLogToDownloads(appContext)
            val msg = buildString {
                append("Exported to Downloads/CareerOpsMobile: ")
                append(
                    listOfNotNull(
                        crash?.let { "crash=$it" },
                        session?.let { "session=$it" }
                    ).ifEmpty { listOf("(nothing yet)") }.joinToString(", ")
                )
            }
            withContext(Dispatchers.Main) {
                push(_uiState.value.copy(statusMessage = msg))
            }
        }
    }

    fun smokeTestLocalModel() {
        val state = _uiState.value
        viewModelScope.launch {
            if (!generatePackInFlight.compareAndSet(false, true)) {
                push(_uiState.value.copy(statusMessage = "Another operation is already running."))
                return@launch
            }
            val rt = Runtime.getRuntime()
            val beforeJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            val beforeNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
            try {
                push(state.copy(isGenerating = true, statusMessage = "Smoke test: loading model + tiny generation…"))
                AppLogger.log(appContext, "dev", "smokeTestLocalModel start javaUsedMB=$beforeJavaMb nativeMB=$beforeNativeMb")

                val job = JobInput(
                    company = "Test",
                    role = "Test",
                    url = "",
                    jdText = "Reply with exactly: OK"
                )
                val out = withContext(Dispatchers.Default) {
                    llmEngine.generateCoverLetter(job, _uiState.value.profile).trim()
                }.take(280)

                val afterJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val afterNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(appContext, "dev", "smokeTestLocalModel done outLen=${out.length} javaUsedMB=$afterJavaMb nativeMB=$afterNativeMb")
                push(
                    _uiState.value.copy(
                        isGenerating = false,
                        statusMessage = "Smoke test OK. Output: ${out.ifBlank { "(empty)" }} | Java ${beforeJavaMb}→${afterJavaMb}MB, Native ${beforeNativeMb}→${afterNativeMb}MB"
                    )
                )
            } catch (t: Throwable) {
                CrashLogWriter.writeCaughtThrowable(appContext, "smokeTestLocalModel", t)
                push(
                    _uiState.value.copy(
                        isGenerating = false,
                        statusMessage = "Smoke test failed: ${t.message ?: t.javaClass.simpleName}. Use Share diagnostics / Export logs."
                    )
                )
            } finally {
                generatePackInFlight.set(false)
            }
        }
    }

    fun setManualCaptureMode(enabled: Boolean) {
        push(_uiState.value.copy(manualCaptureMode = enabled))
    }

    fun updateDevChatPrompt(value: String) {
        push(_uiState.value.copy(devChatPrompt = value))
    }

    fun runDevChat() {
        val state = _uiState.value
        val prompt = state.devChatPrompt.trim()
        if (prompt.isBlank()) {
            push(state.copy(statusMessage = "Enter a prompt first."))
            return
        }
        viewModelScope.launch {
            if (!generatePackInFlight.compareAndSet(false, true)) {
                push(_uiState.value.copy(statusMessage = "Another operation is already running."))
                return@launch
            }
            val rt = Runtime.getRuntime()
            val beforeJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            val beforeNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
            try {
                push(_uiState.value.copy(isDevChatRunning = true, isGenerating = true, statusMessage = "Local chat: generating…"))
                AppLogger.log(appContext, "chat", "runDevChat start len=${prompt.length} javaUsedMB=$beforeJavaMb nativeMB=$beforeNativeMb")
                val out = withContext(Dispatchers.Default) {
                    llmEngine.chat(prompt, _uiState.value.profile).trim()
                }
                val afterJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val afterNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(appContext, "chat", "runDevChat done outLen=${out.length} javaUsedMB=$afterJavaMb nativeMB=$afterNativeMb")
                push(
                    _uiState.value.copy(
                        isDevChatRunning = false,
                        isGenerating = false,
                        devChatOutput = out.ifBlank { "(empty)" },
                        statusMessage = "Chat done. Java ${beforeJavaMb}→${afterJavaMb}MB, Native ${beforeNativeMb}→${afterNativeMb}MB"
                    )
                )
            } catch (t: Throwable) {
                CrashLogWriter.writeCaughtThrowable(appContext, "devChat", t)
                push(
                    _uiState.value.copy(
                        isDevChatRunning = false,
                        isGenerating = false,
                        statusMessage = "Chat failed: ${t.message ?: t.javaClass.simpleName}. Use Share diagnostics."
                    )
                )
            } finally {
                generatePackInFlight.set(false)
            }
        }
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

    fun beginLocalModelImport() {
        push(
            _uiState.value.copy(
                isImportingLocalModel = true,
                statusMessage = "Copying model into app storage…"
            )
        )
    }

    fun finishLocalModelImport(path: String?, errorMessage: String?) {
        val current = _uiState.value
        push(
            current.copy(
                isImportingLocalModel = false,
                aiSetupLocalModelPathDraft = path ?: current.aiSetupLocalModelPathDraft,
                statusMessage = when {
                    path != null -> "Model copied. Tap Save below to store this path in your profile."
                    else -> errorMessage ?: "Could not import the selected file."
                }
            )
        )
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
            if (!generatePackInFlight.compareAndSet(false, true)) {
                push(_uiState.value.copy(statusMessage = "Generation is already running."))
                return@launch
            }
            try {
                val rt = Runtime.getRuntime()
                val startJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val startNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(
                    appContext,
                    "generate",
                    "generatePack start url=${state.url.take(200)} javaUsedMB=$startJavaMb nativeMB=$startNativeMb " +
                        "hasExtracted=${state.extractedPageText.isNotBlank()} jdLen=${state.jdText.length} extractedLen=${state.extractedPageText.length}"
                )
                push(state.copy(isGenerating = true, statusMessage = "Generating pack..."))

                val effectiveJd = when {
                    state.extractedPageText.isNotBlank() -> state.extractedPageText
                    state.jdText.isNotBlank() -> state.jdText
                    else -> ""
                }
                AppLogger.log(appContext, "generate", "effectiveJd len=${effectiveJd.length} jsonLdLen=${state.lastJsonLdRaw.length}")

                AppLogger.log(appContext, "generate", "structuredExtract start")
                val structuredLlm: StructuredJobDraft? = withContext(Dispatchers.IO) {
                    if (effectiveJd.isBlank()) null
                    else llmEngine.extractStructuredJobFromJd(effectiveJd, state.profile)
                }
                AppLogger.log(
                    appContext,
                    "generate",
                    "structuredExtract done company=${structuredLlm?.company.orEmpty().take(40)} role=${structuredLlm?.role.orEmpty().take(40)} salaryRawLen=${structuredLlm?.salaryRaw?.length ?: 0}"
                )

                val structuredFromHeader = JobPageExtractor.parseInjectedStructuredMetadataHeader(effectiveJd)
                val structuredFromJsonLd = JobPageExtractor.structuredDraftFromJsonLd(state.lastJsonLdRaw)
                val structuredMerged = JobPageExtractor.mergeStructuredJobDrafts(
                    structuredFromHeader,
                    structuredFromJsonLd,
                    structuredLlm
                )

                var detectedCompany = if (state.company.isBlank()) {
                    structuredMerged.company.ifBlank { JobPageExtractor.detectCompany(effectiveJd) }
                } else {
                    state.company.trim()
                }
                var detectedRole = if (state.role.isBlank()) {
                    structuredMerged.role.ifBlank { JobPageExtractor.detectRole(effectiveJd) }
                } else {
                    state.role.trim()
                }

                var detectedSalary = JobPageExtractor.detectSalaryExpanded(effectiveJd)
                if (detectedSalary.isBlank()) detectedSalary = structuredMerged.salaryRaw.ifBlank {
                    structuredLlm?.salaryRaw?.trim().orEmpty()
                }

                val jdForGeneration = JobPageExtractor.stripInjectedStructuredMetadata(effectiveJd)

                val salaryBlob = listOf(detectedSalary, structuredMerged.salaryRaw, structuredLlm?.salaryRaw.orEmpty())
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" | ")
                val lpaApprox = SalaryNormalizer.approximateAnnualLpa(
                    salaryText = salaryBlob,
                    payPeriodHint = structuredMerged.payPeriodHint.ifBlank {
                        structuredLlm?.payPeriodHint ?: "unknown"
                    },
                    extraContext = effectiveJd.take(4000)
                )

                val resumeSummary = state.profile.resumeTextSnapshot.ifBlank { state.profile.strengths }.take(6000)

                val jobInput = JobInput(
                    company = detectedCompany.ifBlank { "Unknown Company" },
                    role = detectedRole.ifBlank { "Unknown Role" },
                    url = state.url.trim(),
                    jdText = jdForGeneration,
                    salaryHint = detectedSalary,
                    salaryAnnualLpaApprox = lpaApprox,
                    jobSpecificNotes = state.jobExtraContext.trim(),
                    resumeSummaryForPrompt = resumeSummary
                )
                AppLogger.log(
                    appContext,
                    "generate",
                    "jobInput ready company=${jobInput.company.take(40)} role=${jobInput.role.take(40)} jdLen=${jobInput.jdText.length} resumeCtxLen=${jobInput.resumeSummaryForPrompt.length}"
                )

                val insight = scoringEngine.score(jobInput, state.profile)

                AppLogger.log(appContext, "generate", "repository.generatePack start")
                val pack: ApplicationPack = withContext(Dispatchers.IO) {
                    repository.generatePack(jobInput, state.profile)
                }
                AppLogger.log(appContext, "generate", "repository.generatePack done folder=${pack.folderName}")

                val beforeCoverJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val beforeCoverNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(appContext, "llm", "coverLetter start javaUsedMB=$beforeCoverJavaMb nativeMB=$beforeCoverNativeMb")
                val coverLetter = withContext(Dispatchers.IO) {
                    llmEngine.generateCoverLetter(jobInput, state.profile)
                }
                AppLogger.log(appContext, "llm", "coverLetter done len=${coverLetter.length}")

                val beforeResJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val beforeResNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(appContext, "llm", "resumeHighlights start javaUsedMB=$beforeResJavaMb nativeMB=$beforeResNativeMb")
                val resumeHighlights = withContext(Dispatchers.IO) {
                    llmEngine.generateResumeHighlights(jobInput, state.profile)
                }
                AppLogger.log(appContext, "llm", "resumeHighlights done len=${resumeHighlights.length}")

                val beforeDecJavaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val beforeDecNativeMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
                AppLogger.log(appContext, "llm", "applyDecision start javaUsedMB=$beforeDecJavaMb nativeMB=$beforeDecNativeMb")
                val applyDecision = withContext(Dispatchers.IO) {
                    llmEngine.suggestApplyDecision(jobInput, state.profile)
                }
                AppLogger.log(appContext, "llm", "applyDecision done value=${applyDecision.take(30)}")

                AppLogger.log(appContext, "generate", "writeGeneratedOutputs start")
                withContext(Dispatchers.IO) {
                    writeGeneratedOutputs(pack.folderName, jobInput, coverLetter, resumeHighlights)
                }
                AppLogger.log(appContext, "generate", "writeGeneratedOutputs done")

                push(
                    _uiState.value.copy(
                        isGenerating = false,
                        isAutoGenerating = false,
                        pendingAutoGenerateAfterExtraction = false,
                        pendingGenerateAfterManualCapture = false,
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
            } catch (t: Throwable) {
                CrashLogWriter.writeCaughtThrowable(appContext, "generatePack", t)
                AppLogger.log(appContext, "error", "generatePack caught ${t.javaClass.simpleName}: ${t.message.orEmpty().take(200)}")
                push(
                    _uiState.value.copy(
                        isGenerating = false,
                        isAutoGenerating = false,
                        pendingAutoGenerateAfterExtraction = false,
                        pendingGenerateAfterManualCapture = false,
                        statusMessage = "Generation failed: ${t.message ?: t.javaClass.simpleName}. Use AI Setup → Share diagnostics if you need a log file."
                    )
                )
            } finally {
                generatePackInFlight.set(false)
            }
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

