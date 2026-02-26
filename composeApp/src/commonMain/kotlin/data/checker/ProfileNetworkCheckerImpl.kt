package data.checker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import domain.model.Profile
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import util.interfaces.ILogger
import util.interfaces.IAppPathProvider
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.IOException
import domain.WinwsLauncherService

object ProcessUtils {
    class ProcessNotFoundException(message: String) : Exception(message)
    fun killProcess(processName: String, logger: ILogger? = null) {
        try {
            val process = ProcessBuilder("taskkill", "/F", "/IM", processName).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            if (output.contains("not found", ignoreCase = true) || output.contains("Не найден", ignoreCase = true)) {
                logger?.error("[ProcessUtils] Process $processName is not found")
                throw ProcessNotFoundException("Process $processName is not found")
            }
            logger?.info("[ProcessUtils] Process $processName killed")
        } catch (e: ProcessNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger?.error("[ProcessUtils] Error killing process $processName: ", e)
        }
    }

    fun waitForProcess(processName: String, timeoutMillis: Long = 5000, logger: ILogger? = null): Boolean {
        val start = System.currentTimeMillis()
        var found = false
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (isProcessRunning(processName, logger)) {
                found = true
                break
            }
            Thread.sleep(200)
        }
        if (!found) {
            logger?.warn("[ProcessUtils] Process $processName not found in $timeoutMillis ms")
            throw ProcessNotFoundException("Process $processName is not found")
        }
        return found
    }

    fun isProcessRunning(processName: String, logger: ILogger? = null): Boolean {
        return try {
            val processList = ProcessBuilder("tasklist").start().inputStream.bufferedReader().readText()
            val running = processList.contains(processName, ignoreCase = true)
            logger?.info("[ProcessUtils] Process $processName is ${if (running) "running" else "not found"}")
            running
        } catch (e: Exception) {
            logger?.error("[ProcessUtils] Error checking process $processName: ${e.message}")
            false
        }
    }
}

