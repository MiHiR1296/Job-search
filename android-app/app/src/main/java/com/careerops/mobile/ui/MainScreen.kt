package com.careerops.mobile.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.service.BubbleOverlayService

private enum class MainTab { ONBOARD, AI_SETUP, JOB, WEBVIEW, RESULTS, MEMORY }

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.ONBOARD) }
    LaunchedEffect(state.isAutoGenerating, state.url) {
        if (state.isAutoGenerating && state.url.isNotBlank()) {
            selectedTab = MainTab.WEBVIEW
        }
    }
    LaunchedEffect(state.autoFlowRequestedAtMs, state.isGenerating, state.isAutoGenerating, state.latestPackFolder) {
        if (
            state.autoFlowRequestedAtMs > 0L &&
            !state.isGenerating &&
            !state.isAutoGenerating &&
            state.latestPackFolder.isNotBlank()
        ) {
            selectedTab = MainTab.RESULTS
        }
    }
    LaunchedEffect(state.shouldAutoStartFromShare, state.url) {
        if (state.shouldAutoStartFromShare && state.url.isNotBlank()) {
            viewModel.startAutoFlowFromShare()
            viewModel.onSharedJobAutoStartHandled()
            selectedTab = MainTab.WEBVIEW
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == MainTab.ONBOARD,
                    onClick = { selectedTab = MainTab.ONBOARD },
                    text = { Text("Onboarding") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == MainTab.AI_SETUP,
                    onClick = { selectedTab = MainTab.AI_SETUP },
                    text = { Text("AI Setup") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == MainTab.JOB,
                    onClick = { selectedTab = MainTab.JOB },
                    text = { Text("Job Input") }
                )
                Tab(
                    selected = selectedTab == MainTab.WEBVIEW,
                    onClick = { selectedTab = MainTab.WEBVIEW },
                    text = { Text("In-App Page") },
                    icon = { Icon(Icons.Default.Web, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == MainTab.RESULTS,
                    onClick = { selectedTab = MainTab.RESULTS },
                    text = { Text("Results") }
                )
                Tab(
                    selected = selectedTab == MainTab.MEMORY,
                    onClick = { selectedTab = MainTab.MEMORY },
                    text = { Text("Memory") }
                )
            }

            when (selectedTab) {
                MainTab.ONBOARD -> OnboardingTab(
                    profile = state.profile,
                    onSave = viewModel::saveProfile,
                    onStartVoice = viewModel::beginVoiceCapture
                )
                MainTab.AI_SETUP -> AiSetupTab(
                    state = state,
                    onSave = viewModel::saveProfile,
                    onTestApi = viewModel::testApiConnection,
                    onManualCaptureMode = viewModel::setManualCaptureMode
                )
                MainTab.JOB -> JobInputTab(
                    state = state,
                    onCompany = viewModel::updateCompany,
                    onRole = viewModel::updateRole,
                    onUrl = viewModel::updateUrl,
                    onJd = viewModel::updateJdText,
                    onGenerate = viewModel::generatePack,
                    onAutoGenerate = viewModel::startAutoGenerateFromUrlOnly,
                    onStartBubble = {
                        val serviceIntent = Intent(context, BubbleOverlayService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                )
                MainTab.WEBVIEW -> {
                    if (state.url.isBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Enter/share a job URL in Job Input first.")
                        }
                    } else {
                        JobWebViewScreen(
                            url = state.url,
                            autoCaptureOnLoad = !state.manualCaptureMode,
                            onPageTextCaptured = { viewModel.updateExtractedPageText(it) },
                            onVisibleTextCaptured = { viewModel.refreshSuggestionsFromVisibleText(it) },
                            onCaptureAndGenerate = { viewModel.requestGenerateAfterManualCapture() }
                        )
                    }
                }
                MainTab.RESULTS -> ResultsTab(state = state)
                MainTab.MEMORY -> MemoryTab(
                    state = state,
                    onStartVoice = viewModel::beginVoiceCapture,
                    onClearDraft = viewModel::clearNarrativeDraft,
                    onEditDraft = viewModel::updateNarrativeDraft,
                    onSummarize = viewModel::summarizeNarrativeIntoMemory
                )
            }
        }
    }
}

@Composable
private fun AiSetupTab(
    state: MainUiState,
    onSave: (CandidateProfile) -> Unit,
    onTestApi: () -> Unit,
    onManualCaptureMode: (Boolean) -> Unit
) {
    var draft by remember(state.profile) { mutableStateOf(state.profile) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("AI Setup", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Configure local/API model access once. This keeps app flow cleaner and avoids setup friction in Job Input.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.llmProviderMode.equals("local", ignoreCase = true),
                onClick = { draft = draft.copy(llmProviderMode = "local") },
                label = { Text("Local model mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
                selected = draft.llmProviderMode.equals("api", ignoreCase = true),
                onClick = { draft = draft.copy(llmProviderMode = "api") },
                label = { Text("API mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            draft.localModelPath,
            { draft = draft.copy(localModelPath = it) },
            label = { Text("Local GGUF model path") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            draft.apiBaseUrl,
            { draft = draft.copy(apiBaseUrl = it) },
            label = { Text("API Base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            draft.apiModel,
            { draft = draft.copy(apiModel = it) },
            label = { Text("API Model") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            draft.apiKey,
            { draft = draft.copy(apiKey = it) },
            label = { Text("API Key (encrypted locally)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save AI setup")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onTestApi,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isTestingApiConnection
        ) {
            Text("Test API connection")
        }
        if (state.isTestingApiConnection) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        if (state.apiTestStatus.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.apiTestStatus, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Manual capture mode", style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = state.manualCaptureMode,
                onCheckedChange = onManualCaptureMode
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "ON = app waits for your Capture + Generate button (safer when login pages/ads appear first).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun OnboardingTab(
    profile: CandidateProfile,
    onSave: (CandidateProfile) -> Unit,
    onStartVoice: (String) -> Unit
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Candidate Onboarding", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Fill once, update anytime. Stored locally on device.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(draft.fullName, { draft = draft.copy(fullName = it) }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onStartVoice("fullName") }) { Text("Dictate full name") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.email, { draft = draft.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.phone, { draft = draft.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.location, { draft = draft.copy(location = it) }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.currentTitle, { draft = draft.copy(currentTitle = it) }, label = { Text("Current title") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onStartVoice("currentTitle") }) { Text("Dictate current title") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.targetRole, { draft = draft.copy(targetRole = it) }, label = { Text("Target role") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onStartVoice("targetRole") }) { Text("Dictate target role") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            draft.resumeUri,
            { draft = draft.copy(resumeUri = it) },
            label = { Text("Resume file path/URI (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.yearsExperience, { draft = draft.copy(yearsExperience = it) }, label = { Text("Years experience") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.currentCtcLpa, { draft = draft.copy(currentCtcLpa = it) }, label = { Text("Current CTC (LPA)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.expectedCtcLpa, { draft = draft.copy(expectedCtcLpa = it) }, label = { Text("Expected CTC (LPA)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.minimumAcceptableLpa, { draft = draft.copy(minimumAcceptableLpa = it) }, label = { Text("Minimum acceptable CTC (LPA)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.noticePeriodDays, { draft = draft.copy(noticePeriodDays = it) }, label = { Text("Notice period (days)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.strengths, { draft = draft.copy(strengths = it) }, label = { Text("Top strengths (comma-separated)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        TextButton(onClick = { onStartVoice("strengths") }) { Text("Dictate strengths") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.achievements, { draft = draft.copy(achievements = it) }, label = { Text("Key achievements (comma-separated)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        TextButton(onClick = { onStartVoice("achievements") }) { Text("Dictate achievements") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            draft.careerMemory,
            { draft = draft.copy(careerMemory = it) },
            label = { Text("Long-term career memory") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save profile locally")
        }
    }
}

@Composable
private fun JobInputTab(
    state: MainUiState,
    onCompany: (String) -> Unit,
    onRole: (String) -> Unit,
    onUrl: (String) -> Unit,
    onJd: (String) -> Unit,
    onGenerate: () -> Unit,
    onAutoGenerate: () -> Unit,
    onStartBubble: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Job Input", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Paste/share a URL and use One-tap URL flow. Company, role, and JD are auto-extracted from the in-app page.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            state.company,
            onCompany,
            label = { Text("Company (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            state.role,
            onRole,
            label = { Text("Role (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(state.url, onUrl, label = { Text("Job URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            state.jdText,
            onJd,
            label = { Text("JD text override (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.detectedCompany.isNotBlank() || state.detectedRole.isNotBlank() || state.detectedSalaryHint.isNotBlank()) {
            Text("Detected from page:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            if (state.detectedCompany.isNotBlank()) {
                Text("Company: ${state.detectedCompany}", style = MaterialTheme.typography.bodySmall)
            }
            if (state.detectedRole.isNotBlank()) {
                Text("Role: ${state.detectedRole}", style = MaterialTheme.typography.bodySmall)
            }
            if (state.detectedSalaryHint.isNotBlank()) {
                Text("Salary hint: ${state.detectedSalaryHint}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onJd("") },
                label = { Text("Clear JD override") },
                colors = AssistChipDefaults.assistChipColors()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGenerate, modifier = Modifier.weight(1f), enabled = !state.isGenerating) {
                Text("Generate full output")
            }
            Button(
                onClick = onAutoGenerate,
                modifier = Modifier.weight(1f),
                enabled = !state.isGenerating && !state.isAutoGenerating
            ) {
                Text(if (state.manualCaptureMode) "Open for manual capture" else "One-tap URL flow")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onStartBubble, modifier = Modifier.weight(1f)) {
                Text("Start bubble")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.isGenerating || state.isAutoGenerating) CircularProgressIndicator()
        if (state.statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)
        }
        if (state.isAutoGenerating) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (state.manualCaptureMode) {
                    "Manual mode: open In-App Page tab and press Capture + Generate."
                } else {
                    "Auto mode running: in-app page capture, scoring, and generation are in progress."
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ResultsTab(state: MainUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Generation Output", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Fit Score: ${String.format("%.2f", state.fitScore)} / 5", style = MaterialTheme.typography.titleMedium)
        Text(
            "Recommendation: ${state.recommendation}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        if (state.recommendationReasons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Why:", style = MaterialTheme.typography.titleSmall)
            state.recommendationReasons.forEach { reason ->
                Text("- $reason", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (state.generatedCoverLetter.isNotBlank()) {
            Text("Cover Letter", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(state.generatedCoverLetter, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.generatedResumeHighlights.isNotBlank()) {
            Text("Resume Highlights", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(state.generatedResumeHighlights, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.formSuggestions.isNotEmpty()) {
            Text("Live Autofill Suggestions (from page text)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            state.formSuggestions.forEach { suggestion ->
                Text("- ${suggestion.label}: ${suggestion.suggestedValue}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MemoryTab(
    state: MainUiState,
    onStartVoice: (String) -> Unit,
    onClearDraft: () -> Unit,
    onEditDraft: (String) -> Unit,
    onSummarize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Voice Career Memory", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Dictate your story. The app summarizes important long-term points for future applications.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Prompt:", style = MaterialTheme.typography.titleSmall)
        Text(state.knowledgePrompt, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onStartVoice("careerNarrative") }, modifier = Modifier.fillMaxWidth()) {
            Text("Start voice dictation")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClearDraft, modifier = Modifier.fillMaxWidth()) {
            Text("Clear dictated draft")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.voiceDraftNarrative,
            onValueChange = onEditDraft,
            label = { Text("Dictated draft") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            readOnly = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSummarize,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSummarizingMemory
        ) {
            Text("Summarize into long-term memory")
        }
        if (state.isSummarizingMemory) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Current long-term memory", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (state.profile.careerMemory.isBlank()) "No memory saved yet." else state.profile.careerMemory,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
