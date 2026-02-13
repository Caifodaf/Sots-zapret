package util.impl

import util.interfaces.ILogger
import util.interfaces.IAppPathProvider
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LoggerImpl(private val appPathProvider: IAppPathProvider) : ILogger {
    private var logFileInitialized = false
    private val logFilePath by lazy {
        val logsDir = appPathProvider.getLogsDir()
        logsDir.toFile().mkdirs()
        logsDir.resolve("app.log") as java.nio.file.Path
    }

    private fun writeErrorToFile(msg: String, throwable: Throwable?) {
        if (!logFileInitialized) {
            val logsDir = appPathProvider.getLogsDir()
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir)
            }
            logFileInitialized = true
        }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val errorMsg = buildString {
            append("[$timestamp] [ERROR] $msg\n")
            throwable?.let {
                append(it.stackTraceToString())
                append("\n")
            }
        }
        Files.writeString(
            logFilePath,
            errorMsg,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND
        )
    }

    override fun info(msg: String) {
        println("[INFO] $msg")
    }
    override fun error(msg: String, throwable: Throwable?) {
        println("[ERROR] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
        throwable?.printStackTrace()
        writeErrorToFile(msg, throwable)
    }
    override fun warn(msg: String, throwable: Throwable?) {
        println("[WARN] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
        throwable?.printStackTrace()
    }
    override fun debug(msg: String) {
        println("[DEBUG] $msg")
    }
} 