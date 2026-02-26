package presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import domain.repository.WhitelistManager
import domain.ServiceManager
import util.interfaces.ILogger
import domain.model.Profile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import data.checker.ProfileNetworkChecker
import data.checker.ProfileCheckResult
import data.checker.ServiceTargets
import kotlinx.coroutines.Job
import data.api.ApiDownloadResult
import data.api.ApiMergeResult
import data.api.ApiUpdateService
import data.api.GithubRawLinkApi
import data.api.ProfilesDownloadResult
import data.api.AppInstallerDownloadService
import data.api.AppInstallerDownloadResult
import domain.repository.ProfileService
import util.onFailureLog
import util.path.NamespaceProject.APP_VERSION
import data.api.deprecated.SupabaseStorageApi
import domain.model.BugReportData
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import data.checker.IProfileCheckLogWriter
import data.checker.ProfileCheckLogWriterImpl
import data.service.ProfileAdapterService
import util.interfaces.IAppPathProvider
import util.interfaces.ISystemScriptService
import util.interfaces.IProviderDetector
import domain.model.ProviderInfo
import kotlinx.coroutines.CancellationException
import kotlin.system.exitProcess

class RepositoryViewModel(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val vmSettings: SettingsViewModel,
    private val appPathProvider: IAppPathProvider,
    private val systemScriptService: ISystemScriptService,
    private val providerDetector: IProviderDetector,
    private val profileCheckLogWriter: IProfileCheckLogWriter = ProfileCheckLogWriterImpl(appPathProvider),
    private val profileAdapterService : ProfileAdapterService
): KoinComponent {
    private val whitelistManager: WhitelistManager by inject()
    private val settingsViewModel: SettingsViewModel by inject()
    private val logger: ILogger by inject()
    private val serviceManager: ServiceManager by inject()
    private val profileNetworkChecker: ProfileNetworkChecker by inject()
    private val apiUpdateService: ApiUpdateService by inject()
    private val profileService: ProfileService by inject()
    private val supabaseStorageApi: SupabaseStorageApi by inject()
    private val appInstallerDownloadService: AppInstallerDownloadService by inject()

    sealed class DownloadState {
        object Idle : DownloadState()
        object CheckingUpdates : DownloadState() // Проверка обновлений при запуске
        object Downloading : DownloadState() // API обновляется
        object DownloadingProfiles : DownloadState() // Загрузка только профилей
        object Processing : DownloadState()
        data class Success(val version: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
        data class UpdateAvailable(val currentVersion: String, val newVersion: String) : DownloadState()
        object AppInstallerDownloading : DownloadState()
        data class AppInstallerDownloadError(val message: String) : DownloadState()
        data class AppUpdateAvailable(val currentVersion: String, val newVersion: String) : DownloadState()
    }

    enum class ProfileCheckStatus { Idle, Checking, Success, Error }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    private val _whiteListLinks = MutableStateFlow<List<String>?>(null)
    val whiteListLinks: StateFlow<List<String>?> = _whiteListLinks.asStateFlow()

    private val _profileCheckStatuses = MutableStateFlow<Map<String, ProfileCheckStatus>>(emptyMap())
    val profileCheckStatuses: StateFlow<Map<String, ProfileCheckStatus>> = _profileCheckStatuses.asStateFlow()

    data class ProfileWithStatus(
        val profile: Profile,
        val status: ProfileCheckStatus,
        val serviceStatuses: Map<ServiceTargets.ServiceType, Boolean> = emptyMap()
    )

    private val _profileWithStatuses = MutableStateFlow<List<ProfileWithStatus>>(emptyList())
    val profileWithStatuses: StateFlow<List<ProfileWithStatus>> = _profileWithStatuses.asStateFlow()
    
    private val _profileServiceStatuses = MutableStateFlow<Map<String, Map<ServiceTargets.ServiceType, Boolean>>>(emptyMap())

    private var autoCheckJob: Job? = null

    init {
        loadProfiles()
        getWhiteList()

        coroutineScope.launch {
            val savedProfileFileName = settingsViewModel.getSelectedProfileFileName()
            if (savedProfileFileName != null) {
                val savedProfile = _profiles.value.find { it.fileName == savedProfileFileName }
                if (savedProfile != null) {
                    _selectedProfile.value = savedProfile
                }
            }
        }
    }

    fun getWhiteList() {
        coroutineScope.launch {
            try {
                _whiteListLinks.value = whitelistManager.getWhiteList()
            } catch (e: Exception) {
                logger.error("[RepositoryViewModel] Error loads whitelist: ${e.message}")
            }
        }
    }

    fun selectProfile(profile: Profile) {
        logger.info("[RepositoryViewModel] Selecting profile: ${profile.fileName}")
        _selectedProfile.value = profile
        settingsViewModel.saveSelectedProfile(profile)
        logger.info("[RepositoryViewModel] Profile selected and saved: ${profile.fileName}")
    }

    fun updateProfilesList() {
        val lang = when (vmSettings.getCurrentLanguage()) {
            LanqSelect.RU -> "ru"
            LanqSelect.EN, LanqSelect.UK -> "en"
        }
        val newProfiles = profileService.sortProfiles(profileService.updateProfilesList(), lang)
        logger.info("[updateProfilesList] Profile list updated. Count: ${newProfiles.size}")
        _profiles.value = newProfiles
        updateProfileWithStatuses(_profileCheckStatuses.value)
    }

    private fun loadProfiles() {
        val lang = when (vmSettings.getCurrentLanguage()) {
            LanqSelect.RU -> "ru"
            LanqSelect.EN, LanqSelect.UK -> "en"
        }
        val profilesList = profileService.getProfilesList()
        logger.info("[loadProfiles] Profiles received: ${profilesList.size}")
        val sortedProfiles = profileService.sortProfiles(profilesList, lang)
        logger.info("[loadProfiles] After sorting: ${sortedProfiles.size}")
        _profiles.value = sortedProfiles
        logger.info("[loadProfiles] Profile list updated. Count: ${sortedProfiles.size}")
        updateProfileWithStatuses(_profileCheckStatuses.value)
    }

    fun checkApiForUpdates() {
        coroutineScope.launch {
            _downloadState.value = DownloadState.CheckingUpdates
            val result = apiUpdateService.checkForApiAndAppUpdates()
            if (result.apiUpdateAvailable && result.remoteApiVersion != null) {
                _downloadState.value = DownloadState.Downloading
                val downloadResult = apiUpdateService.downloadApi()
                if (downloadResult is ApiDownloadResult.Success) {
                    _downloadState.value = DownloadState.Processing
                    val mergeResult = apiUpdateService.mergeApi(downloadResult.archivePath, result.remoteApiVersion)
                    if (mergeResult is ApiMergeResult.Success) {
                        _downloadState.value = DownloadState.Success(result.remoteApiVersion)
                        updateProfilesList()
                        getWhiteList()
                        if (result.appUpdateAvailable && result.remoteAppVersion != null) {
                            _downloadState.value = DownloadState.AppUpdateAvailable(APP_VERSION, result.remoteAppVersion)
                        }
                    } else if (mergeResult is ApiMergeResult.Error) {
                        _downloadState.value = DownloadState.Error(mergeResult.message)
                    }
                } else if (downloadResult is ApiDownloadResult.Error) {
                    _downloadState.value = DownloadState.Error(downloadResult.message)
                }
            } else if (result.appUpdateAvailable && result.remoteAppVersion != null) {
                _downloadState.value = DownloadState.AppUpdateAvailable(APP_VERSION, result.remoteAppVersion)
            } else {
                val apiVersion = settingsViewModel.getApiVersion() ?: ""
                _downloadState.value = DownloadState.Success(apiVersion)
                updateProfilesList()
                getWhiteList()
            }
        }
    }

    fun downloadProfilesOnly() {
        coroutineScope.launch {
            _downloadState.value = DownloadState.DownloadingProfiles
            val result = apiUpdateService.downloadProfilesOnly()
            if (result is ProfilesDownloadResult.Success) {
                val currentApiVersion = settingsViewModel.getApiVersion() ?: ""
                _downloadState.value = DownloadState.Success(currentApiVersion)
                updateProfilesList()
                logger.info("[RepositoryViewModel] Profiles downloaded successfully")
            } else if (result is ProfilesDownloadResult.Error) {
                _downloadState.value = DownloadState.Error(result.message)
                logger.error("[RepositoryViewModel] Error downloading profiles: ${result.message}")
            }
        }
    }

    fun downloadAndRunAppInstaller(newVersion: String) {
        coroutineScope.launch {
            _downloadState.value = DownloadState.AppInstallerDownloading
            when (val result = appInstallerDownloadService.downloadAndRunLatestInstaller()) {
                is AppInstallerDownloadResult.Success -> {
                    val currentApiVersion = settingsViewModel.getApiVersion() ?: ""
                    logger.info("[RepositoryViewModel] App installer downloaded and started from ${result.installerPath}")
                    _downloadState.value = DownloadState.Success(currentApiVersion.ifEmpty { newVersion })
                    exitProcess(0)
                }
                is AppInstallerDownloadResult.Error -> {
                    logger.error("[RepositoryViewModel] Error downloading or starting app installer: ${result.message}")
                    _downloadState.value = DownloadState.AppInstallerDownloadError(result.message)
                }
            }
        }
    }

    fun removeWhiteListLink(link: String) {
        val currentList = _whiteListLinks.value
        if (currentList != null) {
            _whiteListLinks.value = currentList - link
        }
        coroutineScope.launch {
            try {
                whitelistManager.deleteLink(link).onFailureLog(logger, "RepositoryViewModel.removeWhiteListLink") {
                    getWhiteList()
                }
            } catch (e: Exception) {
                logger.error("[RepositoryViewModel] Error removing whitelist link, reverting UI: ${e.message}")
                getWhiteList()
            }
        }
    }

    fun addNewWhiteListLink(newLink: String, currentServiceStatus: GeneralViewmodel.ServiceStatus): Result<Unit> {
        val result = whitelistManager.addNewLink(newLink)
        if (result.isSuccess) {
            getWhiteList()
            val selectedProfileFileName = vmSettings.getSelectedProfileFileName()
            if (!selectedProfileFileName.isNullOrEmpty()
                && (currentServiceStatus == GeneralViewmodel.ServiceStatus.ACTIVE)
            ) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        serviceManager.restartService(selectedProfileFileName)
                    } catch (e: Exception) {
                        logger.error("[RepositoryViewModel] Error restarting service after adding link: ${e.message}")
                    }
                }
            }
        }
        return result
    }

    fun getLocalizedProfileName(fileName: String): String {
        val lang = when (vmSettings.getCurrentLanguage()) {
            LanqSelect.RU -> "ru"
            LanqSelect.EN, LanqSelect.UK -> "en"
        }
        return profileService.getLocalizedProfileName(fileName, lang)
    }

    fun selectProfileByFileName(fileName: String) {
        val profile = _profiles.value.find { it.fileName == fileName }
        if (profile != null) {
            selectProfile(profile)
        }
    }

    private fun updateProfileWithStatuses(statuses: Map<String, ProfileCheckStatus>) {
        val currentProfiles = _profiles.value
        val serviceStatuses = _profileServiceStatuses.value
        _profileWithStatuses.value = currentProfiles.map { profile ->
            ProfileWithStatus(
                profile = profile,
                status = statuses[profile.fileName] ?: ProfileCheckStatus.Idle,
                serviceStatuses = serviceStatuses[profile.fileName] ?: emptyMap()
            )
        }
    }

    private fun updateProfileWithStatusesForProfiles(profiles: List<Profile>, statuses: Map<String, ProfileCheckStatus>) {
        val serviceStatuses = _profileServiceStatuses.value
        _profileWithStatuses.value = profiles.map { profile ->
            ProfileWithStatus(
                profile = profile,
                status = statuses[profile.fileName] ?: ProfileCheckStatus.Idle,
                serviceStatuses = serviceStatuses[profile.fileName] ?: emptyMap()
            )
        }
    }

    fun autoCheckProfiles(profilesToCheck: List<Profile>? = null, serviceDomain: String = "all", onSuccess: ((Profile) -> Unit)? = null) {
        autoCheckJob?.cancel()
        autoCheckJob = coroutineScope.launch {
            try {
                val profiles = profilesToCheck ?: _profiles.value
                val statuses = profiles.associate { it.fileName to ProfileCheckStatus.Idle }.toMutableMap()
                logger.info("[ProfileCheck] Auto profile check started for all services (${profiles.size} profiles)")
                _profileCheckStatuses.value = statuses
                _profileServiceStatuses.value = emptyMap()
                updateProfileWithStatusesForProfiles(profiles, statuses)

                val logFile = profileCheckLogWriter.createSessionLogFile("all_services")
                val providerInfo: ProviderInfo? = try {
                    providerDetector.detectProvider().getOrNull()
                } catch (e: Exception) {
                    logger.error("[ProfileCheck] Provider detection failed: ${e.message}", e)
                    null
                }
                profileCheckLogWriter.writeSessionStart(
                    logFile,
                    "all_services",
                    systemScriptService = systemScriptService,
                    providerInfo = providerInfo
                )

                for (profile in profiles) {
                    logger.info("[ProfileCheck] Checking profile: ${profile.fileName}")
                    statuses[profile.fileName] = ProfileCheckStatus.Checking
                    _profileCheckStatuses.value = statuses.toMap()
                    updateProfileWithStatusesForProfiles(profiles, statuses)
                    
                    val args = profileService.getProfileArgs(profile.fileName)
                    logger.info("[ProfileCheck] Launch parameters for ${profile.fileName}: $args")
                    var result: ProfileCheckResult
                    var exception: Exception? = null
                    try {
                        val  profileAdapt = profileAdapterService.adaptProfile(args)
                        result = profileNetworkChecker.checkProfileWithAllServices(profile, profileAdapt)
                    } catch (e: CancellationException) {
                        result = ProfileCheckResult.Error("The checking was stopped by the user")
                        exception = null
                        logger.info("[ProfileCheck] Profile ${profile.fileName} check cancelled by user")
                    } catch (e: Exception) {
                        result = ProfileCheckResult.Error(e.message ?: "Unknown error")
                        exception = e
                        logger.error("[ProfileCheck] Exception for profile ${profile.fileName}: ${e.message}", e)
                    }
                    
                    when (result) {
                        is ProfileCheckResult.ServicesResult -> {
                            val serviceStatusesMap = result.servicesCheckResult.serviceResults.mapValues { (_, serviceResult) ->
                                serviceResult.isSuccess
                            }
                            val currentServiceStatuses = _profileServiceStatuses.value.toMutableMap()
                            currentServiceStatuses[profile.fileName] = serviceStatusesMap
                            _profileServiceStatuses.value = currentServiceStatuses
                            updateProfileWithStatusesForProfiles(profiles, statuses)
                            
                            if (result.servicesCheckResult.isAllSuccess()) {
                                logger.info("[ProfileCheck] Profile ${profile.fileName} passed the check for all services")
                                statuses[profile.fileName] = ProfileCheckStatus.Success
                                _profileCheckStatuses.value = statuses.toMap()
                                updateProfileWithStatusesForProfiles(profiles, statuses)
                                onSuccess?.invoke(profile)
                            } else if (result.servicesCheckResult.hasAnySuccess()) {
                                logger.info("[ProfileCheck] Profile ${profile.fileName} partially passed the check")
                                statuses[profile.fileName] = ProfileCheckStatus.Success
                                _profileCheckStatuses.value = statuses.toMap()
                                updateProfileWithStatusesForProfiles(profiles, statuses)
                                onSuccess?.invoke(profile)
                            } else {
                                logger.info("[ProfileCheck] Profile ${profile.fileName} did not pass the check for any service")
                                statuses[profile.fileName] = ProfileCheckStatus.Error
                                _profileCheckStatuses.value = statuses.toMap()
                                updateProfileWithStatusesForProfiles(profiles, statuses)
                            }
                        }
                        is ProfileCheckResult.Success -> {
                            logger.info("[ProfileCheck] Profile ${profile.fileName} passed the check")
                            statuses[profile.fileName] = ProfileCheckStatus.Success
                            _profileCheckStatuses.value = statuses.toMap()
                            updateProfileWithStatusesForProfiles(profiles, statuses)
                            onSuccess?.invoke(profile)
                        }
                        is ProfileCheckResult.Error -> {
                            logger.info("[ProfileCheck] Profile ${profile.fileName} did not pass the check: ${result.message}")
                            statuses[profile.fileName] = ProfileCheckStatus.Error
                            _profileCheckStatuses.value = statuses.toMap()
                            updateProfileWithStatusesForProfiles(profiles, statuses)
                        }
                    }
                    
                    profileCheckLogWriter.appendProfileCheckLog(logFile, profile, "all_services", result, exception, args)
                }
                logger.info("[ProfileCheck] Profile check completed")
                profileCheckLogWriter.writeSessionEnd(logFile)
            } catch (e: Exception) {
                logger.error("[ProfileCheck] FATAL error in autoCheckProfiles: ${e.message}", e)
            }
        }
    }

    fun clearProfileCheckStatuses() {
        _profileCheckStatuses.value = emptyMap()
        _profileServiceStatuses.value = emptyMap()
        updateProfileWithStatuses(emptyMap())
    }

    fun cancelAutoCheckProfiles() {
        autoCheckJob?.cancel()
        autoCheckJob = null
        clearProfileCheckStatuses()
    }

    suspend fun sendBugReport(name: String, content: String, idUser: String = "unknown"): Result<Unit> {
        val report = BugReportData(name = name, content = content, id_user = idUser)
        return supabaseStorageApi.sendBugReport(report)
    }
} 