package com.jacobstechnologies.birthdayreminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class DailyReceiver : BroadcastReceiver() {
    
    override fun onReceive(context : Context, intent : Intent) {
        if (intent.action == null) {
            Log.d("jjj", "NULL INTENT ACTION!!!!")
        }
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val reminders = ArrayList<Reminder>()
            reminders.forEach {
                setTimer(context, it)
            }
        } else {
            val notificationManager = context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            Log.v("jjj", "BROADCAST RECEIVED!!!")

            val name = intent.getStringExtra("name")
            val daysUntilBirthday = intent.getIntExtra("daysUntilBirthday", 0)

            val daysUntilBirthdayString =
                if (daysUntilBirthday == 0){"today!"}
                else {"in $daysUntilBirthday days!"}

            val bigText = "It's $name's birthday $daysUntilBirthdayString"
            val contentText = "Wish $name a happy happy birthday!"
            val mNotifyBuilder : NotificationCompat.Builder =
                NotificationCompat.Builder(context, "10000")
                    .setSmallIcon(R.mipmap.app_icon)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(bigText)
                    )
                    .setContentTitle("Birthday Reminder")
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
            notificationManager.notify(10000, mNotifyBuilder.build())
            Log.d("jjj", "NOTIFICATION SENT!")
        }
    }
}