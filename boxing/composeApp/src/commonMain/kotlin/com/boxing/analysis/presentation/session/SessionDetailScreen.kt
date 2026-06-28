package com.boxing.analysis.presentation.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.boxing.analysis.domain.model.SessionMetrics
import com.boxing.analysis.domain.model.SessionResults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    vm: SessionDetailViewModel = viewModel { SessionDetailViewModel(sessionId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is SessionDetailState.Loading    -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is SessionDetailState.Polling    -> PollingView(s.status, Modifier.padding(padding))
            is SessionDetailState.Ready      -> ResultsView(s.results, Modifier.padding(padding))
            is SessionDetailState.Error      -> ErrorView(s.message, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PollingView(status: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Analysing your session…", style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResultsView(results: SessionResults, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { MetricsDashboard(results.metrics) }
        item { ScoresRow(results.metrics) }
        if (results.coachingTips.isNotEmpty()) {
            item { CoachingCard(results.coachingTips) }
        }
        item { Text("Punch Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(results.punches.take(50)) { punch ->
            Text(
                "%.1fs — %s (%s)".format(punch.timestampMs / 1000.0, punch.punchType.replace("_", " "), punch.hand),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetricsDashboard(metrics: SessionMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Session Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricChip("Punches", "${metrics.totalPunches}")
                MetricChip("PPM", "%.1f".format(metrics.punchesPerMinute))
                MetricChip("Combos", "${metrics.totalCombinations}")
                MetricChip("Max Combo", "${metrics.maxComboLength}")
            }
        }
    }
}

@Composable
private fun ScoresRow(metrics: SessionMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScoreBar("Guard",    metrics.guardScore)
            ScoreBar("Footwork", metrics.footworkScore)
            ScoreBar("Balance",  metrics.balanceScore)
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("%.0f".format(score), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(progress = { (score / 100).toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CoachingCard(tips: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Coach Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            tips.forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorView(message: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}
