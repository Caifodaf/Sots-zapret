package data.api

import domain.model.HostsExtendData
import domain.model.ProfilesData
import domain.model.VersionData

interface GithubRawLinkApi {
    suspend fun getApiVersionList(): Result<List<VersionData>>
    suspend fun getApiLinksList()
    suspend fun getWinFolder()
    suspend fun getProfilesList(): Result<List<ProfilesData>>
    suspend fun getHostsList(): Result<List<HostsExtendData>>
}