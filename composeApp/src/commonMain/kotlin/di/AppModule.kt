package di

import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import util.impl.LoggerImpl
import org.koin.core.parameter.parametersOf
import util.interfaces.ILogger
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.RepositoryViewModel
import presentation.viewmodel.SettingsViewModel
import data.checker.ProfileNetworkCheckerImpl
import data.checker.IProfileCheckLogWriter
import data.checker.ProfileCheckLogWriterImpl
import data.repo.WhitelistManagerImpl
import data.api.ApiUpdateServiceImpl
import data.api.deprecated.SupabaseStorageApiImpl
import data.service.ProfileServiceImpl
import data.service.ServiceManagerImpl
import data.service.ProfileAdapterService
import data.service.WinwsLauncherServiceImpl
import data.checker.ProfileNetworkChecker
import domain.ServiceManager
import domain.WinwsLauncherService
import domain.repository.WhitelistManager
import domain.HostsUpdateService
import data.api.ApiUpdateService
import data.api.deprecated.SupabaseStorageApi
import data.api.GithubRawLinkApi
import data.api.GithubRawLinkApiImpl
import domain.repository.ProfileService
import data.service.HostsUpdateServiceImpl
import util.impl.SystemThemeProviderImpl
import util.interfaces.ISystemThemeProvider
import util.interfaces.IStartupManager
import util.impl.AppPathProviderImpl
import util.impl.StartupManagerImpl
import util.impl.SystemScriptServiceImpl
import util.interfaces.IAppPathProvider
import util.interfaces.ISystemScriptService
import util.impl.IpApiProviderDetector
import util.interfaces.IProviderDetector

private const val APP_NAME = "Sots"
private const val APP_DESCRIPTION = "Auto-start Sots app for bypassing locks"

private fun resolveExePath(): String {
    val fromProcess: String? = runCatching {
        ProcessHandle.current().info().command().orElse(null)
    }.getOrNull()

    return fromProcess
        ?.takeIf { it.endsWith(".exe", ignoreCase = true) }
        ?: runCatching { System.getProperty("java.class.path") }.getOrNull()
        ?: APP_NAME
}

val appModule = module {
    single { (scope: CoroutineScope) ->
        SettingsViewModel(scope, get())
    }
    single { (scope: CoroutineScope) ->
        val settingsViewModel = get<SettingsViewModel> { parametersOf(scope) }
        RepositoryViewModel(scope, settingsViewModel, get(), get(), get(), get(), get())
    }
    single { (scope: CoroutineScope, vmRepository: RepositoryViewModel, vmSettings: SettingsViewModel) ->
        GeneralViewmodel(
            viewModelScope = scope,
            vmRepository = vmRepository,
            vmSettings = vmSettings
        )
    }
    single<ILogger> { LoggerImpl(get()) }
    single<ISystemThemeProvider> { SystemThemeProviderImpl() }

    single<ServiceManager> { ServiceManagerImpl() }
    single<ProfileNetworkChecker> { ProfileNetworkCheckerImpl(get(), get(), get()) }
    single<IProfileCheckLogWriter> { ProfileCheckLogWriterImpl(get()) }
    single<SupabaseStorageApi> { SupabaseStorageApiImpl(logger = get()) }
    single<GithubRawLinkApi> { GithubRawLinkApiImpl(logger = get(), appPathProvider = get()) }
    single<ApiUpdateService> {
        ApiUpdateServiceImpl(
            githubRawApi = get(),
            settingsViewModel = get(),
            logger = get(),
            appPathProvider = get(),
        )
    }
    single<IStartupManager> { StartupManagerImpl(APP_NAME, resolveExePath(), get(), APP_DESCRIPTION) }
    single<IAppPathProvider> { AppPathProviderImpl() }
    single<ProfileService> { ProfileServiceImpl() }
    single<ProfileAdapterService> { ProfileAdapterService() }
    single<WinwsLauncherService> { WinwsLauncherServiceImpl() }
    single<WhitelistManager> { WhitelistManagerImpl(get()) }
    single<ISystemScriptService> { SystemScriptServiceImpl() }
    single<IProviderDetector> { IpApiProviderDetector(get()) }
    single<HostsUpdateService> { HostsUpdateServiceImpl(githubRawLinkApi = get(), logger = get()) }
}
