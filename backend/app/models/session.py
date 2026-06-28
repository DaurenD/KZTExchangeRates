import uuid
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Session(Base):
    __tablename__ = "sessions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    session_type: Mapped[str] = mapped_column(String, nullable=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    duration_ms: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    gcs_video_path: Mapped[str | None] = mapped_column(String, nullable=True)
    # PENDING | PROCESSING | COMPLETE | FAILED
    analysis_state: Mapped[str] = mapped_column(String, default="PENDING")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    user: Mapped["User"] = relationship("User", back_populates="sessions")
    metrics: Mapped["SessionMetrics | None"] = relationship("SessionMetrics", back_populates="session", uselist=False, cascade="all, delete-orphan")
    punches: Mapped[list["Punch"]] = relationship("Punch", back_populates="session", cascade="all, delete-orphan")
    coaching: Mapped["CoachingTip | None"] = relationship("CoachingTip", back_populates="session", uselist=False, cascade="all, delete-orphan")
