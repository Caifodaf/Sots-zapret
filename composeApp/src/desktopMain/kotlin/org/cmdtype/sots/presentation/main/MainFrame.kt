package org.cmdtype.sots.presentation.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import presentation.viewmodel.GeneralViewmodel
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import presentation.theme.UIComponents.corner_radius
import systemGray100
import systemGray500
import systemGrayIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.back_logo_gray
import sots.composeapp.generated.resources.back_logo_green
import sots.composeapp.generated.resources.back_logo_orange
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.zIndex
import sots.composeapp.generated.resources.central_logo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextAlign
import systemGreen500
import systemOrange500
import theme.LocalLang
import presentation.viewmodel.GeneralViewmodel.ServiceStatus
import presentation.viewmodel.LanqSelect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sots.composeapp.generated.resources.btn_launch_back_white
import sots.composeapp.generated.resources.ic_arrow_bottom
import systemBlackIcon

@Composable
fun MainFrame(vm: GeneralViewmodel) {
    val settings by vm.vmSettings.settings.collectAsState()
    val serviceState by vm.serviceState.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }
   CompositionLocalProvider(LocalLang provides langCode) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedLogo(serviceState, modifier = Modifier.zIndex(0f))
            Box(
                modifier = Modifier.fillMaxSize().zIndex(1f)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Spacer(Modifier.height(UIScale.dp(56)))
                    LogoBlock()
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    AttentionBlock(serviceState)
                    Spacer(Modifier.height(UIScale.dp(8)))
                    ProfileBlock(vm, serviceState)
                    Spacer(Modifier.height(UIScale.dp(4)))
                    StartButtonBlock(serviceState, onClick = { vm.btnActivatedService() })
                    Spacer(Modifier.height(UIScale.dp(4)))
                }
            }
        }
    }
}

@Composable
@Preview
private fun ProfileBlock(vm: GeneralViewmodel, serviceState: ServiceStatus) {
    val selectedProfile by vm.vmRepository.selectedProfile.collectAsState()
    val isProfileChangeEnabled = serviceState != ServiceStatus.STARTING &&
        serviceState != ServiceStatus.ACTIVE &&
        serviceState != ServiceStatus.SHUTDOWN

    Row(
        modifier = Modifier
            .padding(horizontal = UIScale.dp(10))
            .fillMaxWidth()
            .then(
                if (isProfileChangeEnabled)
                    Modifier.clickable { vm.navigateTo(util.Screen.ProfileList) }
                else Modifier
            )
            .background(systemGray100, shape = RoundedCornerShape(corner_radius))
            .padding(start = UIScale.dp(12), end = UIScale.dp(12), top = UIScale.dp(10), bottom = UIScale.dp(10)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Column {
                TextStyles(
                    text = "ms_drop_top",
                    type = TextStylesType.LabelHint,
                )
                Spacer(Modifier.width(UIScale.dp(2)))
                TextStyles(
                    text = selectedProfile?.displayName?.toString() ?: "ms_drop_down",
                    type = TextStylesType.LabelDrop,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        if (isProfileChangeEnabled) {
            Column {
                Icon(
                    painterResource(Res.drawable.ic_arrow_bottom),
                    contentDescription = null,
                    tint = systemGray500,
                    modifier = Modifier
                        .size(UIScale.dp(10))
                        .graphicsLayer(rotationZ = 270f)
                )
            }
        }
    }
}

@Composable
@Preview
private fun LogoBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = UIScale.dp(8)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(Res.drawable.central_logo),
                contentDescription = null,
                modifier = Modifier.size(width = UIScale.dp(84), height = UIScale.dp(24)),
                tint = systemGrayIcon
            )

        }
    }
}

