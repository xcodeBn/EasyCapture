package com.pisces.xcodebn.easycapture.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pisces.xcodebn.easycapture.domain.usecase.GetQualitySettingsUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.GetSavedQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.SaveQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StartRecordingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StopRecordingUseCase
import com.pisces.xcodebn.easycapture.ui.MainViewModel

class ViewModelFactory(
    private val getQualitySettingsUseCase: GetQualitySettingsUseCase,
    private val getSavedQualitySettingUseCase: GetSavedQualitySettingUseCase,
    private val saveQualitySettingUseCase: SaveQualitySettingUseCase,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                application,
                getQualitySettingsUseCase,
                getSavedQualitySettingUseCase,
                saveQualitySettingUseCase,
                startRecordingUseCase,
                stopRecordingUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}