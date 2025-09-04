package com.pisces.xcodebn.easycapture.domain.usecase

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository

class GetQualitySettingsUseCase(private val recordingRepository: RecordingRepository) {
    operator fun invoke(): List<RecordingQuality> {
        return recordingRepository.getQualitySettings()
    }
}