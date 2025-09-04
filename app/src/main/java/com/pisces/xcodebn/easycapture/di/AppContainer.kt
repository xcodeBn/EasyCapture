package com.pisces.xcodebn.easycapture.di

import android.content.Context
import com.pisces.xcodebn.easycapture.data.ScreenCaptureService
import com.pisces.xcodebn.easycapture.data.local.SettingsLocalDataSource
import com.pisces.xcodebn.easycapture.data.repository.RecordingRepositoryImpl
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository
import com.pisces.xcodebn.easycapture.domain.usecase.GetQualitySettingsUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.GetSavedQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.SaveQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StartRecordingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StopRecordingUseCase

class AppContainer(private val context: Context) {

    private val settingsLocalDataSource by lazy {
        SettingsLocalDataSource(context)
    }

    lateinit var recordingRepository: RecordingRepository

    fun initializeRepository() {
        recordingRepository = RecordingRepositoryImpl(
            context = context,
            settingsLocalDataSource = settingsLocalDataSource
        )
        android.util.Log.d("AppContainer", "Repository initialized with context")
    }

    private val getQualitySettingsUseCase by lazy {
        GetQualitySettingsUseCase(getRepositorySafely())
    }

    private val getSavedQualitySettingUseCase by lazy {
        GetSavedQualitySettingUseCase(getRepositorySafely())
    }

    private val saveQualitySettingUseCase by lazy {
        SaveQualitySettingUseCase(getRepositorySafely())
    }

    private val startRecordingUseCase by lazy {
        StartRecordingUseCase(getRepositorySafely())
    }

    private val stopRecordingUseCase by lazy {
        StopRecordingUseCase(getRepositorySafely())
    }
    
    private fun getRepositorySafely(): RecordingRepository {
        if (!::recordingRepository.isInitialized) {
            initializeRepository()
        }
        return recordingRepository
    }

    val viewModelFactory by lazy {
        ViewModelFactory(
            getQualitySettingsUseCase,
            getSavedQualitySettingUseCase,
            saveQualitySettingUseCase,
            startRecordingUseCase,
            stopRecordingUseCase,
            context.applicationContext as android.app.Application
        )
    }
}