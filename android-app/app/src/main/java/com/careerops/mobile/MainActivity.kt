package com.careerops.mobile

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import java.io.File
import java.io.FileOutputStream

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

    private val localModelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handlePickedLocalModel(uri)
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
                    onPickLocalModelFile = ::openLocalModelFilePicker,
                    onOpenJobInCustomTab = ::openJobUrlInCustomTab
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

    private fun openJobUrlInCustomTab(url: String) {
        if (url.isBlank()) return
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.scheme != "http" && uri.scheme != "https") return
        val schemeParams = CustomTabColorSchemeParams.Builder().build()
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(schemeParams)
            .setShowTitle(true)
            .build()
        intent.launchUrl(this, uri)
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

    private fun openLocalModelFilePicker() {
        localModelPickerLauncher.launch(
            arrayOf(
                "*/*",
                "application/octet-stream",
                "application/x-gzip"
            )
        )
    }

    private fun handlePickedLocalModel(uri: Uri?) {
        if (uri == null) return
        viewModel.beginLocalModelImport()
        lifecycleScope.launch {
            val (path, err) = withContext(Dispatchers.IO) {
                runCatching { copyUriToInternalModelFile(uri).absolutePath }.fold(
                    onSuccess = { it to null },
                    onFailure = { null to (it.message ?: "Could not read the selected file.") }
                )
            }
            viewModel.finishLocalModelImport(path, err)
        }
    }

    private fun copyUriToInternalModelFile(uri: Uri): File {
        val dir = File(filesDir, "llm-models").apply { mkdirs() }
        val rawName = queryDisplayName(uri)?.trim()?.takeIf { it.isNotBlank() } ?: "model.gguf"
        val safe = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(96)
        val withExt = if (safe.endsWith(".gguf", ignoreCase = true)) safe else "$safe.gguf"
        val dest = File(dir, withExt)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open the selected file.")
        return dest
    }

    private fun queryDisplayName(uri: Uri): String? {
        val fromMeta = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(0)
        }
        if (!fromMeta.isNullOrBlank()) return fromMeta
        val seg = uri.lastPathSegment ?: return null
        return seg.substringAfterLast('/').takeIf { it.isNotBlank() }
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
