package util.impl

import util.interfaces.IStartupManager
import util.interfaces.ISystemScriptService

class StartupManagerImpl(
    private val appName: String,
    private val exePath: String,
    private val systemScriptService: ISystemScriptService,
    private val description: String
) : IStartupManager {
    override suspend fun setStartupEnabled(enabled: Boolean) {
        systemScriptService.setStartupEnabled(appName, exePath, description, enabled)
    }
    override suspend fun isStartupEnabled(): Boolean {
        return systemScriptService.isStartupEnabled(appName)
    }

    override fun getExePath(): String = exePath
}