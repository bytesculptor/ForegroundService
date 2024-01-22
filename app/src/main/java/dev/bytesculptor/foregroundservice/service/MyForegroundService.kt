/*
 * Copyright (c) 2024 Byte Sculptor Software - All Rights Reserved
 *
 * All information contained herein is and remains the property of Byte Sculptor Software.
 * Unauthorized copying of this file, via any medium, is strictly prohibited unless prior
 * written permission is obtained from Byte Sculptor Software.
 *
 * bytesculptor@gmail.com
 *
 */

package dev.bytesculptor.foregroundservice.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.bytesculptor.foregroundservice.MainActivity
import dev.bytesculptor.foregroundservice.R

class MyForegroundService : Service() {

    private var startTime = 0L
    private var batteryChangeReceiver: BroadcastReceiver? = null
    private lateinit var builderPermanentStatusNotification: NotificationCompat.Builder
    private val binder = LocalBinder()
    private var notificationManager: NotificationManagerCompat? = null
    private var resultPendingIntent: PendingIntent? = null

    private var actLevel = 0
    private var oldLevel = -1

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForegroundForService()
        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initBroadcastReceiver()

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val name: CharSequence = "Battery Level"
        val description = "Shows the battery level"
        val channelLevel = NotificationChannel(
            LEVEL_NOTIFICATION_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_LOW
        )
        channelLevel.setShowBadge(true)
        channelLevel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channelLevel.description = description

        // create
        notificationManager = NotificationManagerCompat.from(this)
        notificationManager?.createNotificationChannel(channelLevel)
    }

    private fun initBroadcastReceiver() {
        val intentFilterBatteryChanged = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryChangeReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                actLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                if (oldLevel != actLevel) {
                    oldLevel = actLevel
                    postStatusNotification()
                }
            }
        }
        registerReceiver(batteryChangeReceiver, intentFilterBatteryChanged)
    }

    private fun startForegroundForService() {
        builderPermanentStatusNotification =
            NotificationCompat.Builder(this, LEVEL_NOTIFICATION_CHANNEL_ID)

        builderPermanentStatusNotification.setUsesChronometer(true)

        if (startTime > 0L) {
            builderPermanentStatusNotification.setWhen(startTime)
        } else {
            startTime = System.currentTimeMillis()
        }

        val notification = builderPermanentStatusNotification
            .setOngoing(true)
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_battery_4_bar)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(resultPendingIntent)
            .build()

        startForeground(LEVEL_NOTIFICATION_ID, notification)
        postStatusNotification()
    }

    private fun postStatusNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        builderPermanentStatusNotification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentTitle("Level: $actLevel%")

        notificationManager?.notify(
            LEVEL_NOTIFICATION_ID,
            builderPermanentStatusNotification.build()
        )
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MyForegroundService = this@MyForegroundService
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    companion object {
        const val LEVEL_NOTIFICATION_CHANNEL_ID = "dev.bytesculptor.foregroundservice.level"

        private const val LEVEL_NOTIFICATION_ID = 1200

        val TAG = MyForegroundService::class.java.simpleName
    }
}
