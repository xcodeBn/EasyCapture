package com.pisces.xcodebn.easycapture.domain.usecase

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository

class SaveQualitySettingUseCase(private val recordingRepository: RecordingRepository) {
    suspend operator fun invoke(quality: RecordingQuality) {
        recordingRepository.saveQualitySetting(quality)
    }
}