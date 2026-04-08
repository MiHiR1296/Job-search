package com.careerops.mobile.llm

data class LocalLlmConfig(
    val provider: String = "mlc-llm",
    val modelId: String = "Qwen2.5-1.5B-Instruct-q4f16_1",
    val maxTokens: Int = 600,
    val temperature: Float = 0.3f
)

