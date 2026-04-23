package com.qicheng.workbenchkeeper.model

import kotlinx.serialization.Serializable

@Serializable
enum class KeepAwakeDuration(
    val minutes: Int,
    val label: String,
) {
    THIRTY_MINUTES(30, "30分钟"),
    ONE_HOUR(60, "1小时"),
    THREE_HOURS(180, "3小时"),
    SIX_HOURS(360, "6小时"),
    EIGHT_HOURS(480, "8小时"),
    TWELVE_HOURS(720, "12小时"),
    ;

    val timeoutMillis: Long
        get() = minutes.toLong() * 60_000L

    companion object {
        val default: KeepAwakeDuration = TWELVE_HOURS
    }
}
