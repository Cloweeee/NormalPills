package com.danana.normalpills;

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.core.app.NotificationCompat
import androidx.compose.material.icons.outlined.*

class NotificationBroadcastReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val notificationManager = context!!.applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val title = it.getStringExtra("NOTIFICATION_TITLE")
            val message = it.getStringExtra("NOTIFICATION_MESSAGE")

            val notificationChannel = NotificationChannel("normalPills", "Normal Pills", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)

            val notification = NotificationCompat.Builder(context!!.applicationContext, "normalPills")
                .setSmallIcon(R.drawable.pill_off)
                .setContentTitle(title)
                .setContentText(message)
                .build()
            notificationManager.notify(0, notification)
        }
    }
}