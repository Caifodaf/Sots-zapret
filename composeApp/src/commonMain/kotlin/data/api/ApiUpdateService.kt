package data.api

import java.nio.file.Path

sealed class ApiUpdateResult {
    object NoUpdate : ApiUpdateResult()
    data class UpdateAvailable(val remoteVersion: String) : ApiUpdateResult()
    data class AppUpdateAvailable(val currentVersion: String, val newVersion: String) : ApiUpdateResult()
    data class Error(val message: String) : ApiUpdateResult()
}

sealed class ApiDownloadResult {
    data class Success(val archivePath: Path) : ApiDownloadResult()
    data class Error(val message: String) : ApiDownloadResult()
}

sealed class ApiMergeResult {
    object Success : ApiMergeResult()
    data class Error(val message: String) : ApiMergeResult()
}

// Новый результат для загрузки только профилей
sealed class ProfilesDownloadResult {
    object Success : ProfilesDownloadResult()
    data class Error(val message: String) : ProfilesDownloadResult()
}

// Новый результат для одновременной проверки
data class ApiAndAppUpdateResult(
    val apiUpdateAvailable: Boolean,
    val remoteApiVersion: String?,
    val appUpdateAvailable: Boolean,
    val remoteAppVersion: String?
)

interface ApiUpdateService {
    suspend fun checkForApiUpdates(): ApiUpdateResult
    suspend fun checkForApiAndAppUpdates(): ApiAndAppUpdateResult // новый метод
    suspend fun downloadApi(): ApiDownloadResult
    suspend fun mergeApi(archivePath: Path, remoteApiVersion: String): ApiMergeResult
    suspend fun downloadProfilesOnly(): ProfilesDownloadResult // новый метод для загрузки только профилей
}