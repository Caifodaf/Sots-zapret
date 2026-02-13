package org.cmdtype.sots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.appModule
import presentation.viewmodel.GeneralViewmodel
import presentation.theme.UIComponents.sizeWindowHeight
import presentation.theme.UIComponents.sizeWindowWidth
import org.koin.core.context.GlobalContext.startKoin
import java.awt.Toolkit
import presentation.theme.UIScale
import org.koin.compose.koinInject
import org.cmdtype.sots.presentation.MainScreen
import org.cmdtype.sots.presentation.main.MainFrame
import util.Screen
import org.cmdtype.sots.presentation.settings.SettingsFrame
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import org.cmdtype.sots.presentation.donation.DonatFrame
import org.cmdtype.sots.presentation.firstpage.FirstFrame
import org.cmdtype.sots.presentation.settings.SettingsLinksFrame
import org.koin.core.parameter.parametersOf
import presentation.viewmodel.RepositoryViewModel
import presentation.viewmodel.SettingsViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.tray.impl.AwtTrayInitializer.dispose
import org.cmdtype.sots.presentation.profile.ProfileAutoSelectFrame
import org.cmdtype.sots.presentation.profile.ProfileListFrame
import org.jetbrains.compose.resources.painterResource
import org.koin.java.KoinJavaComponent
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.logo_tray
import theme.Strings
import util.interfaces.ILogger
import javax.swing.SwingUtilities
import kotlin.system.exitProcess
import java.net.ServerSocket
import data.service.VersionCleanupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import org.cmdtype.sots.presentation.settings.BugReportFrame
import java.net.Socket
import java.awt.SystemTray

fun main(args: Array<String>) {
    VersionCleanupService().runCleanupIfNeeded()
    val port = 46483
    val singleInstanceSocket = try {
        ServerSocket(port)
    } catch (e: Exception) {
        try {
            Socket("127.0.0.1", port).use { socket ->
                socket.getOutputStream().write("show\n".toByteArray())
            }
        } catch (_: Exception) {}
        return
    }
    startKoin {
        modules(appModule)
    }
    val isAutostart = args.contains("--autostart")
    application(exitProcessOnExit = false) {
        val scope = rememberCoroutineScope()
        val logger: ILogger = KoinJavaComponent.get(ILogger::class.java)
        
        logger.info("Application started with args: ${args.joinToString()}")
        logger.info("Is autostart: $isAutostart")

        val vmSet: SettingsViewModel = koinInject { parametersOf(scope) }
        val vmRep: RepositoryViewModel = koinInject { parametersOf(scope) }
        val vm: GeneralViewmodel = koinInject { parametersOf(scope, vmRep, vmSet) }

        val currentScreen by vm.currentScreen.collectAsState()
        val settings by vmSet.settings.collectAsState()

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenWidth = screenSize.width
        val screenHeight = screenSize.height

        val screenScale = when (screenHeight) {
            720 -> 1f
            1080 -> 1.5f
            1440 -> 2f
            2160 -> 3f
            else -> 1f
        }
        UIScale.scale = screenHeight.toFloat() / 720

        val stateX = screenWidth - 50 // Отступ от правого края
        val stateY = screenHeight - 60 // Отступ от нижнего края

        val state = rememberWindowState(
            size = DpSize(sizeWindowWidth, sizeWindowHeight),
            position = WindowPosition(stateX.dp - sizeWindowWidth, stateY.dp - sizeWindowHeight)
        )

        logger.info("Screen size: ${screenWidth}x${screenHeight} px")
        logger.info("Window size: ${state.size} dp")

        val isTraySupported = SystemTray.isSupported()
        logger.info("System tray supported: $isTraySupported")
        
        var isWindowVisible by remember {
            mutableStateOf(
                when {
                    isAutostart && settings.isVisibilityTray && isTraySupported -> false
                    isAutostart && (!settings.isVisibilityTray || !isTraySupported) -> true
                    else -> true
                }
            )
        }
        
        logger.info("Initial window visibility: $isWindowVisible")
        logger.info("Is autostart: $isAutostart, isVisibilityTray: ${settings.isVisibilityTray}, isTraySupported: $isTraySupported")
        var mainWindowRef: java.awt.Window? by remember { mutableStateOf(null) }

        val textLabelOpenWindow = Strings.get("tray_open_window")
        val textLabelCloseApp = Strings.get("tray_close_app")

        fun showMainWindow() {
            logger.info("Showing main window")
            isWindowVisible = true
            SwingUtilities.invokeLater {
                mainWindowRef?.let {
                    it.isVisible = true
                    it.toFront()
                }
            }
        }

        fun closeApp() {
            dispose()
            exitProcess(0)
        }

        if (settings.isVisibilityTray && isTraySupported) {
            Tray(
                iconContent = {
                    Icon(
                        painterResource(Res.drawable.logo_tray),
                        contentDescription = "",
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                tooltip = "Sots",
                primaryAction = { showMainWindow() },
                primaryActionLinuxLabel = "Sots"
            ) {
                Item(label = textLabelOpenWindow) { showMainWindow() }
                Divider()
                Item(label = textLabelCloseApp, isEnabled = true) {
                    closeApp()
                }
            }
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    val client = singleInstanceSocket.accept()
                    client.getInputStream().bufferedReader().use { reader ->
                        val line = reader.readLine()
                        if (line == "show") {
                            withContext(Dispatchers.Main) {
                                showMainWindow()
                            }
                        }
                    }
                    client.close()
                }
            }
        }

        if (isWindowVisible) {
            Window(
                onCloseRequest = {
                    isWindowVisible = false
                },
                state = state,
                title = "Sots",
                undecorated = true,
                resizable = false,
                transparent = true,
            ) {
                WindowDraggableArea {
                    Box(Modifier.fillMaxWidth().height(48.dp))
                }

                val window = this.window
                mainWindowRef = window

                MainScreen(vm, window, screenScale) {
                    logger.info("Swap screen: $currentScreen")
                    when (currentScreen) {
                        Screen.First -> FirstFrame(vm)
                        Screen.Main -> MainFrame(vm)
                        Screen.Donat -> DonatFrame(vm)
                        Screen.Settings -> SettingsFrame(vm)
                        Screen.SettingsLinks -> SettingsLinksFrame(vm)
                        Screen.ProfileList -> ProfileListFrame(vm)
                        Screen.AutoProfile -> ProfileAutoSelectFrame(vm)
                        Screen.BugReportFrame -> BugReportFrame(vm)
                    }
                }
            }
        }
    }
}