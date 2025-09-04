package com.pisces.xcodebn.easycapture.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingStateManager {
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()
    
    fun setRecordingState(recording: Boolean) {
        android.util.Log.d("RecordingStateManager", "Setting recording state to: $recording")
        _isRecording.value = recording
        if (!recording) {
            _recordingDuration.value = 0L
        }
    }
    
    fun updateDuration(seconds: Long) {
        _recordingDuration.value = seconds
    }
}