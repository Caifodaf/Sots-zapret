package data.checker

object ServiceTargets {
    
    /**
     * Целевые адреса для Discord
     */
    val DISCORD_TARGETS = listOf(
        "https://discord.com",
        //"https://gateway.discord.gg",
        //"https://cdn.discordapp.com",
        //"https://updates.discord.com"
    )
    
    /**
     * Целевые адреса для YouTube
     */
    val YOUTUBE_TARGETS = listOf(
        "https://www.youtube.com",
        "https://youtu.be",
        //"https://i.ytimg.com",
        //"https://redirector.googlevideo.com"
    )
    
    /**
     * Целевые адреса для Google
     */
    val GOOGLE_TARGETS = listOf(
        "https://www.google.com",
        //"https://www.gstatic.com"
    )
    
    /**
     * Целевые адреса для Riot Games
     */
    val RIOT_TARGETS = listOf(
        "https://riotgames.com",
        "https://www.riotgames.com"
    )
    
    /**
     * Целевые адреса для Cloudflare
     */
    val CLOUDFLARE_TARGETS = listOf(
        "https://www.cloudflare.com",
        "https://cdnjs.cloudflare.com",
        "PING:1.1.1.1",
        "PING:1.0.0.1"
    )
    
    /**
     * Маппинг URL к их имена
     */
    val TARGET_NAMES = mapOf(
        // Discord
        "https://discord.com" to "DiscordMain",
        "https://gateway.discord.gg" to "DiscordGateway",
        "https://cdn.discordapp.com" to "DiscordCDN",
        "https://updates.discord.com" to "DiscordUpdates",
        // YouTube
        "https://www.youtube.com" to "YouTubeWeb",
        "https://youtu.be" to "YouTubeShort",
        "https://i.ytimg.com" to "YouTubeImage",
        "https://redirector.googlevideo.com" to "YouTubeVideoRedirect",
        // Google
        "https://www.google.com" to "GoogleMain",
        "https://www.gstatic.com" to "GoogleGstatic",
        // Riot
        "https://riotgames.com" to "RiotMain",
        "https://www.riotgames.com" to "RiotWeb",
        // Cloudflare
        "https://www.cloudflare.com" to "CloudflareWeb",
        "https://cdnjs.cloudflare.com" to "CloudflareCDN",
        "PING:1.1.1.1" to "CloudflareDNS1111",
        "PING:1.0.0.1" to "CloudflareDNS1001",
        "PING:8.8.8.8" to "GoogleDNS8888",
        "PING:8.8.4.4" to "GoogleDNS8844",
        "PING:9.9.9.9" to "Quad9DNS9999"
    )

    val ALL_SERVICES = mapOf(
        ServiceType.DISCORD to DISCORD_TARGETS,
        ServiceType.YOUTUBE to YOUTUBE_TARGETS,
        ServiceType.GOOGLE to GOOGLE_TARGETS,
        ServiceType.RIOT to RIOT_TARGETS,
        ServiceType.CLOUDFLARE to CLOUDFLARE_TARGETS
    )
    
    /**
     * Типы сервисов для проверки
     */
    enum class ServiceType {
        DISCORD,
        YOUTUBE,
        GOOGLE,
        RIOT,
        CLOUDFLARE
    }
    
    /**
     * Результат проверки одного сервиса
     */
    data class ServiceCheckResult(
        val serviceType: ServiceType,
        val isSuccess: Boolean,
        val failedTargets: List<String> = emptyList()
    )
    
    /**
     * Результат проверки всех сервисов для профиля
     */
    data class ProfileServicesCheckResult(
        val serviceResults: Map<ServiceType, ServiceCheckResult>
    ) {
        /**
         * Проверяет, все ли сервисы успешно прошли проверку
         */
        fun isAllSuccess(): Boolean {
            return serviceResults.values.all { it.isSuccess }
        }
        
        /**
         * Проверяет, есть ли хотя бы один успешный сервис
         */
        fun hasAnySuccess(): Boolean {
            return serviceResults.values.any { it.isSuccess }
        }
    }
}

