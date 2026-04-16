package com.careerops.mobile.llm

import com.careerops.mobile.data.StructuredJobDraft
import org.json.JSONObject

object LlmStructuredJobParser {

    fun parseStructuredJobJson(raw: String): StructuredJobDraft? {
        if (raw.isBlank()) return null
        val jsonBlock = extractJsonObjectSubstring(raw) ?: return null
        return runCatching {
            val o = JSONObject(jsonBlock)
            StructuredJobDraft(
                company = o.optString("company").trim(),
                role = o.optString("role").trim().ifBlank { o.optString("title").trim() },
                location = o.optString("location").trim(),
                salaryRaw = o.optString("salary_raw").trim().ifBlank { o.optString("salaryRaw").trim() },
                payPeriodHint = o.optString("pay_period", o.optString("payPeriodHint", "unknown")).trim()
                    .ifBlank { "unknown" },
                responsibilitiesSnippet = o.optString("responsibilities", o.optString("responsibilitiesSnippet", ""))
                    .trim().take(1200)
            )
        }.getOrNull()
    }

    private fun extractJsonObjectSubstring(raw: String): String? {
        val trimmed = raw.trim()
        val fenceStart = trimmed.indexOf("```")
        val text = if (fenceStart >= 0) {
            val after = trimmed.substring(fenceStart + 3).let { s ->
                val nl = s.indexOf('\n')
                if (nl >= 0) s.substring(nl + 1) else s
            }
            after.substringBefore("```").trim()
        } else trimmed
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
