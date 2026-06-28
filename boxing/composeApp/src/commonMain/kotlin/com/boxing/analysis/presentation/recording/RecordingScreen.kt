package com.boxing.analysis.presentation.recording

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Platform-agnostic recording screen shell.
 *
 * The actual camera preview is platform-specific:
 *   - Android: CameraXPreview composable (androidMain)
 *   - iOS:     AVFoundationPreview composable (iosMain)
 *
 * Both are injected via [CameraPreviewSlot] expect/actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onSessionSubmitted: (sessionId: String) -> Unit,
    onCancel: () -> Unit,
    vm: RecordingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is RecordingState.Submitted) {
            onSessionSubmitted((state as RecordingState.Submitted).sessionId)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Camera preview fills the screen — platform specific
        CameraPreviewSlot(
            isRecording = state is RecordingState.Recording,
            modifier    = Modifier.fillMaxSize(),
            onVideoReady = { bytes, durationMs -> vm.onVideoReady(bytes, durationMs) },
        )

        // Top bar
        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title    = {
                if (state is RecordingState.Recording) {
                    val elapsed = (state as RecordingState.Recording).elapsedMs
                    Text(formatDuration(elapsed), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            navigationIcon = {
                if (state !is RecordingState.Recording) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        // Bottom controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                is RecordingState.Idle -> {
                    SessionTypePicker(selected = s.sessionType, onSelect = vm::setSessionType)
                    RecordButton(isRecording = false, onClick = vm::startRecording)
                }
                is RecordingState.Recording -> {
                    RecordButton(isRecording = true, onClick = vm::stopRecording)
                }
                is RecordingState.Uploading -> {
                    CircularProgressIndicator(color = Color.White)
                    Text("Uploading…", color = Color.White)
                }
                is RecordingState.Submitted -> Unit
            }
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            if (isRecording) Icons.Default.Stop else Icons.Default.Add,
            contentDescription = if (isRecording) "Stop" else "Record",
        )
    }
}

@Composable
private fun SessionTypePicker(selected: String, onSelect: (String) -> Unit) {
    val types = listOf("SHADOWBOXING", "BAG_WORK", "PAD_WORK", "SPARRING", "DRILL")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value    = selected.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            label    = { Text("Session type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors   = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text    = { Text(type.replace("_", " ")) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}
