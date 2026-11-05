package com.meshlink

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.meshlink.di.AppContainer
import com.meshlink.util.MeshLogger

class MeshApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        MeshLogger.i(MeshLogger.TAG_MESH, "MeshLink Application Initialized")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "MeshLink Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps peer-to-peer mesh routing active in background"
            }

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "MeshLink Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for received mesh messages"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
            notificationManager?.createNotificationChannel(messageChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "meshlink_service_channel"
        const val CHANNEL_MESSAGES_ID = "meshlink_messages_channel"
    }
}
