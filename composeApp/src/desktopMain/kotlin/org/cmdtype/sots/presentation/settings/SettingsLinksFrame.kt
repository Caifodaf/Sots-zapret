package org.cmdtype.sots.presentation.settings

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import presentation.viewmodel.GeneralViewmodel
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import systemGray50
import systemGrayIcon
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.ic_plus
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import systemGray300
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cmdtype.sots.presentation.settings.components.LinkItem
import org.cmdtype.sots.presentation.settings.components.TextFieldLinks
import theme.LocalLang
import presentation.viewmodel.LanqSelect

@Composable
@Preview
fun SettingsLinksFrame(vm: GeneralViewmodel) {
    val whiteListLinks by vm.vmRepository.whiteListLinks.collectAsState()
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }
    CompositionLocalProvider(LocalLang provides langCode) {
        LaunchedEffect(Unit) {
            vm.vmRepository.getWhiteList()
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Header()
            Spacer(Modifier.height(UIScale.dp(8)))

            TextFieldLinks(
                whiteListLinks?.toHashSet() ?: emptySet(),
                onAddLink = { newLink ->
                    withContext(Dispatchers.IO) {
                        val result = vm.vmRepository.addNewWhiteListLink(newLink, vm.serviceState.value)
                        result
                    }
                })
            Spacer(Modifier.height(UIScale.dp(8)))

           if (whiteListLinks.isNullOrEmpty()) {
               EmptyListState()
           } else {
               LinksList(links = whiteListLinks!!.reversed()) { link ->
                   vm.vmRepository.removeWhiteListLink(link)
               }
           }
        }
        AddLinkButton()
        Spacer(Modifier.height(UIScale.dp(12)))
    }
}

@Composable
private fun Header() {
    Spacer(Modifier.fillMaxWidth().height(UIScale.dp(16)))
    TextStyles(
        text = "st_links_title",
        type = TextStylesType.HeaderOne,
        modifier = Modifier.padding(horizontal = UIScale.dp(10))
    )
}

@Composable
private fun LinksList(links: List<String>, onDeleteLink: (String) -> Unit) {
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxWidth().height(UIScale.dp(160))) {
        LazyColumn(state = listState) {
            items(items = links, key = { it }) { link ->
                LinkItem(
                    link = link,
                    onDeleteClick = { onDeleteLink(link) },
                )
                Spacer(Modifier.height(UIScale.dp(2)))
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState = listState),
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = UIScale.dp(-4))
                .width(UIScale.dp(2)),
            style = ScrollbarStyle(
                minimalHeight = UIScale.dp(10),
                thickness = UIScale.dp(2),
                shape = RoundedCornerShape(UIScale.dp(4)),
                hoverColor = systemGray300,
                unhoverColor = systemGray300.copy(alpha = 0.5f),
                hoverDurationMillis = 300
            )
        )
    }
}

@Composable
private fun AddLinkButton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(UIScale.dp(36))
            .background(color = systemGray50)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UIScale.dp(28))
                .padding(top = UIScale.dp(4), start = UIScale.dp(10), end = UIScale.dp(10))
                .background(color = systemGrayIcon, shape = RoundedCornerShape(corner_radius_settings)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(Res.drawable.ic_plus),
                contentDescription = null,
                tint = systemGray50,
                modifier = Modifier.size(UIScale.dp(10))
            )
            Spacer(Modifier.width(UIScale.dp(4)))
            TextStyles(
                text = "st_links_add_bnt",
                type = TextStylesType.Label,
                color = systemGray50
            )
        }
    }
}

@Composable
private fun EmptyListState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextStyles(
            text = "st_links_list_is_empty",
            type = TextStylesType.Label,
            modifier = Modifier.padding(UIScale.dp(16)),
            textAlign = TextAlign.Center
        )
    }
}