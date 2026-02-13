package data.checker

import domain.model.Profile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import util.interfaces.IAppPathProvider
import util.interfaces.ISystemScriptService
import domain.model.MachineInfo
import util.path.NamespaceProject.APP_VERSION
import domain.model.ProviderInfo

interface IProfileCheckLogWriter {
    fun createSessionLogFile(serviceDomain: String): Path
    suspend fun writeSessionStart(logFile: Path, serviceDomain: String, systemScriptService: ISystemScriptService, providerInfo: ProviderInfo?)
    fun appendProfileCheckLog(
        logFile: Path,
        profile: Profile,
        domain: String,
        result: ProfileCheckResult,
        exception: Exception? = null,
        args: String,
    )
    fun writeSessionEnd(logFile: Path)
}

class ProfileCheckLogWriterImpl(
    private val appPathProvider: IAppPathProvider
) : IProfileCheckLogWriter {
    private val logsDir: Path
        get() = appPathProvider.getLogsDir()

    private val profileArgsMap = mutableMapOf<String, String>()

    override fun createSessionLogFile(serviceDomain: String): Path {
        if (!Files.exists(logsDir)) Files.createDirectories(logsDir)
        val sessionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val safeDomain = serviceDomain.replace(Regex("[:/\\\\]"), "_")
        val logFile = logsDir.resolve("profile_check_session_${safeDomain}_$sessionTimestamp.log")
        Files.createFile(logFile)
        return logFile
    }

    override suspend fun writeSessionStart(logFile: Path, serviceDomain: String, systemScriptService: ISystemScriptService, providerInfo: ProviderInfo?) {
        val sessionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val sb = StringBuilder()
        sb.appendLine("Profile check session started: $sessionTimestamp")
        sb.appendLine("Service domain: $serviceDomain")
        sb.appendLine("App version: $APP_VERSION")
        if (providerInfo != null) {
            sb.appendLine("Provider info:")
            sb.appendLine("  ISP: ${providerInfo.isp}")
            sb.appendLine("  Org: ${providerInfo.org ?: "-"}")
            sb.appendLine("  Country: ${providerInfo.country ?: "-"}")
            //sb.appendLine("  IP: ${providerInfo.ip}")
        } else {
            sb.appendLine("Provider info: unavailable")
        }
        sb.appendLine("")
        Files.writeString(logFile, sb.toString(), StandardOpenOption.APPEND)
    }

    override fun appendProfileCheckLog(
        logFile: Path,
        profile: Profile,
        domain: String,
        result: ProfileCheckResult,
        exception: Exception?,
        args: String,
    ) {
        try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            val sb = StringBuilder()
            sb.appendLine("[${timestamp}] Profile: ${profile.fileName} (${profile.displayName ?: "no displayName"})")
            sb.appendLine("Domain: $domain")
            
            when (result) {
                is ProfileCheckResult.Success -> {
                    sb.appendLine("Result: Success")
                }
                is ProfileCheckResult.Error -> {
                    sb.appendLine("Result: Error: ${result.message}")
                }
                is ProfileCheckResult.ServicesResult -> {
                    sb.appendLine("> Starting config...")
                    sb.appendLine("> Running tests...")
                    sb.appendLine("")
                    
                    result.servicesCheckResult.serviceResults.forEach { (serviceType: ServiceTargets.ServiceType, serviceResult: ServiceTargets.ServiceCheckResult) ->
                        val targets = ServiceTargets.ALL_SERVICES[serviceType] ?: emptyList()
                        
                        targets.forEach { target: String ->
                            val isSuccess = !serviceResult.failedTargets.contains(target)
                            val status = if (isSuccess) "OK" else "Failed"
                            
                            val targetName = ServiceTargets.TARGET_NAMES[target] 
                                ?: target.removePrefix("https://").removePrefix("http://").split("/").firstOrNull() 
                                ?: target
                            
                            if (target.startsWith("PING:")) {
                                sb.appendLine("$targetName: $status")
                            } else {
                                sb.appendLine("$targetName: HTTP:$status, TLS1.2:$status, TLS1.3:$status")
                            }
                        }
                    }
                }
            }
            
            exception?.let {
                sb.appendLine("")
                sb.appendLine("Exception: ${it.message}")
                sb.appendLine(it.stackTraceToString())
            }
            sb.appendLine("")
            Files.writeString(logFile, sb.toString(), StandardOpenOption.APPEND)
            profileArgsMap[profile.fileName] = args
        } catch (e: Exception) {
        }
    }

    override fun writeSessionEnd(logFile: Path) {
        val sessionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val sb = StringBuilder()
        sb.appendLine("Profile check session completed: $sessionTimestamp\n")
        sb.appendLine("Profile arguments:")
        for ((name, args) in profileArgsMap) {
            sb.appendLine("")
            sb.appendLine("$name \\/")
            sb.appendLine(args)
        }
        sb.appendLine("")
        Files.writeString(
            logFile,
            sb.toString(),
            StandardOpenOption.APPEND
        )
    }
} 