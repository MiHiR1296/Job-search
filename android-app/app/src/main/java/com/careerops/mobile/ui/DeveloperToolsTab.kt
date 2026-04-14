package com.careerops.mobile.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeveloperToolsTab(
    state: MainUiState,
    onToggleLiveLogging: (Boolean) -> Unit,
    onExportLogs: () -> Unit,
    onSmokeTestLocal: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onDevChatPromptChange: (String) -> Unit,
    onRunDevChat: () -> Unit,
    onGenerateDecision: () -> Unit,
    onGenerateHighlights: () -> Unit,
    onGenerateCoverLetter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Developer tools", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Small tests + logs to debug local models. (This page only appears in debug builds.)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Live logging", style = MaterialTheme.typography.titleSmall)
                Switch(checked = state.liveLoggingEnabled, onCheckedChange = onToggleLiveLogging)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onExportLogs, modifier = Modifier.fillMaxWidth()) {
                Text("Export latest logs to Downloads")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onShareDiagnostics, modifier = Modifier.fillMaxWidth()) {
                Text("Share diagnostics (email / Drive / …)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Local model", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSmokeTestLocal,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) {
                Text(if (state.isGenerating) "Running…" else "Smoke test (load + tiny generation)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Step-by-step generation", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGenerateDecision,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) { Text("Generate Apply Decision (smallest)") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGenerateHighlights,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) { Text("Generate Resume Highlights") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGenerateCoverLetter,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) { Text("Generate Cover Letter (heaviest)") }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Local chat", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.devChatPrompt,
                onValueChange = onDevChatPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRunDevChat,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating && !state.isDevChatRunning
            ) {
                Text(if (state.isDevChatRunning) "Generating…" else "Send to local model")
            }
            if (state.devChatOutput.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusMessageBox(state.devChatOutput)
            }

            if (state.statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                StatusMessageBox(state.statusMessage)
            }
        }
    }
}

