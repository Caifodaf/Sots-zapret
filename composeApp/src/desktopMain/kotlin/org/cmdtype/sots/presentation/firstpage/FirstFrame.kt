package org.cmdtype.sots.presentation.firstpage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import presentation.viewmodel.GeneralViewmodel
import util.Screen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import presentation.theme.UIComponents.corner_radius_first
import systemWhiteAlpha900
import org.jetbrains.compose.resources.DrawableResource
import sots.composeapp.generated.resources.back_logo_gray
import sots.composeapp.generated.resources.image_info_four
import sots.composeapp.generated.resources.image_info_one
import sots.composeapp.generated.resources.image_info_three
import sots.composeapp.generated.resources.image_info_two
import sots.composeapp.generated.resources.logos_first
import systemGray300
import systemGray800
import systemGrayIcon
import systemGrayMine
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIScale

private data class FirstInfoFrameData(
    val image: DrawableResource,
    val textL: String
)

@Composable
@Preview
fun FirstFrame(vm: GeneralViewmodel) {
    val infoList: List<FirstInfoFrameData> =
        listOf(
            FirstInfoFrameData(Res.drawable.image_info_one, "fs_one_info_block"),
            FirstInfoFrameData(Res.drawable.image_info_two, "fs_two_info_block"),
            FirstInfoFrameData(Res.drawable.image_info_three, "fs_three_info_block"),
            FirstInfoFrameData(Res.drawable.image_info_four, "fs_four_info_block"),
        )
    var infoIndex by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundDots()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LogoBlockFirst()
            InfoImageBlock(image = infoList[infoIndex].image, modifier = Modifier.weight(1f))
            DotsBlock(
                infoIndex = infoIndex,
                infoList = infoList,
                onInfoIndexChange = { newIndex -> infoIndex = newIndex }
            )
            Spacer(Modifier.height(UIScale.dp(8)))
            StartButtonBlockFirst(
                vm = vm,
                infoIndex = infoIndex,
                infoList = infoList,
                onIndexChange = { newIndex -> infoIndex = newIndex }
            )
            Spacer(Modifier.height(UIScale.dp(12)))
        }
    }
}

@Composable
private fun BackgroundDots() {
    Icon(
        painter = painterResource(Res.drawable.back_logo_gray),
        contentDescription = null,
        modifier = Modifier.size(UIScale.dp(200))
            .offset(y = UIScale.dp(-34)),
        tint = Color.Unspecified
    )
}

@Composable
private fun LogoBlockFirst() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = UIScale.dp(0)),
        contentAlignment = Alignment.TopCenter
    ) {
        Icon(
            painterResource(Res.drawable.logos_first),
            contentDescription = null,
            tint = systemGrayIcon,
            modifier = Modifier.size(
                width = UIScale.dp(56),
                height = UIScale.dp(16)
            )
        )
    }
}

@Composable
private fun InfoImageBlock(image: DrawableResource, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UIScale.dp(10), bottom = UIScale.dp(8)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DotsBlock(infoIndex: Int, infoList: List<FirstInfoFrameData>, onInfoIndexChange: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal =  UIScale.dp(12)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        TextStyles(
            textAlign = TextAlign.Center,
            text = infoList[infoIndex].textL,
            type = TextStylesType.HeaderFirst,
        )
        Spacer(Modifier.height(UIScale.dp(8)))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            infoList.forEachIndexed { idx, _ ->
                DotIndicator(isSelected = infoIndex == idx) { onInfoIndexChange(idx) }
                if (idx != infoList.lastIndex) Spacer(Modifier.width(UIScale.dp(3)))
            }
        }
    }
}

@Composable
private fun DotIndicator(isSelected: Boolean, onClick: () -> Unit) {
    val size: Dp by animateDpAsState(
        targetValue = if (isSelected) UIScale.dp(10) else UIScale.dp(3),
        animationSpec = tween(durationMillis = 200)
    )
    val color = if (isSelected) systemWhiteAlpha900 else systemGray300

    Box(
        modifier = Modifier
            .size(
                width = size,
                height = UIScale.dp(3)
            )
            .clip(shape = RoundedCornerShape(UIScale.dp(4)))
            .background(color)
            .clickable { onClick() }
    )
}

@Composable
private fun StartButtonBlockFirst(
    vm: GeneralViewmodel,
    infoIndex: Int,
    infoList: List<FirstInfoFrameData>,
    onIndexChange: (Int) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable {
                if (infoIndex < infoList.lastIndex) {
                    onIndexChange(infoIndex + 1)
                } else {
                    vm.vmSettings.saveReadingInfoStatus()
                    vm.navigateTo(Screen.Main)
                }
            }
            .padding(horizontal = UIScale.dp(10))
            .fillMaxWidth()
            .height(UIScale.dp(28))
            .clip(RoundedCornerShape(corner_radius_first))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(systemGray800)
        )

        val buttonText = if (infoIndex < infoList.lastIndex) "fs_btn_continue" else "fs_btn_launch"

        TextStyles(
            text = buttonText,
            type = TextStylesType.ButtonStart,
            color = systemGrayMine,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}