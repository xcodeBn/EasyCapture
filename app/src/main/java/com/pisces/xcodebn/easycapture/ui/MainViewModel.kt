package com.pisces.xcodebn.easycapture.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pisces.xcodebn.easycapture.EasyCaptureApplication
import com.pisces.xcodebn.easycapture.data.RecordingStateManager
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.domain.usecase.GetQualitySettingsUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.GetSavedQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.SaveQualitySettingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StartRecordingUseCase
import com.pisces.xcodebn.easycapture.domain.usecase.StopRecordingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MainUiState(
    val isRecording: Boolean = false,
    val qualitySettings: List<RecordingQuality> = emptyList(),
    val selectedQuality: RecordingQuality = RecordingQuality.MEDIUM,
    val showSettingsBottomSheet: Boolean = false,
    val recordingDurationSeconds: Long = 0,
    val customBitrate: Float = 5f, // Mbps
    val customFrameRate: Float = 30f, // fps
    val customResolutionIndex: Int = 1, // 0=720p, 1=1080p, 2=1440p, 3=4K
    val isMicEnabled: Boolean = false,
    val showPermissionDialog: Boolean = false
)

class MainViewModel(
    private val application: Application,
    private val getQualitySettingsUseCase: GetQualitySettingsUseCase,
    private val getSavedQualitySettingUseCase: GetSavedQualitySettingUseCase,
    private val saveQualitySettingUseCase: SaveQualitySettingUseCase,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    private var timerJob: Job? = null

    init {
        loadQualitySettings()
        observeSavedQuality()
        observeRecordingState()
    }

    fun onServiceStarted() {
        android.util.Log.d("MainViewModel", "Service started, reinitializing repository...")
        // Force reinitialization of repository with actual service
        (application as EasyCaptureApplication).container.initializeRepository()
    }
    
    fun onRecordEvent() {
        android.util.Log.d("MainViewModel", "onRecordEvent called, isRecording: ${uiState.value.isRecording}")
        if (uiState.value.isRecording) {
            android.util.Log.d("MainViewModel", "Stopping recording...")
            stopRecordingUseCase()
        } else {
            android.util.Log.d("MainViewModel", "Starting recording with custom quality")
            val customQuality = createCustomQualityFromSettings()
            startRecordingUseCase(customQuality)
        }
    }
    
    private fun startTimer() {
        RecordingStateManager.updateDuration(0)
        timerJob = viewModelScope.launch {
            var seconds = 0L
            while (RecordingStateManager.isRecording.value) {
                delay(1000)
                seconds++
                RecordingStateManager.updateDuration(seconds)
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        RecordingStateManager.updateDuration(0)
    }
    
    private fun createCustomQualityFromSettings(): RecordingQuality {
        val resolutions = listOf("720p", "1080p", "1440p", "4K")
        val resolution = resolutions[uiState.value.customResolutionIndex]
        
        return RecordingQuality.CUSTOM(
            displayName = "Custom ($resolution)",
            bitrate = (uiState.value.customBitrate * 1_000_000).toInt(),
            frameRate = uiState.value.customFrameRate.toInt(),
            resolution = resolution,
            id = "custom_live"
        )
    }

    private fun loadQualitySettings() {
        val qualitySettings = getQualitySettingsUseCase()
        _uiState.value = _uiState.value.copy(qualitySettings = qualitySettings)
    }

    private fun observeSavedQuality() {
        getSavedQualitySettingUseCase().onEach {
            _uiState.value = _uiState.value.copy(selectedQuality = it)
        }.launchIn(viewModelScope)
    }
    
    private fun observeRecordingState() {
        RecordingStateManager.isRecording.onEach { isRecording ->
            _uiState.value = _uiState.value.copy(isRecording = isRecording)
            if (isRecording) {
                startTimer()
            } else {
                stopTimer()
            }
        }.launchIn(viewModelScope)
        
        RecordingStateManager.recordingDuration.onEach { duration ->
            _uiState.value = _uiState.value.copy(recordingDurationSeconds = duration)
        }.launchIn(viewModelScope)
    }

    fun onQualitySelected(quality: RecordingQuality) {
        viewModelScope.launch {
            saveQualitySettingUseCase(quality)
        }
    }
    
    fun onShowSettings() {
        _uiState.value = _uiState.value.copy(showSettingsBottomSheet = true)
    }
    
    fun onDismissSettings() {
        _uiState.value = _uiState.value.copy(showSettingsBottomSheet = false)
    }
    
    fun onBitrateChange(bitrate: Float) {
        _uiState.value = _uiState.value.copy(customBitrate = bitrate)
    }
    
    fun onFrameRateChange(frameRate: Float) {
        _uiState.value = _uiState.value.copy(customFrameRate = frameRate)
    }
    
    fun onResolutionChange(index: Int) {
        _uiState.value = _uiState.value.copy(customResolutionIndex = index)
    }
    
    fun onPresetSelected(preset: RecordingQuality) {
        _uiState.value = _uiState.value.copy(
            customBitrate = preset.bitrate / 1_000_000f,
            customFrameRate = preset.frameRate.toFloat(),
            customResolutionIndex = when (preset.resolution) {
                "720p" -> 0
                "1080p" -> 1
                "1440p" -> 2
                "4K" -> 3
                else -> 1
            },
            selectedQuality = preset
        )
    }
    
    fun onMicToggle() {
        _uiState.value = _uiState.value.copy(isMicEnabled = !_uiState.value.isMicEnabled)
    }
    
    fun onShowPermissionDialog() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = true)
    }
    
    fun onDismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = false)
    }
}