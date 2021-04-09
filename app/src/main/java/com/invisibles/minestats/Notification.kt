package com.invisibles.minestats

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Notification(private var context: Context) {

    private val CHANNELS = mapOf(
        PAYOUT_NOTIFICATION to "Payouts"
    )

    companion object{
        const val NOTIFICATION_ID = 101
        const val PAYOUT_NOTIFICATION = "minestats.payout"

    }

    fun getNotification(title: String, message: String, intent: Intent, icon: Int, channelID: String, defaults: Int = Notification.DEFAULT_SOUND ): Notification {

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        builder.setDefaults(defaults)

        return builder.build()

    }

    fun getChannel(channelID: String): NotificationChannel {
        return NotificationChannel(channelID, CHANNELS.get(channelID), NotificationManager.IMPORTANCE_DEFAULT)
    }

    fun create(title: String, message: String, intent: Intent, icon: Int, channelID: String){

        val notifyChannel = NotificationChannel(channelID, CHANNELS.get(channelID), NotificationManager.IMPORTANCE_DEFAULT)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, channelID)
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