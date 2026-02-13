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
import androidx.compose.runtime.LaunchedEffect

sealed class ValidationResult {
    object Empty : ValidationResult()
    object Invalid : ValidationResult()
    object TooLong : ValidationResult()
    object Success : ValidationResult()
}

enum class TextFieldMailState {
    Idle, // обычное
    Error // ошибка валидации
}

@Composable
fun TextFieldMail(
    value: String = "",
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val label = "st_hint_textfield_name_email"

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var state by remember { mutableStateOf(TextFieldMailState.Idle) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(value) {
        if (textFieldValue.text != value) textFieldValue = TextFieldValue(value)
    }

    val borderColor = when (state) {
        TextFieldMailState.Error -> systemOrange500
        else -> Color.Transparent
    }

    fun validateInput(text: String): ValidationResult {
        val trimmed = text.trim()
        
        if (trimmed.isBlank()) return ValidationResult.Empty
        
        if (trimmed.length > 64) return ValidationResult.TooLong
        
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        if (emailRegex.matches(trimmed)) return ValidationResult.Success
        
        val usernameRegex = Regex("^[a-zA-Zа-яА-Я0-9._-]+$")
        if (usernameRegex.matches(trimmed) && trimmed.length >= 3) return ValidationResult.Success
        
        return ValidationResult.Invalid
    }

    fun sanitizeInput(input: String): String {
        return input
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
            .take(64) // Ограничение на 64 символа
    }

    fun validateAndUpdateState() {
        val text = textFieldValue.text
        state = when (validateInput(text)) {
            ValidationResult.Empty, ValidationResult.Invalid, ValidationResult.TooLong -> TextFieldMailState.Error
            ValidationResult.Success -> TextFieldMailState.Idle
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
                onValueChange = { newValue ->
                    val sanitizedText = sanitizeInput(newValue.text)
                    val newTextFieldValue = newValue.copy(text = sanitizedText)
                    textFieldValue = newTextFieldValue
                    
                    validateAndUpdateState()
                    onValueChange(sanitizedText)
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
                modifier = Modifier
                    .weight(1f)
                    .padding(start = UIScale.dp(0), end = UIScale.dp(0))
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    }
                    .focusRequester(focusRequester)
            )
        }
    }
}