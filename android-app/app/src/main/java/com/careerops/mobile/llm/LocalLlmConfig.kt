package com.careerops.mobile.llm

data class LocalLlmConfig(
    val provider: String = "llama.cpp-android",
    val modelPath: String = "",
    /** Larger default so JD + instructions fit 7B/8B on-device runs. */
    val contextSize: Int = 4096,
    val maxTokens: Int = 768,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val threads: Int = 4,
    val threadsBatch: Int = 4
)

