package com.careerops.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.PackRepository
import com.careerops.mobile.llm.LocalLlmEngine
import com.careerops.mobile.llm.StubLocalLlmEngine

class MainViewModelFactory(
    private val repository: PackRepository,
    private val llmEngine: LocalLlmEngine = StubLocalLlmEngine(),
    private val profile: CandidateProfile = CandidateProfile()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, llmEngine, profile) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
