package com.careerops.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careerops.mobile.data.ApplicationPack
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.FormSuggestion
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.data.ProfileStore
import com.careerops.mobile.llm.LocalLlmEngine
import com.careerops.mobile.llm.StubLocalLlmEngine
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
    val statusMessage: String = "",
    val latestPackFolder: String = "",
    val generatedCoverLetter: String = "",
    val generatedResumeHighlights: String = "",
    val formSuggestions: List<FormSuggestion> = emptyList()
)

class MainViewModel(
    private val repository: PackRepository,
    private val profileStore: ProfileStore,
    private val llmEngine: LocalLlmEngine = StubLocalLlmEngine(),
    private val formSuggestionEngine: FormSuggestionEngine = FormSuggestionEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileStore.profileFlow.collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile)
            }
        }
    }

    fun ingestSharedText(sharedText: String?) {
        val text = sharedText?.trim().orEmpty()
        if (text.isBlank()) return
        val maybeUrl = if (text.startsWith("http")) text else ""
        _uiState.value = _uiState.value.copy(
            url = maybeUrl.ifBlank { _uiState.value.url },
            jdText = if (maybeUrl.isBlank()) text else _uiState.value.jdText,
            statusMessage = "Shared content imported."
        )
    }

    fun updateCompany(value: String) { _uiState.value = _uiState.value.copy(company = value) }
    fun updateRole(value: String) { _uiState.value = _uiState.value.copy(role = value) }
    fun updateUrl(value: String) { _uiState.value = _uiState.value.copy(url = value) }
    fun updateJdText(value: String) { _uiState.value = _uiState.value.copy(jdText = value) }

    fun updateExtractedPageText(value: String) {
        _uiState.value = _uiState.value.copy(
            extractedPageText = JobPageExtractor.cleanExtractedText(value)
        )
    }

    fun refreshSuggestionsFromVisibleText(visibleText: String) {
        val suggestions = formSuggestionEngine.suggestFromVisibleText(
            visibleText = visibleText,
            profile = _uiState.value.profile
        )
        _uiState.value = _uiState.value.copy(formSuggestions = suggestions)
    }

    fun saveProfile(updated: CandidateProfile) {
        viewModelScope.launch {
            profileStore.saveProfile(updated)
            _uiState.value = _uiState.value.copy(statusMessage = "Profile saved locally.")
        }
    }

    fun generatePack() {
        val state = _uiState.value
        if (state.company.isBlank() || state.role.isBlank() || state.url.isBlank()) {
            _uiState.value = state.copy(statusMessage = "Company, role, and URL are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isGenerating = true, statusMessage = "Generating pack...")

            val effectiveJd = when {
                state.extractedPageText.isNotBlank() -> state.extractedPageText
                state.jdText.isNotBlank() -> state.jdText
                else -> ""
            }

            val jobInput = JobInput(
                company = state.company.trim(),
                role = state.role.trim(),
                url = state.url.trim(),
                jdText = effectiveJd
            )

            val pack: ApplicationPack = withContext(Dispatchers.IO) {
                repository.generatePack(jobInput, state.profile)
            }
            val coverLetter = llmEngine.generateCoverLetter(jobInput, state.profile)
            val resumeHighlights = llmEngine.generateResumeHighlights(jobInput, state.profile)

            withContext(Dispatchers.IO) {
                writeGeneratedOutputs(pack.folderName, jobInput, coverLetter, resumeHighlights)
            }

            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                latestPackFolder = pack.folderName,
                generatedCoverLetter = coverLetter,
                generatedResumeHighlights = resumeHighlights,
                statusMessage = "Pack generated at ${pack.folderPath}"
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
}

