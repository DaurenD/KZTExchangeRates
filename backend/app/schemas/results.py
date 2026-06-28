from pydantic import BaseModel


class PunchResult(BaseModel):
    timestamp_ms: int
    punch_type: str
    hand: str
    speed_estimate: float
    confidence: float

    model_config = {"from_attributes": True}


class MetricsResult(BaseModel):
    total_punches: int
    punches_per_minute: float
    punch_breakdown: dict[str, int]
    total_combinations: int
    max_combo_length: int
    avg_combo_length: float
    active_ratio: float
    guard_score: float
    footwork_score: float
    balance_score: float
    peak_speed: float
    avg_speed: float

    model_config = {"from_attributes": True}


class SessionResultsResponse(BaseModel):
    session_id: str
    metrics: MetricsResult
    punches: list[PunchResult]
    coaching_tips: list[str]
    video_url: str           # signed GCS URL for playback


class ProgressPoint(BaseModel):
    date: str                # ISO date
    punches_per_minute: float
    guard_score: float
    footwork_score: float
    balance_score: float
    total_punches: int


class ProgressResponse(BaseModel):
    data_points: list[ProgressPoint]
    sessions_analysed: int
