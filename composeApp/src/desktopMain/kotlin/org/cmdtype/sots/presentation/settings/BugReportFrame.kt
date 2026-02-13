package org.cmdtype.sots.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cmdtype.sots.presentation.settings.components.TextFieldLinks
import org.cmdtype.sots.presentation.settings.components.TextFieldMail
import org.cmdtype.sots.presentation.settings.components.TextFieldBugReport
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.LanqSelect
import systemGray50
import systemGrayIcon
import theme.LocalLang
import androidx.compose.runtime.LaunchedEffect
import util.Screen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
@Preview
fun BugReportFrame(vm: GeneralViewmodel) {
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when (settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }
    val email by vm.bugReportEmail.collectAsState()
    val content by vm.bugReportContent.collectAsState()
    val buttonState by vm.bugReportButtonState.collectAsState()
    val currentScreen by vm.currentScreen.collectAsState()

    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.BugReportFrame && buttonState == GeneralViewmodel.BugReportButtonState.Success) {
            vm.resetBugReport()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLang provides langCode) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.height(UIScale.dp(200))) {
                Header()
                Spacer(Modifier.height(UIScale.dp(8)))
                TextFieldMail(
                    value = email,
                    onValueChange = { vm.setBugReportEmail(it) }
                )
                Spacer(Modifier.height(UIScale.dp(16)))
                SettingsTitleHeader("st_header_bug_report_field")
                Spacer(Modifier.height(UIScale.dp(8)))
                TextFieldBugReport(
                    modifier = Modifier.weight(1f),
                    value = content,
                    onValueChange = { vm.setBugReportContent(it) }
                )
            }
            Spacer(Modifier.height(UIScale.dp(8)))
            BugRequestButton(vm, buttonState, coroutineScope)
        }
    }
}

@Composable
private fun Header() {
    Spacer(Modifier.fillMaxWidth().height(UIScale.dp(16)))
    TextStyles(
        text = "st_title_bug_report",
        type = TextStylesType.HeaderOne,
        modifier = Modifier.padding(horizontal = UIScale.dp(10)),
    )
}

@Composable
private fun BugRequestButton(
    vm: GeneralViewmodel,
    buttonState: GeneralViewmodel.BugReportButtonState,
    coroutineScope: CoroutineScope
) {
    val isEnabled = buttonState == GeneralViewmodel.BugReportButtonState.Idle
    val buttonText = when (buttonState) {
        GeneralViewmodel.BugReportButtonState.Idle -> "st_bug_report_btn_idle"
        GeneralViewmodel.BugReportButtonState.Sending -> "st_bug_report_btn_load"
        GeneralViewmodel.BugReportButtonState.Success -> "st_bug_report_btn_complete"
        GeneralViewmodel.BugReportButtonState.Error -> "st_bug_report_btn_error"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UIScale.dp(36))
            .padding(horizontal = UIScale.dp(10))
            .background(color = systemGrayIcon, shape = RoundedCornerShape(corner_radius_settings))
            .clickable(enabled = isEnabled) {
                coroutineScope.launch {
                    vm.sendBugReport()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        TextStyles(
            text = buttonText,
            type = TextStylesType.Label,
            color = systemGray50,
            modifier = Modifier.padding(horizontal = UIScale.dp(4))
        )
    }
}