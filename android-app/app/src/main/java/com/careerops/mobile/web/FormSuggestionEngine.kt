package com.careerops.mobile.web

import com.careerops.mobile.data.CandidateProfile
import com.careerops.mobile.data.FormSuggestion

class FormSuggestionEngine {
    fun suggestFromVisibleText(
        visibleText: String,
        profile: CandidateProfile
    ): List<FormSuggestion> {
        val text = normalize(visibleText)
        if (text.isBlank()) return emptyList()

        val suggestions = mutableListOf<FormSuggestion>()

        fun addIfContains(label: String, vararg patterns: String, value: String) {
            if (value.isBlank()) return
            if (patterns.any { text.contains(it) }) {
                suggestions.add(FormSuggestion(label, value))
            }
        }

        addIfContains("Full Name", "full name", "applicant name", value = profile.fullName)
        addIfContains("Email", "email", "e-mail", value = profile.email)
        addIfContains("Phone", "phone", "mobile", "contact number", value = profile.phone)
        addIfContains("Location", "location", "current location", value = profile.location)
        addIfContains("LinkedIn", "linkedin", value = profile.linkedin)
        addIfContains("GitHub", "github", value = profile.github)
        addIfContains("Portfolio", "portfolio", "website", "behance", value = profile.portfolio)
        addIfContains("Current Title", "current title", "designation", value = profile.currentTitle)
        addIfContains("Target Role", "target role", "applied role", value = profile.targetRole)
        addIfContains("Years Experience", "experience", "years of experience", value = profile.yearsExperience)
        addIfContains("Notice Period", "notice period", value = profile.noticePeriodDays)
        addIfContains("Current CTC", "current ctc", "current salary", value = profile.currentCtcLpa)
        addIfContains("Expected CTC", "expected ctc", "expected salary", value = profile.expectedCtcLpa)
        addIfContains("Minimum CTC", "minimum salary", "minimum compensation", value = profile.minimumAcceptableLpa)
        addIfContains("Sponsorship", "sponsorship", "visa", value = profile.requiresSponsorship)
        addIfContains("Relocation", "relocate", "relocation", value = profile.willingToRelocate)

        return suggestions.distinctBy { it.label }
    }

    private fun normalize(input: String): String {
        return input.lowercase()
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
