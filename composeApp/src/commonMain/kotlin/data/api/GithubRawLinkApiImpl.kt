package data.api

import domain.model.HostsExtendData
import domain.model.ProfilesData
import domain.model.VersionData
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readCsv
import util.interfaces.IAppPathProvider
import util.interfaces.ILogger
import util.path.GithubRawLinks
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class GithubRawLinkApiImpl(
    private val logger: ILogger,
    private val appPathProvider: IAppPathProvider,
    private val json: Json = Json { ignoreUnknownKeys = true },
    client: HttpClient? = null
) : GithubRawLinkApi {

    private val client: HttpClient = client ?: HttpClient(CIO)

    @Serializable
    private data class GithubContentItem(
        val name: String,
        val type: String,
        val path: String? = null,
        @SerialName("download_url") val downloadUrl: String? = null
    )

    /**
     * Загружает список версий из CSV (version_rows.csv) через Kotlin DataFrame.
     * Формат: id, type, version.
     */
    override suspend fun getApiVersionList(): Result<List<VersionData>> = withContext(Dispatchers.IO) {
        try {
            logger.info("[GithubRawLinkApi] Fetching versions CSV via DataFrame from ${GithubRawLinks.VERSIONS_CSV_URL}")
            val df = DataFrame.readCSV(GithubRawLinks.VERSIONS_CSV_URL)

            val list = df.rows().mapNotNull { row ->
                val type = row["type"]?.toString()?.trim().orEmpty()
                val version = row["version"]?.toString()?.trim().orEmpty()
                if (type.isEmpty() || version.isEmpty()) return@mapNotNull null
                VersionData(
                    type = type,
                    version = version,
                    changelog = null
                )
            }

            logger.info("[GithubRawLinkApi] Versions CSV(DataFrame): загружено ${list.size} записей")
            Result.success(list)
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] getApiVersionList (DataFrame) failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Загружает все файлы из папки lists репозитория.
     * Файлы динамически добавляются/удаляются: мы
     *  - обновляем/создаём файлы по именам, что пришли из GitHub;
     *  - удаляем локальные list-/ipset-* файлы, которых больше нет в репозитории.
     */
    override suspend fun getApiLinksList() = withContext(Dispatchers.IO) {
        try {
            logger.info("[GithubRawLinkApi] Fetching lists directory from ${GithubRawLinks.LISTS_CONTENT_URL}")
            val response = client.get(GithubRawLinks.LISTS_CONTENT_URL)
            if (response.status != HttpStatusCode.OK) {
                logger.error("[GithubRawLinkApi] lists HTTP ${response.status}")
                return@withContext
            }

            val body = response.bodyAsText()

            val items = try {
                json.decodeFromString<List<GithubContentItem>>(body)
            } catch (e: Exception) {
                logger.error("[GithubRawLinkApi] Failed to parse lists JSON: ${e.message}", e)
                return@withContext
            }

            val whitelistDir = appPathProvider.getWhitelistDir()
            Files.createDirectories(whitelistDir)

            val remoteNames = mutableSetOf<String>()

            for (item in items) {
                if (item.type != "file" || item.downloadUrl.isNullOrBlank()) continue

                remoteNames += item.name
                val targetPath = whitelistDir.resolve(item.name)

                try {
                    logger.info("[GithubRawLinkApi] Downloading list file ${item.name}")
                    val fileResp = client.get(item.downloadUrl)
                    if (fileResp.status != HttpStatusCode.OK) {
                        logger.error("[GithubRawLinkApi] Failed to download ${item.name}: HTTP ${fileResp.status}")
                        continue
                    }
                    val text = fileResp.bodyAsText()
                    Files.writeString(
                        targetPath,
                        text,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    )
                } catch (e: Exception) {
                    logger.error("[GithubRawLinkApi] Error downloading ${item.name}: ${e.message}", e)
                }
            }

            // Удаляем локальные файлы, которых больше нет в lists
            try {
                Files.list(whitelistDir).use { stream ->
                    stream
                        .filter { Files.isRegularFile(it) }
                        .forEach { path ->
                            val name = path.fileName.toString()
                            val isWhileList = name == "whitelist.txt"
                            //val isManaged = name.startsWith("list-") || name.startsWith("ipset-")
                            //if (isManaged && !remoteNames.contains(name)) {
                            if (!isWhileList && !remoteNames.contains(name)) {
                                logger.info("[GithubRawLinkApi] Deleting obsolete list file $name")
                                Files.deleteIfExists(path)
                            }
                        }
                }
            } catch (e: Exception) {
                logger.error("[GithubRawLinkApi] Error cleaning obsolete list files: ${e.message}", e)
            }
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] getApiLinksList failed: ${e.message}", e)
        }
    }

    /**
     * Загружает папку base/win из репозитория
     * Рекурсивно обходит подпапки; файлы могут добавляться, меняться и удаляться в репозитории.
     */
    override suspend fun getWinFolder() = withContext(Dispatchers.IO) {
        try {
            logger.info("[GithubRawLinkApi] Fetching win folder from ${GithubRawLinks.WIN_FOLDER_CONTENT_URL}")
            val winDir = appPathProvider.getWinDir()
            Files.createDirectories(winDir)
            fetchWinFolderRecursive("base/win", winDir)
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] getWinFolder failed: ${e.message}", e)
        }
    }

    /**
     * Рекурсивно загружает содержимое папки [apiPath] из GitHub и сохраняет в [localDir].
     * Файлы — скачиваются; папки — обход рекурсивно.
     */
    private suspend fun fetchWinFolderRecursive(apiPath: String, localDir: java.nio.file.Path) {
        val url = "${GithubRawLinks.SOTS_ZAPRET_CONTENTS_BASE}/$apiPath?ref=master"
        val response = client.get(url)
        if (response.status != HttpStatusCode.OK) {
            logger.error("[GithubRawLinkApi] Win folder HTTP ${response.status}: $apiPath")
            return
        }
        val body = response.bodyAsText()
        val items = try {
            json.decodeFromString<List<GithubContentItem>>(body)
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] Failed to parse win folder JSON ($apiPath): ${e.message}", e)
            return
        }
        val remoteNames = mutableSetOf<String>()
        for (item in items) {
            remoteNames += item.name
            when (item.type) {
                "file" -> {
                    val downloadUrl = item.downloadUrl
                    val displayName = item.path ?: item.name
                    if (downloadUrl.isNullOrBlank()) {
                        logger.info("[GithubRawLinkApi] win: $displayName — skipped (not found URL)")
                        continue
                    }
                    val targetPath = localDir.resolve(item.name)
                    try {
                        val fileResp = client.get(downloadUrl)
                        if (fileResp.status != HttpStatusCode.OK) {
                            logger.info("[GithubRawLinkApi] win: $displayName — skipped (HTTP ${fileResp.status})")
                            continue
                        }
                        val bytes = fileResp.bodyAsChannel().toInputStream().readBytes()
                        Files.createDirectories(targetPath.parent)
                        Files.write(
                            targetPath,
                            bytes,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                        )
                        logger.info("[GithubRawLinkApi] win: $displayName — loaded|updated")
                    } catch (e: Exception) {
                        val isBusy = e is FileSystemException ||
                            e.message?.contains("занят", ignoreCase = true) == true ||
                            e.message?.contains("being used", ignoreCase = true) == true ||
                            e.message?.contains("used by another", ignoreCase = true) == true
                        if (isBusy) {
                            logger.warn("[GithubRawLinkApi] win: $displayName — skipped-locked: ${e.message}")
                        } else {
                            logger.error("[GithubRawLinkApi] win: $displayName — skipped: ${e.message}", e)
                        }
                    }
                }
                "dir" -> {
                    try {
                        val subPath = if (apiPath.endsWith("/")) "$apiPath${item.name}" else "$apiPath/${item.name}"
                        val subLocal = localDir.resolve(item.name)
                        Files.createDirectories(subLocal)
                        fetchWinFolderRecursive(subPath, subLocal)
                    } catch (e: Exception) {
                        logger.error("[GithubRawLinkApi] win: ${item.path ?: item.name}/ — error while traversing folder: ${e.message}", e)
                    }
                }
                else -> { /* skip */ }
            }
        }
        // Удаляем локальные файлы/папки, которых больше нет в репозитории
        try {
            if (!Files.exists(localDir)) return
            Files.list(localDir).use { stream ->
                stream.forEach { path ->
                    val name = path.fileName.toString()
                    if (!remoteNames.contains(name)) {
                        logger.info("[GithubRawLinkApi] Deleting obsolete win entry: $name")
                        path.toFile().deleteRecursively()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] Error cleaning obsolete win files: ${e.message}", e)
        }
    }

    /**
     * Загружает профили из CSV (profiles_rows.csv).
     * Формат: id, content, name, provider, service.
     */
    override suspend fun getProfilesList(): Result<List<ProfilesData>> = withContext(Dispatchers.IO) {
        try {
            logger.info("[GithubRawLinkApi] Fetching profiles CSV - ${GithubRawLinks.PROFILES_CSV_URL}")
            val df = DataFrame.readCSV(GithubRawLinks.PROFILES_CSV_URL)

            val list = df.rows().mapNotNull { row ->
                val id = row["id"]?.toString()?.trim()?.toIntOrNull() ?: return@mapNotNull null
                val name = row["name"]?.toString()?.trim().orEmpty()
                val content = row["content"]?.toString()?.trim().orEmpty()
                val provider = row["provider"]?.toString()?.trim()?.toIntOrNull() ?: 0
                val service = row["service"]?.toString()?.trim()?.toIntOrNull() ?: 0
                ProfilesData(
                    id = id,
                    name = name,
                    content = content,
                    service = service,
                    provider = provider
                )
            }

            logger.info("[GithubRawLinkApi] Profiles CSV: загружено ${list.size} профилей")
            Result.success(list)
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] getProfilesList failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getHostsList(): Result<List<HostsExtendData>> = withContext(Dispatchers.IO) {
        try {
            logger.info("[GithubRawLinkApi] Fetching hosts from ${GithubRawLinks.HOSTS_URL}")
            val response = client.get(GithubRawLinks.HOSTS_URL)
            if (response.status != HttpStatusCode.OK) {
                logger.error("[GithubRawLinkApi] HTTP ${response.status}")
                return@withContext Result.failure(Exception("HTTP ${response.status}"))
            }

            val list = response.bodyAsText().lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapIndexed { index, line -> HostsExtendData(id = index + 1, content = line) }

            Result.success(list)
        } catch (e: Exception) {
            logger.error("[GithubRawLinkApi] getHostsList failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
