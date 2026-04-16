package com.careerops.mobile.web

import com.careerops.mobile.data.StructuredJobDraft

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
            "designer", "analyst", "specialist", "consultant", "lead", "intern",
            "visualizer", "visualiser", "generalist", "modeler", "modeller",
            "animator", "rendering", "creative", "director", "supervisor",
            "technician", "producer", "coordinator", "head", "officer"
        )
        return lines.firstOrNull { line ->
            roleHints.any { hint -> line.contains(hint, ignoreCase = true) } &&
                line.length in 5..120 &&
                !line.contains("skip to main content", true) &&
                !line.contains("sign in", true)
        } ?: ""
    }

    fun detectSalary(text: String): String = detectSalaryExpanded(text)

    /**
     * INR/LPA/CTC plus Indeed-style monthly (USD/INR), per hour, and generic ranges.
     */
    fun detectSalaryExpanded(text: String): String {
        if (text.isBlank()) return ""
        val patterns = listOf(
            // INR / LPA / CTC (existing)
            Regex("""(?i)(₹|rs\.?|inr)\s?[0-9][0-9,.\s]*(?:\s?-\s?(?:₹|rs\.?|inr)?\s?[0-9][0-9,.\s]*)?"""),
            Regex("""(?i)\b[0-9]{1,3}\s?lpa\b"""),
            Regex("""(?i)\bctc\b\s*[:\-]?\s*(₹|rs\.?|inr)?\s*[0-9][0-9,.\s]*"""),
            // USD / month (Indeed, etc.)
            Regex("""(?i)\$\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*(/|\s*per\s*)?\s*(mo|month|monthly)\b"""),
            Regex("""(?i)\b[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*usd\s*(/|\s*per\s*)?\s*(mo|month|monthly)\b"""),
            Regex("""(?i)\b[0-9][0-9,]*\s*k\s*/\s*mo\b"""),
            Regex("""(?i)\b[0-9][0-9,]{2,}\s*(?:per\s+month|/month|monthly)\b"""),
            // EUR / GBP monthly
            Regex("""(?i)(€|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*(/|\s*per\s*)?\s*(mo|month|monthly)\b"""),
            // Per hour
            Regex("""(?i)\$\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*(/|\s*per\s*)?\s*(hr|hour)\b"""),
            Regex("""(?i)\b[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*/\s*hr\b""")
        )
        val hits = patterns.mapNotNull { rx -> rx.find(text)?.value?.trim()?.takeIf { it.isNotBlank() } }
        return hits.firstOrNull() ?: ""
    }

    /**
     * Prepend structured JobPosting fields from JSON-LD so downstream extractors and LLMs see pay/title.
     */
    fun mergeJsonLdIntoPageText(cleanInnerText: String, jsonLdConcatenated: String, maxChars: Int = 14000): String {
        val draft = JobPostingJsonLdParser.parseFromConcatenatedBlocks(jsonLdConcatenated) ?: return cleanInnerText
        val header = buildString {
            appendLine("---STRUCTURED_JOB_METADATA---")
            if (draft.company.isNotBlank()) appendLine("Employer: ${draft.company}")
            if (draft.role.isNotBlank()) appendLine("Title: ${draft.role}")
            if (draft.location.isNotBlank()) appendLine("Location: ${draft.location}")
            if (draft.salaryRaw.isNotBlank()) {
                appendLine("Compensation (schema): ${draft.salaryRaw} (${draft.payPeriodHint})")
            }
            if (draft.responsibilitiesSnippet.isNotBlank()) {
                appendLine("Description (schema excerpt):")
                appendLine(draft.responsibilitiesSnippet.take(2500))
            }
            appendLine("---END_STRUCTURED_JOB_METADATA---")
        }
        return (header + "\n" + cleanInnerText).take(maxChars)
    }

    fun structuredDraftFromJsonLd(jsonLdConcatenated: String): StructuredJobDraft? =
        JobPostingJsonLdParser.parseFromConcatenatedBlocks(jsonLdConcatenated)

    /**
     * Parses the header block produced by [mergeJsonLdIntoPageText] so company/role/salary
     * are available even when line-based heuristics miss (e.g. "Employer:", "Title:").
     */
    fun parseInjectedStructuredMetadataHeader(text: String): StructuredJobDraft? {
        val start = "---STRUCTURED_JOB_METADATA---"
        val end = "---END_STRUCTURED_JOB_METADATA---"
        val si = text.indexOf(start)
        val ei = text.indexOf(end)
        if (si < 0 || ei <= si) return null
        val block = text.substring(si + start.length, ei).trim()
        var company = ""
        var role = ""
        var location = ""
        var salaryRaw = ""
        var payPeriodHint = "unknown"
        val responsibilities = StringBuilder()
        var inDescription = false
        for (rawLine in block.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("Employer:", ignoreCase = true) ->
                    company = line.substringAfter("Employer:").trim()
                line.startsWith("Title:", ignoreCase = true) ->
                    role = line.substringAfter("Title:").trim()
                line.startsWith("Location:", ignoreCase = true) ->
                    location = line.substringAfter("Location:").trim()
                line.startsWith("Compensation (schema):", ignoreCase = true) -> {
                    val rest = line.substringAfter("Compensation (schema):").trim()
                    val open = rest.lastIndexOf('(')
                    if (open > 0 && rest.endsWith(')')) {
                        salaryRaw = rest.substring(0, open).trim()
                        payPeriodHint = rest.substring(open + 1, rest.length - 1).trim()
                            .ifBlank { "unknown" }
                    } else {
                        salaryRaw = rest
                    }
                }
                line.equals("Description (schema excerpt):", ignoreCase = true) -> inDescription = true
                inDescription -> {
                    responsibilities.appendLine(line)
                }
            }
        }
        if (company.isBlank() && role.isBlank() && salaryRaw.isBlank() && location.isBlank()) return null
        return StructuredJobDraft(
            company = company,
            role = role,
            location = location,
            salaryRaw = salaryRaw,
            payPeriodHint = payPeriodHint,
            responsibilitiesSnippet = responsibilities.toString().trim()
        )
    }

    /** Removes injected schema header so LLMs do not echo it in letters. */
    fun stripInjectedStructuredMetadata(text: String): String {
        val start = "---STRUCTURED_JOB_METADATA---"
        val end = "---END_STRUCTURED_JOB_METADATA---"
        val si = text.indexOf(start)
        val ei = text.indexOf(end)
        if (si < 0 || ei <= si) return text
        val tail = text.substring(ei + end.length)
        return (text.substring(0, si) + tail)
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /** Prefer first non-blank field in order (earlier drafts win). */
    fun mergeStructuredJobDrafts(vararg drafts: StructuredJobDraft?): StructuredJobDraft {
        var company = ""
        var role = ""
        var location = ""
        var salaryRaw = ""
        var payPeriodHint = "unknown"
        var responsibilities = ""
        for (d in drafts) {
            if (d == null) continue
            if (company.isBlank() && d.company.isNotBlank()) company = d.company.trim()
            if (role.isBlank() && d.role.isNotBlank()) role = d.role.trim()
            if (location.isBlank() && d.location.isNotBlank()) location = d.location.trim()
            if (salaryRaw.isBlank() && d.salaryRaw.isNotBlank()) {
                salaryRaw = d.salaryRaw.trim()
                payPeriodHint = d.payPeriodHint.ifBlank { payPeriodHint }
            }
            if (responsibilities.isBlank() && d.responsibilitiesSnippet.isNotBlank()) {
                responsibilities = d.responsibilitiesSnippet.trim()
            }
        }
        return StructuredJobDraft(
            company = company,
            role = role,
            location = location,
            salaryRaw = salaryRaw,
            payPeriodHint = payPeriodHint,
            responsibilitiesSnippet = responsibilities
        )
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
