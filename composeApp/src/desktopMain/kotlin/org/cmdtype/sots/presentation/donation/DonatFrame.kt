package org.cmdtype.sots.presentation.donation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.GeneralViewmodel.UrlBrowser
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.back_frame_donat
import sots.composeapp.generated.resources.ic_copy
import sots.composeapp.generated.resources.main_logo
import systemGrayIcon
import theme.LocalLang
import presentation.viewmodel.LanqSelect

@Composable
fun DonatFrame(vm: GeneralViewmodel) {
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when (settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }
    CompositionLocalProvider(LocalLang provides langCode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DonatLogo()
                Spacer(Modifier.height(UIScale.dp(8)))
                DonatDescription()
                Spacer(Modifier.height(UIScale.dp(10)))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DonatQrBlock()
                }
                DonatButton(
                    onClick = { vm.openUrlBrowser(UrlBrowser.DONAT) },
                )
            }
        }
    }
}

@Composable
private fun DonatLogo() {
    Icon(
        painter = painterResource(Res.drawable.main_logo),
        contentDescription = null,
        tint = systemGrayIcon,
        modifier = Modifier.size(width = UIScale.dp(70), height = UIScale.dp(20))
            .offset(y = UIScale.dp(-10))
    )
}

@Composable
private fun DonatDescription() {
    TextStyles(
        text = "ms_donat_desc",
        type = TextStylesType.LabelDonat,
        modifier = Modifier.padding(horizontal = UIScale.dp(14)),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DonatQrBlock() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Image(
            painter = painterResource(Res.drawable.back_frame_donat),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = UIScale.dp(14))
                .padding(bottom = UIScale.dp(16), top = UIScale.dp(8))
        )
    }
}

@Composable
private fun DonatButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(bottom = UIScale.dp(10))
            .fillMaxWidth()
            .height(UIScale.dp(28))
            .padding(horizontal = UIScale.dp(28))
            .background(Color.White, RoundedCornerShape(UIScale.dp(20)))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_copy),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(UIScale.dp(10))
        )
        Spacer(Modifier.width(UIScale.dp(4)))
        TextStyles(
            text = "ms_donat_main_screen",
            type = TextStylesType.Label,
            color = Color.Black
        )
    }
}