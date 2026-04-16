package com.careerops.mobile.scoring

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.JobInsight
import com.careerops.mobile.data.JobInput
import com.careerops.mobile.web.JobPageExtractor

class JobScoringEngine {
    fun score(jobInput: JobInput, profile: CandidateProfile): JobInsight {
        val text = buildString {
            append(jobInput.jdText)
            append("\n")
            append(jobInput.resumeSummaryForPrompt)
            append("\n")
            append(profile.careerMemory)
            append("\n")
            append(jobInput.jobSpecificNotes)
        }.lowercase()

        var score = 0.0
        val reasons = mutableListOf<String>()

        // Role fit
        val targetRole = profile.targetRole.lowercase()
        if (targetRole.isNotBlank() && text.contains(targetRole)) {
            score += 1.2
            reasons.add("JD aligns with your target role.")
        }

        // Title fit
        if (profile.currentTitle.isNotBlank() && text.contains(profile.currentTitle.lowercase())) {
            score += 0.6
            reasons.add("JD has overlap with your current title domain.")
        }

        // Strengths overlap
        val strengthTokens = profile.strengths.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val strengthHits = strengthTokens.count { text.contains(it) }
        if (strengthHits > 0) {
            score += minOf(1.2, strengthHits * 0.4)
            reasons.add("Matched $strengthHits of your declared strengths.")
        }

        if (profile.careerMemory.isNotBlank()) {
            val memTokens = profile.careerMemory.lowercase().split(Regex("\\W+")).filter { it.length > 5 }.distinct().take(40)
            val memHits = memTokens.count { tok -> tok.isNotBlank() && text.contains(tok) }
            if (memHits > 0) {
                score += minOf(0.8, memHits * 0.15)
                reasons.add("JD overlaps with $memHits themes from your saved career memory.")
            }
        }

        if (jobInput.jobSpecificNotes.isNotBlank()) {
            reasons.add("Per-job notes from you were included in this evaluation.")
        }

        // Experience alignment
        val years = profile.yearsExperience.toIntOrNull() ?: 0
        if (years >= 7) {
            if (text.contains("senior") || text.contains("lead")) {
                score += 0.8
                reasons.add("Seniority expectations likely align.")
            }
        } else if (years in 3..6) {
            if (text.contains("mid") || text.contains("associate") || text.contains("engineer")) {
                score += 0.6
                reasons.add("Experience level appears reasonably aligned.")
            }
        }

        // Compensation hint alignment
        val salaryText = jobInput.salaryHint.ifBlank { JobPageExtractor.detectSalaryExpanded(jobInput.jdText) }
        if (salaryText.isNotBlank()) {
            reasons.add("Salary hint detected: $salaryText")
            val expected = profile.expectedCtcLpa.toDoubleOrNull()
            val minAccept = profile.minimumAcceptableLpa.toDoubleOrNull()
            val jdLpa = extractLpaNumber(salaryText) ?: jobInput.salaryAnnualLpaApprox
            if (jdLpa != null && minAccept != null && expected != null) {
                when {
                    jdLpa >= expected -> {
                        score += 0.9
                        reasons.add("Detected pay is at/above expected CTC.")
                    }
                    jdLpa >= minAccept -> {
                        score += 0.5
                        reasons.add("Detected pay is above your minimum acceptable CTC.")
                    }
                    else -> reasons.add("Detected pay appears below your minimum target.")
                }
                if (jobInput.salaryAnnualLpaApprox != null && extractLpaNumber(salaryText) == null) {
                    reasons.add("CTC comparison used an approximate annual LPA from monthly/non-LPA wording.")
                }
            } else if (Regex("""(?i)(/mo|/month|\bper month\b|\bmonthly\b|\b/hr\b|\bper hour\b)""").containsMatchIn(salaryText)) {
                reasons.add("Pay appears to be hourly or monthly (not LPA); compare manually to your CTC targets.")
            }
        }

        // Normalize to 0..5
        val normalized = score.coerceIn(0.0, 5.0)
        val recommendation = when {
            normalized >= 4.1 -> "Strong Apply"
            normalized >= 3.6 -> "Apply"
            normalized >= 2.8 -> "Borderline"
            else -> "Skip"
        }

        val detectedCompany = maxByNonBlank(jobInput.company, JobPageExtractor.detectCompany(jobInput.jdText))
        val detectedRole = maxByNonBlank(jobInput.role, JobPageExtractor.detectRole(jobInput.jdText))

        return JobInsight(
            detectedCompany = detectedCompany,
            detectedRole = detectedRole,
            detectedSalaryText = salaryText,
            score = String.format("%.2f", normalized).toDouble(),
            recommendation = recommendation,
            reasons = reasons
        )
    }

    private fun extractLpaNumber(text: String): Double? {
        val lpaRegex = Regex("""([0-9]{1,3})\s?lpa""", RegexOption.IGNORE_CASE)
        val m = lpaRegex.find(text) ?: return null
        return m.groupValues[1].toDoubleOrNull()
    }

    private fun maxByNonBlank(a: String, b: String): String {
        val aTrim = a.trim()
        val bTrim = b.trim()
        if (aTrim.isBlank()) return bTrim
        if (bTrim.isBlank()) return aTrim
        return if (aTrim.length >= bTrim.length) aTrim else bTrim
    }
}

