package com.careerops.mobile.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.Locale

object ResumeParser {
    fun parseToProfile(context: Context, uriString: String, existing: CandidateProfile): CandidateProfile {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return existing
        val text = extractText(context, uri).ifBlank { return existing }

        val normalized = text.replace("\r", "\n")
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
            .find(normalized)?.value.orEmpty()
        val phone = Regex("""(?:\+91[\s-]?)?[6-9]\d{9}""")
            .find(normalized)?.value.orEmpty()
        val years = Regex("""(\d{1,2}\+?)\s*(?:years|yrs)""", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.getOrNull(1).orEmpty()
        val linkedin = Regex("""https?://(?:www\.)?linkedin\.com/[^\s]+""", RegexOption.IGNORE_CASE)
            .find(normalized)?.value.orEmpty()
        val github = Regex("""https?://(?:www\.)?github\.com/[^\s]+""", RegexOption.IGNORE_CASE)
            .find(normalized)?.value.orEmpty()

        val roleHints = listOf(
            "software engineer",
            "android developer",
            "backend engineer",
            "frontend developer",
            "data engineer",
            "data analyst",
            "machine learning engineer",
            "product manager",
            "devops engineer",
            "sre",
            "qa engineer"
        )
        val lower = normalized.lowercase(Locale.ENGLISH)
        val inferredRole = roleHints.firstOrNull { lower.contains(it) }.orEmpty()

        val topLine = lines.firstOrNull().orEmpty()
        val fullName = topLine.takeIf {
            it.length in 4..60 &&
                !it.contains("@") &&
                !it.contains("resume", ignoreCase = true) &&
                it.split(" ").size in 2..4
        }.orEmpty()

        val existingStrengths = existing.strengths.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        val existingAchievements = existing.achievements.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        val skillCandidates = extractSkills(normalized)
        existingStrengths.addAll(skillCandidates.take(10))
        existingAchievements.addAll(extractAchievementHints(lines).take(8))

        return existing.copy(
            fullName = existing.fullName.takeUnless { isDefaultName(it) || it.isBlank() } ?: fullName.ifBlank { existing.fullName },
            email = existing.email.takeUnless { isDefaultEmail(it) || it.isBlank() } ?: email.ifBlank { existing.email },
            phone = existing.phone.takeUnless { isDefaultPhone(it) || it.isBlank() } ?: phone.ifBlank { existing.phone },
            linkedin = existing.linkedin.takeUnless { isDefaultLinkedin(it) || it.isBlank() } ?: linkedin.ifBlank { existing.linkedin },
            github = existing.github.takeUnless { isDefaultGithub(it) || it.isBlank() } ?: github.ifBlank { existing.github },
            currentTitle = existing.currentTitle.ifBlank { inferredRole.replaceFirstChar { c -> c.titlecase(Locale.ENGLISH) } },
            targetRole = existing.targetRole.ifBlank { inferredRole.replaceFirstChar { c -> c.titlecase(Locale.ENGLISH) } },
            yearsExperience = existing.yearsExperience.ifBlank { years },
            strengths = existingStrengths.joinToString(", "),
            achievements = existingAchievements.joinToString(", "),
            resumeUri = uriString
        )
    }

    private fun extractText(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri).orEmpty()
        return if (mime.contains("pdf", ignoreCase = true) || uri.toString().lowercase(Locale.ENGLISH).endsWith(".pdf")) {
            extractPdfText(context, uri)
        } else {
            extractPlainText(context, uri)
        }
    }

    private fun extractPlainText(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
    }

    private fun extractPdfText(context: Context, uri: Uri): String {
        return runCatching {
            PDFBoxResourceLoader.init(context)
            val temp = File.createTempFile("resume-upload-", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            PDDocument.load(temp).use { doc ->
                PDFTextStripper().getText(doc).orEmpty()
            }.also {
                temp.delete()
            }
        }.getOrDefault("")
    }

    private fun extractSkills(text: String): List<String> {
        val dictionary = listOf(
            "kotlin", "java", "python", "javascript", "typescript", "react", "android",
            "jetpack compose", "spring boot", "node.js", "sql", "postgresql", "mysql",
            "aws", "gcp", "azure", "docker", "kubernetes", "ci/cd", "machine learning",
            "nlp", "llm", "tensorflow", "pytorch", "tableau", "power bi", "excel", "git"
        )
        val lower = text.lowercase(Locale.ENGLISH)
        return dictionary.filter { lower.contains(it) }.distinct()
    }

    private fun extractAchievementHints(lines: List<String>): List<String> {
        return lines
            .filter { line ->
                val lower = line.lowercase(Locale.ENGLISH)
                line.length in 20..180 &&
                    (Regex("""\d+%|\d+x|\d+\+?""").containsMatchIn(line) ||
                        lower.contains("improved") ||
                        lower.contains("reduced") ||
                        lower.contains("built") ||
                        lower.contains("shipped") ||
                        lower.contains("increased"))
            }
            .take(12)
    }

    private fun isDefaultName(value: String): Boolean = value.equals("Your Name", ignoreCase = true)
    private fun isDefaultEmail(value: String): Boolean = value.equals("you@example.com", ignoreCase = true)
    private fun isDefaultPhone(value: String): Boolean = value.equals("+91XXXXXXXXXX", ignoreCase = true)
    private fun isDefaultLinkedin(value: String): Boolean = value.contains("your-handle", ignoreCase = true)
    private fun isDefaultGithub(value: String): Boolean = value.contains("your-handle", ignoreCase = true)
}
