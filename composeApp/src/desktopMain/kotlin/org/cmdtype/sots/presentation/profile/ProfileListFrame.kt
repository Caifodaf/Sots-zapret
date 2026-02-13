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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import domain.model.Profile
import domain.model.ProfileProviderType
import domain.model.ProfileServiceType
import org.cmdtype.sots.presentation.profile.components.ProfileItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sots.composeapp.generated.resources.Res
import sots.composeapp.generated.resources.ic_filter
import systemGray300
import systemGray50
import systemGrayIcon
import theme.LocalLang
import presentation.theme.TextStyles
import presentation.theme.TextStylesType
import presentation.theme.UIComponents.corner_radius_settings
import presentation.theme.UIScale
import presentation.viewmodel.GeneralViewmodel
import presentation.viewmodel.LanqSelect
import systemGray100

@Composable
@Preview
fun ProfileListFrame(vm: GeneralViewmodel) {
    val profileList by vm.vmRepository.profiles.collectAsState()
    val selectedProfile by vm.vmRepository.selectedProfile.collectAsState()
    val serviceState by vm.serviceState.collectAsState()
    val settings by vm.vmSettings.settings.collectAsState()
    val langCode = when(settings.language) {
        LanqSelect.RU -> "ru"
        LanqSelect.EN, LanqSelect.UK -> "en"
    }
    val isProfileChangeEnabled = serviceState != GeneralViewmodel.ServiceStatus.STARTING &&
        serviceState != GeneralViewmodel.ServiceStatus.ACTIVE &&
        serviceState != GeneralViewmodel.ServiceStatus.SHUTDOWN

    var isProfileListEmpty by remember { mutableStateOf(false) }

    var isFilterVisible by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<FilterItem?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedFilter) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        vm.vmRepository.updateProfilesList()
    }

    val filteredProfiles = selectedFilter?.let { filter ->
        when (filter.type) {
            FilterType.PROVIDER -> profileList.filter {
                it.provider.value == (filter.value as ProfileProviderType).value
            }
            FilterType.SERVICE -> profileList.filter { it.service == filter.value }
        }
    } ?: profileList

    isProfileListEmpty = filteredProfiles.isEmpty()
    val profilesBlockHeight = if (!isProfileListEmpty) UIScale.dp(170) else UIScale.dp(150)

    CompositionLocalProvider(LocalLang provides langCode) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onFilterClick = { isFilterVisible = !isFilterVisible }
            )
            Spacer(Modifier.height(UIScale.dp(4)))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(profilesBlockHeight)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isFilterVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ServiceFilterBlock(
                            selectedFilter = selectedFilter,
                            onSelect = { selectedFilter = it },
                        )
                    }
                    Spacer(Modifier.height(UIScale.dp(8)))

                    if (isProfileListEmpty) {
                        EmptyListState()
                    } else {
                        val filterHeight = if (isFilterVisible) UIScale.dp(36) else UIScale.dp(0)
                        val profileListHeight = profilesBlockHeight - filterHeight - UIScale.dp(8)
                        ProfileList(
                            vm = vm,
                            links = filteredProfiles,
                            selectedProfileName = selectedProfile?.fileName,
                            isProfileChangeEnabled = isProfileChangeEnabled,
                            onSelectClick = { link ->
                                if (isProfileChangeEnabled) {
                                    vm.vmRepository.selectProfileByFileName(link)
                                    vm.updateServiceStateAfterProfileSelection()
                                    vm.navigateBack()
                                }
                            },
                            height = profileListHeight,
                            listState = listState
                        )
                    }
                }
            }
            ProfileActionButtons(vm = vm, isProfileListEmpty = isProfileListEmpty)
        }
    }
}

@Composable
private fun Header(onFilterClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UIScale.dp(16))
            .padding(horizontal = UIScale.dp(10))
    ) {
        TextStyles(
            text = "pf_heading_title",
            type = TextStylesType.HeaderOne,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painterResource(Res.drawable.ic_filter),
            contentDescription = null,
            tint = systemGrayIcon,
            modifier = Modifier
                .size(UIScale.dp(12))
                .clickable { onFilterClick?.invoke() }
        )
    }
}


@Composable
private fun ServiceFilterBlock(
    selectedFilter: FilterItem?,
    onSelect: (FilterItem?) -> Unit,
) {
    val filterList = buildList {
        add(FilterItem(type = FilterType.PROVIDER, value = null, label = "pf_filter_all"))
        ProfileProviderType.entries
            .filter { it != ProfileProviderType.PROVIDER_UNKNOWN }
            .forEach { provider ->
                add(FilterItem(FilterType.PROVIDER, provider, provider.name.lowercase()))
            }
        ProfileServiceType.entries
            //.filter { it != ProfileServiceType.UNIVERSAL }
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
                            .clickable {
                                if (filter.value == null) onSelect(null)
                                else onSelect(filter)
                            }
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
    links: List<Profile>,
    selectedProfileName: String?,
    isProfileChangeEnabled: Boolean,
    onSelectClick: (String) -> Unit,
    height: Dp,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        LazyColumn(state = listState) {
            items(items = links, key = { it.fileName }) { profile ->
                ProfileItem(
                    name = vm.vmRepository.getLocalizedProfileName(profile.fileName),
                    selected = profile.fileName == selectedProfileName,
                    onSelectClick = { if (isProfileChangeEnabled) onSelectClick(profile.fileName) },
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
private fun ProfileActionButtons(vm: GeneralViewmodel, isProfileListEmpty: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().height(UIScale.dp(52))
            .offset(y = UIScale.dp(0)),
    ) {
        if (isProfileListEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UIScale.dp(24))
                    .padding(horizontal = UIScale.dp(10))
                    .background(color = systemGrayIcon, shape = RoundedCornerShape(corner_radius_settings))
                    .clickable { 
                        vm.openProfilesFolder()
                    },
                contentAlignment = Alignment.Center
            ) {
                TextStyles(
                    text = "pf_btn_open_folder",
                    type = TextStylesType.Label,
                    color = systemGray50,
                    modifier = Modifier.padding(horizontal = UIScale.dp(4)),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(UIScale.dp(4)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UIScale.dp(24))
                .padding(horizontal = UIScale.dp(10))
                .background(color = systemGrayIcon, shape = RoundedCornerShape(corner_radius_settings))
                .clickable { 
                    if (isProfileListEmpty) {
                        vm.reloadProfiles()
                    } else {
                        vm.navigateTo(util.Screen.AutoProfile)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            TextStyles(
                text = if (isProfileListEmpty) "pf_btn_reupload_api" else "pf_auto_select_btn",
                type = TextStylesType.Label,
                color = systemGray50,
                modifier = Modifier.padding(horizontal = UIScale.dp(4)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyListState() {
    Spacer(Modifier.height(UIScale.dp(54)))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextStyles(
            text = "pf_profiles_list_is_empty",
            type = TextStylesType.Label,
            modifier = Modifier.padding(UIScale.dp(16)),
            textAlign = TextAlign.Center
        )
    }
}

