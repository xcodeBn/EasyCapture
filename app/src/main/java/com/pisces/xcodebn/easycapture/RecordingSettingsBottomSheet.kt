package com.pisces.xcodebn.easycapture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import com.pisces.xcodebn.easycapture.ui.MainUiState

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
            modifier = Modifier.Companion.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)

        ) {
            item {
                Text(
                    stringResource(R.string.recording_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.Companion.padding(bottom = 16.dp)
                )
            }

            // Presets
            item {
                Text(
                    stringResource(R.string.quick_presets),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.Companion.height(8.dp))

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.qualitySettings.forEach { preset ->
                        OutlinedButton(
                            onClick = { onPresetSelected(preset) },
                            modifier = Modifier.Companion.weight(1f)
                        ) {
                            Text(preset.resolution)
                        }
                    }
                }
            }

            // Custom Settings
            item {
                Card(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.Companion.padding(20.dp)) {
                        Text(
                            stringResource(R.string.custom_settings),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.Companion.height(20.dp))

                        // Resolution Selector
                        Text(
                            stringResource(R.string.resolution),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.Companion.height(12.dp))

                        val resolutions = listOf("720p", "1080p", "1440p", "4K")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.Companion.fillMaxWidth()) {
                            resolutions.forEachIndexed { index, resolution ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = resolutions.size
                                    ),
                                    onClick = { onResolutionChange(index) },
                                    selected = index == uiState.customResolutionIndex,
                                    modifier = Modifier.Companion.height(48.dp)
                                ) {
                                    Text(resolution, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.Companion.height(24.dp))

                        // Bitrate Slider
                        Text(
                            "Bitrate: ${uiState.customBitrate.toInt()} Mbps",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.Companion.height(8.dp))
                        Slider(
                            value = uiState.customBitrate,
                            onValueChange = onBitrateChange,
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.Companion.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.Companion.height(24.dp))

                        // Frame Rate Slider
                        Text(
                            "Frame Rate: ${uiState.customFrameRate.toInt()} fps",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.Companion.height(8.dp))
                        Slider(
                            value = uiState.customFrameRate,
                            onValueChange = onFrameRateChange,
                            valueRange = 15f..120f,
                            steps = 20,
                            modifier = Modifier.Companion.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.Companion.height(48.dp))
            }
        }
    }
}