@Composable
@Preview
private fun AttentionBlock(serviceState: ServiceStatus,) {
    val (isEnable, text, textColor) = when (serviceState) {
        ServiceStatus.ACTIVE -> Triple(true, "ms_attention_hint_active", systemGreen500)
        ServiceStatus.SHUTDOWN -> Triple(true, "ms_attention_hint_off", systemGreen500)
        ServiceStatus.STARTING -> Triple(true, "ms_attention_hint_starting", systemGreen500)
        ServiceStatus.INACTIVE -> Triple(true, "", systemGreen500)
        ServiceStatus.ERROR -> Triple(true, "ms_attention_hint_error", systemOrange500)
        ServiceStatus.ERROR_NULL -> Triple(true, "ms_attention_hint_error_not_found_profiles", systemOrange500)
        ServiceStatus.ERROR_ANALOG -> Triple(true, "ms_attention_hint_error_analog", systemOrange500)
        ServiceStatus.ERROR_SERVICE_START -> Triple(true, "ms_attention_hint_error_service_start", systemOrange500)
        ServiceStatus.CHECK -> Triple(false, "ms_attention_hint_check", systemGray500)
        ServiceStatus.UNSELECTED -> Triple(false, "", systemGray500)
        ServiceStatus.AUTO_SELECT -> Triple(false, "", systemGray500)
        ServiceStatus.NON_ADMIN -> Triple(true, "ms_attention_hint_non_admin", systemOrange500)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(16)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnable) {
            TextStyles(
                text = text,
                type = TextStylesType.Label,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
@Preview
private fun StartButtonBlock(
    serviceState: ServiceStatus,
    onClick: () -> Unit
) {
    val (btnRes, btnText, textColor) = when (serviceState) {
        ServiceStatus.ACTIVE -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_active", systemBlackIcon)
        ServiceStatus.SHUTDOWN -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_off", systemBlackIcon)
        ServiceStatus.STARTING -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_starting", systemBlackIcon)
        ServiceStatus.INACTIVE -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_launch", systemBlackIcon)
        ServiceStatus.ERROR -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_try", systemBlackIcon)
        ServiceStatus.ERROR_NULL -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_try_error_not_found_profiles", systemBlackIcon)
        ServiceStatus.ERROR_ANALOG -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_off_analog", systemBlackIcon)
        ServiceStatus.ERROR_SERVICE_START -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_try", systemBlackIcon)
        ServiceStatus.CHECK -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_check", systemBlackIcon)
        ServiceStatus.UNSELECTED -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_unselect", systemBlackIcon)
        ServiceStatus.AUTO_SELECT -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_auto_select", systemBlackIcon)
        ServiceStatus.NON_ADMIN -> Triple(Res.drawable.btn_launch_back_white, "ms_start_btn_restart_admin", systemBlackIcon)
    }
    val corner = UIScale.dp(16)

    var isClickable by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(enabled = isClickable) {
                isClickable = false
                onClick()
                coroutineScope.launch {
                    delay(3000)
                    isClickable = true
                }
            }
            .padding(horizontal = UIScale.dp(10))
            .fillMaxWidth()
            .height(UIScale.dp(32))
            .clip(RoundedCornerShape(corner))
    ) {
        Image(
            painter = painterResource(btnRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        TextStyles(
            text = btnText,
            type = TextStylesType.ButtonStart,
            color = textColor
        )
    }
}

@Composable
@Preview
private fun AnimatedLogo(
    serviceState: ServiceStatus,
    modifier: Modifier = Modifier
) {
    val state = when (serviceState) {
        ServiceStatus.ACTIVE, ServiceStatus.SHUTDOWN -> LogoState.GREEN
        ServiceStatus.STARTING, ServiceStatus.INACTIVE, ServiceStatus.CHECK, ServiceStatus.UNSELECTED, ServiceStatus.AUTO_SELECT -> LogoState.GRAY
        ServiceStatus.ERROR,ServiceStatus.NON_ADMIN, ServiceStatus.ERROR_NULL, ServiceStatus.ERROR_ANALOG, ServiceStatus.ERROR_SERVICE_START -> LogoState.ORANGE
    }

    Crossfade(targetState = state, modifier = modifier) { logoState ->
        val logoRes = when (logoState) {
            LogoState.GREEN -> Res.drawable.back_logo_green
            LogoState.ORANGE -> Res.drawable.back_logo_orange
            LogoState.GRAY -> Res.drawable.back_logo_gray
        }
        Icon(
            painter = painterResource(logoRes),
            contentDescription = null,
            modifier = Modifier.size(UIScale.dp(200))
                .offset(y = UIScale.dp(-34)),
            tint = Color.Unspecified
        )
    }
}

private enum class LogoState { GREEN, ORANGE, GRAY }