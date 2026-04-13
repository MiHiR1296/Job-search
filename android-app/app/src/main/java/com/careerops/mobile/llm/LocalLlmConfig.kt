package com.careerops.mobile.llm

data class LocalLlmConfig(
    val provider: String = "llama.cpp-android",
    val modelPath: String = "",
    /** Room for Qwen chat template + JD slice; too small → native overflow / crash on long captures. */
    val contextSize: Int = 8192,
    val maxTokens: Int = 768,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val threads: Int = 4,
    val threadsBatch: Int = 4
)