class ProfileNetworkCheckerImpl(
    private val logger: ILogger,
    val appPathProvider: IAppPathProvider,
    private val winwsLauncherService: WinwsLauncherService,
    private val winwsProcessName: String = "winws.exe"
) : ProfileNetworkChecker {
    override suspend fun checkProfile(profile: Profile, args: String, domain: String): ProfileCheckResult =
        withContext(Dispatchers.IO) {
            var checkResult: ProfileCheckResult? = null
            try {
                logger.info("[ProfileNetworkChecker] Killing $winwsProcessName if running")
                try {
                    ProcessUtils.killProcess(winwsProcessName, logger)
                } catch (e: ProcessUtils.ProcessNotFoundException) {
                    logger.error("[ProfileNetworkChecker] ${e.message}")
                    checkResult = ProfileCheckResult.Error(e.message ?: "Process $winwsProcessName is not found")
                    return@withContext checkResult
                }

                val winwsPath = try {
                    winwsLauncherService.getWinwsPath()
                } catch (e: Exception) {
                    logger.error("[ProfileNetworkChecker] Failed to get path to winws.exe: ${e.message}")
                    checkResult = ProfileCheckResult.Error("Failed to get path to winws.exe")
                    return@withContext checkResult
                }
                if (!winwsLauncherService.validateArgs(args)) {
                    logger.error("[ProfileNetworkChecker] Invalid characters in launch arguments: $args")
                    checkResult = ProfileCheckResult.Error("Invalid launch arguments")
                    return@withContext checkResult
                }

                var started = false
                var process: Process? = null
                var attempt = 0
                val maxAttempts = 3
                while (attempt < maxAttempts && !started) {
                    attempt++
                    logger.info("[ProfileNetworkChecker] Launching $winwsProcessName with arguments: $args (attempt $attempt)")
                    process = ProcessBuilder(winwsPath, *args.split(" ").toTypedArray())
                        .redirectErrorStream(true)
                        .start()
                    try {
                        if (ProcessUtils.waitForProcess(winwsProcessName, 3000, logger)) {
                            started = true
                        }
                    } catch (e: ProcessUtils.ProcessNotFoundException) {
                        process.destroy()
                        logger.error("[ProfileNetworkChecker] ${e.message}")
                        checkResult = ProfileCheckResult.Error(e.message ?: "Process $winwsProcessName is not found")
                        return@withContext checkResult
                    }
                    if (!started) {
                        process.destroy()
                        try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}
                        logger.warn("[ProfileNetworkChecker] $winwsProcessName did not start on attempt $attempt")
                    }
                }
                if (!started) {
                    logger.error("[ProfileNetworkChecker] $winwsProcessName did not start after $maxAttempts attempts")
                    checkResult = ProfileCheckResult.Error("$winwsProcessName did not start")
                    return@withContext checkResult
                }

                logger.info("[ProfileNetworkChecker] Checking connection to https://$domain")
                val connectionOk = testConnection("https://$domain")

                process?.destroy()
                try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}

                checkResult = if (connectionOk) {
                    logger.info("[ProfileNetworkChecker] Profile check successful")
                    ProfileCheckResult.Success
                } else {
                    logger.error("[ProfileNetworkChecker] No connection to $domain")
                    ProfileCheckResult.Error("No connection")
                }
                checkResult
            } catch (e: ProcessUtils.ProcessNotFoundException) {
                logger.error("[ProfileNetworkChecker] ${e.message}")
                checkResult = ProfileCheckResult.Error(e.message ?: "Process $winwsProcessName is not found")
                checkResult
            } catch (e: Exception) {
                try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}
                logger.error("[ProfileNetworkChecker] Error: ", e)
                checkResult = ProfileCheckResult.Error(e.message ?: "Unknown error")
                checkResult
            }
        }


    private fun testConnection(urlStr: String): Boolean {
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36")
                conn.instanceFollowRedirects = true
                
                // Пытаемся подключиться
                conn.connect()
                
                // Читаем только заголовки для ускорения (HEAD запрос через GET с игнорированием тела)
                val responseCode = conn.responseCode
                if (responseCode in 200..399) {
                    try {
                        conn.inputStream.use { it.readBytes() } // Читаем только первые 1KB
                    } catch (e: Exception) {
                        // Если не удалось прочитать, но код ответа OK, считаем успешным
                        if (responseCode in 200..399) {
                            return true
                        }
                    }
                    return true
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    Thread.sleep(500)
                }
            }
        }
        logger.warn("[ProfileNetworkChecker] Connection test failed for $urlStr after 3 attempts: ${lastException?.message}")
        return false
    }

    override suspend fun checkProfileWithAllServices(profile: Profile, args: String): ProfileCheckResult =
        withContext(Dispatchers.IO) {
            val checkResult: ProfileCheckResult? = null
            try {
                logger.info("[ProfileNetworkChecker] Starting check for all services for profile: ${profile.fileName}")
                
                // Убиваем процесс если запущен
                logger.info("[ProfileNetworkChecker] Killing $winwsProcessName if running")
                try {
                    ProcessUtils.killProcess(winwsProcessName, logger)
                } catch (e: ProcessUtils.ProcessNotFoundException) {
                    logger.info("[ProfileNetworkChecker] Process $winwsProcessName not running")
                }

                val winwsPath = try {
                    winwsLauncherService.getWinwsPath()
                } catch (e: Exception) {
                    logger.error("[ProfileNetworkChecker] Failed to get path to winws.exe: ${e.message}")
                    return@withContext ProfileCheckResult.Error("Failed to get path to winws.exe")
                }
                
                if (!winwsLauncherService.validateArgs(args)) {
                    logger.error("[ProfileNetworkChecker] Invalid characters in launch arguments: $args")
                    return@withContext ProfileCheckResult.Error("Invalid launch arguments")
                }

                // Запускаем профиль
                var started = false
                var process: Process? = null
                var attempt = 0
                val maxAttempts = 3
                while (attempt < maxAttempts && !started) {
                    attempt++
                    logger.info("[ProfileNetworkChecker] Launching $winwsProcessName with arguments: $args (attempt $attempt)")
                    process = ProcessBuilder(winwsPath, *args.split(" ").toTypedArray())
                        .redirectErrorStream(true)
                        .start()
                    try {
                        if (ProcessUtils.waitForProcess(winwsProcessName, 3000, logger)) {
                            started = true
                        }
                    } catch (e: ProcessUtils.ProcessNotFoundException) {
                        process.destroy()
                        logger.error("[ProfileNetworkChecker] ${e.message}")
                        return@withContext ProfileCheckResult.Error(e.message ?: "Process $winwsProcessName is not found")
                    }
                    if (!started) {
                        process.destroy()
                        try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}
                        logger.warn("[ProfileNetworkChecker] $winwsProcessName did not start on attempt $attempt")
                    }
                }
                
                if (!started) {
                    logger.error("[ProfileNetworkChecker] $winwsProcessName did not start after $maxAttempts attempts")
                    return@withContext ProfileCheckResult.Error("$winwsProcessName did not start")
                }

                logger.info("[ProfileNetworkChecker] Waiting 2 seconds for connection stabilization...")
                kotlinx.coroutines.delay(2000)

                logger.info("[ProfileNetworkChecker] Checking all services for profile: ${profile.fileName}")
                val serviceResults = ServiceTargets.ALL_SERVICES.map { (serviceType, targets) ->
                    async {
                        checkService(serviceType, targets)
                    }
                }.awaitAll()

                val servicesCheckResult = ServiceTargets.ProfileServicesCheckResult(
                    serviceResults.associateBy { it.serviceType }
                )

                // Останавливаем процесс
                process?.destroy()
                try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}

                logger.info("[ProfileNetworkChecker] Profile check completed for ${profile.fileName}. All services success: ${servicesCheckResult.isAllSuccess()}")
                ProfileCheckResult.ServicesResult(servicesCheckResult)
            } catch (e: ProcessUtils.ProcessNotFoundException) {
                try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}
                logger.error("[ProfileNetworkChecker] ${e.message}")
                ProfileCheckResult.Error(e.message ?: "Process $winwsProcessName is not found")
            } catch (e: Exception) {
                try { ProcessUtils.killProcess(winwsProcessName, logger) } catch (_: Exception) {}
                logger.error("[ProfileNetworkChecker] Error: ", e)
                ProfileCheckResult.Error(e.message ?: "Unknown error")
            }
        }

    /**
     * Проверяет доступность всех целевых адресов для сервиса
     */
    private suspend fun checkService(
        serviceType: ServiceTargets.ServiceType,
        targets: List<String>
    ): ServiceTargets.ServiceCheckResult = withContext(Dispatchers.IO) {
        val results = targets.map { target ->
            async {
                val isSuccess = if (target.startsWith("PING:")) {
                    val ip = target.removePrefix("PING:")
                    testPing(ip)
                } else {
                    testConnection(target)
                }
                // Возвращаем пару (target, isSuccess) для безопасного сбора результатов
                target to isSuccess
            }
        }.awaitAll()
        
        val failedTargets = results.filter { !it.second }.map { it.first }
        val isSuccess = results.all { it.second }
        
        ServiceTargets.ServiceCheckResult(
            serviceType = serviceType,
            isSuccess = isSuccess,
            failedTargets = failedTargets
        )
    }

    /**
     * Проверяет доступность через ping
     */
    private fun testPing(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isReachable(3000) // 3 секунды таймаут
        } catch (e: Exception) {
            logger.warn("[ProfileNetworkChecker] Ping failed for $ip: ${e.message}")
            false
        }
    }

    private fun writeProfileCheckLog(profile: domain.model.Profile, args: String, domain: String, result: ProfileCheckResult, exception: Exception? = null) {
        try {
            val logsDir = appPathProvider.getLogsDir()
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir)
            }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
            val fileName = "profile_check_${profile.fileName}_$timestamp.log"
            val logFile = logsDir.resolve(fileName)
            val sb = StringBuilder()
            sb.appendLine("Profile: ${profile.fileName} (${profile.displayName ?: "no displayName"})")
            sb.appendLine("Args: $args")
            sb.appendLine("Domain: $domain")
            sb.appendLine("Timestamp: $timestamp")
            sb.appendLine("Result: ${when(result) { 
                is ProfileCheckResult.Success -> "Success"
                is ProfileCheckResult.Error -> "Error: ${result.message}"
                is ProfileCheckResult.ServicesResult -> {
                    val servicesStatus = result.servicesCheckResult.serviceResults.map { (serviceType, serviceResult) ->
                        "${serviceType.name}: ${if (serviceResult.isSuccess) "Success" else "Failed (${serviceResult.failedTargets.joinToString(", ")})"}"
                    }.joinToString("; ")
                    "Services check - $servicesStatus"
                }
            }}")
            exception?.let {
                sb.appendLine("Exception: ${it.message}")
                sb.appendLine(it.stackTraceToString())
            }
            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE_NEW)
        } catch (e: Exception) {
            logger.error("[ProfileNetworkChecker] Failed to write profile check log: ${e.message}")
        }
    }
}