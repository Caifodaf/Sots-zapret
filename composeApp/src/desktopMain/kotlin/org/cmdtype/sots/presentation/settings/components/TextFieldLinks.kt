package org.cmdtype.sots.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import systemGray100
import systemGray500
import systemGrayIcon
import systemGreen500
import systemOrange500
import systemWhite900
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_check
import sots.composeapp.generated.resources.ic_plus
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import systemGray300

sealed class AddLinkResult {
    object Success : AddLinkResult()
    object Empty : AddLinkResult()
    object Invalid : AddLinkResult()
    object Exists : AddLinkResult()
    object ExistsMain : AddLinkResult()
}

enum class AddLinkState {
    Idle, // обычное
    Active, // активное
    Empty, // попытка добавить пустое
    Adding, // добавление
    Invalid, // неправильная ссылка
    Exists, // уже есть
    ExistsMain, // уже есть api
    Error // error
}

@Composable
fun TextFieldLinks(
    whitelist: Set<String>,
    onAddLink: suspend (String) -> Result<Unit>,
    label: String = "st_hint_textfield_add_link",
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var state by remember { mutableStateOf(AddLinkState.Idle) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val iconRes = when {
        state == AddLinkState.Adding -> Res.drawable.ic_check
        textFieldValue.text.isNotEmpty() -> Res.drawable.ic_plus
        else -> null
    }
    val iconTint = when (state) {
        AddLinkState.Invalid, AddLinkState.Exists, AddLinkState.Empty -> systemOrange500
        AddLinkState.Adding -> systemGreen500
        else -> systemGrayIcon
    }
    val hintText = when (state) {
        AddLinkState.Idle -> "st_hint_label_add_link"
        AddLinkState.Active -> "st_hint_label_add_link_active"
        AddLinkState.Empty -> "st_hint_label_add_link_empty"
        AddLinkState.Adding -> "st_hint_label_add_link_adding"
        AddLinkState.Invalid -> "st_hint_label_add_link_invalid"
        AddLinkState.Exists -> "st_hint_label_add_link_exists"
        AddLinkState.ExistsMain -> "st_hint_label_add_link_existsapi"
        AddLinkState.Error -> "st_hint_label_add_link_error"
    }
    val hintColor = when (state) {
        AddLinkState.Invalid, AddLinkState.Exists, AddLinkState.ExistsMain -> systemOrange500
        AddLinkState.Error, -> systemOrange500
        AddLinkState.Active, -> systemGrayIcon
        else -> systemGray500
    }
    val borderColor = when (state) {
        AddLinkState.Active -> Color.Transparent
        AddLinkState.Invalid, AddLinkState.Exists, AddLinkState.ExistsMain, AddLinkState.Empty -> systemOrange500
        AddLinkState.Error, -> systemOrange500
        else -> Color.Transparent
    }

    fun validateLink(text: String): AddLinkResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return AddLinkResult.Empty
        if (!trimmed.contains(".")) return AddLinkResult.Invalid
        if (whitelist.contains(trimmed)) return AddLinkResult.Exists
        return AddLinkResult.Success
    }



    fun tryAddLink() {
        val text = textFieldValue.text
        when (val result = validateLink(text)) {
            AddLinkResult.Empty ->  state = AddLinkState.Empty
            AddLinkResult.Invalid -> state = AddLinkState.Invalid
            AddLinkResult.Exists -> state = AddLinkState.Exists
            AddLinkResult.ExistsMain -> state = AddLinkState.ExistsMain
            AddLinkResult.Success -> {
                state = AddLinkState.Adding
                coroutineScope.launch {
                    val result = onAddLink(text.trim())
                    if (result.isSuccess) {
                        delay(2000)
                        state = AddLinkState.Idle
                        textFieldValue = TextFieldValue("")
                        isFocused = false
                    } else {
                        val msg = result.exceptionOrNull()?.message.orEmpty()
                        when {
                            msg.contains("exists in general list") -> state = AddLinkState.ExistsMain
                            msg.contains("Link already exists in user links") -> state = AddLinkState.Exists
                            msg.contains("Invalid link format", true) -> state = AddLinkState.Invalid
                            else -> state = AddLinkState.Error
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(UIScale.dp(28))
            .padding(horizontal = UIScale.dp(10))
            .background(systemGray100, shape = RoundedCornerShape(UIScale.dp(12)))
            .border(width = UIScale.dp(1), color = borderColor, shape = RoundedCornerShape(UIScale.dp(12))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val customSelectionColors = TextSelectionColors(
            handleColor = Color.White,
            backgroundColor = systemGray300
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
            TextField(
                value = textFieldValue,
                onValueChange = {
                    val singleLineText = it.text.replace("\n", "").replace("\r", "")
                    val newValue = it.copy(text = singleLineText)
                    textFieldValue = newValue
                    state = if (newValue.text.isNotEmpty()) AddLinkState.Active else AddLinkState.Idle
                },
                placeholder = {
                    if (!isFocused && textFieldValue.text.isEmpty()) {
                        TextStyles(
                            text = label,
                            type = TextStylesType.LabelHint,
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    textColor = systemWhite900,
                    cursorColor = Color.White
                ),
                maxLines = 1,
                singleLine = true,
                enabled = state != AddLinkState.Adding,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = UIScale.dp(0), end = UIScale.dp(0))
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused && textFieldValue.text.isNotEmpty()) {
                            state = AddLinkState.Active
                        } else if (!focusState.isFocused && state == AddLinkState.Active) {
                            state = AddLinkState.Idle
                        }
                    }
                    .focusRequester(focusRequester)
                    .onKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            tryAddLink()
                            true
                        } else false
                    }
            )
        }
        Spacer(Modifier.width(UIScale.dp(4)))
        if (iconRes != null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(end = UIScale.dp(10))
                        .size(UIScale.dp(10))
                        .clickable(
                            enabled = textFieldValue.text.isNotEmpty() && state != AddLinkState.Adding,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            tryAddLink()
                        }
                )
            }
        }
    }
    Spacer(Modifier.height(UIScale.dp(6)))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextStyles(
            text = hintText,
            type = TextStylesType.LabelHintSettings,
            modifier = Modifier.padding(horizontal = UIScale.dp(10)),
            color = hintColor,
            textAlign = TextAlign.Center,
        )
    }

}