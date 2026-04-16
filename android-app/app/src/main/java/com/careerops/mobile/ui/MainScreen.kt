package com.careerops.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.ApplicationInfo
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.diagnostics.AppLogger
import com.careerops.mobile.service.BubbleOverlayService
import kotlinx.coroutines.launch

private enum class MainRoute {
    HomeJob,
    HomeResults,
    JobWebView,
    AiSetup,
    Profile,
    DevTools
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickResumeDocument: () -> Unit,
    onPickLocalModelFile: () -> Unit,
    onOpenJobInCustomTab: (String) -> Unit,
    onShareDiagnostics: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.profile.onboardingCompleted) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            OnboardingTab(
                profile = state.profile,
                onSave = viewModel::saveProfile,
                onPickResumeDocument = onPickResumeDocument,
                onFinished = { }
            )
        }
        return
    }

    CareerOpsMainShell(
        state = state,
        viewModel = viewModel,
        onPickResumeDocument = onPickResumeDocument,
        onPickLocalModelFile = onPickLocalModelFile,
        onOpenJobInCustomTab = onOpenJobInCustomTab,
        onShareDiagnostics = onShareDiagnostics
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareerOpsMainShell(
    state: MainUiState,
    viewModel: MainViewModel,
    onPickResumeDocument: () -> Unit,
    onPickLocalModelFile: () -> Unit,
    onOpenJobInCustomTab: (String) -> Unit,
    onShareDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var routeKey by rememberSaveable { mutableStateOf(MainRoute.HomeJob.name) }
    val route = runCatching { MainRoute.valueOf(routeKey) }.getOrDefault(MainRoute.HomeJob)
    val isDebuggable = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    fun navigateTo(r: MainRoute) {
        routeKey = r.name
        AppLogger.log(context, "ui", "navigateTo=${r.name}")
    }

    LaunchedEffect(state.profile.onboardingCompleted) {
        if (state.profile.onboardingCompleted) {
            navigateTo(MainRoute.HomeJob)
        }
    }
    LaunchedEffect(state.isAutoGenerating, state.url) {
        if (state.isAutoGenerating && state.url.isNotBlank()) {
            navigateTo(MainRoute.JobWebView)
        }
    }
    LaunchedEffect(state.autoFlowRequestedAtMs, state.isGenerating, state.isAutoGenerating, state.latestPackFolder) {
        if (
            state.autoFlowRequestedAtMs > 0L &&
            !state.isGenerating &&
            !state.isAutoGenerating &&
            state.latestPackFolder.isNotBlank()
        ) {
            navigateTo(MainRoute.HomeResults)
        }
    }
    LaunchedEffect(state.shouldAutoStartFromShare, state.url) {
        if (state.shouldAutoStartFromShare && state.url.isNotBlank()) {
            viewModel.startAutoFlowFromShare()
            viewModel.onSharedJobAutoStartHandled()
            navigateTo(MainRoute.JobWebView)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight(0.92f)) {
                Text(
                    "Career Ops",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Job input") },
                    selected = route == MainRoute.HomeJob,
                    onClick = {
                        AppLogger.log(context, "ui", "drawerClick=HomeJob")
                        navigateTo(MainRoute.HomeJob)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Results") },
                    selected = route == MainRoute.HomeResults,
                    onClick = {
                        AppLogger.log(context, "ui", "drawerClick=HomeResults")
                        navigateTo(MainRoute.HomeResults)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Job page (sign-in & capture)") },
                    selected = route == MainRoute.JobWebView,
                    onClick = {
                        AppLogger.log(context, "ui", "drawerClick=JobWebView")
                        navigateTo(MainRoute.JobWebView)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Web, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("AI setup") },
                    selected = route == MainRoute.AiSetup,
                    onClick = {
                        AppLogger.log(context, "ui", "drawerClick=AiSetup")
                        navigateTo(MainRoute.AiSetup)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = route == MainRoute.Profile,
                    onClick = {
                        AppLogger.log(context, "ui", "drawerClick=Profile")
                        navigateTo(MainRoute.Profile)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                if (isDebuggable) {
                    NavigationDrawerItem(
                        label = { Text("Developer tools") },
                        selected = route == MainRoute.DevTools,
                        onClick = {
                            AppLogger.log(context, "ui", "drawerClick=DevTools")
                            navigateTo(MainRoute.DevTools)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (route) {
                                MainRoute.HomeJob -> "Job input"
                                MainRoute.HomeResults -> "Results"
                                MainRoute.JobWebView -> "Job page"
                                MainRoute.AiSetup -> "AI setup"
                                MainRoute.Profile -> "Profile"
                                MainRoute.DevTools -> "Developer tools"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            },
            bottomBar = {
                if (route == MainRoute.HomeJob || route == MainRoute.HomeResults) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            label = { Text("Job") },
                            selected = route == MainRoute.HomeJob,
                            onClick = { navigateTo(MainRoute.HomeJob) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Article, contentDescription = null) },
                            label = { Text("Results") },
                            selected = route == MainRoute.HomeResults,
                            onClick = { navigateTo(MainRoute.HomeResults) }
                        )
                    }
                }
            }
        ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (route) {
                    MainRoute.HomeJob -> JobInputTab(
                        state = state,
                        onCompany = viewModel::updateCompany,
                        onRole = viewModel::updateRole,
                        onUrl = viewModel::updateUrl,
                        onJd = viewModel::updateJdText,
                        onGenerate = viewModel::generatePack,
                        onAutoGenerate = viewModel::startAutoGenerateFromUrlOnly,
                        onOpenJobPage = {
                            navigateTo(MainRoute.JobWebView)
                            scope.launch { drawerState.close() }
                        },
                        onOpenProfile = {
                            navigateTo(MainRoute.Profile)
                            scope.launch { drawerState.close() }
                        },
                        onStartBubble = {
                            val serviceIntent = Intent(context, BubbleOverlayService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    )
                    MainRoute.HomeResults -> ResultsTab(
                        state = state,
                        onJobExtraChange = viewModel::updateJobExtraContext,
                        onRegenerate = viewModel::generatePack
                    )
                    MainRoute.JobWebView -> {
                        if (state.url.isBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Add a job URL on Job input first (menu → Job input).")
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                if (state.webViewLoadError.isNotBlank()) {
                                    StatusMessageBox(state.webViewLoadError)
                                }
                                JobWebViewScreen(
                                    url = state.url,
                                    autoCaptureOnLoad = !state.manualCaptureMode,
                                    onPageTextCaptured = {
                                        viewModel.updateExtractedPageText(it)
                                        if (state.pendingBucketCapture) {
                                            viewModel.captureToSelectedBucket(it)
                                            viewModel.onBucketCaptureHandled()
                                        }
                                    },
                                    onVisibleTextCaptured = { viewModel.refreshSuggestionsFromVisibleText(it) },
                                    onJsonLdCaptured = { viewModel.updateJsonLdFromPage(it) },
                                    onLoadError = { viewModel.reportWebViewLoadError(it) },
                                    onClearLoadError = { viewModel.clearWebViewLoadError() },
                                    onOpenCustomTab = { onOpenJobInCustomTab(state.url) },
                                    onCaptureAndGenerate = { viewModel.requestGenerateAfterManualCapture() },
                                    captureBucketSelected = state.captureBucketSelected,
                                    captureAppendMode = state.captureAppendMode,
                                    onToggleAppendMode = viewModel::setCaptureAppendMode,
                                    onCaptureToBucket = viewModel::beginBucketCapture,
                                    onClearBucket = { viewModel.clearCaptureBucket(state.captureBucketSelected) },
                                    onFinalizeCapture = viewModel::finalizeCaptureBuckets,
                                    onSelectBucket = viewModel::selectCaptureBucket
                                )
                            }
                        }
                    }
                    MainRoute.AiSetup -> AiSetupTab(
                        state = state,
                        onModeChange = viewModel::updateAiSetupModeDraft,
                        onLocalModelPathChange = viewModel::updateAiSetupLocalModelPathDraft,
                        onPickLocalModelFile = onPickLocalModelFile,
                        onApiBaseUrlChange = viewModel::updateAiSetupApiBaseUrlDraft,
                        onApiModelChange = viewModel::updateAiSetupApiModelDraft,
                        onApiKeyChange = viewModel::updateAiSetupApiKeyDraft,
                        onSave = viewModel::saveAiSetupDraftsToProfile,
                        onTestApi = viewModel::testApiConnection,
                        onManualCaptureMode = viewModel::setManualCaptureMode,
                        onShareDiagnostics = onShareDiagnostics,
                        onToggleLiveLogging = viewModel::setLiveLoggingEnabled,
                        onExportLogs = viewModel::exportLatestLogsToDownloads,
                        onSmokeTestLocal = viewModel::smokeTestLocalModel
                    )
                    MainRoute.Profile -> OnboardingTab(
                        profile = state.profile,
                        onSave = viewModel::saveProfile,
                        onPickResumeDocument = onPickResumeDocument,
                        onFinished = {
                            navigateTo(MainRoute.HomeJob)
                            scope.launch { drawerState.close() }
                        }
                    )
                    MainRoute.DevTools -> DeveloperToolsTab(
                        state = state,
                        onToggleLiveLogging = viewModel::setLiveLoggingEnabled,
                        onExportLogs = viewModel::exportLatestLogsToDownloads,
                        onSmokeTestLocal = viewModel::smokeTestLocalModel,
                        onShareDiagnostics = onShareDiagnostics,
                        onDevChatPromptChange = viewModel::updateDevChatPrompt,
                        onRunDevChat = viewModel::runDevChat,
                        onGenerateDecision = { viewModel.generateSingle(MainViewModel.GenerateSingleType.ApplyDecision) },
                        onGenerateHighlights = { viewModel.generateSingle(MainViewModel.GenerateSingleType.ResumeHighlights) },
                        onGenerateCoverLetter = { viewModel.generateSingle(MainViewModel.GenerateSingleType.CoverLetter) },
                        onPreviewDecisionPrompt = { viewModel.previewPrompts(MainViewModel.GenerateSingleType.ApplyDecision) },
                        onPreviewHighlightsPrompt = { viewModel.previewPrompts(MainViewModel.GenerateSingleType.ResumeHighlights) },
                        onPreviewCoverPrompt = { viewModel.previewPrompts(MainViewModel.GenerateSingleType.CoverLetter) }
                    )
                }
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
    onManualCaptureMode: (Boolean) -> Unit,
    onShareDiagnostics: () -> Unit,
    onToggleLiveLogging: (Boolean) -> Unit,
    onExportLogs: () -> Unit,
    onSmokeTestLocal: () -> Unit
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
                selected = state.aiSetupModeDraft.equals("litert", ignoreCase = true),
                onClick = { onModeChange("litert") },
                label = { Text("Gemma (LiteRT)") },
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
                    label = {
                        Text(
                            if (state.aiSetupModeDraft.equals("litert", ignoreCase = true)) {
                                "Gemma .litertlm path"
                            } else {
                                "Local GGUF path"
                            }
                        )
                    },
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
                if (state.aiSetupModeDraft.equals("litert", ignoreCase = true)) {
                    "Use Pick file to choose a .litertlm model (Gemma). If you pick from Drive/Files, the app may copy it to private storage. You can also point to a direct path if accessible."
                } else {
                    "Use Pick file to choose a .gguf (Downloads, Files, Google Drive, etc.). The app copies it to private storage and sets the path—you can still edit the path manually. Presets below are common /sdcard paths."
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!state.aiSetupModeDraft.equals("litert", ignoreCase = true)) {
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
                Text("Gemma (LiteRT) notes", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Gemma uses .litertlm model files and runs via Google AI Edge LiteRT-LM. Start with a small model (≈1B/2B) for stability on 8GB RAM devices.",
                    style = MaterialTheme.typography.bodySmall
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
        Spacer(modifier = Modifier.height(16.dp))
        Text("Live logging", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Tap Start once before testing. Logs are written live to Downloads/CareerOpsMobile.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onToggleLiveLogging(true) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.liveLoggingEnabled
        ) {
            Text(if (state.liveLoggingEnabled) "Live logging is ON" else "Start live logging")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onExportLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export latest crash log (if any)")
        }
        if (!isApiMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSmokeTestLocal,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) {
                Text("Smoke test local model (tiny prompt)")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Diagnostics (no USB needed)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "If the app disappears with no message, it may be a native crash (not captured here). " +
                "This still shares device memory info and any Java/Kotlin errors we did record.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onShareDiagnostics, modifier = Modifier.fillMaxWidth()) {
            Text("Share diagnostics (email / Drive / …)")
        }
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
                "Update your details anytime. The app opens on Job input after setup."
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
fun StatusMessageBox(message: String) {
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
    onOpenJobPage: () -> Unit,
    onOpenProfile: () -> Unit,
    onStartBubble: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Job input", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Paste or share a job link. Use the menu (☰) → Job page to sign in and capture the listing, or One-tap flow when manual capture is off.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenJobPage) {
                Text("Open job page")
            }
            TextButton(onClick = onOpenProfile) {
                Text("Profile")
            }
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
                    "Manual mode: open Job page from the menu (☰), then tap Capture + Generate."
                } else {
                    "Auto mode: job page opens, then capture and generation run automatically."
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

