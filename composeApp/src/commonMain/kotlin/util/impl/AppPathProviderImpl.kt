package util.impl

import util.path.PathFilesProject
import util.interfaces.IAppPathProvider
import java.nio.file.Path

class AppPathProviderImpl : IAppPathProvider {
    override fun getAppData(): String = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")

    override fun getLocalSotsDir(): Path = Path.of(getAppData(), PathFilesProject.LOCAL_SOTS_DIR)

    override fun getProfilesDir(): Path = getLocalSotsDir().resolve(PathFilesProject.PROFILES_DIR)

    override fun getCustomProfilesDir(): Path = getLocalSotsDir().resolve(PathFilesProject.CUSTOM_PROFILES_DIR)

    override fun getWhitelistDir(): Path = getLocalSotsDir().resolve(PathFilesProject.WHITELIST_DIR)

    override fun getWinDir(): Path = getLocalSotsDir().resolve(PathFilesProject.WIN_DIR)

    override fun getWinDirTLS(): Path = getLocalSotsDir().resolve(PathFilesProject.WIN_DIR_TLS)

    override fun getTempDir(): Path = Path.of(PathFilesProject.TEMP_DIR)

    override fun getLogsDir(): Path = getLocalSotsDir().resolve(PathFilesProject.LOGS_DIR)

    override fun getProfileCheckLogsDir(): Path = getLocalSotsDir().resolve(PathFilesProject.PROFILES_CHECK_LOGS_DIR)

    override fun getGameFilter(): String = PathFilesProject.GAME_FILTER














} 