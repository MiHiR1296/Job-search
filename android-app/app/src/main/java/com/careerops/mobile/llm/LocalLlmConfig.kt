package com.careerops.mobile.llm

data class LocalLlmConfig(
    val provider: String = "llama.cpp-android",
    val modelPath: String = "",
    /**
     * KV cache scales with this value; 8k + 1.5B can OOM-kill the process on 8GB phones.
     * 4k is a safer default; raise in code only if you have headroom.
     */
    val contextSize: Int = 4096,
    val maxTokens: Int = 512,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    /** Fewer threads reduces peak RAM and CPU contention on mid-range devices. */
    val threads: Int = 2,
    val threadsBatch: Int = 2
)

