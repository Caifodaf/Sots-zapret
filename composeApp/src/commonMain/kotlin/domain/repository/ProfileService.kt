package domain.repository

import domain.model.Profile

interface ProfileService {
    fun getProfilesList(): List<Profile>
    fun updateProfilesList(): List<Profile>
    fun getLocalizedProfileName(fileName: String, lang: String): String
    fun sortProfiles(profiles: List<Profile>, lang: String): List<Profile>
    fun getProfileArgs(profileFileName: String): String
} 