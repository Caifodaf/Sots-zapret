package util.path

object PathFilesProject {

    const val LOCAL_SOTS_DIR = "Sots"
    const val WIN_DIR = "win"
    const val WIN_DIR_TLS = "win/blockcheck/zapret/files/fake/tls_clienthello_iana_org.bin"
    const val WHITELIST_DIR = "whitelist"
    const val PROFILES_DIR = "profiles"
    const val CUSTOM_PROFILES_DIR = "profiles/custom profiles"
    const val LOGS_DIR = "logs"
    const val PROFILES_CHECK_LOGS_DIR = "logs/autoCheckLogs"

    const val REPOSITORY_URL = "https://github.com/Flowseal/zapret-discord-youtube"
    const val GITHUB_API_URL = "https://api.github.com/repos/Flowseal/zapret-discord-youtube/tags"

    const val VERSION_FILE_NAME = "version.txt"
    const val TEMP_DIR = "C:\\Windows\\Temp"

    const val WHITELIST_FILE_NAME = "whitelist.txt"
    const val GENERAL_FILE_NAME = "list-general.txt"
    const val DISCORD_FILE_NAME = "list-discord.txt"
    const val IPSET_CLOUDWARE_FILE_NAME = "ipset-cloudflare.txt"
    const val GAME_FILTER = "1024-65535"

    const val URL_README = "https://github.com/Flowseal/zapret-discord-youtube"
    const val URL_GIT = "https://github.com/Flowseal/zapret-discord-youtube"
    const val URL_FAQ = "https://github.com/Flowseal/zapret-discord-youtube"
    const val URL_DONAT = "https://pay.cloudtips.ru/p/b5b4dba2"

    val API_BASE_URL: String get() = ApiSecrets.API_BASE_URL
    val API_BASE_KEY: String get() = ApiSecrets.API_BASE_KEY

    const val PROFILE_TABLE_NAME = "/rest/v1/profiles"
    const val PROFILE_EXPANDED_TABLE_NAME = "/rest/v1/profiles_expanded"
    const val VERSION_TABLE_NAME = "/rest/v1/version"
    const val BUG_REPORT_TABLE_NAME = "/rest/v1/reports"
    const val HOSTS_EXTEND_TABLE_NAME = "/rest/v1/hosts_extend"

    const val URL_SOTS_PAGE = "null"
    const val URL_SOTS_PAGE_DOWNLOAD = "https://drive.google.com/drive/folders/1OQsPxJKOm9A8SA3hd4IeqMAUDaALYRve?usp=sharing" //Todo: Debug

    val SUPABASE_API_ZIP_URL: String get() = ApiSecrets.SUPABASE_API_ZIP_URL
}