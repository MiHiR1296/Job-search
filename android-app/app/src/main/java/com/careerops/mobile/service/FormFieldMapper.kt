package com.careerops.mobile.service

import android.view.accessibility.AccessibilityNodeInfo
import com.careerops.mobile.data.CandidateProfile
import java.util.Locale

class FormFieldMapper(profile: CandidateProfile) {

    private data class Rule(
        val patterns: List<String>,
        val value: String
    )

    private val rules = listOf(
        Rule(listOf("first name", "firstname", "given name"), firstToken(profile.fullName)),
        Rule(listOf("last name", "lastname", "surname"), lastToken(profile.fullName)),
        Rule(listOf("full name", "applicant name", "your name"), profile.fullName),
        Rule(listOf("email", "e-mail"), profile.email),
        Rule(listOf("phone", "mobile", "contact"), profile.phone),
        Rule(listOf("location", "current location", "city"), profile.location),
        Rule(listOf("linkedin"), profile.linkedin),
        Rule(listOf("github"), profile.github),
        Rule(listOf("portfolio", "website", "behance"), profile.portfolio),
        Rule(listOf("experience", "years of experience"), profile.yearsExperience),
        Rule(listOf("notice period"), profile.noticePeriodDays),
        Rule(listOf("current ctc", "current salary"), profile.currentCtcLpa),
        Rule(listOf("expected ctc", "expected salary"), profile.expectedCtcLpa),
        Rule(listOf("minimum salary", "minimum compensation"), profile.minimumAcceptableLpa),
        Rule(listOf("sponsorship", "visa"), profile.requiresSponsorship),
        Rule(listOf("relocate", "relocation"), profile.willingToRelocate)
    )

    fun match(node: AccessibilityNodeInfo): String? {
        val contextText = buildContextText(node)
        if (contextText.isBlank()) return null

        for (rule in rules) {
            if (rule.value.isBlank()) continue
            if (rule.patterns.any { contextText.contains(it) }) {
                return rule.value
            }
        }
        return null
    }

    private fun buildContextText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()

        parts.add(node.text?.toString().orEmpty())
        parts.add(node.contentDescription?.toString().orEmpty())
        parts.add(node.hintText?.toString().orEmpty())
        parts.add(node.viewIdResourceName.orEmpty())

        val parent = node.parent
        if (parent != null) {
            parts.add(parent.text?.toString().orEmpty())
            parts.add(parent.contentDescription?.toString().orEmpty())
        }

        return normalize(parts.joinToString(" "))
    }

    private fun normalize(input: String): String {
        return input.lowercase(Locale.ENGLISH)
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun firstToken(text: String): String {
        return text.trim().split(" ").firstOrNull().orEmpty()
    }

    private fun lastToken(text: String): String {
        return text.trim().split(" ").lastOrNull().orEmpty()
    }
}

