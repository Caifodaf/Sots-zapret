package data.service

import domain.WinwsLauncherService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ILogger
import util.interfaces.IAppPathProvider
import java.nio.file.Files

class WinwsLauncherServiceImpl : WinwsLauncherService, KoinComponent {
    private val logger: ILogger by inject()
    private val appPathProvider: IAppPathProvider by inject()
    private val profileAdapterService: ProfileAdapterService by inject()

    override suspend fun prepareProfileArgs(selectedProfileFileName: String, adaptProfile: Boolean): String {
        logger.info("[WinwsLauncherService] Preparing profile args for: $selectedProfileFileName, adaptProfile=$adaptProfile")
        
        val mainProfileFileTxt = appPathProvider.getProfilesDir().resolve("$selectedProfileFileName.txt")
        val customProfileFileTxt = appPathProvider.getCustomProfilesDir().resolve("$selectedProfileFileName.txt")
        val customProfileFileBat = appPathProvider.getCustomProfilesDir().resolve("$selectedProfileFileName.bat")
        
        val profileFile = when {
            Files.exists(mainProfileFileTxt) -> mainProfileFileTxt
            Files.exists(customProfileFileTxt) -> customProfileFileTxt
            Files.exists(customProfileFileBat) -> customProfileFileBat
            else -> throw Exception("Profile file not found: $selectedProfileFileName (checked .txt and .bat)")
        }
        
        logger.info("[WinwsLauncherService] Using profile file: $profileFile")
        
        val originalContent = Files.readAllLines(profileFile).joinToString("\n")
        
        val processedContent = if (adaptProfile) {
            profileAdapterService.adaptProfile(originalContent, removeWinwsPath = true)
        } else {
            originalContent
        }
        
        val args = processedContent.replace("^", "").replace("\n", " ").trim()
        logger.info("[WinwsLauncherService] Prepared args for $selectedProfileFileName: ${args.take(100)}...")
        
        return args
    }

    override fun getWinwsPath(): String {
        val winwsPath = appPathProvider.getWinDir().resolve("winws.exe").toString()
        if (winwsPath.isBlank()) {
            logger.error("[WinwsLauncherService] Winws.exe path is blank")
            throw Exception("Failed to get path to winws.exe")
        }
        return winwsPath
    }

    override fun validateArgs(args: String): Boolean {
        val isValid = !args.any { it == ';' || it == '|' }
        if (!isValid) {
            logger.error("[WinwsLauncherService] Invalid characters in launch arguments: $args")
        }
        return isValid
    }
}
