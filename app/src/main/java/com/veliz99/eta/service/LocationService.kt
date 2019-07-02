package com.veliz99.eta.service

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "location"
private const val CHANNEL_NAME = "Location Update"
private const val CHANNEL_DESCRIPTION = "Updates your location and traffic information while you drive"

class LocationService : IntentService("ETA") {

    override fun onHandleIntent(p0: Intent?) {
        //startForeground(0, Notification.)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = CHANNEL_DESCRIPTION
            channel.enableLights(true)

            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ETA Location Update")
            .setContentText("Updating your location and ETA")
        startForeground(1, builder.build())

        TODO("Implement location/traffic updating")
    }
}
