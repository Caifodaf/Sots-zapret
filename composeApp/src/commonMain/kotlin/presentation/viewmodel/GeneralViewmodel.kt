package presentation.viewmodel

import data.api.AppInstallerDownloadService
import data.api.GithubRawLinkApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import data.service.ServiceManagerImpl
import domain.ServiceManager
import domain.HostsUpdateService
import domain.HostsUpdateResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.path.PathFilesProject
import util.Screen
import util.interfaces.ILogger
import java.awt.Desktop
import java.net.URI
import kotlin.collections.plus
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.onSuccessOrLog
import util.path.NamespaceProject.ANALOG_SERVICE_NAME
import util.path.NamespaceProject.ANALOG_SERVICE_NAME_SEC
import util.interfaces.ISystemScriptService
import util.path.NamespaceProject.SOTS_SERVICE_NAME
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.update

class GeneralViewmodel(
    private val viewModelScope: CoroutineScope,
    val vmRepository: RepositoryViewModel,
    val vmSettings: SettingsViewModel
) : KoinComponent {

    private val logger: ILogger by inject()
    private val serviceManager: ServiceManager by inject()
    private val systemScriptService: ISystemScriptService by inject()
    private val hostsUpdateService: HostsUpdateService by inject()
    private val githubRawLinkApi: GithubRawLinkApi by inject()

    enum class UrlBrowser{
        GIT, FAQ, README, DONAT
    }

    enum class ServiceStatus{
        CHECK, UNSELECTED, NON_ADMIN,
        INACTIVE, STARTING, ACTIVE, SHUTDOWN,
        ERROR, ERROR_ANALOG, ERROR_NULL, ERROR_SERVICE_START,
        AUTO_SELECT,
    }

    enum class StatusAutoFrame{
        INACTIVE, ACTIVE, ERROR, FAIL, FOUND
    }

    private val _serviceState = MutableStateFlow<ServiceStatus>(ServiceStatus.INACTIVE)
    val serviceState: StateFlow<ServiceStatus> = _serviceState.asStateFlow()

    private val _profileAutoStatus = MutableStateFlow(StatusAutoFrame.INACTIVE)
    val profileAutoStatus: StateFlow<StatusAutoFrame> = _profileAutoStatus.asStateFlow()

    private var _navigationStack = MutableStateFlow(listOf(Screen.Main))

    enum class BugReportButtonState { Idle, Sending, Success, Error }
    private val _bugReportEmail = MutableStateFlow("")
    val bugReportEmail: StateFlow<String> = _bugReportEmail.asStateFlow()
    private val _bugReportContent = MutableStateFlow("")
    val bugReportContent: StateFlow<String> = _bugReportContent.asStateFlow()
    private val _bugReportButtonState = MutableStateFlow(BugReportButtonState.Idle)
    val bugReportButtonState: StateFlow<BugReportButtonState> = _bugReportButtonState.asStateFlow()
    private val _bugReportError = MutableStateFlow<String?>(null)
    val bugReportError: StateFlow<String?> = _bugReportError.asStateFlow()

    enum class HostsUpdateButtonState { Idle, Updating, Success, AlreadyUpdated, Error }
    private val _hostsUpdateButtonState = MutableStateFlow(HostsUpdateButtonState.Idle)
    val hostsUpdateButtonState: StateFlow<HostsUpdateButtonState> = _hostsUpdateButtonState.asStateFlow()
    private val _hostsUpdateMessage = MutableStateFlow("")
    val hostsUpdateMessage: StateFlow<String> = _hostsUpdateMessage.asStateFlow()

    init {
        _serviceState.value = ServiceStatus.CHECK
        checkInitialServiceState()
        _navigationStack = if(vmSettings.checkFirstStart()) {
            MutableStateFlow(listOf(Screen.Main))
        } else {
            vmRepository.checkApiForUpdates()
            MutableStateFlow(listOf(Screen.First))
        }
        vmSettings.getCurrentLanguage()
        vmSettings.getCurrentTheme()

        vmRepository.checkApiForUpdates()
    }

    private suspend fun checkAnyAnalogServiceRunning(): Boolean {
        val analogServiceNames = listOf(ANALOG_SERVICE_NAME, ANALOG_SERVICE_NAME_SEC)
        return serviceManager.isAnyServiceRunning(analogServiceNames)
    }

    private fun checkInitialServiceState() {
        viewModelScope.launch(Dispatchers.IO) {
            val isAdmin = systemScriptService.isAdmin()
            if (!isAdmin) {
                withContext(Dispatchers.Main) {
                    setServiceState(ServiceStatus.NON_ADMIN)
                }
                return@launch
            }
            
            val selectedProfileFileName = vmSettings.getSelectedProfileFileName()
            if (selectedProfileFileName.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    setServiceState(ServiceStatus.AUTO_SELECT)
                }
                return@launch
            }
            
            val sotsStatus = serviceManager.checkServiceStatus(SOTS_SERVICE_NAME)
            val isAnyAnalogRunning = checkAnyAnalogServiceRunning()
            
            withContext(Dispatchers.Main) {
                when {
                    isAnyAnalogRunning -> {
                        setErrorAnalogState()
                    }
                    sotsStatus.getOrNull() == ServiceManagerImpl.ServiceStatusRequest.RUNNING -> {
                        setServiceState(ServiceStatus.ACTIVE)
                    }
                    sotsStatus.getOrNull() == ServiceManagerImpl.ServiceStatusRequest.STOPPED ||
                    sotsStatus.getOrNull() == ServiceManagerImpl.ServiceStatusRequest.NOT_FOUND -> {
                        setServiceState(ServiceStatus.INACTIVE)
                    }
                    else -> {
                        setServiceState(ServiceStatus.ERROR)
                    }
                }
            }
        }
    }

    private fun setServiceState(state: ServiceStatus) {
        val previousState = _serviceState.value
        logger.info("[GeneralViewmodel] setServiceState: $state (was $previousState)")
        _serviceState.value = state
        logger.info("[GeneralViewmodel] State updated: $previousState -> $state")
    }

    private fun setErrorAnalogState() {
        setServiceState(ServiceStatus.ERROR_ANALOG)
        logger.info("[GeneralViewmodel] Detected running analog service(s): $ANALOG_SERVICE_NAME, $ANALOG_SERVICE_NAME_SEC")
    }

    val currentScreen: StateFlow<Screen> = _navigationStack.asStateFlow().map { it.last() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = _navigationStack.value.last()
    )

    fun navigateTo(screen: Screen) {
        _navigationStack.value += screen
    }

    fun navigateBack(): Boolean {
        if (_navigationStack.value.size > 1) {
            _navigationStack.value = _navigationStack.value.dropLast(1)
            return true
        }
        return false
    }

    fun openUrlBrowser(type: UrlBrowser) {
        try {
            val url = when(type){
                UrlBrowser.GIT -> PathFilesProject.URL_GIT
                UrlBrowser.FAQ -> PathFilesProject.URL_FAQ
                UrlBrowser.README -> PathFilesProject.URL_README
                UrlBrowser.DONAT -> PathFilesProject.URL_DONAT
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logger.error("[GeneralViewmodel] Failed to open link: $type, error: ${e.message}")
        }
    }

    fun btnActivatedService() {
        if (_serviceState.value == ServiceStatus.AUTO_SELECT) {
            navigateToAutoSelect()
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val sotsStatus = serviceManager.checkServiceStatus(SOTS_SERVICE_NAME)
            val analogStatus = serviceManager.checkServiceStatus(ANALOG_SERVICE_NAME)
            withContext(Dispatchers.Main) {
                when (_serviceState.value) {
                    ServiceStatus.INACTIVE, ServiceStatus.ERROR, ServiceStatus.ERROR_SERVICE_START -> handleSotsStart(sotsStatus)
                    ServiceStatus.ACTIVE -> handleSotsShutdown(sotsStatus)
                    ServiceStatus.ERROR_ANALOG -> handleAnalogShutdown(analogStatus)
                    ServiceStatus.NON_ADMIN -> handleRestartSotsOnAdmin()
                    else -> {}
                }
            }
        }
    }

    private fun handleSotsStart(sotsStatus: Result<ServiceManagerImpl.ServiceStatusRequest>) {
        setServiceState(ServiceStatus.STARTING)
        sotsStatus.onSuccessOrLog({ status ->
            logger.info("GeneralViewmodel.btnActivatedService ServiceStatus.INACTIVE: $status")
            when (status) {
                ServiceManagerImpl.ServiceStatusRequest.NOT_FOUND, ServiceManagerImpl.ServiceStatusRequest.STOPPED -> {
                    val selectedProfileFileName = vmSettings.getSelectedProfileFileName()
                    if (selectedProfileFileName.isNullOrEmpty()) {
                        logger.error("[GeneralViewmodel] No profile selected to start the service!")
                        setServiceState(ServiceStatus.ERROR)
                        return@onSuccessOrLog
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            serviceManager.startOrRecreateService(selectedProfileFileName)
                            logger.info("[GeneralViewmodel] Service started successfully, setting ACTIVE")
                            withContext(Dispatchers.Main) { setServiceState(ServiceStatus.ACTIVE) }
                        } catch (e: Exception) {
                            logger.error("[GeneralViewmodel] Error during service creation/start: ${e.message}")
                            withContext(Dispatchers.Main) {
                                if (e.message?.contains("An error occurred while starting the service. Please check your profile") == true) {
                                    setServiceState(ServiceStatus.ERROR_SERVICE_START)
                                } else {
                                    setServiceState(ServiceStatus.ERROR)
                                }
                            }
                        }
                    }
                }
                ServiceManagerImpl.ServiceStatusRequest.RUNNING -> {
                    logger.info("[GeneralViewmodel] Service was already running, setting ACTIVE")
                    setServiceState(ServiceStatus.ACTIVE)
                }
                else -> setServiceState(ServiceStatus.ERROR)
            }
        }, logger, "GeneralViewmodel.btnActivatedService.ServiceStatus.INACTIVE")
    }

    private fun handleSotsShutdown(sotsStatus: Result<ServiceManagerImpl.ServiceStatusRequest>) {
        setServiceState(ServiceStatus.SHUTDOWN)
        sotsStatus.onSuccessOrLog({ status ->
            logger.info("GeneralViewmodel.btnActivatedService ServiceStatus.ACTIVE: $status")
            when (status) {
                ServiceManagerImpl.ServiceStatusRequest.NOT_FOUND -> {
                    logger.error("[GeneralViewmodel] Error, service not found: $SOTS_SERVICE_NAME")
                    setServiceState(ServiceStatus.INACTIVE)
                }
                ServiceManagerImpl.ServiceStatusRequest.RUNNING, ServiceManagerImpl.ServiceStatusRequest.STOPPED -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            serviceManager.serviceShutdown(SOTS_SERVICE_NAME)
                            withContext(Dispatchers.Main) { setServiceState(ServiceStatus.INACTIVE) }
                        } catch (e: Exception) {
                            logger.error("[GeneralViewmodel] Error during service shutdown: ${e.message}")
                            withContext(Dispatchers.Main) { setServiceState(ServiceStatus.ERROR) }
                        }
                    }
                }
                else -> setServiceState(ServiceStatus.ERROR)
            }
        }, logger, "GeneralViewmodel.btnActivatedService.ServiceStatus.ACTIVE")
    }

    private fun handleAnalogShutdown(analogStatus: Result<ServiceManagerImpl.ServiceStatusRequest>) {
        viewModelScope.launch(Dispatchers.IO) {
            val isAnyAnalogRunning = checkAnyAnalogServiceRunning()
            withContext(Dispatchers.Main) {
                setServiceState(ServiceStatus.ACTIVE)
                if (!isAnyAnalogRunning) {
                    logger.error("[GeneralViewmodel] Analog service not found")
                    setServiceState(ServiceStatus.INACTIVE)
                    return@withContext
                }
                val analogServiceNames = listOf(ANALOG_SERVICE_NAME, ANALOG_SERVICE_NAME_SEC)
                analogServiceNames.forEach { name ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            serviceManager.serviceShutdown(name)
                            withContext(Dispatchers.Main) { setServiceState(ServiceStatus.INACTIVE) }
                        } catch (e: Exception) {
                            logger.error("[GeneralViewmodel] Error during analog service shutdown: ${e.message}")
                            withContext(Dispatchers.Main) { setServiceState(ServiceStatus.INACTIVE) }
                        }
                    }
                }
            }
        }
    }

    private fun handleRestartSotsOnAdmin() {
        try {
            val exePath = vmSettings.startupManager.getExePath()
            if (!exePath.endsWith(".exe", ignoreCase = true)) {
                logger.error("[GeneralViewmodel] Restart as admin is only supported for exe builds!")
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    systemScriptService.launchExeWithDelay(exePath)
                    logger.info("[GeneralViewmodel] Relaunching as admin with delay and exiting current process")
                    exitProcess(0)
                } catch (e: Exception) {
                    logger.error("[GeneralViewmodel] Failed to restart as admin with delay: "+e.message)
                }
            }
        } catch (e: Exception) {
            logger.error("[GeneralViewmodel] Failed to restart as admin: ${e.message}")
        }
    }

    private fun navigateToAutoSelect() {
        logger.info("[GeneralViewmodel] Navigating to auto select screen")
        navigateTo(Screen.AutoProfile)
    }

    fun updateServiceStateAfterProfileSelection() {
        _serviceState.value = ServiceStatus.INACTIVE
    }

    fun onStartupSystemChange(enabled: Boolean) {
        logger.info("[GeneralViewmodel] onStartupSystemChange: $enabled")
        vmSettings.setStartupSystem(enabled)
    }

    fun onVisibilityTrayChange(enabled: Boolean) {
        logger.info("[GeneralViewmodel] onVisibilityTrayChange: $enabled")
        vmSettings.setVisibilityTray(enabled)
    }

    fun setProfileAutoStatus(status: StatusAutoFrame) {
        _profileAutoStatus.value = status
    }

    fun setBugReportEmail(value: String) { _bugReportEmail.value = value }
    fun setBugReportContent(value: String) { _bugReportContent.value = value }
    fun resetBugReport() {
        _bugReportEmail.value = ""
        _bugReportContent.value = ""
        _bugReportButtonState.value = BugReportButtonState.Idle
        _bugReportError.value = null
    }
    suspend fun sendBugReport() {
        val idUser = systemScriptService.getMachineInfo()
        val email = _bugReportEmail.value.trim()
        val content = _bugReportContent.value.trim()
        if (email.isBlank() || content.isBlank()) {
            _bugReportError.value = "st_error_bug_report_fields_empty"
            return
        }
        _bugReportButtonState.value = BugReportButtonState.Sending
        _bugReportError.value = null
        viewModelScope.launch {
            val result = vmRepository.sendBugReport(name = email, content = content, idUser = idUser.machineGuid)
            if (result.isSuccess) {
                _bugReportButtonState.value = BugReportButtonState.Success
            } else {
                _bugReportButtonState.value = BugReportButtonState.Error
                _bugReportError.value = result.exceptionOrNull()?.message ?: "st_error_bug_report_send"
            }
        }
    }

    fun openProfilesFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appPathProvider: util.interfaces.IAppPathProvider by inject()
                val profilesDir = appPathProvider.getProfilesDir().toString()
                systemScriptService.openFolder(profilesDir)
                logger.info("[GeneralViewmodel] Opened profiles folder: $profilesDir")
            } catch (e: Exception) {
                logger.error("[GeneralViewmodel] Failed to open profiles folder: ${e.message}")
            }
        }
    }

    fun reloadProfiles() {
        logger.info("[GeneralViewmodel] reloadProfiles: start")
        vmRepository.downloadProfilesOnly()
    }

    fun updateHostsList() {
        viewModelScope.launch(Dispatchers.IO) {
            logger.info("[GeneralViewmodel] getHostsList at startup: start")
            githubRawLinkApi.getHostsList().onSuccess {
                logger.info("[GeneralViewmodel] getHostsList at startup: loaded ${it.size} entries")
            }.onFailure { e ->
                logger.warn("[GeneralViewmodel] getHostsList at startup failed: ${e.message}")
            }
        }


        logger.info("[GeneralViewmodel] updateHostsList: start")
        if (_hostsUpdateButtonState.value == HostsUpdateButtonState.Updating) {
            logger.warn("[GeneralViewmodel] updateHostsList: already updating, ignoring request")
            return
        }

        _hostsUpdateButtonState.value = HostsUpdateButtonState.Updating
        _hostsUpdateMessage.value = "ms_hosts_checking"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = hostsUpdateService.updateHostsList()
                withContext(Dispatchers.Main) {
                    when (result) {
                        is HostsUpdateResult.Success -> {
                            _hostsUpdateButtonState.value = HostsUpdateButtonState.Success
                            _hostsUpdateMessage.value = "ms_hosts_list_updated"
                            logger.info("[GeneralViewmodel] updateHostsList: success")
                        }
                        is HostsUpdateResult.AlreadyUpdated -> {
                            _hostsUpdateButtonState.value = HostsUpdateButtonState.AlreadyUpdated
                            _hostsUpdateMessage.value = "ms_hosts_already_updated"
                            logger.info("[GeneralViewmodel] updateHostsList: already updated")
                        }
                        is HostsUpdateResult.Error -> {
                            _hostsUpdateButtonState.value = HostsUpdateButtonState.Error
                            _hostsUpdateMessage.value = "ms_hosts_error_updating"
                            logger.error("[GeneralViewmodel] updateHostsList: error - ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _hostsUpdateButtonState.value = HostsUpdateButtonState.Error
                    _hostsUpdateMessage.value = "ms_hosts_error_updating"
                    logger.error("[GeneralViewmodel] updateHostsList: exception - ${e.message}", e)
                }
            }
        }
    }

}