package data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.path.PathFilesProject
import util.path.PathFilesProject.GENERAL_FILE_NAME
import util.impl.ZipDownloader
import util.interfaces.ILogger
import util.interfaces.IAppPathProvider
import presentation.viewmodel.SettingsViewModel
import data.service.ProfileAdapterService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import domain.repository.WhitelistManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import util.path.NamespaceProject.APP_VERSION

class ApiUpdateServiceImpl(
    private val githubRawApi: GithubRawLinkApi,
    private val settingsViewModel: SettingsViewModel,
    private val logger: ILogger,
    private val appPathProvider: IAppPathProvider,
) : ApiUpdateService, KoinComponent {
    private val profileAdapterService: ProfileAdapterService by inject()

    override suspend fun checkForApiUpdates(): ApiUpdateResult = withContext(Dispatchers.IO) {
        try {
            val localApiVersion = settingsViewModel.getApiVersion() ?: "0.0.1"
            val remoteResult = githubRawApi.getApiVersionList()
            if (remoteResult.isSuccess) {
                val remoteList = remoteResult.getOrNull().orEmpty()
                logger.info("[ApiUpdateService] Results version on API: " + remoteList.joinToString { "type=${it.type}, version=${it.version}" })
                val remoteApiVersion = remoteList.filter { it.type == "api" }.maxByOrNull { it.version }?.version ?: "0.0.0"
                val remoteAppVersion = remoteList.filter { it.type == "app_win" }.maxByOrNull { it.version }?.version ?: APP_VERSION
                if (compareVersionStrings(remoteApiVersion, localApiVersion) > 0) {
                    return@withContext ApiUpdateResult.UpdateAvailable(remoteApiVersion)
                }
                if (compareVersionStrings(remoteAppVersion, APP_VERSION) > 0) {
                    return@withContext ApiUpdateResult.AppUpdateAvailable(APP_VERSION, remoteAppVersion)
                }
                return@withContext ApiUpdateResult.NoUpdate
            } else {
                logger.error("[ApiUpdateService] Error getting remote versions: ${remoteResult.exceptionOrNull()?.message}")
                return@withContext ApiUpdateResult.Error("Error getting remote versions")
            }
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Exception: ${e.message}")
            return@withContext ApiUpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun checkForApiAndAppUpdates(): ApiAndAppUpdateResult = withContext(Dispatchers.IO) {
        try {
            val localApiVersion = settingsViewModel.getApiVersion() ?: "0.0.1"
            val remoteResult = githubRawApi.getApiVersionList()
            if (remoteResult.isSuccess) {
                val remoteList = remoteResult.getOrNull().orEmpty()
                logger.info("[ApiUpdateService] Results version on API: " + remoteList.joinToString { "type=${it.type}, version=${it.version}" })
                val remoteApiVersion = remoteList.filter { it.type == "api" }.maxByOrNull { it.version }?.version ?: "0.0.0"
                val remoteAppVersion = remoteList.filter { it.type == "app_win" }.maxByOrNull { it.version }?.version ?: APP_VERSION
                val apiUpdateAvailable = compareVersionStrings(remoteApiVersion, localApiVersion) > 0
                val appUpdateAvailable = compareVersionStrings(remoteAppVersion, APP_VERSION) > 0
                return@withContext ApiAndAppUpdateResult(
                    apiUpdateAvailable = apiUpdateAvailable,
                    remoteApiVersion = if (apiUpdateAvailable) remoteApiVersion else null,
                    appUpdateAvailable = appUpdateAvailable,
                    remoteAppVersion = if (appUpdateAvailable) remoteAppVersion else null
                )
            } else {
                logger.error("[ApiUpdateService] Error getting remote versions: ${remoteResult.exceptionOrNull()?.message}")
                return@withContext ApiAndAppUpdateResult(false, null, false, null)
            }
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Exception: ${e.message}")
            return@withContext ApiAndAppUpdateResult(false, null, false, null)
        }
    }

    override suspend fun downloadApi(): ApiDownloadResult = withContext(Dispatchers.IO) {
        try {
            val tempDir = Path.of(PathFilesProject.TEMP_DIR)
            val zipUrl = PathFilesProject.SUPABASE_API_ZIP_URL
            val unzipResult = ZipDownloader().downloadAndUnzip(zipUrl, tempDir)
            if (unzipResult.isFailure) {
                logger.error("[ApiUpdateService] Error downloading or unpacking zip: ${unzipResult.exceptionOrNull()?.message}")
                return@withContext ApiDownloadResult.Error("Error downloading/unpacking API")
            }
            val unzipDir =
                unzipResult.getOrNull() ?: return@withContext ApiDownloadResult.Error("Error unpacking archive")
            return@withContext ApiDownloadResult.Success(unzipDir)
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Exception during API download: ${e.message}")
            return@withContext ApiDownloadResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun mergeApi(archivePath: Path, remoteApiVersion: String): ApiMergeResult = withContext(Dispatchers.IO) {
        logger.info("[ApiUpdateService] mergeApi: start, archivePath=$archivePath, remoteApiVersion=$remoteApiVersion")
        try {
            val repoRoot = archivePath.toFile().listFiles()?.firstOrNull { it.isDirectory }?.toPath() ?: archivePath
            ensureDirectoriesExist()
            saveProfilesFromApi()
            copyDirectoryFromRepo(repoRoot, "whitelist", appPathProvider.getWhitelistDir(), overwrite = true)
           copyDirectoryFromRepo(repoRoot, "win", appPathProvider.getWinDir(), overwrite = false)
            try {
                logger.info("[ApiUpdateService] mainFile 1")
                val whitelistManager: WhitelistManager by inject()
                val userLinks = whitelistManager.getWhiteList()
                val mainFile = appPathProvider.getWhitelistDir().resolve(GENERAL_FILE_NAME)
                val mainLinks = if (Files.exists(mainFile)) Files.readAllLines(mainFile) else emptyList()
                logger.info("[ApiUpdateService] mainFile path: $mainFile")
                logger.info("[ApiUpdateService] userLinks.size=${userLinks.size}, mainLinks.size=${mainLinks.size}")
                logger.info("[ApiUpdateService] userLinks=$userLinks, mainLinks=$mainLinks")
                val merged = whitelistManager.mergeWhiteList(userLinks, mainLinks)
                Files.write(mainFile, merged)
                logger.info("[ApiUpdateService] Whitelist after merging with user links: ${merged.size} records")
            } catch (e: Exception) {
                logger.error("[ApiUpdateService] Error merging user links: ${e.message}", e)
                e.printStackTrace()
            }
            logger.info("[ApiUpdateService] Saving API version: $remoteApiVersion")
            try {
                settingsViewModel.saveApiVersionSuspend(remoteApiVersion)
                logger.info("[ApiUpdateService] API version saved successfully: $remoteApiVersion")
            } catch (e: Exception) {
                logger.error("[ApiUpdateService] Error saving API version: ${e.message}", e)
                return@withContext ApiMergeResult.Error("Error saving API version: ${e.message}")
            }
            try {
                archivePath.toFile().deleteRecursively()
            } catch (e: Exception) {
                logger.warn("[ApiUpdateService] Failed to delete temporary files: ${e.message}")
            }
            return@withContext ApiMergeResult.Success
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Exception during API merge: ${e.message}")
            return@withContext ApiMergeResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun downloadProfilesOnly(): ProfilesDownloadResult = withContext(Dispatchers.IO) {
        logger.info("[ApiUpdateService] downloadProfilesOnly: start")
        try {
            ensureDirectoriesExist()
            saveProfilesFromApi()
            logger.info("[ApiUpdateService] downloadProfilesOnly: profiles downloaded successfully")
            return@withContext ProfilesDownloadResult.Success
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Exception during profiles download: ${e.message}")
            return@withContext ProfilesDownloadResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun saveProfilesFromApi() {
        logger.info("[ApiUpdateService] saveProfilesFromApi: start")
        val profilesResult = githubRawApi.getProfilesList()
        if (profilesResult.isSuccess) {
            val profiles = profilesResult.getOrNull().orEmpty()
            val profilesDir = appPathProvider.getProfilesDir()
            val customProfilesDir = appPathProvider.getCustomProfilesDir()
            
            clearMainProfilesDirectory(profilesDir, customProfilesDir)
            
            Files.createDirectories(profilesDir)
            profiles.forEach { profile ->
                val fileName = profile.name.trim() + ".txt"
                //val content = profileAdapterService.adaptProfile(profile.content, removeWinwsPath = false)
                val filePath = profilesDir.resolve(fileName)
                Files.writeString(filePath, profile.content)
                logger.info("[ApiUpdateService] Profile saved: $fileName (service=${profile.service}, provider=${profile.provider})")
            }
            @Serializable
            data class ProfileMeta(val provider: Int, val service: Int)
            val metaMap = profiles.associate { it.name.trim() to ProfileMeta(it.provider, it.service) }
            logger.info("[ApiUpdateService] saveProfilesFromApi: before metaJson serialization")
            val metaJson = Json.encodeToString(metaMap)
            logger.info("[ApiUpdateService] saveProfilesFromApi: after metaJson serialization")
            val metaFile = profilesDir.resolve("profiles_meta.json")
            Files.writeString(metaFile, metaJson)
        } else {
            logger.error("[ApiUpdateService] Error getting profiles: ${profilesResult.exceptionOrNull()?.message}")
        }
        logger.info("[ApiUpdateService] saveProfilesFromApi: end")
    }

    private fun copyDirectoryFromRepo(repoRoot: Path, dirName: String, dst: Path, overwrite: Boolean) {
        logger.info("[ApiUpdateService] copyDirectoryFromRepo: start, repoRoot=$repoRoot, dirName=$dirName, dst=$dst, overwrite=$overwrite")
        val src = repoRoot.resolve(dirName)
        if (Files.exists(src)) {
            logger.info("[ApiUpdateService] Copying api/$dirName -> $dst (${if (overwrite) "with overwrite" else "only if different"})")
            Files.walk(src).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { srcFile ->
                    val rel = src.relativize(srcFile)
                    val dstFile = dst.resolve(rel)
                    Files.createDirectories(dstFile.parent)
                    if (dirName == "win") {
                        if (!Files.exists(dstFile)) {
                            Files.copy(srcFile, dstFile)
                            logger.info("[ApiUpdateService] Copied file (win, no check): $srcFile -> $dstFile")
                        } else {
                            logger.info("[ApiUpdateService] Skipped (already exists, win): $srcFile")
                        }
                    } else if (dirName == "whitelist") {
                        Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING)
                        logger.info("[ApiUpdateService] Copied/updated file (whitelist): $srcFile -> $dstFile")
                    }
                }
            }
        } else {
            logger.warn("[ApiUpdateService] Directory api/$dirName not found in archive")
        }
        logger.info("[ApiUpdateService] copyDirectoryFromRepo: end, repoRoot=$repoRoot, dirName=$dirName, dst=$dst, overwrite=$overwrite")
    }

    private fun ensureDirectoriesExist() {
        val dirs = listOf(
            appPathProvider.getWhitelistDir(),
            appPathProvider.getWinDir(),
            appPathProvider.getProfilesDir(),
            appPathProvider.getCustomProfilesDir(),
            appPathProvider.getLocalSotsDir()
        )
        dirs.forEach {
            try {
                Files.createDirectories(it)
            } catch (e: Exception) {
                logger.warn("[ApiUpdateService] Failed to create directory $it: ${e.message}")
            }
        }
    }

    private fun clearMainProfilesDirectory(profilesDir: Path, customProfilesDir: Path) {
        logger.info("[ApiUpdateService] clearMainProfilesDirectory: start, profilesDir=$profilesDir, customProfilesDir=$customProfilesDir")
        try {
            if (Files.exists(profilesDir)) {
                Files.list(profilesDir).use { paths ->
                    paths.forEach { path ->
                        if (Files.isRegularFile(path)) {
                            Files.deleteIfExists(path)
                            logger.info("[ApiUpdateService] Deleted file: $path")
                        } else if (Files.isDirectory(path) && path != customProfilesDir) {
                            deleteDirectoryRecursively(path)
                            logger.info("[ApiUpdateService] Deleted directory: $path")
                        } else if (path == customProfilesDir) {
                            logger.info("[ApiUpdateService] Preserved custom profiles directory: $path")
                        }
                    }
                }
                logger.info("[ApiUpdateService] Main profiles directory cleared successfully")
            } else {
                logger.info("[ApiUpdateService] Main profiles directory does not exist, nothing to clear")
            }
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Error clearing main profiles directory: ${e.message}", e)
        }
        logger.info("[ApiUpdateService] clearMainProfilesDirectory: end")
    }

    private fun deleteDirectoryRecursively(directory: Path) {
        try {
            Files.walk(directory).use { paths ->
                paths.sorted(compareByDescending { it.nameCount }).forEach { path ->
                    Files.deleteIfExists(path)
                }
            }
        } catch (e: Exception) {
            logger.error("[ApiUpdateService] Error deleting directory recursively: $directory, error: ${e.message}", e)
        }
    }

    private fun compareVersionStrings(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}