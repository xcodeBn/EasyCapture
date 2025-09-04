package com.pisces.xcodebn.easycapture.domain.usecase

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository

class StartRecordingUseCase(private val recordingRepository: RecordingRepository) {
    operator fun invoke(quality: RecordingQuality) {
        recordingRepository.startRecording(quality)
    }
}