package com.pisces.xcodebn.easycapture.domain.repository

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun startRecording(quality: RecordingQuality)
    fun stopRecording()
    fun getQualitySettings(): List<RecordingQuality>
    suspend fun saveQualitySetting(quality: RecordingQuality)
    fun getSavedQualitySetting(): Flow<RecordingQuality>
}