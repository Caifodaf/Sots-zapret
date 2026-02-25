package data.service

import data.api.GithubRawLinkApi
import domain.HostsUpdateService
import domain.HostsUpdateResult
import data.api.deprecated.SupabaseStorageApi
import util.interfaces.ILogger
import util.interfaces.ISystemScriptService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HostsUpdateServiceImpl(
    private val githubRawLinkApi: GithubRawLinkApi,
    private val logger: ILogger
) : HostsUpdateService, KoinComponent {
    private val systemScriptService: ISystemScriptService by inject()

    companion object {
        private const val HOSTS_FILE_PATH = "C:\\Windows\\System32\\drivers\\etc\\hosts"
        private const val SOTS_HEADER = "# Sots"
        private const val SOTS_HEADER_SECOND = "# ip for Discord by Flowseal"
        private const val SOTS_FOOTER = "# end Sots"
    }

    override suspend fun updateHostsList(): HostsUpdateResult = withContext(Dispatchers.IO) {
        try {
            val isAdmin = systemScriptService.isAdmin()
            if (!isAdmin) {
                logger.error("[HostsUpdateService] Admin rights required to update hosts file")
                return@withContext HostsUpdateResult.Error("Admin rights required to update hosts file")
            }

            logger.info("[HostsUpdateService] Fetching hosts_extend list from API")
            val apiResult = githubRawLinkApi.getHostsList()
            
            if (apiResult.isFailure) {
                val error = apiResult.exceptionOrNull()?.message ?: "Unknown error"
                logger.error("[HostsUpdateService] Failed to fetch hosts_extend: $error")
                return@withContext HostsUpdateResult.Error("Error fetching list: $error")
            }

            val hostsExtendList = apiResult.getOrNull() ?: emptyList()
            if (hostsExtendList.isEmpty()) {
                logger.warn("[HostsUpdateService] hosts_extend list is empty")
                return@withContext HostsUpdateResult.Error("List is empty")
            }

            val allLinks = hostsExtendList.flatMap { entry ->
                entry.content.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }.distinct()

            logger.info("[HostsUpdateService] Found ${allLinks.size} unique links to add")

            val hostsPath = Paths.get(HOSTS_FILE_PATH)
            if (!Files.exists(hostsPath)) {
                logger.error("[HostsUpdateService] Hosts file not found: $HOSTS_FILE_PATH")
                return@withContext HostsUpdateResult.Error("Hosts file not found")
            }

            val currentLines = Files.readAllLines(hostsPath)
            logger.info("[HostsUpdateService] Read ${currentLines.size} lines from hosts file")

            val (beforeBlock, sotsBlock, afterBlock) = extractSotsBlock(currentLines)
            
            val currentLinks = extractLinksFromSotsBlock(sotsBlock)
            
            val currentLinksSet = currentLinks.toSet()
            val newLinksSet = allLinks.toSet()
            
            if (currentLinksSet == newLinksSet && sotsBlock.isNotEmpty()) {
                logger.info("[HostsUpdateService] Hosts list is already up to date")
                return@withContext HostsUpdateResult.AlreadyUpdated
            }
            
            logger.info("[HostsUpdateService] Current links: ${currentLinksSet.size}, New links: ${newLinksSet.size}")
            
            val newSotsBlock = buildSotsBlock(allLinks)

            val updatedLines = mutableListOf<String>()
            updatedLines.addAll(beforeBlock)
            updatedLines.addAll(newSotsBlock)
            updatedLines.addAll(afterBlock)

            Files.write(hostsPath, updatedLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            logger.info("[HostsUpdateService] Successfully updated hosts file")

            return@withContext HostsUpdateResult.Success
        } catch (e: Exception) {
            logger.error("[HostsUpdateService] Exception during hosts update: ${e.message}", e)
            return@withContext HostsUpdateResult.Error("Error updating: ${e.message}")
        }
    }

    private fun extractSotsBlock(lines: List<String>): Triple<List<String>, List<String>, List<String>> {
        var startIndex = -1
        var endIndex = -1

        for (i in lines.indices) {
            if (lines[i].trim() == SOTS_HEADER) {
                startIndex = i
                break
            }
        }

        if (startIndex == -1) {
            return Triple(lines, emptyList(), emptyList())
        }

        for (i in startIndex + 1 until lines.size) {
            if (lines[i].trim() == SOTS_FOOTER) {
                endIndex = i
                break
            }
        }

        if (endIndex == -1) {
            val before = lines.subList(0, startIndex)
            val block = lines.subList(startIndex, lines.size)
            return Triple(before, block, emptyList())
        }

        val before = lines.subList(0, startIndex)
        val block = lines.subList(startIndex, endIndex + 1)
        val after = if (endIndex < lines.size - 1) lines.subList(endIndex + 1, lines.size) else emptyList()

        return Triple(before, block, after)
    }

    private fun extractLinksFromSotsBlock(block: List<String>): List<String> {
        if (block.isEmpty()) return emptyList()
        
        val links = mutableListOf<String>()
        var inBlock = false
        
        for (line in block) {
            val trimmed = line.trim()
            when {
                trimmed == SOTS_HEADER -> inBlock = true
                trimmed == SOTS_FOOTER -> break
                inBlock && trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    links.add(trimmed)
                }
            }
        }
        
        return links
    }

    private fun buildSotsBlock(links: List<String>): List<String> {
        val block = mutableListOf<String>()
        block.add(SOTS_HEADER)
        block.add(SOTS_HEADER_SECOND)
        block.add("")
        block.addAll(links)
        block.add("")
        block.add(SOTS_FOOTER)
        return block
    }
}

