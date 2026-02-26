package data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import util.interfaces.IAppPathProvider
import util.interfaces.ILogger
import util.path.GithubRawLinks
import util.path.NamespaceProject.APP_VERSION
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class AppInstallerDownloadServiceImpl(
    private val logger: ILogger,
    private val appPathProvider: IAppPathProvider,
    private val json: Json = Json { ignoreUnknownKeys = true },
    client: HttpClient? = null
) : AppInstallerDownloadService {

    private val client: HttpClient = client ?: HttpClient(CIO) {
        install(HttpTimeout) {
            //  таймауты для загрузки exe-файла
            requestTimeoutMillis = 3 * 60_000L 
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = 3 * 60_000L
        }
    }

    @Serializable
    private data class GithubReleaseAsset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String
    )

    @Serializable
    private data class GithubRelease(
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val assets: List<GithubReleaseAsset> = emptyList()
    )

    override suspend fun downloadAndRunLatestInstaller(): AppInstallerDownloadResult = withContext(Dispatchers.IO) {
        try {
            logger.info("[AppInstallerDownloadService] Requesting latest release from ${GithubRawLinks.LATEST_RELEASE_API_URL}")

            val response = client.get(GithubRawLinks.LATEST_RELEASE_API_URL) {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "Sots/$APP_VERSION")
            }

            if (response.status != HttpStatusCode.OK) {
                val body = response.bodyAsText()
                logger.error("[AppInstallerDownloadService] GitHub latest release HTTP ${response.status}: $body")
                return@withContext AppInstallerDownloadResult.Error("GitHub API error: ${response.status.value}")
            }

            val body = response.bodyAsText()
            val release = try {
                json.decodeFromString<GithubRelease>(body)
            } catch (e: Exception) {
                logger.error("[AppInstallerDownloadService] Failed to parse release JSON: ${e.message}", e)
                return@withContext AppInstallerDownloadResult.Error("Failed to parse GitHub release information")
            }

            val pattern = Regex("""^sots_\d+\.\d+\.\d+_installer\.exe$""", RegexOption.IGNORE_CASE)
            val asset = release.assets.firstOrNull { pattern.matches(it.name) }

            if (asset == null) {
                logger.error("[AppInstallerDownloadService] Installer asset not found in latest release")
                return@withContext AppInstallerDownloadResult.Error("Installer file not found in latest release")
            }

            val tempDir: Path = appPathProvider.getTempDir()
            Files.createDirectories(tempDir)
            val installerPath = tempDir.resolve(asset.name)

            logger.info("[AppInstallerDownloadService] Downloading installer ${asset.name} to $installerPath from ${asset.browserDownloadUrl}")
            val fileResp = client.get(asset.browserDownloadUrl) {
                header("User-Agent", "Sots/$APP_VERSION")
            }

            if (fileResp.status != HttpStatusCode.OK) {
                logger.error("[AppInstallerDownloadService] Installer download HTTP ${fileResp.status}")
                return@withContext AppInstallerDownloadResult.Error("Failed to download installer: HTTP ${fileResp.status.value}")
            }

            val channel = fileResp.bodyAsChannel()
            channel.toInputStream().use { input ->
                Files.newOutputStream(
                    installerPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                ).use { output ->
                    input.copyTo(output)
                }
            }

            logger.info("[AppInstallerDownloadService] Installer downloaded successfully, starting process: $installerPath")

            try {
                ProcessBuilder(installerPath.toString())
                    .inheritIO()
                    .start()
                logger.info("[AppInstallerDownloadService] Installer process started")
            } catch (e: Exception) {
                logger.error("[AppInstallerDownloadService] Failed to start installer: ${e.message}", e)
                return@withContext AppInstallerDownloadResult.Error("Installer downloaded, but failed to start: ${e.message}")
            }

            AppInstallerDownloadResult.Success(installerPath)
        } catch (e: Exception) {
            logger.error("[AppInstallerDownloadService] Unexpected error: ${e.message}", e)
            AppInstallerDownloadResult.Error(e.message ?: "Unknown error")
        }
    }
}

