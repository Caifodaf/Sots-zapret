package util.impl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ILogger

class ZipDownloader : KoinComponent {
    private val logger: ILogger by inject()

    suspend fun downloadAndUnzip(url: String, destDir: Path): Result<Path> = withContext(Dispatchers.IO) {
        val client = HttpClient(CIO)
        val tempZip = Files.createTempFile("sots_api_update", ".zip")
        try {
            client.use {
                val response = it.get(url)
                val bytes = response.bodyAsChannel().toInputStream().readBytes()
                Files.write(tempZip, bytes)
            }
            val unzipDir = Files.createTempDirectory(destDir, "unzipped_")
            val entries = mutableListOf<String>()
            ZipInputStream(Files.newInputStream(tempZip)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entries.add(entry.name + if (entry.isDirectory) "/" else "")
                    val newFile = unzipDir.resolve(entry.name)
                    if (entry.isDirectory) {
                        Files.createDirectories(newFile)
                    } else {
                        Files.createDirectories(newFile.parent)
                        Files.newOutputStream(newFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            logger.info("[ZipDownloader] Archive contents:")
            entries.forEach { logger.info("[ZipDownloader] $it") }
            Files.deleteIfExists(tempZip)
            Result.success(unzipDir)
        } catch (e: Exception) {
            Files.deleteIfExists(tempZip)
            Result.failure(e)
        }
    }
} 