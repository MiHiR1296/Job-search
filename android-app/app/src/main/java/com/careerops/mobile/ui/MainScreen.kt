package com.careerops.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.LinearProgressIndicator
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
fun MainScreen(
    viewModel: MainViewModel,
    onPickResumeDocument: () -> Unit,
    onPickLocalModelFile: () -> Unit,
    onStartVoiceCapture: (String) -> Unit,
    onOpenJobInCustomTab: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.ONBOARD) }
    var didApplyInitialTab by remember { mutableStateOf(false) }
    LaunchedEffect(state.profile) {
        if (!didApplyInitialTab) {
            didApplyInitialTab = true
            selectedTab = if (state.profile.onboardingCompleted) MainTab.JOB else MainTab.ONBOARD
        }
    }
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
                    onPickResumeDocument = onPickResumeDocument,
                    onFinished = { selectedTab = MainTab.JOB }
                )
                MainTab.AI_SETUP -> AiSetupTab(
                    state = state,
                    onModeChange = viewModel::updateAiSetupModeDraft,
                    onLocalModelPathChange = viewModel::updateAiSetupLocalModelPathDraft,
                    onPickLocalModelFile = onPickLocalModelFile,
                    onApiBaseUrlChange = viewModel::updateAiSetupApiBaseUrlDraft,
                    onApiModelChange = viewModel::updateAiSetupApiModelDraft,
                    onApiKeyChange = viewModel::updateAiSetupApiKeyDraft,
                    onSave = viewModel::saveAiSetupDraftsToProfile,
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
                    onEditProfile = { selectedTab = MainTab.ONBOARD },
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
                        if (state.webViewLoadError.isNotBlank()) {
                            StatusMessageBox(state.webViewLoadError)
                        }
                        JobWebViewScreen(
                            url = state.url,
                            autoCaptureOnLoad = !state.manualCaptureMode,
                            onPageTextCaptured = { viewModel.updateExtractedPageText(it) },
                            onVisibleTextCaptured = { viewModel.refreshSuggestionsFromVisibleText(it) },
                            onJsonLdCaptured = { viewModel.updateJsonLdFromPage(it) },
                            onLoadError = { viewModel.reportWebViewLoadError(it) },
                            onClearLoadError = { viewModel.clearWebViewLoadError() },
                            onOpenCustomTab = { onOpenJobInCustomTab(state.url) },
                            onCaptureAndGenerate = { viewModel.requestGenerateAfterManualCapture() }
                        )
                    }
                }
                MainTab.RESULTS -> ResultsTab(
                    state = state,
                    onJobExtraChange = viewModel::updateJobExtraContext,
                    onRegenerate = viewModel::generatePack
                )
                MainTab.MEMORY -> MemoryTab(
                    state = state,
                    onStartVoice = onStartVoiceCapture,
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
    onModeChange: (String) -> Unit,
    onLocalModelPathChange: (String) -> Unit,
    onPickLocalModelFile: () -> Unit,
    onApiBaseUrlChange: (String) -> Unit,
    onApiModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestApi: () -> Unit,
    onManualCaptureMode: (Boolean) -> Unit
) {
    val isApiMode = state.aiSetupModeDraft.equals("api", ignoreCase = true)
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
                selected = !isApiMode,
                onClick = { onModeChange("local") },
                label = { Text("Local model mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
                selected = isApiMode,
                onClick = { onModeChange("api") },
                label = { Text("API mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (!isApiMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.aiSetupLocalModelPathDraft,
                    onValueChange = onLocalModelPathChange,
                    label = { Text("Local GGUF path") },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isImportingLocalModel,
                    singleLine = true
                )
                Button(
                    onClick = onPickLocalModelFile,
                    enabled = !state.isImportingLocalModel
                ) {
                    Text("Pick file")
                }
            }
            if (state.isImportingLocalModel) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Use Pick file to choose a .gguf (Downloads, Files, Google Drive, etc.). The app copies it to private storage and sets the path—you can still edit the path manually. Presets below are common /sdcard paths.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick local model presets", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onLocalModelPathChange("/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf") },
                    label = { Text("1.5B Q4 (fast)") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = { onLocalModelPathChange("/sdcard/Download/qwen2.5-7b-instruct-q4_k_m.gguf") },
                    label = { Text("7B Q4 (capable)") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = { onLocalModelPathChange("/sdcard/Download/qwen2.5-8b-instruct-q4_k_m.gguf") },
                    label = { Text("8B Q4") },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        } else {
            OutlinedTextField(
                value = state.aiSetupApiBaseUrlDraft,
                onValueChange = onApiBaseUrlChange,
                label = { Text("API Base URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.aiSetupApiModelDraft,
                onValueChange = onApiModelChange,
                label = { Text("API Model (editable)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick model presets", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onApiModelChange("gpt-4o-mini") },
                    label = { Text("gpt-4o-mini") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = { onApiModelChange("gpt-4.1-mini") },
                    label = { Text("gpt-4.1-mini") },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.aiSetupApiKeyDraft,
                onValueChange = onApiKeyChange,
                label = { Text("API Key (encrypted locally)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Key is stored locally with encryption.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onTestApi,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isTestingApiConnection
            ) {
                Text("Test API connection")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save AI setup")
        }
        if (state.isTestingApiConnection) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        if (state.apiTestStatus.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusMessageBox(state.apiTestStatus)
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
        Spacer(modifier = Modifier.height(12.dp))
        StatusMessageBox(state.statusMessage)
    }
}

private enum class OnboardStep { PickResume, MissingFields }

@Composable
private fun OnboardingTab(
    profile: CandidateProfile,
    onSave: (CandidateProfile) -> Unit,
    onPickResumeDocument: () -> Unit,
    onFinished: () -> Unit
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    LaunchedEffect(profile) {
        draft = profile
    }
    var step by remember(profile.onboardingCompleted) {
        mutableStateOf(if (profile.onboardingCompleted) OnboardStep.MissingFields else OnboardStep.PickResume)
    }
    var showMore by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (profile.onboardingCompleted) "Profile" else "Welcome",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (profile.onboardingCompleted) {
                "Update your details anytime. Job flow opens on the Job tab after first setup."
            } else {
                "Upload your resume once. We auto-detect what we can, then only ask for typical gaps (CTC targets, notice, etc.)."
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            OnboardStep.PickResume -> {
                Text("Step 1 — Resume", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPickResumeDocument, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose from Files (PDF/DOC/TXT)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (profile.resumeUri.isBlank()) "No document selected yet."
                    else "Selected: ${extractDisplayFileName(profile.resumeUri)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (profile.resumeUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Auto-detected (edit next step):", style = MaterialTheme.typography.titleSmall)
                    Text("Name: ${draft.fullName}", style = MaterialTheme.typography.bodySmall)
                    Text("Email: ${draft.email}", style = MaterialTheme.typography.bodySmall)
                    Text("Phone: ${draft.phone}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { step = OnboardStep.MissingFields },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = profile.resumeUri.isNotBlank()
                    ) {
                        Text("Continue")
                    }
                }
            }
            OnboardStep.MissingFields -> {
                Text(
                    if (profile.onboardingCompleted) "Details" else "Step 2 — Salary & logistics",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!profile.onboardingCompleted) {
                    TextButton(onClick = { step = OnboardStep.PickResume }) {
                        Text("Back to resume")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                OutlinedTextField(
                    draft.currentCtcLpa,
                    { draft = draft.copy(currentCtcLpa = it) },
                    label = { Text("Current CTC (LPA, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.expectedCtcLpa,
                    { draft = draft.copy(expectedCtcLpa = it) },
                    label = { Text("Expected CTC (LPA)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.minimumAcceptableLpa,
                    { draft = draft.copy(minimumAcceptableLpa = it) },
                    label = { Text("Minimum acceptable CTC (LPA)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.noticePeriodDays,
                    { draft = draft.copy(noticePeriodDays = it) },
                    label = { Text("Notice period (days)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.requiresSponsorship,
                    { draft = draft.copy(requiresSponsorship = it) },
                    label = { Text("Requires sponsorship (Yes/No)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.willingToRelocate,
                    { draft = draft.copy(willingToRelocate = it) },
                    label = { Text("Willing to relocate (Yes/No)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    draft.location,
                    { draft = draft.copy(location = it) },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showMore = !showMore }) {
                    Text(if (showMore) "Hide extra fields" else "Show extra fields (title, links, memory)")
                }
                if (showMore) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.fullName,
                        { draft = draft.copy(fullName = it) },
                        label = { Text("Full name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.email,
                        { draft = draft.copy(email = it) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.phone,
                        { draft = draft.copy(phone = it) },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.currentTitle,
                        { draft = draft.copy(currentTitle = it) },
                        label = { Text("Current title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.targetRole,
                        { draft = draft.copy(targetRole = it) },
                        label = { Text("Target role") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.linkedin,
                        { draft = draft.copy(linkedin = it) },
                        label = { Text("LinkedIn") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.github,
                        { draft = draft.copy(github = it) },
                        label = { Text("GitHub") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.portfolio,
                        { draft = draft.copy(portfolio = it) },
                        label = { Text("Portfolio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.yearsExperience,
                        { draft = draft.copy(yearsExperience = it) },
                        label = { Text("Years experience") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.strengths,
                        { draft = draft.copy(strengths = it) },
                        label = { Text("Strengths (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.achievements,
                        { draft = draft.copy(achievements = it) },
                        label = { Text("Achievements (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        draft.careerMemory,
                        { draft = draft.copy(careerMemory = it) },
                        label = { Text("Long-term career memory") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (profile.onboardingCompleted) {
                    TextButton(onClick = { step = OnboardStep.PickResume }) {
                        Text("Replace resume document")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val merged = draft.copy(
                            resumeUri = profile.resumeUri.ifBlank { draft.resumeUri },
                            resumeTextSnapshot = profile.resumeTextSnapshot.ifBlank { draft.resumeTextSnapshot },
                            onboardingCompleted = true
                        )
                        onSave(merged)
                        onFinished()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = profile.onboardingCompleted || profile.resumeUri.isNotBlank()
                ) {
                    Text(if (profile.onboardingCompleted) "Save profile" else "Save and finish onboarding")
                }
            }
        }
    }
}

private fun extractDisplayFileName(uriText: String): String {
    if (uriText.isBlank()) return "No document selected"
    val parsed = runCatching { Uri.parse(uriText) }.getOrNull() ?: return "Document selected"
    val raw = parsed.lastPathSegment?.substringAfterLast('/')?.trim().orEmpty()
    return if (raw.isBlank()) "Document selected" else raw
}

@Composable
private fun StatusMessageBox(message: String) {
    if (message.isBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp)
        )
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
    onEditProfile: () -> Unit,
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
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onEditProfile) {
            Text("Edit profile / onboarding")
        }
        if (state.webViewLoadError.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusMessageBox(state.webViewLoadError)
        }
        Spacer(modifier = Modifier.height(8.dp))
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
private fun ResultsTab(
    state: MainUiState,
    onJobExtraChange: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Generation Output", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Anything extra for this job only? (highlights, tech, team fit — used in cover letter and scoring.)",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = state.jobExtraContext,
            onValueChange = onJobExtraChange,
            label = { Text("Notes for this job") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isGenerating && state.url.isNotBlank()
        ) {
            Text("Regenerate with these notes")
        }
        Spacer(modifier = Modifier.height(12.dp))
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
        if (state.statusMessage.contains("voice", ignoreCase = true) ||
            state.statusMessage.contains("microphone", ignoreCase = true) ||
            state.statusMessage.contains("speech recognition", ignoreCase = true)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)
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
