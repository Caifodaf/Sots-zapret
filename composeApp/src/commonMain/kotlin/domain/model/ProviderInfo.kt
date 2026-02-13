package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProviderInfo(
    val isp: String,
    val org: String? = null,
    val country: String? = null,
    val ip: String
) 