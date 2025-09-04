package com.pisces.xcodebn.easycapture.data

import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality

interface ScreenCaptureDataSource {
    fun startRecording(quality: RecordingQuality)
    fun stopRecording()
}