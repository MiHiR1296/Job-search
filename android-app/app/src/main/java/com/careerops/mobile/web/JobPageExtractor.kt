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

    fun detectCompany(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val companyPattern = Regex("""(?i)(at|company)\s*[:\-]\s*([A-Za-z0-9 &\-.]{2,80})""")
        for (line in lines.take(80)) {
            val m = companyPattern.find(line)
            if (m != null) return m.groupValues[2].trim()
        }
        return lines.firstOrNull { it.length in 3..50 && !it.contains("job", true) && !it.contains("apply", true) }
            ?.take(50)
            ?: ""
    }

    fun detectRole(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val roleHints = listOf("engineer", "artist", "manager", "developer", "architect", "designer", "analyst")
        return lines.firstOrNull { line ->
            roleHints.any { hint -> line.contains(hint, ignoreCase = true) } && line.length in 5..100
        } ?: ""
    }

    fun detectSalary(text: String): String {
        val regex = Regex("""(?i)(₹|rs\.?|inr)\s?[0-9,]+(\s?-\s?(₹|rs\.?|inr)?\s?[0-9,]+)?|\b[0-9]{1,3}\s?lpa\b|\bctc\b""")
        return regex.find(text)?.value?.trim() ?: ""
    }
}
