"""Internal endpoint called by Cloud Tasks — not exposed publicly."""
import uuid

from fastapi import APIRouter, Depends, Header, HTTPException
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.database import get_db
from app.models.metrics import CoachingTip, Punch, SessionMetrics
from app.models.session import Session
from ml.pipeline import analyse_video

router = APIRouter(prefix="/internal", tags=["internal"])


class AnalyzeRequest(BaseModel):
    session_id: str


@router.post("/analyze")
async def analyze(
    body: AnalyzeRequest,
    db: AsyncSession = Depends(get_db),
    x_cloudtasks_queuename: str | None = Header(default=None),
):
    # Only accept calls that came through Cloud Tasks (header is injected automatically)
    if not x_cloudtasks_queuename and not settings.is_worker:
        raise HTTPException(status_code=403, detail="Forbidden")

    result = await db.execute(select(Session).where(Session.id == uuid.UUID(body.session_id)))
    session = result.scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")

    try:
        analysis = await analyse_video(session.gcs_video_path)

        db.add(SessionMetrics(session_id=session.id, **analysis["metrics"]))
        for p in analysis["punches"]:
            db.add(Punch(session_id=session.id, **p))
        db.add(CoachingTip(session_id=session.id, tips=analysis["coaching_tips"]))

        session.analysis_state = "COMPLETE"
    except Exception as exc:
        session.analysis_state = "FAILED"
        await db.commit()
        raise

    await db.commit()
    return {"status": "complete"}
