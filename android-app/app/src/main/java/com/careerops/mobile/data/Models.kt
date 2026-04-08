package com.careerops.mobile.data

data class CandidateProfile(
    val fullName: String = "Your Name",
    val email: String = "you@example.com",
    val phone: String = "+91XXXXXXXXXX",
    val location: String = "Mumbai, India",
    val linkedin: String = "https://linkedin.com/in/your-handle",
    val github: String = "https://github.com/your-handle",
    val portfolio: String = "https://your-portfolio.example.com",
    val currentTitle: String = "",
    val yearsExperience: String = "",
    val noticePeriodDays: String = "",
    val currentCtcLpa: String = "",
    val expectedCtcLpa: String = "",
    val minimumAcceptableLpa: String = "",
    val requiresSponsorship: String = "No",
    val willingToRelocate: String = "Yes"
)

data class JobInput(
    val company: String,
    val role: String,
    val url: String,
    val jdText: String = ""
)

data class ApplicationPack(
    val folderName: String,
    val folderPath: String,
    val packMarkdown: String,
    val formAnswersMarkdown: String,
    val coverLetterMarkdown: String,
    val resumeMarkdown: String
)
