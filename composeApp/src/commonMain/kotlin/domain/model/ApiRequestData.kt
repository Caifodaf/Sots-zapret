package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesData(
    val id: Int,
    val name: String,
    val content: String,
    val service: Int,
    val provider: Int,
)

@Serializable
data class VersionData(
    val type: String,
    val version: String,
    val changelog: String?
)

@Serializable
data class BugReportData(
    val name: String,
    val content: String,
    val id_user: String? = null,
    val created_at: String? = null
)

@Serializable
data class HostsExtendData(
    val id: Int,
    val content: String
)