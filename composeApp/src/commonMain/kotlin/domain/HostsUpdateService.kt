package domain

sealed class HostsUpdateResult {
    data object Success : HostsUpdateResult()
    data object AlreadyUpdated : HostsUpdateResult()
    data class Error(val message: String) : HostsUpdateResult()
}

interface HostsUpdateService {
    suspend fun updateHostsList(): HostsUpdateResult
}

