package com.boxing.analysis.presentation.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxing.analysis.domain.model.ProgressPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    val points by vm.points.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = padding + PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { TrendCard("Punches / Min", points.map { it.punchesPerMinute.toFloat() }, points.map { it.date }) }
                item { TrendCard("Guard Score",   points.map { it.guardScore.toFloat() },       points.map { it.date }) }
                item { TrendCard("Footwork",      points.map { it.footworkScore.toFloat() },    points.map { it.date }) }
                item { TrendCard("Balance",       points.map { it.balanceScore.toFloat() },     points.map { it.date }) }
                item { SessionCountCard(points.size, points.sumOf { it.totalPunches }) }
            }
        }
    }
}

@Composable
private fun TrendCard(title: String, values: List<Float>, labels: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (values.isEmpty()) {
                Text("No data yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Chart placeholder — integrate vico or compose-charts here
                Text(
                    "Latest: %.1f  |  Best: %.1f  |  Avg: %.1f".format(
                        values.last(),
                        values.max(),
                        values.average(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SessionCountCard(sessions: Int, totalPunches: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$sessions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Sessions", style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$totalPunches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Total Punches", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
