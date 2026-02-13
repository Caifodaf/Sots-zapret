package org.cmdtype.sots.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import presentation.theme.UIComponents.corner_radius
import presentation.theme.UIComponents.corner_radius_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.GeneralViewmodel.UrlBrowser
import util.Screen
import org.cmdtype.sots.presentation.VersionBlock
import org.cmdtype.sots.presentation.settings.components.TextFieldLinks
import org.jetbrains.compose.resources.DrawableResource
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.donation_back_st
import sots.composeapp.generated.resources.ic_arrow_right
import sots.composeapp.generated.resources.ic_flag_russia
import sots.composeapp.generated.resources.ic_flag_uk
import sots.composeapp.generated.resources.ic_theme_loon
import sots.composeapp.generated.resources.ic_theme_stars
import sots.composeapp.generated.resources.ic_theme_sun
import sots.composeapp.generated.resources.info_faq
import sots.composeapp.generated.resources.info_instruction
import systemGray100
import systemGray300
import systemGray50
import systemGray600
import systemGrayIcon
import systemGrayMine
import systemWhite900
import systemWhiteAlpha500
import systemOrange500
import presentation.theme.CustomSwitch
import theme.LocalLang
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import presentation.viewmodel.LanqSelect
import presentation.viewmodel.Settings
import presentation.viewmodel.ThemeSelect

private enum class TypeBlock{
    THEME, LANQ
}

@Composable
@Preview
fun SettingsFrame(vm: GeneralViewmodel) {
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }

    CompositionLocalProvider(LocalLang provides langCode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(systemGray50, shape = RoundedCornerShape(corner_radius))
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SettingsHeader()
                Spacer(Modifier.height(UIScale.dp(8)))

                TextFieldLinks(
                    vm.vmRepository.whiteListLinks.value?.toHashSet() ?: emptySet(),
                    onAddLink = { newLink ->
                        withContext(Dispatchers.IO) {
                            val result = vm.vmRepository.addNewWhiteListLink(newLink, vm.serviceState.value)
                            result
                        }
                    }
                )
                Spacer(Modifier.height(UIScale.dp(8)))

                GeneralSwitch(
                    isStartupSystem = settings.isStartupSystem,
                    isVisibilityTray = settings.isVisibilityTray,
                    onStartupSystemChange = { vm.onStartupSystemChange(it) },
                    onVisibilityTrayChange = { vm.onVisibilityTrayChange(it) }
                )
                Spacer(Modifier.height(UIScale.dp(16)))

                OpenLinksButton(vm)
                Spacer(Modifier.height(UIScale.dp(8)))
                UpdateHostsButton(vm)
                Spacer(Modifier.height(UIScale.dp(16)))

                ThemeBlocksRow(vm,settings)
                Spacer(Modifier.height(UIScale.dp(16)))

                LanguageBlock(vm,settings)
                Spacer(Modifier.height(UIScale.dp(16)))

                InfoBlock(vm)
                Spacer(Modifier.height(UIScale.dp(4)))

                DonationBlock(vm)
                Spacer(Modifier.height(UIScale.dp(16)))

                VersionBlock(vm)
            }
        }
    }
}

@Composable
private fun GeneralSwitch(
    isStartupSystem: Boolean,
    isVisibilityTray: Boolean,
    onStartupSystemChange: (Boolean) -> Unit,
    onVisibilityTrayChange: (Boolean) -> Unit
) {
    SettingsTitleHeader("st_header_general")
    Spacer(Modifier.height(UIScale.dp(8)))
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = UIScale.dp(10))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(UIScale.dp(16))
        ){
            TextStyles(
                text = "st_general_switch_startup_system",
                type = TextStylesType.LabelHint,
                color = systemGrayIcon
            )
            Spacer(Modifier.weight(1f))
            CustomSwitch(
                checked = isStartupSystem,
                onCheckedChange = { value -> onStartupSystemChange(value) }
            )
        }
        Spacer(Modifier.height(UIScale.dp(6)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(UIScale.dp(16))
        ){
            TextStyles(
                text = "st_general_switch_visible_tray",
                type = TextStylesType.LabelHint,
                color = systemGrayIcon
            )
            Spacer(Modifier.weight(1f))
            CustomSwitch(
                checked = isVisibilityTray,
                onCheckedChange = { value -> onVisibilityTrayChange(value) }
            )
        }
    }
}


@Composable
private fun SettingsHeader() {
        Spacer(Modifier.fillMaxWidth().height(UIScale.dp(16)))
    TextStyles(
        text = "st_title",
        type = TextStylesType.HeaderOne,
        modifier = Modifier.padding(horizontal = UIScale.dp(10)),
    )
}

