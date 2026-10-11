package io.github.findnet.service.modules

import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.Service
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.findnet.common.Components
import io.github.findnet.common.GlobalState
import io.github.findnet.common.QuickAction
import io.github.findnet.common.ensureNotificationChannel
import io.github.findnet.common.quickIntent
import io.github.findnet.common.receiveBroadcastFlow
import io.github.findnet.common.startForeground
import io.github.findnet.common.toPendingIntent
import io.github.findnet.core.Core
import io.github.findnet.service.R
import io.github.findnet.service.ServiceConfig
import io.github.findnet.service.models.NotificationParams
import io.github.findnet.service.models.getSpeedTrafficText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private data class ExtendedNotificationParams(
    val title: String,
    val stopText: String,
    val showStopAction: Boolean,
    val contentText: String,
)

private val NotificationParams.extended: ExtendedNotificationParams
    get() = ExtendedNotificationParams(
        title,
        stopText,
        showStopAction,
        Core.getSpeedTrafficText(onlyStatisticsProxy),
    )

internal class NotificationModule(
    private val service: Service,
    private val scope: CoroutineScope,
) : ServiceModule {
    override fun start() {
        service.ensureNotificationChannel()
        update(ServiceConfig.notificationParams.value.extended)
        scope.launch {
            service.receiveBroadcastFlow {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }.map { intent ->
                intent.action == Intent.ACTION_SCREEN_ON
            }.onStart {
                emit(isScreenOn())
            }.distinctUntilChanged().collectLatest { screenOn ->
                if (!screenOn) return@collectLatest
                combine(
                    flow {
                        while (true) {
                            delay(1_000)
                            emit(Unit)
                        }
                    },
                    ServiceConfig.notificationParams,
                ) { _, params ->
                    params.extended
                }.distinctUntilChanged()
                    .collect(::update)
            }
        }
    }

    private fun isScreenOn() =
        service.getSystemService<PowerManager>()?.isInteractive ?: true

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val intent = Intent().setComponent(Components.mainActivity)

        NotificationCompat.Builder(
            service,
            GlobalState.NOTIFICATION_CHANNEL,
        ).apply {
            setSmallIcon(R.drawable.ic_service)
            setContentTitle("FindNet")
            setContentIntent(intent.toPendingIntent)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            setOngoing(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
            }
            setShowWhen(true)
            setOnlyAlertOnce(true)
        }
    }

    private val stopIntent by lazy { QuickAction.STOP.quickIntent.toPendingIntent }

    private fun update(params: ExtendedNotificationParams) {
        service.startForeground(
            with(notificationBuilder) {
                setContentTitle(params.title)
                setContentText(params.contentText)
                clearActions()
                if (params.showStopAction) {
                    addAction(0, params.stopText, stopIntent)
                }
                build()
            },
        )
    }

    @Suppress("DEPRECATION")
    override fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            service.stopForeground(true)
        }
    }
}
