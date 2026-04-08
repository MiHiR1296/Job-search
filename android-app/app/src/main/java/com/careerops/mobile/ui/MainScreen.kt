package com.careerops.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.os.Build
import com.careerops.mobile.service.BubbleOverlayService

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Career Ops Mobile (MVP)",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Generate per-job mobile packs and local LLM outputs.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.company,
                onValueChange = viewModel::updateCompany,
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.role,
                onValueChange = viewModel::updateRole,
                label = { Text("Role") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::updateUrl,
                label = { Text("Job URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.jdText,
                onValueChange = viewModel::updateJdText,
                label = { Text("JD text (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = viewModel::generatePack,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            ) {
                Text("Generate Mobile Application Pack")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val serviceIntent = Intent(context, BubbleOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Bubble Service (MVP stub)")
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (state.isGenerating) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.statusMessage.isNotBlank()) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.generatedCoverLetter.isNotBlank()) {
                Text("Generated Cover Letter", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(state.generatedCoverLetter, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.generatedResumeHighlights.isNotBlank()) {
                Text("Generated Resume Highlights", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(state.generatedResumeHighlights, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
