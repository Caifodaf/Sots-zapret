package presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import systemBlue500
import systemGray200
import systemGray300
import systemGray400
import systemGray500
import systemGray700
import systemGrayIcon
import systemWhite900
import androidx.compose.animation.core.animateDpAsState
import theme.Strings

object UIScale {
    var scale: Float = 1f
    fun dp(value: Int): Dp = (value * scale).dp
    fun dp(value: Float): Dp = (value * scale).dp
    fun sp(value: Int): TextUnit = (value * scale).sp
    fun sp(value: Float): TextUnit = (value * scale).sp
}

object UIComponents {
    val sizeWindowWidth = UIScale.dp(180)
    val sizeWindowHeight = UIScale.dp(300)
    val corner_radius = UIScale.dp(16)
    val corner_radius_settings = UIScale.dp(10)
    val corner_radius_first = UIScale.dp(10)
}

enum class TextStylesType(
    val fontWeight: FontWeight = FontWeight.Medium,
    val fontSize: TextUnit,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val color: Color = systemWhite900
) {
    Label(
        fontSize = UIScale.sp(8),
        lineHeight = UIScale.sp(9.9f),
        color = systemGray700
    ),
    LabelDonat(
        fontWeight = FontWeight.Normal,
        fontSize = UIScale.sp(7),
        lineHeight = UIScale.sp(9.9f),
        color = systemGrayIcon
    ),
    LabelDrop(
        fontSize = UIScale.sp(9),
        lineHeight = UIScale.sp(10),
    ),
    LabelFilter(
        fontSize = UIScale.sp(6),
        lineHeight = UIScale.sp(9.9f),
        fontWeight = FontWeight.Normal
    ),
    LabelLink(
        fontSize = UIScale.sp(7),
        lineHeight = UIScale.sp(9.9f),
        color = systemGray700
    ),
    LabelHint(
        fontSize = UIScale.sp(7),
        lineHeight = UIScale.sp(8),
        color = systemGray400
    ),
    LabelWIP(
        fontWeight = FontWeight.Normal,
        fontSize = UIScale.sp(7),
        lineHeight = UIScale.sp(8),
        color = systemGrayIcon
    ),
    LabelHintSettings(
        fontSize = UIScale.sp(6),
        lineHeight = UIScale.sp(9.9f),
        color = systemGray500
    ),
    ButtonStart(
        fontSize = UIScale.sp(9),
    ),
    HeaderOne(
        fontWeight = FontWeight.Bold,
        fontSize = UIScale.sp(11),
        lineHeight = UIScale.sp(11),
        color = systemGrayIcon
    ),
    HeaderTwo(
        fontWeight = FontWeight.SemiBold,
        fontSize = UIScale.sp(9),
        lineHeight = UIScale.sp(9),
        color = systemGrayIcon
    ),
    HeaderFirst(
        fontWeight = FontWeight.Medium,
        fontSize = UIScale.sp(11),
        lineHeight = UIScale.sp(11),
        color = systemGrayIcon
    ),
}

@Composable
fun TextStyles(text: String, type: TextStylesType, color: Color = type.color, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    val customSelectionColors = TextSelectionColors(
        handleColor = Color.White,
        backgroundColor = systemGray300
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        Text(
            textAlign = textAlign,
            text = Strings.get(text),
            color = color,
            fontFamily = InterFontFamily,
            fontWeight = type.fontWeight,
            fontSize = type.fontSize,
            lineHeight = type.lineHeight,
            modifier = modifier
        )
    }
}

@Composable
fun TextStylesRaw(text: String, type: TextStylesType, color: Color = type.color, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    val customSelectionColors = TextSelectionColors(
        handleColor = Color.White,
        backgroundColor = systemGray300
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        Text(
            textAlign = textAlign,
            text = text,
            color = color,
            fontFamily = InterFontFamily,
            fontWeight = type.fontWeight,
            fontSize = type.fontSize,
            lineHeight = type.lineHeight,
            modifier = modifier
        )
    }
}

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    switchWidth: Dp = UIScale.dp(24),
    switchHeight: Dp = UIScale.dp(16),
    thumbSize: Dp = UIScale.dp(12),
    thumbPadding: Dp = UIScale.dp(2)
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - thumbPadding * 2 else 0.dp
    )
    Surface(
        modifier = modifier
            .width(switchWidth)
            .height(switchHeight)
            .clip(RoundedCornerShape(switchHeight / 2))
            .clickable { onCheckedChange(!checked) },
        color = if (checked) systemBlue500 else systemGray200
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .padding(thumbPadding)
                    .size(thumbSize)
                    .offset(x = thumbOffset)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
