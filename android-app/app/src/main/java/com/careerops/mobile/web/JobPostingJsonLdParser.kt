package com.careerops.mobile.web

import com.careerops.mobile.data.StructuredJobDraft
import org.json.JSONArray
import org.json.JSONObject

/**
 * Best-effort parsing of [schema.org JobPosting](https://schema.org/JobPosting) from
 * concatenated `<script type="application/ld+json">` contents.
 */
object JobPostingJsonLdParser {

    fun parseFromConcatenatedBlocks(concatenated: String): StructuredJobDraft? {
        if (concatenated.isBlank()) return null
        val parts = concatenated.split("\n---JSONLD---\n")
        var best: StructuredJobDraft? = null
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isBlank()) continue
            val drafts = mutableListOf<StructuredJobDraft>()
            runCatching {
                when (trimmed.firstOrNull()) {
                    '[' -> {
                        val arr = JSONArray(trimmed)
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.let { o ->
                                extractJobPosting(o)?.let { drafts.add(it) }
                            }
                        }
                    }
                    else -> extractJobPosting(JSONObject(trimmed))?.let { drafts.add(it) }
                }
            }
            val local = drafts.maxByOrNull { it.score() }
            if (local != null && local.score() > (best?.score() ?: 0)) {
                best = local
            }
        }
        return best
    }

    private fun StructuredJobDraft.score(): Int {
        var s = 0
        if (company.isNotBlank()) s += 2
        if (role.isNotBlank()) s += 2
        if (salaryRaw.isNotBlank()) s += 2
        if (location.isNotBlank()) s += 1
        if (responsibilitiesSnippet.isNotBlank()) s += 1
        return s
    }

    private fun findJobPostingObject(obj: JSONObject): JSONObject? {
        val type = obj.optString("@type", obj.optString("type", ""))
        if (type.contains("JobPosting", ignoreCase = true)) return obj
        val graph = obj.optJSONArray("@graph") ?: return null
        for (i in 0 until graph.length()) {
            val el = graph.optJSONObject(i) ?: continue
            if (el.optString("@type", "").contains("JobPosting", ignoreCase = true)) return el
        }
        return null
    }

    private fun extractJobPosting(root: JSONObject): StructuredJobDraft? {
        val type = root.optString("@type", root.optString("type", ""))
        val jobObj = when {
            type.contains("JobPosting", ignoreCase = true) -> root
            root.has("@graph") -> {
                val graph = root.optJSONArray("@graph") ?: return null
                (0 until graph.length())
                    .mapNotNull { graph.optJSONObject(it) }
                    .firstOrNull { it.optString("@type", "").contains("JobPosting", ignoreCase = true) }
            }
            else -> findJobPostingObject(root)
        } ?: return null

        val title = jobObj.optString("title").ifBlank { jobObj.optString("name") }
        val company = parseOrganizationName(jobObj.opt("hiringOrganization"))
        val location = parseLocation(jobObj.opt("jobLocation"))
        val (salaryRaw, payHint) = parseSalary(jobObj.opt("baseSalary"))

        val desc = jobObj.optString("description")
            .replace(Regex("<[^>]+>"), " ")
            .trim()
        val snippet = desc.take(800)

        if (title.isBlank() && company.isBlank() && salaryRaw.isBlank()) return null

        return StructuredJobDraft(
            company = company,
            role = title,
            location = location,
            salaryRaw = salaryRaw,
            payPeriodHint = payHint,
            responsibilitiesSnippet = snippet
        )
    }

    private fun parseOrganizationName(node: Any?): String {
        if (node == null) return ""
        return when (node) {
            is String -> node
            is JSONObject -> node.optString("name").ifBlank { node.optString("legalName") }
            else -> ""
        }
    }

    private fun parseLocation(node: Any?): String {
        if (node == null) return ""
        return when (node) {
            is String -> node
            is JSONObject -> {
                val addr = node.optJSONObject("address")
                if (addr != null) {
                    listOf(
                        addr.optString("addressLocality"),
                        addr.optString("addressRegion"),
                        addr.optString("addressCountry")
                    ).filter { it.isNotBlank() }.joinToString(", ")
                } else {
                    node.optString("name")
                }
            }
            is JSONArray -> {
                (0 until node.length())
                    .mapNotNull { node.optJSONObject(it) }
                    .joinToString(" | ") { parseLocation(it) }
            }
            else -> ""
        }
    }

    private fun parseSalary(node: Any?): Pair<String, String> {
        if (node == null) return "" to "unknown"
        return when (node) {
            is JSONObject -> {
                val currency = node.optString("currency", "")
                val unit = node.optString("unitText", node.optString("@type", "")).lowercase()
                val value = node.opt("value")
                val raw = when (value) {
                    is JSONObject -> {
                        val min = value.optString("minValue")
                        val max = value.optString("maxValue")
                        when {
                            min.isNotBlank() && max.isNotBlank() -> "$currency $min - $max / $unit"
                            min.isNotBlank() -> "$currency $min / $unit"
                            else -> value.optString("value")
                        }
                    }
                    is Number -> "$currency ${value} / $unit"
                    else -> node.toString().take(200)
                    }
                val hint = when {
                    unit.contains("month", ignoreCase = true) -> "monthly"
                    unit.contains("year", ignoreCase = true) -> "yearly"
                    unit.contains("hour", ignoreCase = true) -> "hourly"
                    else -> "unknown"
                }
                raw.trim() to hint
            }
            else -> node.toString().take(200) to "unknown"
        }
    }
}
