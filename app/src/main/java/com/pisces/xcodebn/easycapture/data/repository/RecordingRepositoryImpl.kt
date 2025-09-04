package com.pisces.xcodebn.easycapture.data.repository

import android.content.Context
import android.content.Intent
import com.pisces.xcodebn.easycapture.data.ScreenCaptureDataSource
import com.pisces.xcodebn.easycapture.data.ScreenCaptureService
import com.pisces.xcodebn.easycapture.data.local.SettingsLocalDataSource
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow

class RecordingRepositoryImpl(
    private val context: Context,
    private val settingsLocalDataSource: SettingsLocalDataSource
) : RecordingRepository {

    override fun startRecording(quality: RecordingQuality) {
        android.util.Log.d("RecordingRepository", "startRecording called with quality: $quality")
        // Get projection data from MainActivity (we need a better way to pass this)
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START_RECORDING
            putExtra("quality", quality.toString())
        }
        context.startForegroundService(intent)
    }

    override fun stopRecording() {
        android.util.Log.d("RecordingRepository", "stopRecording called")
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
    }

    override fun getQualitySettings(): List<RecordingQuality> {
        return listOf(RecordingQuality.LOW, RecordingQuality.MEDIUM, RecordingQuality.HIGH)
    }

    override suspend fun saveQualitySetting(quality: RecordingQuality) {
        settingsLocalDataSource.saveQualitySetting(quality)
    }

    override fun getSavedQualitySetting(): Flow<RecordingQuality> {
        return settingsLocalDataSource.getSavedQualitySetting()
    }
}