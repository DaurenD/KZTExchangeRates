package com.boxing.analysis.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxing.analysis.domain.model.SessionSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRecording: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenProgress: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boxing Analysis", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenProgress) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Progress")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartRecording,
                icon    = { Icon(Icons.Default.Add, contentDescription = null) },
                text    = { Text("New Session") },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(contentPadding = padding) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onClick = { onOpenSession(session.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(session.sessionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
        supportingContent = {
            val ppm = session.punchesPerMinute?.let { "%.1f PPM".format(it) } ?: session.analysisState
            Text(ppm)
        },
        trailingContent = {
            session.totalPunches?.let { Text("$it punches", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
            Text("Tap + to record your first session", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
