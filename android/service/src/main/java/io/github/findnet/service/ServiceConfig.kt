package io.github.findnet.service

import io.github.findnet.service.models.NotificationParams
import io.github.findnet.service.models.VpnOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceConfig {
    @Volatile
    private var currentVpnOptions: VpnOptions? = null
    private val mutableNotificationParams = MutableStateFlow(NotificationParams())

    val vpnOptions: VpnOptions?
        get() = currentVpnOptions

    val notificationParams = mutableNotificationParams.asStateFlow()

    fun updateVpnOptions(options: VpnOptions) {
        currentVpnOptions = options
    }

    fun updateNotificationParams(params: NotificationParams) {
        mutableNotificationParams.value = params
    }
}
