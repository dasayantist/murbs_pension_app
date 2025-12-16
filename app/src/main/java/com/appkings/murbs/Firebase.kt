package com.appkings.murbs

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class Firebase : FirebaseMessagingService() {
    private val fcmId = SmartWebView.ASWV_FCM_ID
    private val fcmChannel: String? = SmartWebView.aswFcmChannel

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        if (!s.isEmpty()) {
            Log.d("TOKEN_REFRESHED ", s) // printing new tokens in logcat
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.getNotification() != null) {
            sendMyNotification(
                message.getNotification()?.title,
                message.getNotification()?.body,
                message.getNotification()?.clickAction,
                message.getData()["uri"],
                message.getData()["tag"],
                message.getData()["nid"]
            )
        }
    }

    private fun sendMyNotification(
        title: String?,
        message: String?,
        click_action: String?,
        uri: String?,
        tag: String?,
        nid: String?
    ) {
        //On click of notification it redirect to this Activity
        val intent = Intent(click_action)
        intent.putExtra("uri", uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent?
        val flag =
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

        val notification_id = nid?.toInt() ?: fcmId

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, fcmChannel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$title $notification_id")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
        val noti = notificationBuilder.build()
        noti.flags = Notification.DEFAULT_LIGHTS or Notification.FLAG_AUTO_CANCEL

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notification_id, notificationBuilder.build())
    }
}