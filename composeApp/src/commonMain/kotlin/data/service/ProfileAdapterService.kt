package data.service

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ILogger
import util.interfaces.IAppPathProvider


class ProfileAdapterService : KoinComponent {
    private val logger: ILogger by inject()
    private val appPathProvider: IAppPathProvider by inject()

    /**
     * Адаптирует профиль под стандарт:
     * 1. Находит строку с "%BIN%winws.exe" и удаляет все что было до нее (опционально)
     * 2. Заменяет переменные
     * 3. Удаляет путь к winws.exe из начала файла (опционально)
     * 
     * @param originalContent исходное содержимое профиля
     * @param removeWinwsPath если true, удаляет путь к winws.exe из начала (для адаптации перед запуском)
     * @return обработанное содержимое профиля
     */
    fun adaptProfile(originalContent: String, removeWinwsPath: Boolean = false): String {
        logger.info("[ProfileAdapterService] adaptProfile: start, removeWinwsPath=$removeWinwsPath")
        
        var processedContent = originalContent
        
        if (removeWinwsPath) {
            val possibleTargets = listOf(
                "\"%BIN%winws.exe\" ",
                "\"%BIN%winws.exe\"",
                "%BIN%winws.exe ",
                "%BIN%winws.exe"
            )
            
            var targetIndex = -1
            var foundTarget: String? = null
            
            for (target in possibleTargets) {
                val index = processedContent.indexOf(target)
                if (index >= 0) {
                    targetIndex = index
                    foundTarget = target
                    break
                }
            }
            
            if (targetIndex >= 0 && foundTarget != null) {
                logger.info("[ProfileAdapterService] Found target string \"$foundTarget\" at index: $targetIndex")
                processedContent = processedContent.substring(targetIndex)
            } else {
                logger.warn("[ProfileAdapterService] Target string with \"%BIN%winws.exe\" not found, using original content")
            }
        }
        
        val winDir = appPathProvider.getWinDir().toString().replace("/", "\\")
        val winDirTLS = appPathProvider.getWinDirTLS().toString().replace("/", "\\")
        val listDirPath = appPathProvider.getWhitelistDir().toString().replace("/", "\\")
        val gameFilter = appPathProvider.getGameFilter()
        
        processedContent = processedContent
            .replace("%LISTS%", "$listDirPath\\")
            .replace("%LIST_PATH%", "$listDirPath\\")
            .replace("%BIN%", "$winDir\\")
            .replace("%IANA%", winDirTLS)
            .replace("%GameFilter%", gameFilter)
        
        if (removeWinwsPath) {
            val winwsExeRegex = Regex("^(\"[^\"]*winws\\.exe\"|\\S*winws\\.exe)\\s*", RegexOption.IGNORE_CASE)
            val beforeRemoval = processedContent
            processedContent = processedContent.replace(winwsExeRegex, "").trimStart()
            
            if (processedContent != beforeRemoval) {
                logger.info("[ProfileAdapterService] Removed winws.exe path from beginning")
            }
        }
        
        logger.info("[ProfileAdapterService] adaptProfile: end")
        return processedContent
    }
}

