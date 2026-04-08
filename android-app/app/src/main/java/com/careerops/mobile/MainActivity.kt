package com.careerops.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.data.ProfileStore
import com.careerops.mobile.ui.MainScreen
import com.careerops.mobile.ui.MainViewModel
import com.careerops.mobile.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            repository = PackRepository(applicationContext),
            profileStore = ProfileStore(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ingestShareIntent(intent)

        setContent {
            MaterialTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ingestShareIntent(intent)
    }

    private fun ingestShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            viewModel.ingestSharedText(shared)
        }
    }
}
