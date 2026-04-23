package com.qicheng.workbenchkeeper.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val keepAwakeDuration: KeepAwakeDuration = KeepAwakeDuration.default,
    val hasAcceptedUsageNotice: Boolean = false,
)
