package org.cmdtype.sots.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import systemGray100
import systemGray700
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_close
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import systemGrayIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import systemGray300

@SuppressWarnings("deprecation")
@Composable
fun LinkItem(link: String, onDeleteClick: () -> Unit) {
    var text by remember { mutableStateOf(link) }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboard.current
    Row(
        modifier = Modifier
            .height(UIScale.dp(28))
            .padding(start = UIScale.dp(10), end = UIScale.dp(10))
            .background(systemGray100, shape = RoundedCornerShape(corner_radius_settings))
            .padding(horizontal = UIScale.dp(10)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val scope = rememberCoroutineScope()
        val customSelectionColors = TextSelectionColors(
            handleColor = Color.White,
            backgroundColor = systemGray300
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
            BasicTextField(
                value = text,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        scope.launch {
                            clipboardManager.setClipEntry(ClipEntry(AnnotatedString(text)))
                        }
                        copied = true
                    },
                textStyle = TextStyle(color = if (copied) Color.Green else systemGrayIcon)
            )
        }
        if (copied) {
            LaunchedEffect(Unit) {
                delay(700)
                copied = false
            }
        }
        Icon(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "st_links_delete_hint",
            tint = systemGray700,
            modifier = Modifier
                .size(UIScale.dp(10))
                .clickable { onDeleteClick() }
        )
    }
}