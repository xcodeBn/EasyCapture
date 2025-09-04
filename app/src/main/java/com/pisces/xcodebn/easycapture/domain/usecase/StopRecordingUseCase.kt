package com.pisces.xcodebn.easycapture.domain.usecase

import com.pisces.xcodebn.easycapture.domain.repository.RecordingRepository

class StopRecordingUseCase(private val recordingRepository: RecordingRepository) {
    operator fun invoke() {
        recordingRepository.stopRecording()
    }
}