package util.impl

import domain.model.ProviderInfo
import util.interfaces.IProviderDetector
import util.interfaces.ILogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class IpApiProviderDetector(private val logger: ILogger) : IProviderDetector {
    override suspend fun detectProvider(): Result<ProviderInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = URL("http://ip-api.com/json/").readText()
            val info = Json { ignoreUnknownKeys = true }.decodeFromString(IpApiResponse.serializer(), response)
            if (info.status == "success") {
                Result.success(
                    ProviderInfo(
                        isp = info.isp ?: "Unknown",
                        org = info.org,
                        country = info.country,
                        ip = info.query
                    )
                )
            } else {
                logger.error("[IpApiProviderDetector] API error: ${info.message}")
                Result.failure(Exception("API error: ${info.message}"))
            }
        } catch (e: Exception) {
            logger.error("[IpApiProviderDetector] Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Serializable
    private data class IpApiResponse(
        val status: String,
        val isp: String? = null,
        val org: String? = null,
        val country: String? = null,
        val query: String = "",
        val message: String? = null
    )
} 