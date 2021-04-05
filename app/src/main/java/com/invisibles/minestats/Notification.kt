package com.invisibles.minestats

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Notification(private var context: Context) {

    private var notifyChannel: NotificationChannel =
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)

    companion object{
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "minestats.notify"
        const val CHANNEL_NAME = "InvisiblesMineStats"
    }

    fun create(title: String, message: String, intent: Intent, icon: Int){

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)


        with (NotificationManagerCompat.from(context)){
            createNotificationChannel(notifyChannel)
            notify(NOTIFICATION_ID, builder.build())
        }

    }

}