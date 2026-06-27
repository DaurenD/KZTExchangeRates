from datetime import datetime
from pydantic import BaseModel


class CreateSessionRequest(BaseModel):
    session_type: str  # SHADOWBOXING | BAG_WORK | PAD_WORK | SPARRING | DRILL
    started_at: datetime
    ended_at: datetime
    duration_ms: int


class CreateSessionResponse(BaseModel):
    session_id: str
    upload_url: str        # GCS presigned PUT URL — mobile uploads video directly
    upload_expires_at: datetime


class SessionStatusResponse(BaseModel):
    session_id: str
    analysis_state: str    # PENDING | PROCESSING | COMPLETE | FAILED


class SessionSummary(BaseModel):
    id: str
    session_type: str
    started_at: datetime
    duration_ms: int | None
    analysis_state: str
    total_punches: int | None = None
    punches_per_minute: float | None = None

    model_config = {"from_attributes": True}


class SessionListResponse(BaseModel):
    sessions: list[SessionSummary]
    total: int
    page: int
    page_size: int
