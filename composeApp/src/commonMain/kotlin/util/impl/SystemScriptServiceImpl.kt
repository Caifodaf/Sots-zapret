package util.impl

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import util.interfaces.ISystemScriptService
import util.interfaces.ILogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import domain.model.MachineInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class SystemScriptServiceImpl : ISystemScriptService, KoinComponent {
    private val logger: ILogger by inject()

    override suspend fun runScriptFromResource(resourcePath: String, args: List<String>): String? = withContext(Dispatchers.IO) {
        try {
            val tempScript = kotlin.io.path.createTempFile(suffix = ".ps1").toFile()
            javaClass.getResourceAsStream(resourcePath)?.use { input ->
                tempScript.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                logger.error("[SystemScriptService] Script not found in resources: $resourcePath")
                return@withContext null
            }
            tempScript.deleteOnExit()
            val result = runScriptProcess(
                path = "powershell",
                args = listOf(
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", tempScript.absolutePath
                ) + args
            )
            tempScript.delete()
            result
        } catch (e: Exception) {
            logger.error("[SystemScriptService] Error running script $resourcePath: ${e.message}")
            null
        }
    }

    private fun runScriptProcess(path: String, args: List<String>): String? {
        return try {
            val process = ProcessBuilder(listOf(path) + args)
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            result
        } catch (e: Exception) {
            logger.error("[SystemScriptService] Error running process $path: ${e.message}")
            null
        }
    }

    override suspend fun runScriptFromResourceAndParseBool(resourcePath: String, args: List<String>): Boolean = withContext(Dispatchers.IO) {
        val result = runScriptFromResource(resourcePath, args)
        result?.trim()?.equals("True", ignoreCase = true) == true
    }

    override suspend fun isAdmin(): Boolean = runScriptFromResourceAndParseBool("/scripts/check_admin.ps1")

    override suspend fun setStartupEnabled(appName: String, exePath: String, description: String, enabled: Boolean) {
        val resource = if (enabled) "/scripts/add_startup.ps1" else "/scripts/remove_startup.ps1"
        val args = listOf("-AppName", appName, "-ExePath", exePath, "-Description", description)
        val result = runScriptFromResource(resource, args)
        logger.info("[SystemScriptService] Startup script result: $result")
    }

    override suspend fun isStartupEnabled(appName: String): Boolean {
        val args = listOf("-AppName", appName)
        return runScriptFromResourceAndParseBool("/scripts/check_startup.ps1", args)
    }

    override suspend fun checkServiceStatus(serviceName: String): String? {
        return runScriptFromResource("/scripts/check_zapret_service.ps1", listOf(serviceName))
    }

    override suspend fun createService(serviceName: String, exePath: String, arguments: String): String? {
        val args = listOf("-ServiceName", serviceName, "-ExecutablePath", exePath, "-Arguments", arguments)
        return runScriptFromResource("/scripts/create_sots_service.ps1", args)
    }

    override suspend fun deleteService(serviceName: String): String? {
        return runScriptFromResource("/scripts/delete_sots_service.ps1", listOf(serviceName))
    }

    override suspend fun getWindowsTheme(): String? {
        return runScriptFromResource("/scripts/get_windows_theme.ps1")
    }

    override suspend fun launchExeWithDelay(exePath: String, delaySeconds: Int) {
        runScriptFromResource(
            "/scripts/launch_with_delay.ps1",
            listOf("-ExePath", exePath, "-DelaySeconds", delaySeconds.toString())
        )
    }

    override suspend fun getMachineInfo(): MachineInfo = withContext(Dispatchers.IO) {
        val result = runScriptFromResource("/scripts/get_machine_info.ps1")
        if (result.isNullOrBlank()) {
            throw IllegalStateException("Failed to get machine info from script")
        }
        try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<MachineInfo>(result)
        } catch (e: Exception) {
            logger.error("[SystemScriptService] Failed to parse MachineInfo: ${e.message}")
            throw e
        }
    }

    override suspend fun openFolder(folderPath: String): Unit = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("explorer", folderPath)
                .start()
            process.waitFor()
        } catch (e: Exception) {
            logger.error("[SystemScriptService] Error opening folder $folderPath: ${e.message}")
        }
    }
} 