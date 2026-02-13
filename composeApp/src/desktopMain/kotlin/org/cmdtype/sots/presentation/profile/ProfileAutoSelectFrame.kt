package org.cmdtype.sots.presentation.profile

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import domain.model.Profile
import domain.model.ProfileProviderType
import domain.model.ProfileServiceType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_arrow_bottom
import sots.composeapp.generated.resources.ic_filter
import systemGray100
import systemGray300
import systemGray50
import systemGray500
import systemGrayIcon
import theme.LocalLang
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIComponents.corner_radius
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.GeneralViewmodel.StatusAutoFrame.*
import presentation.viewmodel.LanqSelect
import org.cmdtype.sots.presentation.profile.components.ProfileAutoItem
import presentation.viewmodel.RepositoryViewModel
import kotlinx.coroutines.delay
import org.cmdtype.sots.presentation.profile.components.ServiceDropdownPopup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

@Composable
@Preview
fun ProfileAutoSelectFrame(vm: GeneralViewmodel) {
    val profileList by vm.vmRepository.profiles.collectAsState()
    val status by vm.profileAutoStatus.collectAsState()
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }

    var foundProfile by remember { mutableStateOf<Profile?>(null) }
    var selectedProfile by remember { mutableStateOf<Profile?>(null) }
    
    var isFilterVisible by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<FilterItem?>(null) }

    LaunchedEffect(Unit) {
        vm.setProfileAutoStatus(INACTIVE)
        vm.vmRepository.clearProfileCheckStatuses()
        foundProfile = null
        selectedProfile = null
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.vmRepository.cancelAutoCheckProfiles()
            vm.setProfileAutoStatus(INACTIVE)
        }
    }

    val filteredProfiles = selectedFilter?.let { filter ->
        when (filter.type) {
            FilterType.PROVIDER -> profileList.filter {
                it.provider.value == (filter.value as ProfileProviderType).value
            }
            FilterType.SERVICE -> profileList.filter { it.service == filter.value }
        }
    } ?: profileList

    fun startAutoCheck() {
        vm.setProfileAutoStatus(ACTIVE)
        foundProfile = null
        vm.vmRepository.autoCheckProfiles(filteredProfiles, "all") { profile ->
            if (foundProfile == null) {
                foundProfile = profile
                vm.setProfileAutoStatus(FOUND)
            }
        }
    }
    
    val isFilterEnabled = status != ACTIVE

    val (headerText, buttonText, buttonAction) = when (status) {
        FOUND -> Triple(
            "paf_heading_title", "paf_auto_select_btn_found", {
                (selectedProfile ?: foundProfile)?.let { profile ->
                    vm.vmRepository.selectProfile(profile)
                }
                vm.updateServiceStateAfterProfileSelection()
                vm.navigateTo(util.Screen.Main)
            }
        )
        INACTIVE -> Triple(
            "paf_heading_title", "paf_auto_select_btn", {
                val profile = selectedProfile
                if (profile != null) {
                    vm.vmRepository.selectProfile(profile)
                    vm.updateServiceStateAfterProfileSelection()
                    vm.navigateTo(util.Screen.Main)
                } else {
                    startAutoCheck()
                }
            }
        )
        ACTIVE -> Triple(
            "paf_heading_title_active", "paf_auto_select_btn_active", {
                vm.vmRepository.cancelAutoCheckProfiles()
                vm.setProfileAutoStatus(INACTIVE)
            })
        ERROR -> Triple(
            "paf_heading_title_error", "paf_auto_select_btn_error", {
                val profile = selectedProfile
                if (profile != null) {
                    vm.vmRepository.selectProfile(profile)
                    vm.updateServiceStateAfterProfileSelection()
                    vm.navigateTo(util.Screen.Main)
                } else {
                    startAutoCheck()
                }
            }
        )
        FAIL -> Triple(
            "paf_heading_title_fail", "paf_auto_select_btn_fail", {
                val profile = selectedProfile
                if (profile != null) {
                    vm.vmRepository.selectProfile(profile)
                    vm.updateServiceStateAfterProfileSelection()
                    vm.navigateTo(util.Screen.Main)
                } else {
                    startAutoCheck()
                }
            }
        )
    }

    val isDropdownEnabled = status != ACTIVE

    val profileWithStatuses by vm.vmRepository.profileWithStatuses.collectAsState()
    val filteredProfileFileNames = filteredProfiles.map { it.fileName }.toSet()
    val displayProfiles = profileWithStatuses.filter { 
        it.profile.fileName != "no_bypass" && it.profile.fileName in filteredProfileFileNames 
    }



    CompositionLocalProvider(LocalLang provides langCode) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(
                    headerText = headerText,
                    onFilterClick = if (isFilterEnabled) { { isFilterVisible = !isFilterVisible } } else null
                )
                Spacer(Modifier.height(UIScale.dp(4)))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UIScale.dp(166))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = isFilterVisible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ServiceFilterBlock(
                                selectedFilter = selectedFilter,
                                onSelect = { if (isFilterEnabled) selectedFilter = it },
                                isEnabled = isFilterEnabled
                            )
                        }
                        Spacer(Modifier.height(UIScale.dp(8)))
                        
                        if (profileList.isEmpty()) {
                            EmptyListState()
                        } else {
                            val filterHeight = if (isFilterVisible) UIScale.dp(36) else UIScale.dp(0)
                            val profileListHeight = UIScale.dp(166) - filterHeight - UIScale.dp(8)
                            ProfileList(
                                vm = vm,
                                items = displayProfiles,
                                selectedProfile = selectedProfile,
                                onProfileSelected = { selectedProfile = it },
                                height = profileListHeight
                            )
                        }
                    }
                }
            }
            AutoSelectProfileButton(
                buttonText,
                buttonAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(UIScale.dp(26))
            )
        }
    }
}

