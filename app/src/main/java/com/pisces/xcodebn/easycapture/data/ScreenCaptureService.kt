package com.pisces.xcodebn.easycapture.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.pisces.xcodebn.easycapture.EasyCaptureApplication
import com.pisces.xcodebn.easycapture.R
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service(), ScreenCaptureDataSource {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var resultCode: Int = 0
    private var data: Intent? = null
    private var tempOutputFile: File? = null
    private lateinit var notificationHandler: NotificationHandler

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationHandler = NotificationHandler(this)
        (application as EasyCaptureApplication).container.initializeRepository()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHandler.createRecordingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHandler.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NotificationHandler.NOTIFICATION_ID, notification)
        }

        intent?.let {
            when (it.action) {
                ACTION_START_RECORDING -> {
                    resultCode = it.getIntExtra("resultCode", -1)
                    data = it.getParcelableExtra("data")
                    val qualityString = it.getStringExtra("quality") ?: "MEDIUM"
                    val quality = parseQualityFromString(qualityString)
                    android.util.Log.d("ScreenCapture", "Received START_RECORDING action with quality: $qualityString")
                    startRecording(quality)
                    RecordingStateManager.setRecordingState(true)
                }
                ACTION_STOP_RECORDING -> {
                    android.util.Log.d("ScreenCapture", "Received STOP_RECORDING action")
                    stopRecording()
                    RecordingStateManager.setRecordingState(false)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun startRecording(quality: RecordingQuality) {
        android.util.Log.d("ScreenCapture", "startRecording called with quality: $quality")
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intentData = data
        if (intentData == null) {
            android.util.Log.e("ScreenCapture", "No intent data available for media projection")
            return
        }
        android.util.Log.d("ScreenCapture", "Creating media projection with resultCode: $resultCode")
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intentData)
        
        // Register callback as required by Android
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                android.util.Log.d("ScreenCapture", "MediaProjection stopped")
                stopRecording()
            }
        }, null)

        val (width, height, dpi) = getScreenDimensions()
        android.util.Log.d("ScreenCapture", "Using quality: ${quality.displayName}, bitrate: ${quality.bitrate}, fps: ${quality.frameRate}")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            tempOutputFile = File(cacheDir, "temp_recording.mp4")
            setOutputFile(tempOutputFile!!.absolutePath)
            android.util.Log.d("ScreenCapture", "Recording to temp file: ${tempOutputFile!!.absolutePath}")
            setVideoSize(width, height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(quality.bitrate)
            setVideoFrameRate(quality.frameRate)
        }

        try {
            android.util.Log.d("ScreenCapture", "Preparing MediaRecorder...")
            mediaRecorder?.prepare()
            android.util.Log.d("ScreenCapture", "MediaRecorder prepared successfully")
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Failed to prepare MediaRecorder: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            return
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )

        try {
            android.util.Log.d("ScreenCapture", "Starting MediaRecorder...")
            mediaRecorder?.start()
            android.util.Log.d("ScreenCapture", "Recording started successfully!")
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Failed to start recording: ${e.message}")
            stopRecording()
        }
    }

    override fun stopRecording() {
        try {
            mediaRecorder?.stop()
            android.util.Log.d("ScreenCapture", "Recording stopped successfully")
            
            // Save temp file to gallery
            tempOutputFile?.let { tempFile ->
                saveVideoToGallery(tempFile)
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error stopping MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        stopSelf()
    }


    private fun getScreenDimensions(): Triple<Int, Int, Int> {
        val displayMetrics = resources.displayMetrics
        return Triple(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi)
    }

    private fun saveVideoToGallery(tempFile: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "EasyCapture_$timestamp.mp4"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EasyCapture")
                }
            }
            
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                tempFile.delete()
                android.util.Log.d("ScreenCapture", "Video saved to gallery: $it")
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Failed to save video to gallery: ${e.message}")
        }
    }
    
    private fun parseQualityFromString(qualityString: String): RecordingQuality {
        return when (qualityString) {
            "LOW" -> RecordingQuality.LOW
            "MEDIUM" -> RecordingQuality.MEDIUM  
            "HIGH" -> RecordingQuality.HIGH
            else -> RecordingQuality.MEDIUM
        }
    }

    companion object {
        const val ACTION_START_RECORDING = "com.pisces.xcodebn.easycapture.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.pisces.xcodebn.easycapture.STOP_RECORDING"

        var instance: ScreenCaptureDataSource? = null
            private set
    }
}