package org.cmdtype.sots.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import systemGray100
import org.jetbrains.compose.resources.painterResource
import sots.composeapp.generated.resources.ic_check
import sots.composeapp.generated.resources.Res
import systemGrayIcon
import sots.composeapp.generated.resources.ic_close
import sots.composeapp.generated.resources.ic_refresh
import systemGreen500
import systemRed500
import systemWhite900
import systemGray700
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.viewmodel.RepositoryViewModel
import domain.model.Profile
import sots.composeapp.generated.resources.ic_cloud_line
import sots.composeapp.generated.resources.ic_discord_line
import sots.composeapp.generated.resources.ic_google_line
import sots.composeapp.generated.resources.ic_riot_line
import sots.composeapp.generated.resources.ic_youtube_line
import data.checker.ServiceTargets

private val statusIconSize = UIScale.dp(10)
private val serviceIconBoxSize = UIScale.dp(14)
private val serviceIconInnerSize = UIScale.dp(8)
private val serviceIconBorderWidth = UIScale.dp(0.7f)
private val serviceIconSpacing = UIScale.dp(4)

@Composable
fun ProfileAutoItem(
    profile: Profile,
    name: String,
    status: RepositoryViewModel.ProfileCheckStatus = RepositoryViewModel.ProfileCheckStatus.Idle,
    serviceStatuses: Map<ServiceTargets.ServiceType, Boolean> = emptyMap(),
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .height(UIScale.dp(40))
            .padding(start = UIScale.dp(10), end = UIScale.dp(10))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = UIScale.dp(1.5f),
                        color = systemGray700,
                        shape = RoundedCornerShape(corner_radius_settings)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .background(systemGray100, shape = RoundedCornerShape(corner_radius_settings))
            .padding(horizontal = UIScale.dp(10), vertical = UIScale.dp(4))
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            TextStyles(
                text = name,
                type = TextStylesType.Label,
                color = systemGrayIcon,
                modifier = Modifier.weight(1f),
            )
            when (status) {
                RepositoryViewModel.ProfileCheckStatus.Checking -> Icon(
                    painter = painterResource(Res.drawable.ic_refresh),
                    contentDescription = "checking",
                    tint = systemGrayIcon,
                    modifier = Modifier.size(statusIconSize)
                )
                RepositoryViewModel.ProfileCheckStatus.Success -> Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = "success",
                    tint = systemGreen500,
                    modifier = Modifier.size(statusIconSize)
                )
                RepositoryViewModel.ProfileCheckStatus.Error -> Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = "error",
                    tint = systemRed500,
                    modifier = Modifier.size(statusIconSize)
                )
                else -> {}
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(serviceIconSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discord
                ServiceIcon(
                    painter = painterResource(Res.drawable.ic_discord_line),
                    serviceType = ServiceTargets.ServiceType.DISCORD,
                    serviceStatuses = serviceStatuses
                )
                // YouTube
                ServiceIcon(
                    painter = painterResource(Res.drawable.ic_youtube_line),
                    serviceType = ServiceTargets.ServiceType.YOUTUBE,
                    serviceStatuses = serviceStatuses
                )
                // Google
                ServiceIcon(
                    painter = painterResource(Res.drawable.ic_google_line),
                    serviceType = ServiceTargets.ServiceType.GOOGLE,
                    serviceStatuses = serviceStatuses
                )
                // Riot
                ServiceIcon(
                    painter = painterResource(Res.drawable.ic_riot_line),
                    serviceType = ServiceTargets.ServiceType.RIOT,
                    serviceStatuses = serviceStatuses
                )
                // Cloudflare
                ServiceIcon(
                    painter = painterResource(Res.drawable.ic_cloud_line),
                    serviceType = ServiceTargets.ServiceType.CLOUDFLARE,
                    serviceStatuses = serviceStatuses
                )
            }
        }
    }
}

@Composable
private fun ServiceIcon(
    painter: Painter,
    serviceType: ServiceTargets.ServiceType,
    serviceStatuses: Map<ServiceTargets.ServiceType, Boolean>
) {
    val status = serviceStatuses[serviceType]
    val borderColor = when (status) {
        true -> systemGreen500  // Зеленый если все IP доступны
        false -> systemRed500   // Красный если есть недоступные IP
        null -> systemWhite900     // Белый до проверки
    }
    
    Box(
        modifier = Modifier
            .size(serviceIconBoxSize)
            .border(
                width = serviceIconBorderWidth,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = systemGrayIcon,
            modifier = Modifier.size(serviceIconInnerSize)
        )
    }
}