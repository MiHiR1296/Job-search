package com.careerops.mobile.web

object JobPageExtractor {
    private val noiseTokens = listOf(
        "cookie",
        "privacy policy",
        "sign in",
        "login",
        "agree & join",
        "terms",
        "help center"
    )

    fun cleanExtractedText(raw: String, maxChars: Int = 12000): String {
        if (raw.isBlank()) return ""
        val lines = raw
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { line ->
                val lower = line.lowercase()
                noiseTokens.any { token -> lower.contains(token) }
            }

        val compact = lines.joinToString("\n")
        return compact.take(maxChars)
    }
}
