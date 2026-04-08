package com.careerops.mobile.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PackRepository(private val context: Context) {

    fun getPackBaseDir(): File = File(context.filesDir, "mobile-packs")

    fun generatePack(jobInput: JobInput, profile: CandidateProfile): ApplicationPack {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val slug = "${jobInput.company}-${jobInput.role}".toSlug()
        val folderName = "$date-$slug"

        val baseDir = File(getPackBaseDir(), folderName)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        val packMd = buildPackMarkdown(jobInput, profile, slug)
        val formAnswersMd = buildFormAnswersMarkdown(jobInput, profile)
        val coverLetterMd = buildCoverLetterMarkdown(jobInput)
        val resumeMd = buildResumeMarkdown(jobInput)

        writeUtf8(File(baseDir, "$slug-application-pack.md"), packMd)
        writeUtf8(File(baseDir, "$slug-form-answers.md"), formAnswersMd)
        writeUtf8(File(baseDir, "$slug-cover-letter.md"), coverLetterMd)
        writeUtf8(File(baseDir, "$slug-resume.md"), resumeMd)

        return ApplicationPack(
            folderName = folderName,
            folderPath = baseDir.absolutePath,
            packMarkdown = packMd,
            formAnswersMarkdown = formAnswersMd,
            coverLetterMarkdown = coverLetterMd,
            resumeMarkdown = resumeMd
        )
    }

    private fun buildPackMarkdown(job: JobInput, profile: CandidateProfile, slug: String): String {
        return """
            |# Application Pack - ${job.company} - ${job.role}
            |
            |Source URL: ${job.url}
            |
            |## Quick Apply Checklist
            |
            |1. Open job link in app/browser.
            |2. Upload tailored resume from this pack.
            |3. Paste short cover letter from this pack.
            |4. Fill fields from form answers section.
            |5. Submit manually.
            |
            |## Suggested Files
            |
            |- ${slug}-resume.md
            |- ${slug}-cover-letter.md
            |- ${slug}-form-answers.md
            |
            |## Field Snapshot
            |
            |- Name: ${profile.fullName}
            |- Email: ${profile.email}
            |- Phone: ${profile.phone}
            |- Expected CTC (LPA): ${profile.expectedCtcLpa}
            |- Notice Period (Days): ${profile.noticePeriodDays}
            |
            |## Notes
            |
            |Generate role-specific cover letter and resume bullets using your LLM workflow.
        """.trimMargin()
    }

    private fun buildFormAnswersMarkdown(job: JobInput, profile: CandidateProfile): String {
        return """
            |# Form Answers - ${job.company} - ${job.role}
            |
            |- Full Name: ${profile.fullName}
            |- Email: ${profile.email}
            |- Phone: ${profile.phone}
            |- Location: ${profile.location}
            |- LinkedIn: ${profile.linkedin}
            |- GitHub: ${profile.github}
            |- Portfolio: ${profile.portfolio}
            |- Current Title: ${profile.currentTitle}
            |- Years Experience: ${profile.yearsExperience}
            |- Notice Period (Days): ${profile.noticePeriodDays}
            |- Current CTC (LPA): ${profile.currentCtcLpa}
            |- Expected CTC (LPA): ${profile.expectedCtcLpa}
            |- Minimum Acceptable (LPA): ${profile.minimumAcceptableLpa}
            |- Sponsorship Required: ${profile.requiresSponsorship}
            |- Willing to Relocate: ${profile.willingToRelocate}
        """.trimMargin()
    }

    private fun buildCoverLetterMarkdown(job: JobInput): String {
        return """
            |# Cover Letter - ${job.company} - ${job.role}
            |
            |(Generate using local on-device LLM with JD + profile + knowledge memory.)
        """.trimMargin()
    }

    private fun buildResumeMarkdown(job: JobInput): String {
        return """
            |# Tailored Resume Notes - ${job.company} - ${job.role}
            |
            |- Add JD keywords.
            |- Prioritize role-relevant impact bullets.
            |- Keep claims evidence-backed.
        """.trimMargin()
    }

    private fun String.toSlug(): String {
        return lowercase(Locale.ENGLISH)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun writeUtf8(file: File, content: String) {
        try {
            file.writeText(content)
        } catch (e: IOException) {
            throw IllegalStateException("Failed writing ${file.absolutePath}", e)
        }
    }
}
