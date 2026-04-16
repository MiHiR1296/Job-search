package com.careerops.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.careerops.mobile.security.ApiKeyCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "candidate_profile")

class ProfileStore(private val context: Context) {
    private val keys = Keys()
    private val crypto = ApiKeyCrypto()

    val profileFlow: Flow<CandidateProfile> = context.profileDataStore.data.map { prefs ->
        val decryptedApiKey = crypto.decrypt(prefs[keys.apiKeyEncrypted] ?: "")
        val resumeUri = prefs[keys.resumeUri] ?: ""
        val explicitOnboarding = prefs[keys.onboardingCompleted]
        val migratedOnboarding = resumeUri.isNotBlank()
        val onboardingCompleted = explicitOnboarding ?: migratedOnboarding
        CandidateProfile(
            fullName = prefs[keys.fullName] ?: "Your Name",
            email = prefs[keys.email] ?: "you@example.com",
            phone = prefs[keys.phone] ?: "+91XXXXXXXXXX",
            location = prefs[keys.location] ?: "Mumbai, India",
            linkedin = prefs[keys.linkedin] ?: "https://linkedin.com/in/your-handle",
            github = prefs[keys.github] ?: "https://github.com/your-handle",
            portfolio = prefs[keys.portfolio] ?: "https://your-portfolio.example.com",
            currentTitle = prefs[keys.currentTitle] ?: "",
            targetRole = prefs[keys.targetRole] ?: "",
            resumeUri = resumeUri,
            resumeTextSnapshot = prefs[keys.resumeTextSnapshot] ?: "",
            strengths = prefs[keys.strengths] ?: "",
            achievements = prefs[keys.achievements] ?: "",
            careerMemory = prefs[keys.careerMemory] ?: "",
            yearsExperience = prefs[keys.yearsExperience] ?: "",
            noticePeriodDays = prefs[keys.noticePeriodDays] ?: "",
            currentCtcLpa = prefs[keys.currentCtcLpa] ?: "",
            expectedCtcLpa = prefs[keys.expectedCtcLpa] ?: "",
            minimumAcceptableLpa = prefs[keys.minimumAcceptableLpa] ?: "",
            requiresSponsorship = prefs[keys.requiresSponsorship] ?: "No",
            willingToRelocate = prefs[keys.willingToRelocate] ?: "Yes",
            onboardingCompleted = onboardingCompleted,
            onboardingVersion = prefs[keys.onboardingVersion] ?: 1,
            llmProviderMode = prefs[keys.llmProviderMode] ?: "local",
            localModelPath = prefs[keys.localModelPath] ?: "/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            apiBaseUrl = prefs[keys.apiBaseUrl] ?: "https://api.openai.com/v1",
            apiModel = prefs[keys.apiModel] ?: "gpt-4o-mini",
            apiKey = if (decryptedApiKey.isNotBlank()) decryptedApiKey else (prefs[keys.apiKeyLegacy] ?: ""),
            promptApplyDecisionSystem = prefs[keys.promptApplyDecisionSystem] ?: "",
            promptApplyDecisionUser = prefs[keys.promptApplyDecisionUser] ?: "",
            promptResumeHighlightsSystem = prefs[keys.promptResumeHighlightsSystem] ?: "",
            promptResumeHighlightsUser = prefs[keys.promptResumeHighlightsUser] ?: "",
            promptCoverLetterSystem = prefs[keys.promptCoverLetterSystem] ?: "",
            promptCoverLetterUser = prefs[keys.promptCoverLetterUser] ?: "",
            maxOutputChars = prefs[keys.maxOutputChars] ?: 1800
        )
    }

