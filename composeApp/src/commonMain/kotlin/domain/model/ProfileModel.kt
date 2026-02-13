package domain.model

enum class ProfileServiceType(val value: Int) {
    CUSTOM(-1),
    UNIVERSAL(0),
    YOUTUBE(1),
    DISCORD(2);
    //LOL(3);
    companion object {
        fun fromInt(value: Int): ProfileServiceType = entries.find { it.value == value } ?: UNIVERSAL
    }
}

enum class ProfileProviderType(val value: Int) {
    PROVIDER_UNKNOWN(0),
    PROVIDER_ROSTELECOM(1),
    PROVIDER_BELLINE(1),
    PROVIDER_MGTS(2),
    PROVIDER_TTK(3),
    PROVIDER_TLS(4);
    companion object {
        fun fromInt(value: Int): ProfileProviderType = entries.find { it.value == value } ?: PROVIDER_UNKNOWN
    }
}

data class Profile(
    val fileName: String,
    val displayName: String?,
    val description: String = "",
    val service: ProfileServiceType = ProfileServiceType.UNIVERSAL,
    val provider: ProfileProviderType = ProfileProviderType.PROVIDER_UNKNOWN
)