package data.service

import domain.repository.ProfileService
import domain.model.Profile
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import domain.model.ProfileProviderType
import domain.model.ProfileServiceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.IAppPathProvider
import java.io.InputStreamReader
import util.interfaces.ILogger
import util.path.NamespaceProject.PLACER_NAME_FILE_CUSTOM_PROFILE

class ProfileServiceImpl : ProfileService, KoinComponent {
    private val appPathProvider: IAppPathProvider by inject()
    private val logger: ILogger by inject()
    private val profileOrder = listOf(
        // Универсальные профили
        "general",
        "general_alt_1", "general_alt_2", "general_alt_3", "general_alt_4", "general_alt_5", "general_alt_6", "general_alt_7", "general_alt_8", "general_alt_9", "general_alt_10", "general_alt_11", "general_alt_12",
        // Максимальные профили
        "general_max",
        "general_max_alt_1", "general_max_alt_2", "general_max_alt_3", "general_max_alt_4", "general_max_alt_5","general_max_alt_6","general_max_alt_7",
        "general_max_lol_alt_1", "general_max_lol_alt_2", "general_max_lol_alt_3", "general_max_lol_alt_4", "general_max_lol_alt_5", "general_max_lol_alt_6",
        // Игровые
        "gaming",
        "gaming_alt_1", "gaming_alt_2", "gaming_alt_3", "gaming_alt_4", "gaming_alt_5", "gaming_alt_6",
        "gaming_alt_7", "gaming_alt_8", "gaming_alt_9", "gaming_alt_10", "gaming_alt_11", "gaming_alt_12",
        // Discord
        "discord", "discord_fix_mgts", "discord_fix_alt", "discord_fix_lol",
        // Youtube
        "youtube_fix", "youtube_fix_alt_1", "youtube_fix_mgts", "youtube_fix_mgts_alt", "youtube_fix_ttk", "youtube_fix_ttk_alt_1",
        // Прочие
        "general_fake_tls", "general_fake_tls_alt", "general_fake_tls_auto", "general_fake_tls_auto_alt", "general_fake_tls_auto_alt_2", "general_fake_tls_auto_alt_3",
        "general_max_mgts", "general_max_mgts_alt", "general_max_mgts_alt_1", "general_max_mgts_alt_2", "general_max_mgts_alt_3", "general_max_mgts_alt_4",
        "general_max_rostelecom_belline_alt_1", "general_max_rostelecom_belline_alt_2", "general_max_rostelecom_belline_alt_3", "general_max_rostelecom_belline_alt_4",
        "only_lol", "preset_russia"
    )

    @Serializable
    private data class ProfileMeta(val provider: Int, val service: Int)

    private fun ensureCustomProfilesDirExists(): Path {
        val customProfilesPath = appPathProvider.getCustomProfilesDir()
        if (!Files.exists(customProfilesPath)) {
            try {
                Files.createDirectories(customProfilesPath)
                logger.info("[ProfileServiceImpl] Created custom profiles directory: $customProfilesPath")
                
                // Создаем информационный файл
                val infoFile = customProfilesPath.resolve(PLACER_NAME_FILE_CUSTOM_PROFILE)
                if (!Files.exists(infoFile)) {
                    Files.createFile(infoFile)
                    logger.info("[ProfileServiceImpl] Created info file: $infoFile")
                }
            } catch (e: Exception) {
                logger.error("[ProfileServiceImpl] Error creating custom profiles directory: ${e.message}", e)
            }
        }
        return customProfilesPath
    }

