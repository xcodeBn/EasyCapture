package com.pisces.xcodebn.easycapture

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.ui.MainUiState
import compose.icons.TablerIcons
import compose.icons.tablericons.BrandGithub
import compose.icons.tablericons.DeviceDesktop
import compose.icons.tablericons.Microphone
import compose.icons.tablericons.MicrophoneOff
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.PlayerStop
import compose.icons.tablericons.Settings
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    val haptics = LocalHapticFeedback.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xcodeBn"))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            TablerIcons.BrandGithub,
                            contentDescription = stringResource(R.string.github_profile)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Asymmetric layout with better spacing and hierarchy
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer for asymmetric positioning
                Spacer(modifier = Modifier.height(32.dp))

                // Status section - positioned in upper third
                Box(
                    modifier = Modifier.weight(0.45f),
                    contentAlignment = Alignment.Center
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
                            .graphicsLayer(scaleX = cardScale, scaleY = cardScale),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.isRecording) {
                                RecordingIndicator(modifier = Modifier.size(64.dp))
                            } else {
                                val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ), label = "scale"
                                )
                                Icon(
                                    imageVector = TablerIcons.DeviceDesktop,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).scale(scale),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (uiState.isRecording) stringResource(R.string.recording_status) else stringResource(
                                    R.string.ready_to_record
                                ),
                                style = MaterialTheme.typography.headlineSmall
                            )

                            // Fixed height container for timer to maintain card size
                            Box(
                                modifier = Modifier.height(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = uiState.isRecording,
                                    transitionSpec = {
                                        if (targetState) {
                                            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                                slideOutVertically { height -> -height } + fadeOut()
                                            )
                                        } else {
                                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                                slideOutVertically { height -> height } + fadeOut()
                                            )
                                        }
                                    },
                                    label = "TimerAnimation"
                                ) { isRecording ->
                                    if (isRecording) {
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
                                    } else {
                                        // Keep the space when not recording
                                        Box(modifier = Modifier.height(64.dp)) {}
                                    }
                                }
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
                }

                // Controls section - bottom portion with better spacing
                Box(
                    modifier = Modifier.weight(0.55f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Record Button with Mic Toggle - more prominent positioning
                        val buttonScale by animateFloatAsState(
                            targetValue = if (uiState.isRecording) 0.95f else 1f,
                            animationSpec = tween(200),
                            label = "buttonScale"
                        )

                        // Main record button - hero element
                        ExtendedFloatingActionButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRecordClick()
                            },
                            icon = {
                                Icon(
                                    if (uiState.isRecording) TablerIcons.PlayerStop else TablerIcons.PlayerPlay,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    if (uiState.isRecording) stringResource(R.string.stop_recording) else stringResource(
                                        R.string.start_recording
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(width = 220.dp, height = 64.dp)
                                .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                        )

                        // Secondary controls row - mic and settings
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Toggle Button
                            IconButton(
                                onClick = onMicToggle,
                                enabled = !uiState.isRecording,
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        color = if (uiState.isMicEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                                    )
                            ) {
                                Icon(
                                    if (uiState.isMicEnabled) TablerIcons.Microphone else TablerIcons.MicrophoneOff,
                                    contentDescription = if (uiState.isMicEnabled) "Disable Microphone" else "Enable Microphone",
                                    tint = if (uiState.isMicEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Settings Button with fade animation
                            AnimatedVisibility(
                                visible = !uiState.isRecording,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                IconButton(
                                    onClick = onShowSettings,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                                        )
                                ) {
                                    Icon(
                                        TablerIcons.Settings,
                                        contentDescription = stringResource(R.string.recording_settings_desc),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom padding
                Spacer(modifier = Modifier.height(24.dp))
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

@Composable
fun RecordingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite-transition")

    val outerCircleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "outer-circle-scale"
    )

    val outerCircleAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "outer-circle-alpha"
    )

    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = outerCircleAlpha),
                radius = size.minDimension / 4 * outerCircleScale
            )
            drawCircle(
                color = color,
                radius = size.minDimension / 4
            )
        }
    }
}


@Composable
fun SineWave(
    modifier: Modifier = Modifier,
    amplitude: Float = 20f,
    frequency: Float = 1f,
    phase: Float = 0f,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val path = Path()
        val width = size.width
        val height = size.height
        val mid = height / 2

        path.moveTo(0f, mid)

        for (x in 0..width.toInt()) {
            val y = mid + amplitude * sin((2 * PI * frequency * x / width) + phase)
            path.lineTo(x.toFloat(), y.toFloat())
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}
