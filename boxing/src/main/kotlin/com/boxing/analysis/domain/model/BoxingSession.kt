package com.boxing.analysis.domain.model

data class BoxingSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val sessionType: SessionType,
    val videoPath: String?,
    val roundCount: Int,
    val roundDurationSec: Int,
    val analysisState: AnalysisState = AnalysisState.PENDING,
)

enum class AnalysisState { PENDING, PROCESSING, COMPLETE, FAILED }

data class PunchRecord(
    val id: Long = 0,
    val sessionId: String,
    val timestampMs: Long,
    val type: PunchType,
    val hand: Hand,
    val speedEstimate: Float,    // normalised 0–1 (relative frame velocity)
    val confidence: Float,       // classifier confidence 0–1
)

enum class Hand { LEFT, RIGHT }

data class SessionMetrics(
    val sessionId: String,
    val totalPunches: Int,
    val punchesPerMinute: Float,
    val punchBreakdown: Map<PunchType, Int>,
    val totalCombinations: Int,
    val maxComboLength: Int,
    val avgComboLength: Float,
    val activeRatio: Float,       // 0–1 fraction of time throwing
    val guardScore: Float,        // 0–100
    val footworkScore: Float,     // 0–100
    val balanceScore: Float,      // 0–100
    val peakSpeed: Float,
    val avgSpeed: Float,
)

data class BodyPoseSample(
    val sessionId: String,
    val frameTimestampMs: Long,
    val landmarks: List<NormalisedLandmark>,
)

data class NormalisedLandmark(
    val index: Int,
    val x: Float,   // 0–1 of frame width
    val y: Float,   // 0–1 of frame height
    val z: Float,   // depth relative to hips
    val visibility: Float,
)

data class UserProfile(
    val name: String,
    val stance: Stance,
    val weightKg: Float?,
    val heightCm: Float?,
    val experienceLevel: ExperienceLevel,
)

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL }
