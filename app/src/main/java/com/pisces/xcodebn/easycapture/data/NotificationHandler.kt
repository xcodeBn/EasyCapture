package com.pisces.xcodebn.easycapture.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pisces.xcodebn.easycapture.R

class NotificationHandler(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    fun createRecordingNotification(recordingTime: String = ""): Notification {
        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_STOP_RECORDING_BROADCAST
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (recordingTime.isNotEmpty()) {
            "Recording... $recordingTime"
        } else {
            "Recording screen..."
        }
        
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EasyCapture")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(
                R.mipmap.ic_launcher,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
    
    fun updateNotificationTime(recordingTime: String) {
        val notification = createRecordingNotification(recordingTime)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "screen_capture_channel"
        const val ACTION_STOP_RECORDING_BROADCAST = "com.pisces.xcodebn.easycapture.STOP_RECORDING_BROADCAST"
    }
}