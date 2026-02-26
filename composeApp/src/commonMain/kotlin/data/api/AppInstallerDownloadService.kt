package data.api

import java.nio.file.Path

sealed class AppInstallerDownloadResult {
    data class Success(val installerPath: Path) : AppInstallerDownloadResult()
    data class Error(val message: String) : AppInstallerDownloadResult()
}

interface AppInstallerDownloadService {
    suspend fun downloadAndRunLatestInstaller(): AppInstallerDownloadResult
}

