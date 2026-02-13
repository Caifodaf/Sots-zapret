package data.service

import domain.ServiceManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ILogger
import util.interfaces.ISystemScriptService
import util.path.NamespaceProject.SOTS_SERVICE_NAME
import presentation.viewmodel.SettingsViewModel
import java.nio.file.Files
import util.interfaces.IAppPathProvider
import kotlinx.coroutines.delay
import domain.WinwsLauncherService
class ServiceManagerImpl() : ServiceManager, KoinComponent {
    private val systemScriptService: ISystemScriptService by inject()
    private val logger: ILogger by inject()
    private val settingsViewModel: SettingsViewModel by inject()
    private val appPathProvider: IAppPathProvider by inject()
    private val profileAdapterService: ProfileAdapterService by inject()
    private val winwsLauncherService: WinwsLauncherService by inject()

    enum class ServiceStatusRequest{
        RUNNING, STOPPED, ERROR, NOT_FOUND, ERROR_STATUS
    }

    override suspend fun checkServiceStatus(serviceName: String): Result<ServiceStatusRequest> {
        val result = systemScriptService.checkServiceStatus(serviceName)
        return when {
            result.isNullOrEmpty() -> Result.failure(Exception(ServiceStatusRequest.ERROR.name))
            result.trim() == "NOT_FOUND" -> Result.success(ServiceStatusRequest.NOT_FOUND)
            result.trim().equals("Running", ignoreCase = true) -> Result.success(ServiceStatusRequest.RUNNING)
            result.trim().equals("Stopped", ignoreCase = true) -> Result.success(ServiceStatusRequest.STOPPED)
            else -> Result.failure(Exception(ServiceStatusRequest.ERROR_STATUS.name))
        }
    }

    override suspend fun serviceCreateStart(selectedProfileFileName: String) {
       
        val params = winwsLauncherService.prepareProfileArgs(selectedProfileFileName, adaptProfile = true)
        
        // Сохраняем адаптированный профиль в last.txt для совместимости
        val profilesDir = appPathProvider.getProfilesDir()
        val lastFile = profilesDir.resolve("last.txt")
        Files.writeString(lastFile, params.replace(" ", "\n"))
        logger.info("[ServiceManagerImpl] Created adapted profile file: $lastFile")
        
        settingsViewModel.saveSelectedProfileParams(selectedProfileFileName, params)
        val winwsPath = winwsLauncherService.getWinwsPath()
        val result = systemScriptService.createService(SOTS_SERVICE_NAME, winwsPath, params)
        if (result == null || result.contains("Service already exists")) {
            logger.error("[ServiceManagerImpl] Service already exists or error during creation: $result")
            throw Exception("Service already exists or error during creation: $result")
        }
        if (!result.contains("Service created and started")) {
            logger.error("[ServiceManagerImpl] Error during service creation or start: $result")
            throw Exception("Error during service creation or start: $result")
        }
        logger.info("[ServiceManagerImpl] Service created and started: $selectedProfileFileName")
        
        delay(500)
        val status = checkServiceStatus(SOTS_SERVICE_NAME).getOrNull()
        if (status == ServiceStatusRequest.STOPPED) {
            logger.error("[ServiceManagerImpl] Service created but stopped, shutting down: $selectedProfileFileName")
            try {
                serviceShutdown(SOTS_SERVICE_NAME)
            } catch (e: Exception) {
                logger.error("[ServiceManagerImpl] Error shutting down stopped service: ${e.message}")
            }
            throw Exception("An error occurred while starting the service. Please check your profile")
        }
    }
    override suspend fun serviceShutdown(serviceName: String) {
        val result = systemScriptService.deleteService(serviceName)
        if (result == null) {
            logger.error("[ServiceManagerImpl] Error executing PowerShell script for service deletion")
            throw Exception("Error executing PowerShell script for service deletion")
        }
        if (!result.contains("Service deleted") && !result.contains("Service not found")) {
            logger.error("[ServiceManagerImpl] Error deleting service: $result")
            throw Exception("Error deleting service: $result")
        }
        logger.info("[ServiceManagerImpl] Service deleted: $serviceName")
    }

    override suspend fun startOrRecreateService(profileName: String) {
        val status = checkServiceStatus(SOTS_SERVICE_NAME).getOrNull()
        when (status) {
            ServiceStatusRequest.RUNNING -> {
                logger.info("[ServiceManagerImpl] Service already running, recreation not required")
                return
            }
            ServiceStatusRequest.STOPPED -> {
                logger.info("[ServiceManagerImpl] Service stopped, recreating with new profile: $profileName")
                serviceShutdown(SOTS_SERVICE_NAME)
                serviceCreateStart(profileName)
            }
            ServiceStatusRequest.NOT_FOUND -> {
                logger.info("[ServiceManagerImpl] Service not found, creating: $profileName")
                serviceCreateStart(profileName)
            }
            else -> {
                logger.error("[ServiceManagerImpl] Failed to determine service status for recreation: $status")
                throw Exception("Failed to determine service status for recreation")
            }
        }
    }

    override suspend fun restartService(profileName: String) {
        try {
            logger.info("[ServiceManagerImpl] Service restart: stopping...")
            serviceShutdown(SOTS_SERVICE_NAME)
        } catch (e: Exception) {
            logger.error("[ServiceManagerImpl] Error stopping service for restart: ${e.message}")
        }
        try {
            logger.info("[ServiceManagerImpl] Service restart: starting...")
            serviceCreateStart(profileName)
        } catch (e: Exception) {
            logger.error("[ServiceManagerImpl] Error starting service after stop: ${e.message}")
            throw e
        }
    }

    override suspend fun isAnyServiceRunning(serviceNames: List<String>): Boolean {
        for (name in serviceNames) {
            val status = checkServiceStatus(name).getOrNull()
            if (status != ServiceStatusRequest.NOT_FOUND) return true
        }
        return false
    }
}