@Composable
fun SettingsTitleHeader(text: String = "") {
    TextStyles(
        text = text,
        type = TextStylesType.HeaderTwo,
        modifier = Modifier.padding(horizontal = UIScale.dp(10)),
    )
}



@Composable
private fun OpenLinksButton(vm: GeneralViewmodel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(10))
            .height(UIScale.dp(28))
            .background(systemWhite900, shape = RoundedCornerShape(corner_radius_settings))
            .clickable { vm.navigateTo(Screen.SettingsLinks) },
        contentAlignment = Alignment.Center
    ) {
        TextStyles(
            text = "st_btn_open_list",
            type = TextStylesType.Label,
            color = systemGray50,
        )
    }
}

@Composable
private fun UpdateHostsButton(vm: GeneralViewmodel) {
    val buttonState by vm.hostsUpdateButtonState.collectAsState()
    val message by vm.hostsUpdateMessage.collectAsState()
    val isEnabled = buttonState != GeneralViewmodel.HostsUpdateButtonState.Updating

    val buttonText = when (buttonState) {
        GeneralViewmodel.HostsUpdateButtonState.Updating -> message
        GeneralViewmodel.HostsUpdateButtonState.Success -> message
        GeneralViewmodel.HostsUpdateButtonState.AlreadyUpdated -> message
        GeneralViewmodel.HostsUpdateButtonState.Error -> message
        GeneralViewmodel.HostsUpdateButtonState.Idle -> "st_btn_update_hosts"
    }

    val displayText = if (buttonState == GeneralViewmodel.HostsUpdateButtonState.Idle) {
        buttonText // Это будет локализованная строка
    } else {
        buttonText // Это уже сообщение из состояния
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(10))
            .height(UIScale.dp(28))
            .background(
                if (isEnabled) systemWhite900 else systemGray300,
                shape = RoundedCornerShape(corner_radius_settings)
            )
            .then(
                if (isEnabled) {
                    Modifier.clickable { vm.updateHostsList() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        TextStyles(
            text = displayText,
            type = TextStylesType.Label,
            color = if (isEnabled) systemGray50 else systemGray600,
        )
    }
}

@Composable
private fun ThemeBlocksRow(vm: GeneralViewmodel, settings: Settings) {
    val blockSpacing = UIScale.dp(4)
    var isThemeChanging by remember { mutableStateOf(false) }

    SettingsTitleHeader("st_header_theme")
    Spacer(Modifier.height(UIScale.dp(8)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(10)),
        horizontalArrangement = Arrangement.spacedBy(blockSpacing)
    ) {
        singleSettingsBlock(
            title = "st_theme_light",
            icon = Res.drawable.ic_theme_sun,
            typeBlock = TypeBlock.THEME,
            isSelected = settings.theme == ThemeSelect.Light,
            modifier = Modifier.weight(1f),
            enabled = !isThemeChanging,
            showSoonLabel = true,
            onClick = {
                //isThemeChanging = true
                //vm.vmSettings.setTheme(ThemeSelect.Light)
                //coroutineScope.launch {
                //    kotlinx.coroutines.delay(1500)
                //    isThemeChanging = false
                //}
            },
        )
        singleSettingsBlock(
            title = "st_theme_dark",
            icon = Res.drawable.ic_theme_loon,
            typeBlock = TypeBlock.THEME,
            isSelected = settings.theme == ThemeSelect.Dark,
            modifier = Modifier.weight(1f),
            enabled = !isThemeChanging,
            showSoonLabel = false,
            onClick = {
                //isThemeChanging = true
                //vm.vmSettings.setTheme(ThemeSelect.Dark)
                //coroutineScope.launch {
                //    kotlinx.coroutines.delay(1500)
                //    isThemeChanging = false
                //}
            },
        )
        singleSettingsBlock(
            title = "st_theme_oled",
            icon = Res.drawable.ic_theme_stars,
            typeBlock = TypeBlock.THEME,
            isSelected = settings.theme == ThemeSelect.DarkOLED,
            modifier = Modifier.weight(1f),
            enabled = !isThemeChanging,
            showSoonLabel = true,
            onClick = {
                //isThemeChanging = true
                //vm.vmSettings.setTheme(ThemeSelect.DarkOLED)
                //coroutineScope.launch {
                //    kotlinx.coroutines.delay(1500)
                //    isThemeChanging = false
                //}
            },
        )
    }
}

@Composable
private fun LanguageBlock(vm: GeneralViewmodel, settings: Settings) {
    val blockSpacing = UIScale.dp(4)

    SettingsTitleHeader("st_header_lanq")
    Spacer(Modifier.height(UIScale.dp(8)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(10)),
        horizontalArrangement = Arrangement.spacedBy(blockSpacing)
    ) {
        singleSettingsBlock(
            title = "st_language.ru",
            icon = Res.drawable.ic_flag_russia,
            typeBlock = TypeBlock.LANQ,
            isSelected = settings.language == LanqSelect.RU,
            modifier = Modifier.weight(1f),
            onClick = { vm.vmSettings.setLanguage(LanqSelect.RU) },
        )
        singleSettingsBlock(
            title = "st_language.en",
            icon = Res.drawable.ic_flag_uk,
            typeBlock = TypeBlock.LANQ,
            isSelected = settings.language == LanqSelect.UK,
            modifier = Modifier.weight(1f),
            onClick = { vm.vmSettings.setLanguage(LanqSelect.UK) },
        )
    }
}

@Composable
private fun InfoBlock(vm: GeneralViewmodel) {
    val blockSpacing = UIScale.dp(4)

    SettingsTitleHeader("st_header_other")
    Spacer(Modifier.height(UIScale.dp(8)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIScale.dp(10)),
        horizontalArrangement = Arrangement.spacedBy(blockSpacing)
    ) {
        infoSingleBlock(
            title = "st_info_faq",
            icon = Res.drawable.info_faq,
            modifier = Modifier.weight(1f),
            onClick = {vm.openUrlBrowser(type = UrlBrowser.FAQ)}
        )
        infoSingleBlock(
            title = "st_info_bug_report",
            icon = Res.drawable.info_instruction,
            modifier = Modifier.weight(1f),
            onClick = {vm.navigateTo(Screen.BugReportFrame)}
        )
    }
}

@Composable
private fun DonationBlock(vm: GeneralViewmodel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UIScale.dp(78))
            .padding(horizontal =  UIScale.dp(10))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ){
                vm.navigateTo(Screen.Donat)
            }
    ) {
        Image(
            painter = painterResource(Res.drawable.donation_back_st),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(UIScale.dp(10)),
            ) {
            TextStyles(
                text = "st_donation_label",
                type = TextStylesType.LabelHint,
                color = systemGrayIcon
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(Res.drawable.ic_arrow_right),
                contentDescription = null,
                tint = systemWhiteAlpha500,
                modifier = Modifier.size(UIScale.dp(9))
            )
        }
    }
}

