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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pisces.xcodebn.easycapture.data.ScreenCaptureService
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.ui.MainUiState
import com.pisces.xcodebn.easycapture.ui.MainViewModel
import com.pisces.xcodebn.easycapture.ui.theme.EasyCaptureTheme
import compose.icons.TablerIcons
import compose.icons.tablericons.DeviceDesktop
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.PlayerStop
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Video
import compose.icons.tablericons.BrandGithub
import compose.icons.tablericons.Microphone
import compose.icons.tablericons.MicrophoneOff

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (application as EasyCaptureApplication).container.viewModelFactory
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
            // Check if we should show rationale or if user permanently denied
            val shouldShowRationale = permissionsToRequest.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }
            
            val micPermissionNeeded = permissionsToRequest.contains(Manifest.permission.RECORD_AUDIO)
            val micPermissionDenied = micPermissionNeeded && 
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            
            if (!shouldShowRationale && micPermissionDenied) {
                // User has permanently denied mic permission
                viewModel.onShowPermissionDialog()
            } else {
                // Normal permission request
                requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onRecordClick: () -> Unit,
    onShowSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onBitrateChange: (Float) -> Unit,
    onFrameRateChange: (Float) -> Unit,
    onResolutionChange: (Int) -> Unit,
    onPresetSelected: (RecordingQuality) -> Unit,
    onMicToggle: () -> Unit,
    onPermissionDialogDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xcodeBn"))
                        context.startActivity(intent)
                    }) {
                        Icon(TablerIcons.BrandGithub, contentDescription = stringResource(R.string.github_profile))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Recording Status Card with Animation
                val cardScale by animateFloatAsState(
                    targetValue = if (uiState.isRecording) 1.02f else 1f,
                    animationSpec = if (uiState.isRecording) {
                        infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    } else {
                        tween(300)
                    },
                    label = "cardScale"
                )
                
                ElevatedCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer(scaleX = cardScale, scaleY = cardScale),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconColor by animateColorAsState(
                            targetValue = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            animationSpec = tween(500),
                            label = "iconColor"
                        )
                        
                        Icon(
                            imageVector = if (uiState.isRecording) TablerIcons.Video else TablerIcons.DeviceDesktop,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = iconColor
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (uiState.isRecording) stringResource(R.string.recording_status) else stringResource(R.string.ready_to_record),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        if (uiState.isRecording) {
                            val timerColor by animateColorAsState(
                                targetValue = MaterialTheme.colorScheme.error,
                                animationSpec = tween(500),
                                label = "timerColor"
                            )
                            
                            Text(
                                text = formatTime(uiState.recordingDurationSeconds),
                                style = MaterialTheme.typography.displayMedium,
                                color = timerColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val resolutions = listOf("720p", "1080p", "1440p", "4K")
                        Text(
                            text = "${resolutions[uiState.customResolutionIndex]} • ${uiState.customFrameRate.toInt()} fps • ${uiState.customBitrate.toInt()} Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Record Button with Mic Toggle
                val buttonScale by animateFloatAsState(
                    targetValue = if (uiState.isRecording) 0.95f else 1f,
                    animationSpec = tween(200),
                    label = "buttonScale"
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                ) {
                    // Mic Toggle Button
                    IconButton(
                        onClick = onMicToggle,
                        enabled = !uiState.isRecording,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (uiState.isMicEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Icon(
                            if (uiState.isMicEnabled) TablerIcons.Microphone else TablerIcons.MicrophoneOff,
                            contentDescription = if (uiState.isMicEnabled) "Disable Microphone" else "Enable Microphone",
                            tint = if (uiState.isMicEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Record Button
                    ExtendedFloatingActionButton(
                        onClick = onRecordClick,
                        icon = { 
                            Icon(
                                if (uiState.isRecording) TablerIcons.PlayerStop else TablerIcons.PlayerPlay,
                                contentDescription = null
                            ) 
                        },
                        text = { Text(if (uiState.isRecording) stringResource(R.string.stop_recording) else stringResource(R.string.start_recording)) },
                        modifier = Modifier.size(width = 200.dp, height = 56.dp),
                        shape = RoundedCornerShape(28.dp)
                    )
                }
                
                // Settings Button
                OutlinedButton(
                    onClick = onShowSettings,
                    modifier = Modifier.size(width = 160.dp, height = 48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        TablerIcons.Settings,
                        contentDescription = stringResource(R.string.recording_settings_desc),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings))
                }
            }
        }
    }
    
    if (uiState.showSettingsBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        RecordingSettingsBottomSheet(
            uiState = uiState,
            onDismiss = onDismissSettings,
            onBitrateChange = onBitrateChange,
            onFrameRateChange = onFrameRateChange,
            onResolutionChange = onResolutionChange,
            onPresetSelected = onPresetSelected,
            bottomSheetState = bottomSheetState
        )
    }
    
    // Permission Dialog
    if (uiState.showPermissionDialog) {
        AlertDialog(
            onDismissRequest = onPermissionDialogDismiss,
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(R.string.mic_permission_message)) },
            confirmButton = {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onPermissionDialogDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSettingsBottomSheet(
    uiState: MainUiState,
    onDismiss: () -> Unit,
    onBitrateChange: (Float) -> Unit,
    onFrameRateChange: (Float) -> Unit,
    onResolutionChange: (Int) -> Unit,
    onPresetSelected: (RecordingQuality) -> Unit,
    bottomSheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)

        ) {
            item {
                Text(
                    stringResource(R.string.recording_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Presets
            item {
                Text(stringResource(R.string.quick_presets), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.qualitySettings.forEach { preset ->
                        OutlinedButton(
                            onClick = { onPresetSelected(preset) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preset.resolution)
                        }
                    }
                }
            }
            
            // Custom Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.custom_settings), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Resolution Selector
                        Text(stringResource(R.string.resolution), style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val resolutions = listOf("720p", "1080p", "1440p", "4K")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            resolutions.forEachIndexed { index, resolution ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = resolutions.size),
                                    onClick = { onResolutionChange(index) },
                                    selected = index == uiState.customResolutionIndex,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(resolution, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Bitrate Slider
                        Text("Bitrate: ${uiState.customBitrate.toInt()} Mbps", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.customBitrate,
                            onValueChange = onBitrateChange,
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Frame Rate Slider  
                        Text("Frame Rate: ${uiState.customFrameRate.toInt()} fps", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.customFrameRate,
                            onValueChange = onFrameRateChange,
                            valueRange = 15f..120f,
                            steps = 20,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}