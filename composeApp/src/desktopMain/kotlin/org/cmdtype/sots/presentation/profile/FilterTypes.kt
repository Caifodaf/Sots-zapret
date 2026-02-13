package org.cmdtype.sots.presentation.profile

enum class FilterType { PROVIDER, SERVICE }
data class FilterItem(val type: FilterType, val value: Any?, val label: String)
