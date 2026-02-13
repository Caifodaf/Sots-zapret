package org.cmdtype.sots.presentation.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import systemGray100
import systemGray300
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity

/**
 * Выпадающий поп-ап для выбора сервиса.
 * @param serviceList список сервисов (пары: id и отображаемое имя)
 * @param onServiceSelected вызывается при выборе сервиса
 * @param onDismiss вызывается при закрытии поп-апа
 * @param width ширина поп-апа
 */
@Composable
fun ServiceDropdownPopup(
    serviceList: List<Pair<String, String>>,
    onServiceSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    width: Dp = UIScale.dp(160),
    shape: RoundedCornerShape = RoundedCornerShape(UIScale.dp(10)),
    modifier: Modifier
) {
    val density = LocalDensity.current
    val offsetX = with(density) { UIScale.dp(-10).roundToPx() }
    val offsetY = with(density) { UIScale.dp(30).roundToPx() }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(offsetX, offsetY),
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .background(systemGray100, shape = shape)
                .border(1.dp, systemGray300, shape)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                serviceList.forEachIndexed { idx, service ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onServiceSelected(service.first) }
                            .padding(vertical = UIScale.dp(8), horizontal = UIScale.dp(12))
                    ) {
                        TextStyles(
                            text = service.second,
                            type = TextStylesType.LabelDrop
                        )
                    }
                    if (idx < serviceList.lastIndex) {
                        Canvas(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)) {
                            drawRect(color = Color(0xFF333333))
                        }
                    }
                }
            }
        }
    }
} 