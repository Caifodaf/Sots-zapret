package data.checker

import domain.model.Profile

sealed class ProfileCheckResult {
    object Success : ProfileCheckResult()
    data class Error(val message: String) : ProfileCheckResult()
    data class ServicesResult(val servicesCheckResult: ServiceTargets.ProfileServicesCheckResult) : ProfileCheckResult()
}

interface ProfileNetworkChecker {

    @Deprecated("Use checkProfileWithAllServices instead", ReplaceWith("checkProfileWithAllServices(profile, args)"))
    suspend fun checkProfile(profile: Profile, args: String, domain: String): ProfileCheckResult
    
    /**
     * Проверяет профиль на доступность всех сервисов
     */
    suspend fun checkProfileWithAllServices(profile: Profile, args: String): ProfileCheckResult
} 