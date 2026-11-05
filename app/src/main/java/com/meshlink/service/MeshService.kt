package com.meshlink.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshlink.MainActivity
import com.meshlink.MeshApplication
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MeshService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as MeshApplication

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                MeshLogger.i(MeshLogger.TAG_SERVICE, "Stopping Mesh Service")
                app.container.stopMesh()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundServiceNotification(app)
                app.container.startMesh()
                observeIncomingMessages(app)
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification(app: MeshApplication) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nodeId = app.container.identityManager.nodeId.value
        val alias = app.container.identityManager.nodeAlias.value

        val notification = NotificationCompat.Builder(this, MeshApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("MeshLink Active ($alias)")
            .setContentText("Listening for peer mesh packets (Node: $nodeId)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeIncomingMessages(app: MeshApplication) {
        serviceScope.launch {
            app.container.meshRouter.incomingMessages.collect { packet ->
                showIncomingMessageNotification(packet)
            }
        }
    }

    private fun showIncomingMessageNotification(packet: MeshPacket) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_CONVERSATION_ID", packet.senderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            packet.messageId.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, MeshApplication.CHANNEL_MESSAGES_ID)
            .setContentTitle("Mesh Message from ${packet.senderAlias}")
            .setContentText(packet.payload)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(packet.messageId.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        MeshLogger.i(MeshLogger.TAG_SERVICE, "MeshService Destroyed")
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.meshlink.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.meshlink.action.STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, MeshService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MeshService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
