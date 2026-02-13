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
import data.repo.WhitelistManagerImpl
import data.api.ApiUpdateServiceImpl
import data.api.SupabaseStorageApiImpl
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
import data.api.SupabaseStorageApi
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

val appModule = module {
    single { (scope: CoroutineScope) ->
        val appName = "Sots"
        val description = "Auto-start Sots app for bypassing locks"
        val exePath = try {
            val processPath = ProcessHandle.current().info().command().orElse(null)
            if (processPath != null && processPath.endsWith(".exe", ignoreCase = true))
                processPath else TODO()
        } catch (_: Exception) {
            System.getProperty("java.class.path")
        }
        val systemScriptService: ISystemScriptService = get()
        val startupManager: IStartupManager = StartupManagerImpl(appName, exePath, systemScriptService, description)
        SettingsViewModel(scope, startupManager)
    }
    single { (scope: CoroutineScope) ->
        val settingsViewModel = get<SettingsViewModel> { parametersOf(scope) }
        RepositoryViewModel(scope, settingsViewModel, get(), get(), get())
    }
    single { (scope: CoroutineScope) ->
        val settingsViewModel = get<SettingsViewModel> { parametersOf(scope) }
        val repositoryViewModel = get<RepositoryViewModel> { parametersOf(scope) }
        GeneralViewmodel(
            viewModelScope = scope,
            vmRepository = repositoryViewModel,
            vmSettings = settingsViewModel
        )
    }
    single<ILogger> { LoggerImpl(get()) }
    single<ISystemThemeProvider> { SystemThemeProviderImpl() }

    single<ServiceManager> { ServiceManagerImpl() }
    single<ProfileNetworkChecker> { ProfileNetworkCheckerImpl(get(), get(), get()) }
    single<SupabaseStorageApi> { SupabaseStorageApiImpl(logger = get()) }
    single<ApiUpdateService> {
        ApiUpdateServiceImpl(
            supabaseApi = get(),
            settingsViewModel = get(),
            logger = get(),
            appPathProvider = get(),
        )
    }
    single<IAppPathProvider> { AppPathProviderImpl() }
    single<ProfileService> { ProfileServiceImpl() }
    single<ProfileAdapterService> { ProfileAdapterService() }
    single<WinwsLauncherService> { WinwsLauncherServiceImpl() }
    single<WhitelistManager> { WhitelistManagerImpl(get()) }
    single<ISystemScriptService> { SystemScriptServiceImpl() }
    single<IProviderDetector> { IpApiProviderDetector(get()) }
    single<HostsUpdateService> { HostsUpdateServiceImpl(supabaseApi = get(), logger = get()) }
}
