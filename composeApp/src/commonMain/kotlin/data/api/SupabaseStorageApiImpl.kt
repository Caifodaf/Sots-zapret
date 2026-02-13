package data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import util.path.PathFilesProject
import domain.model.ProfilesData
import domain.model.VersionData
import domain.model.HostsExtendData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import domain.model.BugReportData
import io.ktor.http.contentType
import util.interfaces.ILogger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.put
import io.ktor.serialization.kotlinx.json.json

class SupabaseStorageApiImpl(
    private val logger: ILogger,
    private val json: Json = Json { ignoreUnknownKeys = true },
    client: HttpClient? = null
) : SupabaseStorageApi {
    private val client: HttpClient = client ?: HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private suspend inline fun <reified T> fetchTable(tableUrl: String): Result<List<T>> = try {
        val response = client.get(tableUrl) {
            url { parameters.append("select", "*") }
            header("Authorization", "Bearer ${PathFilesProject.API_BASE_KEY}")
            header("apikey", PathFilesProject.API_BASE_KEY)
            header("Accept", "application/json")
        }
        if (response.status == HttpStatusCode.Companion.OK) {
            val body = response.bodyAsText()
            Result.success(json.decodeFromString(body))
        } else {
            Result.failure(Exception("HTTP ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getApiVersionList(): Result<List<VersionData>> =
        fetchTable(PathFilesProject.API_BASE_URL + PathFilesProject.VERSION_TABLE_NAME)

    override suspend fun getProfilesList(): Result<List<ProfilesData>> =
        fetchTable(PathFilesProject.API_BASE_URL + PathFilesProject.PROFILE_TABLE_NAME)

    override suspend fun getHostsExtendList(): Result<List<HostsExtendData>> =
        fetchTable(PathFilesProject.API_BASE_URL + PathFilesProject.HOSTS_EXTEND_TABLE_NAME)

    override suspend fun sendBugReport(report: BugReportData): Result<Unit> = try {
        logger.info("[SupabaseStorageApi] Sending bug report: name=${report.name}, content.length=${report.content.length}, id_user=${report.id_user}")
        val response = client.post(PathFilesProject.API_BASE_URL + PathFilesProject.BUG_REPORT_TABLE_NAME) {
            url(PathFilesProject.API_BASE_URL + PathFilesProject.BUG_REPORT_TABLE_NAME)
            header("Authorization", "Bearer ${PathFilesProject.API_BASE_KEY}")
            header("apikey", PathFilesProject.API_BASE_KEY)
            contentType(ContentType.Application.Json)
            setBody(report)
        }
        if (response.status.value in 200..299) {
            logger.info("[SupabaseStorageApi] Bug report sent successfully. Status: ${response.status}")
            Result.success(Unit)
        } else {
            logger.error("[SupabaseStorageApi] Bug report failed. HTTP ${response.status}")
            Result.failure(Exception("HTTP ${response.status}"))
        }
    } catch (e: Exception) {
        logger.error("[SupabaseStorageApi] Exception sending bug report: ${e.message}", e)
        Result.failure(e)
    }

    }