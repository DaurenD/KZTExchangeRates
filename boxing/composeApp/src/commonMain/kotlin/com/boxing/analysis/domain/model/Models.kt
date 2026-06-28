package com.boxing.analysis.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class SessionType(val displayName: String) {
    SHADOWBOXING("Shadowboxing"),
    BAG_WORK("Bag Work"),
    PAD_WORK("Pad Work"),
    SPARRING("Sparring"),
    DRILL("Drill"),
}

enum class AnalysisState { PENDING, PROCESSING, COMPLETE, FAILED }

enum class PunchType(val displayName: String) {
    JAB("Jab"),
    CROSS("Cross"),
    LEFT_HOOK("Left Hook"),
    RIGHT_HOOK("Right Hook"),
    LEFT_UPPERCUT("Left Uppercut"),
    RIGHT_UPPERCUT("Right Uppercut"),
}

@Serializable
data class SessionSummary(
    val id: String,
    val sessionType: String,
    val startedAt: String,
    val durationMs: Long?,
    val analysisState: String,
    val totalPunches: Int?,
    val punchesPerMinute: Double?,
)

@Serializable
data class SessionMetrics(
    val totalPunches: Int,
    val punchesPerMinute: Double,
    val punchBreakdown: Map<String, Int>,
    val totalCombinations: Int,
    val maxComboLength: Int,
    val avgComboLength: Double,
    val activeRatio: Double,
    val guardScore: Double,
    val footworkScore: Double,
    val balanceScore: Double,
    val peakSpeed: Double,
    val avgSpeed: Double,
)

@Serializable
data class PunchRecord(
    val timestampMs: Long,
    val punchType: String,
    val hand: String,
    val speedEstimate: Double,
    val confidence: Double,
)

@Serializable
data class SessionResults(
    val sessionId: String,
    val metrics: SessionMetrics,
    val punches: List<PunchRecord>,
    val coachingTips: List<String>,
    val videoUrl: String,
)

@Serializable
data class ProgressPoint(
    val date: String,
    val punchesPerMinute: Double,
    val guardScore: Double,
    val footworkScore: Double,
    val balanceScore: Double,
    val totalPunches: Int,
)
