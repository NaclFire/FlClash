package io.github.findnet.service.models

data class NotificationParams(
    val title: String = "FindNet",
    val stopText: String = "STOP",
    val onlyStatisticsProxy: Boolean = false,
    val showStopAction: Boolean = true,
)
