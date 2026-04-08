package com.careerops.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.data.ProfileStore
import com.careerops.mobile.llm.HybridLlmEngine
import com.careerops.mobile.llm.LocalLlmEngine
import com.careerops.mobile.llm.LlamaCppLocalLlmEngine
import com.careerops.mobile.llm.StubLocalLlmEngine

class MainViewModelFactory(
    private val repository: PackRepository,
    private val profileStore: ProfileStore,
    private val llmEngine: LocalLlmEngine = HybridLlmEngine(
        localEngine = LlamaCppLocalLlmEngine(fallbackEngine = StubLocalLlmEngine())
    )
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, profileStore, llmEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
