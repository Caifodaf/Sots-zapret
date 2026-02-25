package data.api.deprecated

import domain.model.BugReportData
import domain.model.HostsExtendData
import domain.model.ProfilesData
import domain.model.VersionData

interface SupabaseStorageApi {
    suspend fun getApiVersionList(): Result<List<VersionData>>
    suspend fun getProfilesList(): Result<List<ProfilesData>>
    suspend fun getHostsExtendList(): Result<List<HostsExtendData>>
    suspend fun sendBugReport(report: BugReportData): Result<Unit>
}