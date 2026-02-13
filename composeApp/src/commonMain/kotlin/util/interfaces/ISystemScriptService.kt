package util.interfaces

import domain.model.MachineInfo

interface ISystemScriptService {
    suspend fun runScriptFromResource(resourcePath: String, args: List<String> = emptyList()): String?
    suspend fun runScriptFromResourceAndParseBool(resourcePath: String, args: List<String> = emptyList()): Boolean


    suspend fun isAdmin(): Boolean
    suspend fun setStartupEnabled(appName: String, exePath: String, description: String, enabled: Boolean)
    suspend fun isStartupEnabled(appName: String): Boolean
    suspend fun checkServiceStatus(serviceName: String): String?
    suspend fun createService(serviceName: String, exePath: String, arguments: String): String?
    suspend fun deleteService(serviceName: String): String?
    suspend fun getWindowsTheme(): String?
    suspend fun getMachineInfo(): MachineInfo

    suspend fun launchExeWithDelay(exePath: String, delaySeconds: Int = 3)
    suspend fun openFolder(folderPath: String)
} 