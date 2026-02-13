package data.api

import domain.model.VersionData
import domain.model.ProfilesData
import domain.model.HostsExtendData

interface SupabaseStorageApi {
    suspend fun getApiVersionList(): Result<List<VersionData>>
    suspend fun getProfilesList(): Result<List<ProfilesData>>
    suspend fun getHostsExtendList(): Result<List<HostsExtendData>>
    suspend fun sendBugReport(report: domain.model.BugReportData): Result<Unit>
}