    private fun loadProfilesFromDirectory(profilesPath: Path, metaMap: Map<String, ProfileMeta>, isCustomDirectory: Boolean = false): List<Profile> {
        return if (Files.exists(profilesPath)) {
            try {
                Files.list(profilesPath)
                    .filter { path ->
                        val fileName = path.fileName.toString().lowercase()
                        if (fileName == "last.txt") return@filter false
                        if (isCustomDirectory) {
                            fileName.endsWith(".txt", ignoreCase = true) || fileName.endsWith(".bat", ignoreCase = true)
                        } else {
                            fileName.endsWith(".txt", ignoreCase = true)
                        }
                    }
                    .map { path ->
                        val fileName = path.fileName.toString()
                        val nameWithoutExtension = when {
                            fileName.endsWith(".txt", ignoreCase = true) -> fileName.removeSuffix(".txt")
                            fileName.endsWith(".bat", ignoreCase = true) -> fileName.removeSuffix(".bat")
                            else -> fileName
                        }
                        nameWithoutExtension
                    }
                    .map { fileName ->
                        val meta = metaMap[fileName]
                        Profile(
                            fileName = fileName,
                            displayName = fileName,
                            service = if (isCustomDirectory) {
                                ProfileServiceType.CUSTOM
                            } else {
                                meta?.service?.let { ProfileServiceType.fromInt(it) } ?: ProfileServiceType.UNIVERSAL
                            },
                            provider = meta?.provider?.let { ProfileProviderType.fromInt(it) } ?: ProfileProviderType.PROVIDER_UNKNOWN
                        )
                    }
                    .toList()
            } catch (e: Exception) {
                logger.error("[ProfileServiceImpl] Error loading profiles from $profilesPath: ${e.message}", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun getProfilesList(): List<Profile> {
        val profilesPath = appPathProvider.getProfilesDir()
        val customProfilesPath = ensureCustomProfilesDirExists()
        
        val metaFile = profilesPath.resolve("profiles_meta.json")
        val metaMap: Map<String, ProfileMeta> = if (Files.exists(metaFile)) {
            try {
                val json = Files.readString(metaFile)
                Json.decodeFromString(json)
            } catch (e: Exception) {
                logger.error("[ProfileServiceImpl] Error reading profiles_meta.json: ${e.message}", e)
                emptyMap()
            }
        } else emptyMap()

        val mainProfiles = loadProfilesFromDirectory(profilesPath, metaMap, isCustomDirectory = false)
        
        val customProfiles = loadProfilesFromDirectory(customProfilesPath, emptyMap(), isCustomDirectory = true)
        
        val allProfiles = mainProfiles + customProfiles
        logger.info("[ProfileServiceImpl] Loaded profiles: ${mainProfiles.size} main + ${customProfiles.size} custom (with CUSTOM service) = ${allProfiles.size} total")
        
        return allProfiles
    }

    override fun updateProfilesList(): List<Profile> = getProfilesList()

    override fun getLocalizedProfileName(fileName: String, lang: String): String {
        val bundle = getBundle(lang)
        val key = "profile_name_$fileName"
        val localized = bundle?.getProperty(key) ?: fileName
        if (localized == fileName) {
            logger.debug("[ProfileServiceImpl] Localization not found for profile: $fileName ($lang)")
        }
        return localized
    }

    private fun getBundle(lang: String): Properties? {
        return try {
            val resourceName = when (lang) {
                "ru" -> "/strings_ru.properties"
                else -> "/strings_en.properties"
            }
            val props = Properties()
            val stream = javaClass.getResourceAsStream(resourceName)
            stream?.use { input ->
                InputStreamReader(input, Charsets.UTF_8).use { reader ->
                    props.load(reader)
                }
            }
            props
        } catch (e: Exception) {
            logger.error("[ProfileServiceImpl] Error loading localization: ${e.message}", e)
            null
        }
    }

    override fun sortProfiles(profiles: List<Profile>, lang: String): List<Profile> {
        logger.info("[ProfileServiceImpl] Sorting profiles (${profiles.size}) for language $lang")
        val localized = mutableListOf<Profile>()
        val notLocalized = mutableListOf<Profile>()
        for (profile in profiles) {
            val isLocalized = getLocalizedProfileName(profile.fileName, lang) != profile.fileName
            if (isLocalized) localized.add(profile) else notLocalized.add(profile)
        }
        val orderedLocalized = profileOrder.mapNotNull { orderName ->
            localized.find { it.fileName == orderName }
        } + localized.filter { p -> profileOrder.none { it == p.fileName } }
        val orderedNotLocalized = notLocalized.sortedBy { it.fileName }
        val result = (orderedLocalized + orderedNotLocalized).map {
            it.copy(displayName = getLocalizedProfileName(it.fileName, lang))
        }
        logger.debug("[ProfileServiceImpl] Sorting completed. Localized: ${localized.size}, not localized: ${notLocalized.size}")
        return result
    }

    override fun getProfileArgs(profileFileName: String): String {
        val mainProfilePathTxt = appPathProvider.getProfilesDir().resolve("$profileFileName.txt")
        if (Files.exists(mainProfilePathTxt)) {
            return try {
                val args = Files.readAllLines(mainProfilePathTxt).joinToString(" ")
                logger.info("[ProfileServiceImpl] Profile arguments for $profileFileName (main): $args")
                args
            } catch (e: Exception) {
                logger.error("[ProfileServiceImpl] Error reading profile arguments for $profileFileName (main): ${e.message}", e)
                ""
            }
        }
        
        val customProfilePathTxt = appPathProvider.getCustomProfilesDir().resolve("$profileFileName.txt")
        val customProfilePathBat = appPathProvider.getCustomProfilesDir().resolve("$profileFileName.bat")
        
        val customProfilePath = when {
            Files.exists(customProfilePathTxt) -> customProfilePathTxt
            Files.exists(customProfilePathBat) -> customProfilePathBat
            else -> null
        }
        
        return try {
            if (customProfilePath == null) {
                logger.warn("[ProfileServiceImpl] Profile file not found in both locations: $mainProfilePathTxt, $customProfilePathTxt, $customProfilePathBat")
                ""
            } else {
                val args = Files.readAllLines(customProfilePath).joinToString(" ")
                logger.info("[ProfileServiceImpl] Profile arguments for $profileFileName (custom): $args")
                args
            }
        } catch (e: Exception) {
            logger.error("[ProfileServiceImpl] Error reading profile arguments for $profileFileName (custom): ${e.message}", e)
            ""
        }
    }
} 