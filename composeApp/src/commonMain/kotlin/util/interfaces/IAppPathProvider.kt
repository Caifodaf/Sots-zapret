package util.interfaces

import java.nio.file.Path

interface IAppPathProvider {
    fun getAppData(): String
    fun getLocalSotsDir(): Path
    fun getProfilesDir(): Path
    fun getCustomProfilesDir(): Path
    fun getWhitelistDir(): Path
    fun getWinDir(): Path
    fun getWinDirTLS(): Path
    fun getTempDir(): Path
    fun getLogsDir(): Path
    fun getProfileCheckLogsDir(): Path
    fun getGameFilter(): String
}