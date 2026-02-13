package org.cmdtype.sots.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import systemGray100
import systemGray300
import systemGray500
import systemGrayIcon
import systemOrange500
import systemWhite900
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusTarget

// Результат валидации для отчета об ошибках
sealed class BugReportValidationResult {
    object Empty : BugReportValidationResult()
    object TooLong : BugReportValidationResult()
    object ContainsUnsafeContent : BugReportValidationResult()
    object Success : BugReportValidationResult()
}

// Состояния поля ввода отчета об ошибках
enum class TextFieldBugReportState {
    Idle, // обычное
    Error // ошибка валидации
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextFieldBugReport(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {}
) {
    val label = "st_hint_textfield_bug_report"
    val maxLength = 1024

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (textFieldValue.text != value) textFieldValue = TextFieldValue(value)
    }
    var state by remember { mutableStateOf(TextFieldBugReportState.Idle) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val borderColor = when (state) {
        TextFieldBugReportState.Error -> systemOrange500
        else -> Color.Transparent
    }

    // Проверка на небезопасный контент
    fun containsUnsafeContent(text: String): Boolean {
        val unsafePatterns = listOf(
            Regex("<script", RegexOption.IGNORE_CASE),
            Regex("javascript:", RegexOption.IGNORE_CASE),
            Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE),
            Regex("data:text/html", RegexOption.IGNORE_CASE),
            Regex("vbscript:", RegexOption.IGNORE_CASE),
            Regex("expression\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("url\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("eval\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("document\\.", RegexOption.IGNORE_CASE),
            Regex("window\\.", RegexOption.IGNORE_CASE),
            Regex("alert\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("confirm\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("prompt\\s*\\(", RegexOption.IGNORE_CASE)
        )
        
        return unsafePatterns.any { it.containsMatchIn(text) }
    }

    fun validateBugReport(text: String): BugReportValidationResult {
        val trimmed = text.trim()
        
        if (trimmed.isBlank()) return BugReportValidationResult.Empty
        
        if (trimmed.length > maxLength) return BugReportValidationResult.TooLong
        
        if (containsUnsafeContent(trimmed)) return BugReportValidationResult.ContainsUnsafeContent
        
        return BugReportValidationResult.Success
    }

    fun sanitizeInput(input: String): String {
        return input
            .replace("\r", "\n") // Нормализация переносов строк
            .take(maxLength) // Ограничение на 512 символов
    }

    fun validateAndUpdateState() {
        val text = textFieldValue.text
        state = when (validateBugReport(text)) {
            BugReportValidationResult.Empty, 
            BugReportValidationResult.TooLong, 
            BugReportValidationResult.ContainsUnsafeContent -> TextFieldBugReportState.Error
            BugReportValidationResult.Success -> TextFieldBugReportState.Idle
        }
        
        if (state == TextFieldBugReportState.Idle) {
            onValueChange(text)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(UIScale.dp(100))
            .padding(horizontal = UIScale.dp(10))
            .focusable()
            .focusTarget()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                })
            },
        contentAlignment = Alignment.TopStart
    ) {
        val customSelectionColors = TextSelectionColors(
            handleColor = Color.White,
            backgroundColor = systemGray300
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(systemGray100, shape = RoundedCornerShape(UIScale.dp(12)))
                    .border(width = UIScale.dp(1), color = borderColor, shape = RoundedCornerShape(UIScale.dp(12)))
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val sanitizedText = sanitizeInput(newValue.text)
                        val newTextFieldValue = newValue.copy(text = sanitizedText)
                        textFieldValue = newTextFieldValue
                        validateAndUpdateState()
                        onValueChange(sanitizedText)
                    },
                    textStyle = TextStyle(
                        color = systemGrayIcon,
                        fontSize = UIScale.sp(7),
                        lineHeight = UIScale.sp(8)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = UIScale.dp(10), end = UIScale.dp(10), top = UIScale.dp(8), bottom = UIScale.dp(18))
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                if (!isFocused) focusRequester.requestFocus()
                            })
                        },
                    maxLines = 64,
                    cursorBrush = SolidColor(systemGrayIcon),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!isFocused && textFieldValue.text.isEmpty()) {
                                TextStyles(
                                    text = label,
                                    type = TextStylesType.LabelHint,
                                    modifier = Modifier.padding(start = UIScale.dp(10), top = UIScale.dp(8))
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                // Счетчик символов в правом нижнем углу
                val charCount = textFieldValue.text.length
                val isOverLimit = charCount > maxLength
                TextStyles(
                    text = "$charCount/$maxLength",
                    type = TextStylesType.LabelHintSettings,
                    color = if (isOverLimit) systemOrange500 else systemGray500,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = UIScale.dp(8), bottom = UIScale.dp(4))
                )
            }
        }
    }
} 