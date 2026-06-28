import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.dependencies import current_user
from app.models.metrics import CoachingTip, Punch, SessionMetrics
from app.models.session import Session
from app.models.user import User
from app.schemas.results import MetricsResult, PunchResult, SessionResultsResponse
from app.schemas.session import (
    CreateSessionRequest,
    CreateSessionResponse,
    SessionListResponse,
    SessionStatusResponse,
    SessionSummary,
)
from app.services.queue import enqueue_analysis
from app.services.storage import presigned_playback_url, presigned_upload_url, video_exists

router = APIRouter(prefix="/sessions", tags=["sessions"])


@router.post("", response_model=CreateSessionResponse, status_code=status.HTTP_201_CREATED)
async def create_session(
    body: CreateSessionRequest,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    session_id = uuid.uuid4()
    gcs_path = f"users/{user.id}/sessions/{session_id}/video.mp4"

    session = Session(
        id=session_id,
        user_id=user.id,
        session_type=body.session_type,
        started_at=body.started_at,
        ended_at=body.ended_at,
        duration_ms=body.duration_ms,
        gcs_video_path=gcs_path,
        analysis_state="PENDING",
    )
    db.add(session)
    await db.commit()

    upload_url = presigned_upload_url(gcs_path)
    expiry = datetime.now(timezone.utc).replace(second=0, microsecond=0)

    return CreateSessionResponse(
        session_id=str(session_id),
        upload_url=upload_url,
        upload_expires_at=expiry,
    )


@router.get("", response_model=SessionListResponse)
async def list_sessions(
    page: int = 1,
    page_size: int = 20,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    offset = (page - 1) * page_size
    total_result = await db.execute(
        select(func.count()).where(Session.user_id == user.id)
    )
    total = total_result.scalar_one()

    result = await db.execute(
        select(Session)
        .options(selectinload(Session.metrics))
        .where(Session.user_id == user.id)
        .order_by(Session.started_at.desc())
        .offset(offset)
        .limit(page_size)
    )
    sessions = result.scalars().all()

    summaries = [
        SessionSummary(
            id=str(s.id),
            session_type=s.session_type,
            started_at=s.started_at,
            duration_ms=s.duration_ms,
            analysis_state=s.analysis_state,
            total_punches=s.metrics.total_punches if s.metrics else None,
            punches_per_minute=s.metrics.punches_per_minute if s.metrics else None,
        )
        for s in sessions
    ]
    return SessionListResponse(sessions=summaries, total=total, page=page, page_size=page_size)


@router.post("/{session_id}/analyze", status_code=status.HTTP_202_ACCEPTED)
async def trigger_analysis(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    session = await _get_owned_session(session_id, user.id, db)

    if not session.gcs_video_path or not video_exists(session.gcs_video_path):
        raise HTTPException(status_code=400, detail="Video not yet uploaded")
    if session.analysis_state == "PROCESSING":
        raise HTTPException(status_code=409, detail="Analysis already in progress")
    if session.analysis_state == "COMPLETE":
        raise HTTPException(status_code=409, detail="Analysis already complete")

    session.analysis_state = "PROCESSING"
    await db.commit()

    enqueue_analysis(session_id)
    return {"status": "queued"}


@router.get("/{session_id}/status", response_model=SessionStatusResponse)
async def get_status(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    session = await _get_owned_session(session_id, user.id, db)
    return SessionStatusResponse(session_id=session_id, analysis_state=session.analysis_state)


@router.get("/{session_id}/results", response_model=SessionResultsResponse)
async def get_results(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    session = await _get_owned_session(session_id, user.id, db)
    if session.analysis_state != "COMPLETE":
        raise HTTPException(status_code=404, detail="Results not ready")

    result = await db.execute(
        select(Session)
        .options(selectinload(Session.metrics), selectinload(Session.punches), selectinload(Session.coaching))
        .where(Session.id == uuid.UUID(session_id))
    )
    session = result.scalar_one()

    video_url = presigned_playback_url(session.gcs_video_path) if session.gcs_video_path else ""

    return SessionResultsResponse(
        session_id=session_id,
        metrics=MetricsResult.model_validate(session.metrics),
        punches=[PunchResult.model_validate(p) for p in session.punches],
        coaching_tips=session.coaching.tips if session.coaching else [],
        video_url=video_url,
    )


async def _get_owned_session(session_id: str, user_id: uuid.UUID, db: AsyncSession) -> Session:
    result = await db.execute(
        select(Session).where(Session.id == uuid.UUID(session_id), Session.user_id == user_id)
    )
    session = result.scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    return session
