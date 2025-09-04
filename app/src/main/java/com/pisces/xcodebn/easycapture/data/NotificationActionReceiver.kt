package com.pisces.xcodebn.easycapture.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pisces.xcodebn.easycapture.EasyCaptureApplication

class NotificationActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHandler.ACTION_STOP_RECORDING_BROADCAST -> {
                android.util.Log.d("NotificationReceiver", "Stop recording from notification")
                
                // Stop the service
                val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_STOP_RECORDING
                }
                context.startService(serviceIntent)
                
                // Update the UI state through the application
                val app = context.applicationContext as EasyCaptureApplication
                RecordingStateManager.setRecordingState(false)
            }
        }
    }
}