package org.cmdtype.sots.presentation.profile.components

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
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import systemGray100
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.ic_check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import systemGrayIcon
import sots.composeapp.generated.resources.Res
import systemGreen500
import presentation.theme.TextStyles
import presentation.theme.TextStylesType

@Composable
fun ProfileItem(name: String, selected: Boolean = false, onSelectClick: () -> Unit) {
    var text by remember { mutableStateOf(name) }
    Row(
        modifier = Modifier
            .height(UIScale.dp(28))
            .padding(start = UIScale.dp(10), end = UIScale.dp(10))
            .background(systemGray100, shape = RoundedCornerShape(corner_radius_settings))
            .padding(horizontal = UIScale.dp(10))
            .clickable { onSelectClick() }
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextStyles(
            text = text,
            type = TextStylesType.Label,
            color = systemGrayIcon,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = "selected_profile",
                tint = systemGreen500,
                modifier = Modifier.size(UIScale.dp(14))
            )
        }
    }
}