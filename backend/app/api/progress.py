from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.dependencies import current_user
from app.models.session import Session
from app.models.user import User
from app.schemas.results import ProgressPoint, ProgressResponse

router = APIRouter(prefix="/progress", tags=["progress"])


@router.get("", response_model=ProgressResponse)
async def get_progress(
    db: AsyncSession = Depends(get_db),
    user: User = Depends(current_user),
):
    result = await db.execute(
        select(Session)
        .options(selectinload(Session.metrics))
        .where(Session.user_id == user.id, Session.analysis_state == "COMPLETE")
        .order_by(Session.started_at.asc())
    )
    sessions = result.scalars().all()

    points = [
        ProgressPoint(
            date=s.started_at.date().isoformat(),
            punches_per_minute=s.metrics.punches_per_minute,
            guard_score=s.metrics.guard_score,
            footwork_score=s.metrics.footwork_score,
            balance_score=s.metrics.balance_score,
            total_punches=s.metrics.total_punches,
        )
        for s in sessions
        if s.metrics
    ]
    return ProgressResponse(data_points=points, sessions_analysed=len(points))