    suspend fun saveProfile(profile: CandidateProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[keys.fullName] = profile.fullName
            prefs[keys.email] = profile.email
            prefs[keys.phone] = profile.phone
            prefs[keys.location] = profile.location
            prefs[keys.linkedin] = profile.linkedin
            prefs[keys.github] = profile.github
            prefs[keys.portfolio] = profile.portfolio
            prefs[keys.currentTitle] = profile.currentTitle
            prefs[keys.targetRole] = profile.targetRole
            prefs[keys.resumeUri] = profile.resumeUri
            prefs[keys.resumeTextSnapshot] = profile.resumeTextSnapshot.take(12_000)
            prefs[keys.strengths] = profile.strengths
            prefs[keys.achievements] = profile.achievements
            prefs[keys.careerMemory] = profile.careerMemory
            prefs[keys.yearsExperience] = profile.yearsExperience
            prefs[keys.noticePeriodDays] = profile.noticePeriodDays
            prefs[keys.currentCtcLpa] = profile.currentCtcLpa
            prefs[keys.expectedCtcLpa] = profile.expectedCtcLpa
            prefs[keys.minimumAcceptableLpa] = profile.minimumAcceptableLpa
            prefs[keys.requiresSponsorship] = profile.requiresSponsorship
            prefs[keys.willingToRelocate] = profile.willingToRelocate
            prefs[keys.onboardingCompleted] = profile.onboardingCompleted
            prefs[keys.onboardingVersion] = profile.onboardingVersion
            prefs[keys.llmProviderMode] = profile.llmProviderMode
            prefs[keys.localModelPath] = profile.localModelPath
            prefs[keys.apiBaseUrl] = profile.apiBaseUrl
            prefs[keys.apiModel] = profile.apiModel
            prefs[keys.apiKeyEncrypted] = crypto.encrypt(profile.apiKey)
            prefs.remove(keys.apiKeyLegacy)

            prefs[keys.promptApplyDecisionSystem] = profile.promptApplyDecisionSystem.take(20_000)
            prefs[keys.promptApplyDecisionUser] = profile.promptApplyDecisionUser.take(40_000)
            prefs[keys.promptResumeHighlightsSystem] = profile.promptResumeHighlightsSystem.take(20_000)
            prefs[keys.promptResumeHighlightsUser] = profile.promptResumeHighlightsUser.take(40_000)
            prefs[keys.promptCoverLetterSystem] = profile.promptCoverLetterSystem.take(20_000)
            prefs[keys.promptCoverLetterUser] = profile.promptCoverLetterUser.take(40_000)
            prefs[keys.maxOutputChars] = profile.maxOutputChars.coerceIn(200, 20_000)
        }
    }

    private class Keys {
        val fullName = stringPreferencesKey("full_name")
        val email = stringPreferencesKey("email")
        val phone = stringPreferencesKey("phone")
        val location = stringPreferencesKey("location")
        val linkedin = stringPreferencesKey("linkedin")
        val github = stringPreferencesKey("github")
        val portfolio = stringPreferencesKey("portfolio")
        val currentTitle = stringPreferencesKey("current_title")
        val targetRole = stringPreferencesKey("target_role")
        val resumeUri = stringPreferencesKey("resume_uri")
        val resumeTextSnapshot = stringPreferencesKey("resume_text_snapshot")
        val strengths = stringPreferencesKey("strengths")
        val achievements = stringPreferencesKey("achievements")
        val careerMemory = stringPreferencesKey("career_memory")
        val yearsExperience = stringPreferencesKey("years_experience")
        val noticePeriodDays = stringPreferencesKey("notice_period_days")
        val currentCtcLpa = stringPreferencesKey("current_ctc_lpa")
        val expectedCtcLpa = stringPreferencesKey("expected_ctc_lpa")
        val minimumAcceptableLpa = stringPreferencesKey("minimum_acceptable_lpa")
        val requiresSponsorship = stringPreferencesKey("requires_sponsorship")
        val willingToRelocate = stringPreferencesKey("willing_to_relocate")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val onboardingVersion = intPreferencesKey("onboarding_version")
        val llmProviderMode = stringPreferencesKey("llm_provider_mode")
        val localModelPath = stringPreferencesKey("local_model_path")
        val apiBaseUrl = stringPreferencesKey("api_base_url")
        val apiModel = stringPreferencesKey("api_model")
        val apiKeyLegacy = stringPreferencesKey("api_key")
        val apiKeyEncrypted = stringPreferencesKey("api_key_encrypted")

        val promptApplyDecisionSystem = stringPreferencesKey("prompt_apply_decision_system")
        val promptApplyDecisionUser = stringPreferencesKey("prompt_apply_decision_user")
        val promptResumeHighlightsSystem = stringPreferencesKey("prompt_resume_highlights_system")
        val promptResumeHighlightsUser = stringPreferencesKey("prompt_resume_highlights_user")
        val promptCoverLetterSystem = stringPreferencesKey("prompt_cover_letter_system")
        val promptCoverLetterUser = stringPreferencesKey("prompt_cover_letter_user")
        val maxOutputChars = intPreferencesKey("max_output_chars")
    }
}
