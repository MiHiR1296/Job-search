package com.careerops.mobile.web

object JobPageExtractor {
    private val noiseTokens = listOf(
        "cookie",
        "privacy policy",
        "sign in",
        "login",
        "agree & join",
        "terms",
        "help center",
        "skip to main content",
        "accept all cookies",
        "continue with google",
        "continue with apple",
        "create account",
        "advertisement",
        "sponsored"
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
            .filterNot { isLikelyChrome(it) }
            .filterNot { isLikelyNavNoise(it) }
            .distinct()

        val compact = lines.joinToString("\n")
        return compact.take(maxChars)
    }

    fun detectCompany(text: String): String {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { isLowSignalLine(it) }
        val companyPattern = Regex("""(?i)(company|about us|at)\s*[:\-]\s*([A-Za-z0-9 &\-.]{2,80})""")
        for (line in lines.take(120)) {
            val m = companyPattern.find(line)
            if (m != null) return m.groupValues[2].trim()
        }
        return lines.firstOrNull {
            it.length in 3..50 &&
                !it.contains("job", true) &&
                !it.contains("apply", true) &&
                !it.contains("content", true) &&
                !it.contains("menu", true)
        }
            ?.take(50)
            ?: ""
    }

    fun detectRole(text: String): String {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { isLowSignalLine(it) }
        val roleHints = listOf(
            "engineer", "artist", "manager", "developer", "architect",
            "designer", "analyst", "specialist", "consultant", "lead", "intern"
        )
        return lines.firstOrNull { line ->
            roleHints.any { hint -> line.contains(hint, ignoreCase = true) } &&
                line.length in 5..120 &&
                !line.contains("skip to main content", true) &&
                !line.contains("sign in", true)
        } ?: ""
    }

    fun detectSalary(text: String): String {
        val regex = Regex("""(?i)(₹|rs\.?|inr)\s?[0-9,]+(\s?-\s?(₹|rs\.?|inr)?\s?[0-9,]+)?|\b[0-9]{1,3}\s?lpa\b|\bctc\b""")
        return regex.find(text)?.value?.trim() ?: ""
    }

    fun isLikelyJobUrl(url: String): Boolean {
        val normalized = url.lowercase().trim()
        if (normalized.isBlank()) return false
        val jobSignals = listOf(
            "/jobs/",
            "/job/",
            "linkedin.com/jobs",
            "naukri.com",
            "indeed.",
            "wellfound.com/jobs",
            "greenhouse.io",
            "lever.co",
            "/careers/",
            "/vacanc",
            "/openings"
        )
        return jobSignals.any { normalized.contains(it) }
    }

    private fun isLikelyChrome(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    private fun isLikelyNavNoise(line: String): Boolean {
        val lower = line.lowercase()
        return lower.length < 3 ||
            lower in setOf("home", "jobs", "about", "contact", "menu", "next", "back", "apply")
    }

    private fun isLowSignalLine(line: String): Boolean {
        val lower = line.lowercase()
        return noiseTokens.any { lower.contains(it) } ||
            lower.contains("skip to main content") ||
            lower.contains("sign in") ||
            lower.contains("log in") ||
            lower.contains("cookie")
    }
}
