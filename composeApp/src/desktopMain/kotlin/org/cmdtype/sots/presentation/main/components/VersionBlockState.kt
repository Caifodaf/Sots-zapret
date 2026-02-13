package org.cmdtype.sots.presentation.main.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import theme.Strings
import systemBlue500
import systemGrayIcon
import systemOrange500
import systemWhiteAlpha300
import systemWhiteAlpha900
import util.path.NamespaceProject.APP_VERSION

data class VersionBlockState(
    val topRow: VersionBlockRow,
    val bottomRow: VersionBlockRow
)

data class VersionBlockRow(
    val hasIcon: Boolean = false,
    val text: String,
    val hasAction: Boolean = false,
    val action: (() -> Unit)? = null,
    val icon: VersionBlockIcon? = null,
    val iconColor: Color = systemGrayIcon,
    val textColor: Color = systemWhiteAlpha900,
    val isUnderlined: Boolean = false
)

enum class VersionBlockIcon {
    CHECK, DOWNLOAD, REFRESH, ALERT
}

object VersionBlockStates {
    @Composable
    fun createStandardState(versionApi: String? = null, onOpenGit: () -> Unit): VersionBlockState {
        val bottomRowText = if (versionApi != null) {
            Strings.getWithParams("ms_version_git_repo_with_version", APP_VERSION, versionApi)
        } else {
            Strings.get("ms_version_git_repo")
        }
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_actual_version_repo",
                hasAction = false,
                icon = VersionBlockIcon.CHECK,
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = bottomRowText,
                hasAction = true,
                action = onOpenGit,
                textColor = systemWhiteAlpha300
            )
        )
    }

    @Composable
    fun createUpdateAvailableState(currentVersion: String, newVersion: String, onUpdate: () -> Unit): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_update_version_repo",
                hasAction = false
            ),
            bottomRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_version_git_repo_update",
                hasAction = true,
                action = onUpdate,
                icon = VersionBlockIcon.DOWNLOAD,
                textColor = systemBlue500
            )
        )
    }

    @Composable
    fun createDownloadingState(progress: String? = null): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_downloading_version_repo",
                hasAction = false,
                icon = VersionBlockIcon.REFRESH
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = progress ?: "ms_version_git_repo_downloading",
                hasAction = false
            )
        )
    }

    @Composable
    fun createErrorState(onRetry: () -> Unit): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_error_download_version_repo",
                hasAction = false,
                icon = VersionBlockIcon.ALERT,
                iconColor = systemOrange500,
                textColor = systemOrange500
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_version_git_repo_error",
                hasAction = true,
                action = onRetry,
                isUnderlined = true
            )
        )
    }

    @Composable
    fun createAppInstallerDownloadingState(): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_downloading_installer",
                hasAction = false,
                icon = VersionBlockIcon.REFRESH
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_version_installer_downloading",
                hasAction = false
            )
        )
    }

    @Composable
    fun createAppInstallerDownloadErrorState(onRetry: () -> Unit): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_error_download_installer",
                hasAction = false,
                icon = VersionBlockIcon.ALERT,
                iconColor = systemOrange500,
                textColor = systemOrange500
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_version_installer_error",
                hasAction = true,
                action = onRetry,
                isUnderlined = true
            )
        )
    }

    @Composable
    fun createAppUpdateAvailableState(currentVersion: String, newVersion: String, onUpdate: () -> Unit): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_update_app_available",
                hasAction = false
            ),
            bottomRow = VersionBlockRow(
                hasIcon = true,
                text = Strings.getWithParams("ms_app_update_download", currentVersion, newVersion),
                hasAction = true,
                action = onUpdate,
                icon = VersionBlockIcon.DOWNLOAD,
                textColor = systemBlue500
            )
        )
    }

    @Composable
    fun createCheckingUpdatesState(): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_checking_updates",
                hasAction = false,
                icon = VersionBlockIcon.REFRESH
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_version_checking_updates",
                hasAction = false
            )
        )
    }

    @Composable
    fun createDownloadingProfilesState(): VersionBlockState {
        return VersionBlockState(
            topRow = VersionBlockRow(
                hasIcon = true,
                text = "ms_downloading_profiles",
                hasAction = false,
                icon = VersionBlockIcon.REFRESH
            ),
            bottomRow = VersionBlockRow(
                hasIcon = false,
                text = "ms_version_downloading_profiles",
                hasAction = false
            )
        )
    }
} 