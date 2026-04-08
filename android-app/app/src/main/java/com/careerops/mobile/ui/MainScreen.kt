package com.careerops.mobile.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.service.BubbleOverlayService

private enum class MainTab { ONBOARD, JOB, WEBVIEW, RESULTS }

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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == MainTab.ONBOARD,
                    onClick = { selectedTab = MainTab.ONBOARD },
                    text = { Text("Onboarding") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
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
            }

            when (selectedTab) {
                MainTab.ONBOARD -> OnboardingTab(
                    profile = state.profile,
                    onSave = viewModel::saveProfile
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
                            onPageTextCaptured = { viewModel.updateExtractedPageText(it) },
                            onVisibleTextCaptured = { viewModel.refreshSuggestionsFromVisibleText(it) }
                        )
                    }
                }
                MainTab.RESULTS -> ResultsTab(state = state)
            }
        }
    }
}

@Composable
private fun OnboardingTab(
    profile: CandidateProfile,
    onSave: (CandidateProfile) -> Unit
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.llmProviderMode.equals("local", ignoreCase = true),
                onClick = { draft = draft.copy(llmProviderMode = "local") },
                label = { Text("Local mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
                selected = draft.llmProviderMode.equals("api", ignoreCase = true),
                onClick = { draft = draft.copy(llmProviderMode = "api") },
                label = { Text("API key mode") },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(draft.fullName, { draft = draft.copy(fullName = it) }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.email, { draft = draft.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.phone, { draft = draft.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.location, { draft = draft.copy(location = it) }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.currentTitle, { draft = draft.copy(currentTitle = it) }, label = { Text("Current title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.targetRole, { draft = draft.copy(targetRole = it) }, label = { Text("Target role") }, modifier = Modifier.fillMaxWidth())
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
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(draft.achievements, { draft = draft.copy(achievements = it) }, label = { Text("Key achievements (comma-separated)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(modifier = Modifier.height(12.dp))
        Text("LLM Provider Settings", style = MaterialTheme.typography.titleSmall)
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
            label = { Text("API Key (encrypted locally via Android Keystore)") },
            modifier = Modifier.fillMaxWidth()
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
                Text("One-tap URL flow")
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
                "Auto mode running: in-app page capture, scoring, and generation are in progress.",
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
