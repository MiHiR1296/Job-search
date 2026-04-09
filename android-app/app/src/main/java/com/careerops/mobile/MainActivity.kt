package com.careerops.mobile

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.data.ProfileStore
import com.careerops.mobile.data.ResumeParser
import com.careerops.mobile.ui.MainScreen
import com.careerops.mobile.ui.MainViewModel
import com.careerops.mobile.ui.MainViewModelFactory
import com.careerops.mobile.voice.VoiceDictationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            repository = PackRepository(applicationContext),
            profileStore = ProfileStore(applicationContext)
        )
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = VoiceDictationManager.parseResult(result.resultCode, result.data)
        if (spoken != null) {
            val field = viewModel.consumeVoiceField()
            if (field.isNotBlank()) {
                viewModel.applyVoiceInput(field, spoken)
            }
        } else {
            // Avoid getting stuck in "voice pending" state if recognizer returns nothing.
            viewModel.consumeVoiceField()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoiceCaptureIfRequested()
        } else {
            viewModel.onVoicePermissionDenied()
        }
    }

    private val resumePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handlePickedDocument(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ingestShareIntent(intent)

        setContent {
            MaterialTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickResumeDocument = ::openResumeDocumentPicker,
                    onStartVoiceCapture = ::requestVoiceCapture
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ingestShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        launchVoiceCaptureIfRequested()
    }

    private fun ingestShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            viewModel.ingestSharedText(shared)
        } else if (intent?.action == Intent.ACTION_VIEW) {
            val link = intent.dataString ?: intent.data?.toString() ?: ""
            if (link.isNotBlank()) viewModel.ingestSharedText(link)
        }
        val stream = intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (stream != null) {
            handlePickedDocument(stream)
        }
    }

    private fun openResumeDocumentPicker() {
        resumePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "application/*"
            )
        )
    }

    private fun handlePickedDocument(uri: Uri?) {
        if (uri == null) return
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriString = uri.toString()
        val profileSnapshot = viewModel.uiState.value.profile
        lifecycleScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                ResumeParser.parseToProfile(
                    context = applicationContext,
                    uriString = uriString,
                    existing = profileSnapshot.copy(resumeUri = uriString)
                )
            }
            val autoFilled = parsed != profileSnapshot.copy(resumeUri = uriString)
            viewModel.applyImportedResumeProfile(parsed, autoFilled)
        }
    }

    private fun requestVoiceCapture(field: String) {
        viewModel.beginVoiceCapture(field)
        launchVoiceCaptureIfRequested()
    }

    private fun launchVoiceCaptureIfRequested() {
        val field = viewModel.uiState.value.dictationFocusField
        if (field.isBlank()) return
        if (!VoiceDictationManager.isSpeechRecognitionAvailable(this)) {
            viewModel.onVoiceCaptureUnavailable("Speech recognition service is not available on this device.")
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        runCatching {
            speechLauncher.launch(VoiceDictationManager.buildIntent())
        }.onFailure {
            viewModel.onVoiceCaptureUnavailable()
        }
    }
}
