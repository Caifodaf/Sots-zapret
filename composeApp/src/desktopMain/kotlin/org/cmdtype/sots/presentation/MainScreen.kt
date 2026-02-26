package org.cmdtype.sots.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import presentation.theme.AppTypography
import theme.Strings
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIComponents.corner_radius
import presentation.theme.UIComponents.sizeWindowHeight
import presentation.theme.UIComponents.sizeWindowWidth
import presentation.theme.UIScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_close
import sots.composeapp.generated.resources.ic_settings
import java.awt.Desktop
import java.net.URI
import kotlin.system.exitProcess
import androidx.compose.foundation.layout.offset
import presentation.viewmodel.GeneralViewmodel
import sots.composeapp.generated.resources.ic_arrow_left
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import presentation.viewmodel.GeneralViewmodel.UrlBrowser
import org.cmdtype.sots.presentation.main.components.VersionBlockFactory
import org.cmdtype.sots.presentation.main.components.VersionBlockRow
import systemGray100
import systemGray300
import systemGray50
import util.Screen
import androidx.compose.ui.zIndex
import presentation.viewmodel.LanqSelect
import androidx.compose.runtime.CompositionLocalProvider
import presentation.viewmodel.RepositoryViewModel
import theme.LocalLang
import presentation.viewmodel.Settings
import java.awt.Window

@Composable
@Preview
fun MainScreen(vm: GeneralViewmodel, window: Window, screenScale: Float, content: @Composable () -> Unit) {
    val currentScreen by vm.currentScreen.collectAsState()
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }



    CompositionLocalProvider(LocalLang provides langCode) {
        MaterialTheme(typography = AppTypography) {
            Box(
                modifier = Modifier
                    .size(width = sizeWindowWidth, height = sizeWindowHeight)
                    .background(systemGray50, shape = RoundedCornerShape(corner_radius))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                ) {
                    Box( modifier = Modifier.zIndex(1f)){
                        HeaderBlock(vm, window, currentScreen, settings)
                    }
                    Box( modifier = Modifier.weight(1f).zIndex(0f)){
                        Column(
                            modifier = Modifier.offset(y = UIScale.dp(-6))
                        ) {
                            content()
                        }
                    }
                    if (currentScreen != Screen.Settings && currentScreen != Screen.Donat)
                        VersionBlock(vm)
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(vm: GeneralViewmodel, window: Window, currentScreen: Screen, settings: Settings) {
    Row(
        modifier = Modifier.fillMaxWidth().height(UIScale.dp(34)),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .zIndex(2f)
                .padding(start = UIScale.dp(10), top = UIScale.dp(10))
                .clickable (
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ){
                    if (currentScreen == Screen.Main) {
                        vm.navigateTo(Screen.Settings)
                    } else {
                        vm.navigateBack()
                    }
                },
            contentAlignment = Alignment.TopEnd
        ) {
            val iconRes = if (currentScreen == Screen.Main)
                Res.drawable.ic_settings else Res.drawable.ic_arrow_left

            if (currentScreen != Screen.First) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = systemGray300,
                    modifier = Modifier.size(UIScale.dp(14)).align(Alignment.TopStart)
                )
            }
        }
        
        Spacer(Modifier.weight(1f))

        if (currentScreen == Screen.Main) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(top = UIScale.dp(10))
                    .background(systemGray100, shape = RoundedCornerShape(UIScale.dp(24)))
                    .height(UIScale.dp(28))
                    .padding(start = UIScale.dp(10), end = UIScale.dp(10))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ){
                        vm.navigateTo(Screen.Donat)
                    }
            ) {
                TextStyles(
                    "ms_donat_main_screen",
                    type = TextStylesType.Label
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .zIndex(2f)
                .padding(end = UIScale.dp(10), top = UIScale.dp(10))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (settings.isVisibilityTray) {
                        window.isVisible = false
                    } else {
                        window.dispose()
                        exitProcess(0)
                    }
                },
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                painterResource(Res.drawable.ic_close),
                contentDescription = Strings.get("ms_close_hint"),
                tint = systemGray300,
                modifier = Modifier.size(UIScale.dp(14)).align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun VersionBlock(vm: GeneralViewmodel) {
    val downloadState by vm.vmRepository.downloadState.collectAsState()

    val onUpdate = remember(downloadState) {
        when (downloadState) {
            is RepositoryViewModel.DownloadState.UpdateAvailable -> {
                { vm.vmRepository.checkApiForUpdates() }
            }
            is RepositoryViewModel.DownloadState.AppUpdateAvailable -> {
                val newVersion = (downloadState as RepositoryViewModel.DownloadState.AppUpdateAvailable).newVersion
                {
                    vm.vmRepository.downloadAndRunAppInstaller(newVersion)
                }
            }
            else -> ({})
        }
    }

    val versionBlockState = VersionBlockFactory.createStateFromDownloadState(
        downloadState = downloadState,
        onUpdate = onUpdate,
        onOpenGit = { vm.openUrlBrowser(type = UrlBrowser.GIT) },
        onRetry = { vm.vmRepository.checkApiForUpdates() },
        onCheckUpdates = { vm.vmRepository.checkApiForUpdates() }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VersionBlockRow(
            row = versionBlockState.topRow,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(UIScale.dp(4)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = versionBlockState.bottomRow.hasAction) {
                    versionBlockState.bottomRow.action?.invoke()
                }
        ) {
            VersionBlockRow(
                row = versionBlockState.bottomRow,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(Modifier.height(UIScale.dp(12)))
}


