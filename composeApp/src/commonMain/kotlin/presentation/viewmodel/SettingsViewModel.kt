package presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import domain.model.Profile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import util.interfaces.ILogger
import java.util.Locale
import util.interfaces.ISystemThemeProvider
import kotlinx.coroutines.runBlocking
import util.interfaces.IStartupManager

@Serializable
data class Settings(
    val selectedProfileFileName: String? = null,
    val profileParams: String? = null,
    val language: LanqSelect = LanqSelect.RU,
    val theme: ThemeSelect = ThemeSelect.Dark,
    val isInfoReadable: Boolean = false,
    val apiVersion: String? = null,
    val isStartupSystem: Boolean = false,
    val isVisibilityTray: Boolean = false
)

enum class ThemeSelect {
    Light,
    Dark,
    DarkOLED
}

enum class LanqSelect {
    RU, EN, UK
}

class SettingsViewModel(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val startupManager: IStartupManager
) : KoinComponent {
    private val logger: ILogger by inject()
    private val systemThemeProvider: ISystemThemeProvider by inject()
    private val settingsFile = Path.of(System.getenv("LOCALAPPDATA"), "Sots", "settings.json")
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSettingsSync()
    }

    fun checkFirstStart(): Boolean = _settings.value.isInfoReadable

    fun getCurrentLanguage(): LanqSelect = _settings.value.language

    fun getCurrentTheme(): ThemeSelect = _settings.value.theme

    private fun loadSettingsSync() {
        try {
            if (Files.exists(settingsFile)) {
                val settingsJson = Files.readString(settingsFile)
                _settings.value = json.decodeFromString(settingsJson)
            } else {
                runBlocking { getLocalStyleInSystem() }
                setStartupSystemSync(true)
                setVisibilityTraySync(true)
                saveSettingsSync()
            }
        } catch (e: Exception) {
            logger.error("[SettingsViewModel] Error loading settings: ${e.message}")
            _errorMessage.value = "Error loading settings: ${e.message}"
        }
    }

    private fun saveSettingsSync() {
        try {
            val settingsJson = json.encodeToString(_settings.value)
            Files.createDirectories(settingsFile.parent)
            Files.writeString(
                settingsFile,
                settingsJson,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            logger.info("[SettingsViewModel] Settings saved successfully")
        } catch (e: Exception) {
            logger.error("[SettingsViewModel] Error saving settings: ${e.message}")
            _errorMessage.value = " Error saving settings: ${e.message}"
        }
    }

    private fun saveSettings() {
        coroutineScope.launch {
            try {
                val settingsJson = json.encodeToString(_settings.value)
                Files.createDirectories(settingsFile.parent)
                Files.writeString(
                    settingsFile,
                    settingsJson,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
            } catch (e: Exception) {
                logger.error("[SettingsViewModel] Error saving settings: ${e.message}")
                _errorMessage.value = " Error saving settings: ${e.message}"
            }
        }
    }

    fun saveReadingInfoStatus() {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(
                isInfoReadable = true
            )
            saveSettings()
        }
    }

    fun saveSelectedProfile(profile: Profile?) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(
                selectedProfileFileName = profile?.fileName
            )
            saveSettings()
        }
    }

    fun getSelectedProfileFileName(): String? = _settings.value.selectedProfileFileName

    fun setLanguage(language: LanqSelect) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(language = language)
            saveSettings()
            logger.info("[SettingsViewModel] Language changed to: $language")
        }
    }

    // Заготовки для будущей реализации смены темы
    fun setTheme(theme: ThemeSelect) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(theme = theme)
            saveSettings()
            // TODO: Реализовать смену темы
            logger.info("[SettingsViewModel] Theme changed to: $theme")
        }
    }

    // --- Работа с версией API ---
    fun getApiVersion(): String? = _settings.value.apiVersion

    fun saveApiVersionSuspend(version: String) {
        _settings.value = _settings.value.copy(apiVersion = version)
        saveSettingsSync()
    }

    private  fun getLocalStyleInSystem(){
        _settings.value = _settings.value.copy(language = getLocalLanguageInSystem())
        // TODO: Debug close
        //_settings.value = _settings.value.copy(theme = getLocalThemeInSystem())
    }

    private fun getLocalLanguageInSystem():LanqSelect {
        val systemLang = Locale.getDefault().language
        logger.debug("[SettingsViewModel] Get local lanq is: $systemLang")
        return when(systemLang){
            "ru" -> LanqSelect.RU
            "en" -> LanqSelect.EN
            "uk" -> LanqSelect.RU
            else -> LanqSelect.RU
        }
    }

    private suspend fun getLocalThemeInSystem(): ThemeSelect {
        return systemThemeProvider.getSystemTheme()
    }

    fun setStartupSystem(enabled: Boolean) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(isStartupSystem = enabled)
            saveSettings()
            logger.info("[SettingsViewModel] StartupSystem changed to: $enabled")
            startupManager.setStartupEnabled(enabled)
        }
    }

    fun setVisibilityTray(enabled: Boolean) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(isVisibilityTray = enabled)
            saveSettings()
            logger.info("[SettingsViewModel] VisibilityTray changed to: $enabled")
        }
    }

    fun saveSelectedProfileParams(profileName: String?, params: String?) {
        coroutineScope.launch {
            _settings.value = _settings.value.copy(
                selectedProfileFileName = profileName,
                profileParams = params
            )
            saveSettings()
        }
    }

    // Синхронные версии для первого запуска
    private fun setStartupSystemSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(isStartupSystem = enabled)
        saveSettingsSync()
        logger.info("[SettingsViewModel] StartupSystem changed to: $enabled (sync)")
        runBlocking { startupManager.setStartupEnabled(enabled) }
    }

    private fun setVisibilityTraySync(enabled: Boolean) {
        _settings.value = _settings.value.copy(isVisibilityTray = enabled)
        saveSettingsSync()
        logger.info("[SettingsViewModel] VisibilityTray changed to: $enabled (sync)")
    }
}