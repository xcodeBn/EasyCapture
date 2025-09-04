package com.pisces.xcodebn.easycapture.domain.usecase

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow

class GetSavedQualitySettingUseCase(private val recordingRepository: RecordingRepository) {
    operator fun invoke(): Flow<RecordingQuality> {
        return recordingRepository.getSavedQualitySetting()
    }
}