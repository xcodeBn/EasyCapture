package com.pisces.xcodebn.easycapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.pisces.xcodebn.easycapture.data.ScreenCaptureService
import com.pisces.xcodebn.easycapture.ui.MainViewModel
import com.pisces.xcodebn.easycapture.ui.theme.EasyCaptureTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (application as EasyCaptureApplication).container.viewModelFactory
    }

    private val prefs by lazy {
        getSharedPreferences("permission_prefs", MODE_PRIVATE)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startMediaProjection()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            android.util.Log.d("MainActivity", "Starting recording via service intent")
            val startIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_RECORDING
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                putExtra("quality", viewModel.uiState.value.selectedQuality.toString())
                putExtra("micEnabled", viewModel.uiState.value.isMicEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
            viewModel.onRecordEvent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyCaptureTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val uiState by viewModel.uiState.collectAsState()
                    RecordingScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onRecordClick = ::handleRecordClick,
                        onShowSettings = viewModel::onShowSettings,
                        onDismissSettings = viewModel::onDismissSettings,
                        onBitrateChange = viewModel::onBitrateChange,
                        onFrameRateChange = viewModel::onFrameRateChange,
                        onResolutionChange = viewModel::onResolutionChange,
                        onPresetSelected = viewModel::onPresetSelected,
                        onMicToggle = viewModel::onMicToggle,
                        onPermissionDialogDismiss = viewModel::onDismissPermissionDialog,
                        onOpenSettings = ::openAppSettings
                    )
                }
            }
        }
    }

    private fun handleRecordClick() {
        if (viewModel.uiState.value.isRecording) {
            android.util.Log.d("MainActivity", "Stopping recording via service intent")
            val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_STOP_RECORDING
            }
            startService(stopIntent)
            viewModel.onRecordEvent()
            return
        }

        val permissionsToRequest = mutableListOf<String>()

        // Only request mic permission if mic is enabled
        if (viewModel.uiState.value.isMicEnabled) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            startMediaProjection()
        } else {
            val micPermissionNeeded = permissionsToRequest.contains(Manifest.permission.RECORD_AUDIO)
            if (micPermissionNeeded) {
                val hasRequestedMicPermission = prefs.getBoolean("has_requested_mic_permission", false)
                if (hasRequestedMicPermission) {
                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                    if (!shouldShowRationale) {
                        viewModel.onShowPermissionDialog()
                        return // Stop here, don't proceed to requestPermissionsLauncher
                    }
                }
            }

            // Request permissions
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())

            // Mark that we have requested mic permission
            if (micPermissionNeeded) {
                prefs.edit().putBoolean("has_requested_mic_permission", true).apply()
            }
        }
    }

    private fun startMediaProjection() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        viewModel.onDismissPermissionDialog()
    }
}