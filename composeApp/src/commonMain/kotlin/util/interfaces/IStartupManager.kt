package util.interfaces

interface IStartupManager {
    suspend fun setStartupEnabled(enabled: Boolean)
    suspend fun isStartupEnabled(): Boolean
    fun getExePath(): String
} 