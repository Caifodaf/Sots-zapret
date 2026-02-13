package org.cmdtype.sots.presentation.main.components

import presentation.viewmodel.RepositoryViewModel
import androidx.compose.runtime.Composable

object VersionBlockFactory {
    @Composable
    fun createStateFromDownloadState(
        downloadState: RepositoryViewModel.DownloadState,
        onUpdate: () -> Unit,
        onOpenGit: () -> Unit,
        onRetry: () -> Unit,
        onCheckUpdates: () -> Unit
    ): VersionBlockState {
        return when (downloadState) {
            is RepositoryViewModel.DownloadState.Success -> {
                VersionBlockStates.createStandardState(downloadState.version, onOpenGit = onOpenGit)
            }
            is RepositoryViewModel.DownloadState.UpdateAvailable -> {
                VersionBlockStates.createUpdateAvailableState(
                    currentVersion = downloadState.currentVersion,
                    newVersion = downloadState.newVersion,
                    onUpdate = onUpdate
                )
            }
            is RepositoryViewModel.DownloadState.CheckingUpdates -> {
                VersionBlockStates.createCheckingUpdatesState()
            }
            is RepositoryViewModel.DownloadState.Downloading -> {
                VersionBlockStates.createDownloadingState()
            }
            is RepositoryViewModel.DownloadState.DownloadingProfiles -> {
                VersionBlockStates.createDownloadingProfilesState()
            }
            is RepositoryViewModel.DownloadState.Processing -> {
                VersionBlockStates.createDownloadingState("ms_version_processing_files")
            }
            is RepositoryViewModel.DownloadState.Error -> {
                VersionBlockStates.createErrorState(onRetry)
            }
            is RepositoryViewModel.DownloadState.AppInstallerDownloading -> {
                VersionBlockStates.createAppInstallerDownloadingState()
            }
            is RepositoryViewModel.DownloadState.AppInstallerDownloadError -> {
                VersionBlockStates.createAppInstallerDownloadErrorState(onRetry)
            }
            is RepositoryViewModel.DownloadState.AppUpdateAvailable -> {
                VersionBlockStates.createAppUpdateAvailableState(
                    currentVersion = downloadState.currentVersion,
                    newVersion = downloadState.newVersion,
                    onUpdate = onUpdate
                )
            }
            is RepositoryViewModel.DownloadState.Idle -> {
                VersionBlockStates.createStandardState(onOpenGit = onOpenGit)
            }
        }
    }
} 