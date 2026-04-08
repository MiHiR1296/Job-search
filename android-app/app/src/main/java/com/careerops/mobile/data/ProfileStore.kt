package com.careerops.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "candidate_profile")

class ProfileStore(private val context: Context) {
    private val keys = Keys()

    val profileFlow: Flow<CandidateProfile> = context.profileDataStore.data.map { prefs ->
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
            strengths = prefs[keys.strengths] ?: "",
            achievements = prefs[keys.achievements] ?: "",
            yearsExperience = prefs[keys.yearsExperience] ?: "",
            noticePeriodDays = prefs[keys.noticePeriodDays] ?: "",
            currentCtcLpa = prefs[keys.currentCtcLpa] ?: "",
            expectedCtcLpa = prefs[keys.expectedCtcLpa] ?: "",
            minimumAcceptableLpa = prefs[keys.minimumAcceptableLpa] ?: "",
            requiresSponsorship = prefs[keys.requiresSponsorship] ?: "No",
            willingToRelocate = prefs[keys.willingToRelocate] ?: "Yes"
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
            prefs[keys.strengths] = profile.strengths
            prefs[keys.achievements] = profile.achievements
            prefs[keys.yearsExperience] = profile.yearsExperience
            prefs[keys.noticePeriodDays] = profile.noticePeriodDays
            prefs[keys.currentCtcLpa] = profile.currentCtcLpa
            prefs[keys.expectedCtcLpa] = profile.expectedCtcLpa
            prefs[keys.minimumAcceptableLpa] = profile.minimumAcceptableLpa
            prefs[keys.requiresSponsorship] = profile.requiresSponsorship
            prefs[keys.willingToRelocate] = profile.willingToRelocate
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
        val strengths = stringPreferencesKey("strengths")
        val achievements = stringPreferencesKey("achievements")
        val yearsExperience = stringPreferencesKey("years_experience")
        val noticePeriodDays = stringPreferencesKey("notice_period_days")
        val currentCtcLpa = stringPreferencesKey("current_ctc_lpa")
        val expectedCtcLpa = stringPreferencesKey("expected_ctc_lpa")
        val minimumAcceptableLpa = stringPreferencesKey("minimum_acceptable_lpa")
        val requiresSponsorship = stringPreferencesKey("requires_sponsorship")
        val willingToRelocate = stringPreferencesKey("willing_to_relocate")
    }
}
