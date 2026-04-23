package com.qicheng.workbenchkeeper.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WorkbenchPreset(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val accessUrl: String = "",
    val keepAwakeDuration: KeepAwakeDuration = KeepAwakeDuration.default,
)