@Composable
private fun Header(headerText: String, onFilterClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UIScale.dp(16))
            .padding(horizontal = UIScale.dp(10))
    ) {
        TextStyles(
            text = headerText,
            type = TextStylesType.HeaderOne,
        )
        Spacer(Modifier.weight(1f))
        if (onFilterClick != null) {
            Icon(
                painterResource(Res.drawable.ic_filter),
                contentDescription = null,
                tint = systemGrayIcon,
                modifier = Modifier
                    .size(UIScale.dp(12))
                    .clickable { onFilterClick.invoke() }
            )
        }
    }
}

@Composable
private fun ServiceBlock(
    selectedService: String,
    serviceList: List<Pair<String, String>>,
    isDropdownEnabled: Boolean,
    onServiceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentService = serviceList.find { it.first == selectedService } ?: serviceList[0]
    val popupServiceList = serviceList.filter { it.first != selectedService }
    val popupShape = RoundedCornerShape(UIScale.dp(10))
    val popupWidth = UIScale.dp(160)

    Row(
        modifier = Modifier
            .padding(horizontal = UIScale.dp(10))
            .fillMaxWidth()
            .then(if (isDropdownEnabled) Modifier.clickable { expanded = !expanded } else Modifier)
            .background(systemGray100, shape = RoundedCornerShape(corner_radius))
            .padding(start = UIScale.dp(12), end = UIScale.dp(12), top = UIScale.dp(10), bottom = UIScale.dp(10)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Column {
                TextStyles(
                    text = "paf_drop_top",
                    type = TextStylesType.LabelHint,
                )
                Spacer(Modifier.width(UIScale.dp(2)))
                TextStyles(
                    text = currentService.second,
                    type = TextStylesType.LabelDrop,
                )
            }
            if (expanded) {
                ServiceDropdownPopup(
                    serviceList = popupServiceList,
                    onServiceSelected = {
                        onServiceSelected(it)
                        expanded = false
                    },
                    onDismiss = { expanded = false },
                    width = popupWidth,
                    shape = popupShape,
                    modifier = Modifier
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (isDropdownEnabled) {
            Icon(
                painterResource(Res.drawable.ic_arrow_bottom),
                contentDescription = null,
                tint = systemGray500,
                modifier = Modifier.size(UIScale.dp(10))
            )
        }
    }
}

@Composable
private fun ServiceFilterBlock(
    selectedFilter: FilterItem?,
    onSelect: (FilterItem?) -> Unit,
    isEnabled: Boolean
) {
    val filterList = buildList {
        add(FilterItem(type = FilterType.PROVIDER, value = null, label = "pf_filter_all"))
        ProfileProviderType.entries
            .filter { it != ProfileProviderType.PROVIDER_UNKNOWN }
            .forEach { provider ->
                add(FilterItem(FilterType.PROVIDER, provider, provider.name.lowercase()))
            }
        ProfileServiceType.entries
            .forEach { service ->
                add(FilterItem(FilterType.SERVICE, service, "service_${service.name.lowercase()}"))
            }
    }
    Box(Modifier.padding(horizontal = UIScale.dp(10))) {
        Column {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UIScale.dp(4)),
                verticalArrangement = Arrangement.spacedBy(UIScale.dp(2)),
                itemVerticalAlignment = Alignment.CenterVertically,
                maxItemsInEachRow = 5
            ) {
                filterList.forEach { filter ->
                    Box(
                        modifier = Modifier
                            .height(UIScale.dp(14))
                            .background(
                                if (selectedFilter == null && filter.value == null) systemGray300
                                else if (selectedFilter?.type == filter.type && selectedFilter.value == filter.value) systemGray300
                                else systemGray100,
                                shape = RoundedCornerShape(UIScale.dp(8))
                            )
                            .then(if (isEnabled) Modifier.clickable {
                                if (filter.value == null) onSelect(null)
                                else onSelect(filter)
                            } else Modifier)
                    ) {
                        TextStyles(
                            text = filter.label,
                            type = TextStylesType.LabelFilter,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = UIScale.dp(4))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileList(
    vm: GeneralViewmodel,
    items: List<RepositoryViewModel.ProfileWithStatus>,
    selectedProfile: Profile?,
    onProfileSelected: (Profile) -> Unit,
    height: Dp
) {
    val listState = rememberLazyListState()
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableStateOf(0L) }
    var shouldAutoScroll by remember { mutableStateOf(false) }
    
    val checkingIndex = items.indexOfFirst { it.status == RepositoryViewModel.ProfileCheckStatus.Checking }
    
    LaunchedEffect(checkingIndex, items) {
        if (checkingIndex >= 0) {
            println("Found checking item at index: $checkingIndex")
            shouldAutoScroll = true
        } else {
            shouldAutoScroll = false
        }
    }
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isUserScrolling = true
                    shouldAutoScroll = false
                    lastUserScrollTime = System.currentTimeMillis()
                } else {
                    delay(150)
                    if (System.currentTimeMillis() - lastUserScrollTime > 300) {
                        isUserScrolling = false
                        if (checkingIndex >= 0) {
                            shouldAutoScroll = true
                        }
                    }
                }
            }
    }
    
    LaunchedEffect(checkingIndex, shouldAutoScroll, items) {
        if (checkingIndex >= 0 && shouldAutoScroll && !isUserScrolling) {
            delay(100) // Задержка для стабилизации
            if (checkingIndex < items.size) {
                try {
                    println("Scrolling to index: $checkingIndex, current visible: ${listState.firstVisibleItemIndex}")
                    
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val isItemVisible = visibleItems.any { it.index == checkingIndex }
                    
                    if (!isItemVisible) {
                        listState.scrollToItem(
                            index = checkingIndex,
                            scrollOffset = 0
                        )
                        
                        delay(50)
                        val newVisibleItems = listState.layoutInfo.visibleItemsInfo
                        val isNowVisible = newVisibleItems.any { it.index == checkingIndex }
                        
                        if (!isNowVisible) {
                            listState.animateScrollToItem(
                                index = checkingIndex,
                                scrollOffset = 0
                            )
                        }
                    }
                } catch (e: Exception) {
                    println("Scroll error: ${e.message}")
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = items, key = { it.profile.fileName }) { item ->
                ProfileAutoItem(
                    profile = item.profile,
                    name = vm.vmRepository.getLocalizedProfileName(item.profile.fileName),
                    status = item.status,
                    serviceStatuses = item.serviceStatuses,
                    isSelected = selectedProfile?.fileName == item.profile.fileName,
                    onClick = { onProfileSelected(item.profile) }
                )
                Spacer(Modifier.height(UIScale.dp(2)))
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState = listState),
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = UIScale.dp(-4))
                .width(UIScale.dp(2)),
            style = ScrollbarStyle(
                minimalHeight = UIScale.dp(10),
                thickness = UIScale.dp(2),
                shape = RoundedCornerShape(UIScale.dp(4)),
                hoverColor = systemGray300,
                unhoverColor = systemGray300.copy(alpha = 0.5f),
                hoverDurationMillis = 300
            )
        )
    }
}

@Composable
private fun AutoSelectProfileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color = systemGray50)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UIScale.dp(28))
                .padding(top = UIScale.dp(4), start = UIScale.dp(10), end = UIScale.dp(10))
                .background(color = systemGrayIcon, shape = RoundedCornerShape(corner_radius_settings)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextStyles(
                text = text,
                type = TextStylesType.Label,
                color = systemGray50
            )
        }
    }
}

@Composable
private fun EmptyListState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextStyles(
            text = "st_links_list_is_empty",
            type = TextStylesType.Label,
            modifier = Modifier.padding(UIScale.dp(16)),
            textAlign = TextAlign.Center
        )
    }
}