@Composable
private fun infoSingleBlock(
    title: String,
    icon: DrawableResource,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(UIScale.dp(78))
            .background(systemGray100, shape = RoundedCornerShape(corner_radius_settings))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = UIScale.dp(10)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = UIScale.dp(10), end = UIScale.dp(8)),
            ) {
                TextStyles(
                    text = title,
                    type = TextStylesType.LabelHint,
                    color = systemGrayIcon
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painterResource(Res.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = systemGray600,
                    modifier = Modifier.size(UIScale.dp(9))
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = systemGray600,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = UIScale.dp(10), end = UIScale.dp(10), bottom = UIScale.dp(10))
                .height(UIScale.dp(32))
        )
    }
}

@Composable
private fun singleSettingsBlock(
    title: String,
    icon: DrawableResource,
    typeBlock: TypeBlock,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSoonLabel: Boolean = false,
    onClick: () -> Unit,
) {
    var textColor: Color
    var backColor: Color
    var iconTint: Color

    val iconSize = when (typeBlock) {
        TypeBlock.THEME -> 12
        TypeBlock.LANQ -> 16
    }

    when (isSelected) {
        true -> {
            textColor = systemGrayMine
            backColor = systemGrayIcon
            iconTint = systemGray100
        }

        false -> {
            textColor = systemGrayIcon
            backColor = systemGray100
            iconTint = systemGray300
        }
    }

    Box(
        modifier = modifier
            .height(UIScale.dp(64))
            .background(
                backColor,
                shape = RoundedCornerShape(corner_radius_settings)
            )
            .clipToBounds()
    ) {
        if (showSoonLabel) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(UIScale.dp(50))
                    .height(UIScale.dp(12))
                    .graphicsLayer {
                        rotationZ = 45f
                        translationY = 10f
                        translationX = 30f
                    }
                    .background(systemOrange500)
            ) {
                TextStyles(
                    text = "st_theme_in_progress",
                    type = TextStylesType.LabelWIP,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(UIScale.dp(8))
                .then(if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() } else Modifier),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (typeBlock == TypeBlock.LANQ) Color.Unspecified else iconTint,
                modifier = Modifier.size(UIScale.dp(iconSize))
            )
            Spacer(Modifier.weight(1f))
            TextStyles(
                text = title,
                type = TextStylesType.LabelHint,
                color = textColor
            )
        }
    }
}
