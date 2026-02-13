package data.repo

import util.path.PathFilesProject.GENERAL_FILE_NAME
import domain.repository.WhitelistManager
import java.nio.file.Files
import java.nio.file.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.IAppPathProvider
import util.interfaces.ILogger
import util.path.PathFilesProject.WHITELIST_FILE_NAME
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import java.net.URL

class WhitelistManagerImpl(
    private val appPathProvider: IAppPathProvider,
) : WhitelistManager, KoinComponent {

    private val logger: ILogger by inject()

    private fun getLocalSotsWhitelistPath() = appPathProvider.getWhitelistDir()
    private fun getMainWhitelistFile(): Path = getLocalSotsWhitelistPath().resolve(GENERAL_FILE_NAME)
    private fun getUserLinksFile(): Path = getLocalSotsWhitelistPath().resolve(WHITELIST_FILE_NAME)

    private val urlPattern: Pattern = Pattern.compile(
        """^((https?://)?([a-zA-Z0-9-]+\.)+[a-zA-Z0-9-]{2,}(/\S*)?)$""",
        Pattern.CASE_INSENSITIVE
    )

    private val domainPattern: Pattern = Pattern.compile(
        """^([a-zA-Z0-9-]+\.)+[a-zA-Z0-9-]{2,}$""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Извлекает домен из URL
     * @param url полная ссылка или домен
     * @return домен в формате example.com
     */
    private fun extractDomain(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        
        return try {
            if (domainPattern.matcher(trimmed).matches()) {
                return trimmed.lowercase()
            }
            
            // Если это полная ссылка, извлекаем домен
            if (urlPattern.matcher(trimmed).matches()) {
                val urlObj = URL(if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed)
                urlObj.host.lowercase()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Error extracting domain from: $trimmed", e)
            null
        }
    }

    private fun isValidLink(link: String): Boolean {
        val trimmed = link.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.length > 126) return false
        
        return domainPattern.matcher(trimmed).matches() || urlPattern.matcher(trimmed).matches()
    }

    private fun readLinesSafe(file: Path): List<String> = try {
        if (Files.exists(file)) Files.readAllLines(file).map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
    } catch (e: Exception) {
        logger.error("Error reading file: ${file.fileName}", e)
        emptyList()
    }

    private fun writeLinesAtomic(file: Path, lines: List<String>): Boolean {
        return try {
            val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
            Files.write(tmp, lines)
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            true
        } catch (e: Exception) {
            logger.error("Error writing file: ${file.fileName}", e)
            false
        }
    }

    override fun addNewLink(newLink: String): Result<Unit> {
        val trimmed = newLink.trim()
        if (!isValidLink(trimmed)) {
            logger.info("Invalid link format: $trimmed")
            return Result.failure(Exception("Invalid link format"))
        }
        
        // Извлекаем домен из ссылки
        val domain = extractDomain(trimmed)
        if (domain == null) {
            logger.info("Could not extract domain from: $trimmed")
            return Result.failure(Exception("Could not extract domain from link"))
        }
        
        val mainFile = getMainWhitelistFile()
        val userFile = getUserLinksFile()
        val mainLinks = readLinesSafe(mainFile)
        val userLinks = readLinesSafe(userFile)
        
        if (mainLinks.contains(domain)) {
            logger.info("Domain already exists in main whitelist: $domain")
            return Result.failure(Exception("exists in general list"))
        }
        if (userLinks.contains(domain)) {
            logger.info("Domain already exists in user links: $domain")
            return Result.failure(Exception("Domain already exists in user links"))
        }
        
        val newUserLinks = userLinks + domain
        if (!writeLinesAtomic(userFile, newUserLinks)) {
            return Result.failure(Exception("Error writing user file"))
        }
        
        val newMainLinks = mainLinks + domain
        if (!writeLinesAtomic(mainFile, newMainLinks)) {
            writeLinesAtomic(userFile, userLinks)
            return Result.failure(Exception("Error writing main file"))
        }
        
        logger.info("Added new domain to user and main whitelist: $domain (from: $trimmed)")
        return Result.success(Unit)
    }

    override fun deleteLink(link: String): Result<Unit> {
        val trimmed = link.trim()
        
        val domain = extractDomain(trimmed) ?: trimmed
        
        val mainFile = getMainWhitelistFile()
        val userFile = getUserLinksFile()
        val mainLinks = readLinesSafe(mainFile)
        val userLinks = readLinesSafe(userFile)
        
        val newUserLinks = userLinks.filter { it != domain }
        val newMainLinks = mainLinks.filter { it != domain }
        
        val userOk = writeLinesAtomic(userFile, newUserLinks)
        val mainOk = writeLinesAtomic(mainFile, newMainLinks)
        
        if (userOk && mainOk) {
            logger.info("Deleted domain from user and main whitelist: $domain")
            return Result.success(Unit)
        } else {
            logger.error("Error deleting domain: $domain")
            return Result.failure(Exception("Error deleting domain"))
        }
    }

    override fun getWhiteList(): List<String> {
        val userFile = getUserLinksFile()
        return readLinesSafe(userFile)
    }

    override fun saveWhitelist(links: List<String>): Result<Unit> {
        // Сохраняет только в пользовательский файл (для совместимости)
        val userFile = getUserLinksFile()
        val filtered = links.mapNotNull { extractDomain(it.trim()) }.distinct()
        return if (writeLinesAtomic(userFile, filtered)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Error saving user file"))
        }
    }

    override fun mergeWhiteList(local: List<String>, api: List<String>): List<String> {
        return (local + api).mapNotNull { extractDomain(it.trim()) }.distinct()
    }
}