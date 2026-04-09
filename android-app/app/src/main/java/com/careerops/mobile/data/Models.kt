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
    val targetRole: String = "",
    val resumeUri: String = "",
    val strengths: String = "",
    val achievements: String = "",
    val careerMemory: String = "",
    val yearsExperience: String = "",
    val noticePeriodDays: String = "",
    val currentCtcLpa: String = "",
    val expectedCtcLpa: String = "",
    val minimumAcceptableLpa: String = "",
    val requiresSponsorship: String = "No",
    val willingToRelocate: String = "Yes",
    val llmProviderMode: String = "local",
    val localModelPath: String = "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf",
    val apiBaseUrl: String = "https://api.openai.com/v1",
    val apiModel: String = "gpt-4o-mini",
    val apiKey: String = ""
)

data class JobInput(
    val company: String,
    val role: String,
    val url: String,
    val jdText: String = "",
    val salaryHint: String = ""
)

data class ApplicationPack(
    val folderName: String,
    val folderPath: String,
    val packMarkdown: String,
    val formAnswersMarkdown: String,
    val coverLetterMarkdown: String,
    val resumeMarkdown: String
)

data class FormSuggestion(
    val label: String,
    val suggestedValue: String
)

data class JobInsight(
    val detectedCompany: String = "",
    val detectedRole: String = "",
    val detectedSalaryText: String = "",
    val score: Double = 0.0,
    val recommendation: String = "Review",
    val reasons: List<String> = emptyList()
)
