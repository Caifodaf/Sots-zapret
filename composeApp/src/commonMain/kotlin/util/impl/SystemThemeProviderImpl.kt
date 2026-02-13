package util.impl

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ISystemThemeProvider
import util.interfaces.ILogger
import util.interfaces.ISystemScriptService
import presentation.viewmodel.ThemeSelect
import kotlin.getValue

class SystemThemeProviderImpl() : ISystemThemeProvider, KoinComponent {
    private val logger: ILogger by inject()
    private val systemScriptService: ISystemScriptService by inject()

    override suspend fun getSystemTheme(): ThemeSelect {
        return when {
            isWindows() -> getWindowsTheme()
            else -> ThemeSelect.Light
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    private suspend fun getWindowsTheme(): ThemeSelect {
        return try {
            val result = systemScriptService.getWindowsTheme()
            logger.info("[SystemThemeProvider] Windows theme script result: $result")
            if (result.isNullOrEmpty()) {
                logger.error("[SystemThemeProvider] Script did not return a result, returning Light")
                return ThemeSelect.Light
            }
            val normalized = result.trim().lowercase()
            return when (normalized) {
                "dark" -> ThemeSelect.Dark
                "light" -> ThemeSelect.Light
                else -> {
                    logger.error("[SystemThemeProvider] Unexpected script result: $result. Returning Light.")
                    ThemeSelect.Light
                }
            }
        } catch (e: Exception) {
            logger.error("[SystemThemeProvider] Error determining Windows theme: ${e.message}")
            ThemeSelect.Light
        }
    }
} 