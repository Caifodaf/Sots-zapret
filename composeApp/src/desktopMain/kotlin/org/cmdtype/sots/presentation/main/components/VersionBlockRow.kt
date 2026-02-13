package org.cmdtype.sots.presentation.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import presentation.theme.TextStyles
import presentation.theme.TextStylesRaw
import presentation.theme.TextStylesType
import presentation.theme.UIScale
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_alert
import sots.composeapp.generated.resources.ic_check
import sots.composeapp.generated.resources.ic_download
import sots.composeapp.generated.resources.ic_refresh

@Composable
fun VersionBlockRow(
    row: VersionBlockRow,
    modifier: Modifier = Modifier
) {
    val iconRes = when (row.icon) {
        VersionBlockIcon.CHECK -> Res.drawable.ic_check
        VersionBlockIcon.DOWNLOAD -> Res.drawable.ic_download
        VersionBlockIcon.REFRESH -> Res.drawable.ic_refresh
        VersionBlockIcon.ALERT -> Res.drawable.ic_alert
        null -> null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        if (row.hasIcon && iconRes != null) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = row.iconColor,
                modifier = Modifier.size(UIScale.dp(10))
            )
            Spacer(Modifier.width(2.dp))
        }

        val isLocalizationKey = row.text.startsWith("ms_")
        
        if (isLocalizationKey) {
            TextStyles(
                text = row.text,
                type = TextStylesType.Label,
                color = row.textColor,
                modifier = Modifier.clickable(enabled = row.hasAction) {
                    row.action?.invoke()
                }
            )
        } else {
            TextStylesRaw(
                text = row.text,
                type = TextStylesType.Label,
                color = row.textColor,
                modifier = Modifier.clickable(enabled = row.hasAction) {
                    row.action?.invoke()
                }
            )
        }
    }
} 