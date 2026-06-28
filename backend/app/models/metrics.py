import uuid

from sqlalchemy import BigInteger, Float, ForeignKey, Integer
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class SessionMetrics(Base):
    __tablename__ = "session_metrics"

    session_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("sessions.id", ondelete="CASCADE"), primary_key=True)
    total_punches: Mapped[int] = mapped_column(Integer, default=0)
    punches_per_minute: Mapped[float] = mapped_column(Float, default=0.0)
    punch_breakdown: Mapped[dict] = mapped_column(JSONB, default=dict)  # {PunchType: count}
    total_combinations: Mapped[int] = mapped_column(Integer, default=0)
    max_combo_length: Mapped[int] = mapped_column(Integer, default=0)
    avg_combo_length: Mapped[float] = mapped_column(Float, default=0.0)
    active_ratio: Mapped[float] = mapped_column(Float, default=0.0)
    guard_score: Mapped[float] = mapped_column(Float, default=0.0)
    footwork_score: Mapped[float] = mapped_column(Float, default=0.0)
    balance_score: Mapped[float] = mapped_column(Float, default=0.0)
    peak_speed: Mapped[float] = mapped_column(Float, default=0.0)
    avg_speed: Mapped[float] = mapped_column(Float, default=0.0)

    session: Mapped["Session"] = relationship("Session", back_populates="metrics")


class Punch(Base):
    __tablename__ = "punches"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("sessions.id", ondelete="CASCADE"), nullable=False, index=True)
    timestamp_ms: Mapped[int] = mapped_column(BigInteger, nullable=False)
    punch_type: Mapped[str] = mapped_column(str, nullable=False)  # JAB | CROSS | LEFT_HOOK | RIGHT_HOOK | LEFT_UPPERCUT | RIGHT_UPPERCUT
    hand: Mapped[str] = mapped_column(str, nullable=False)         # LEFT | RIGHT
    speed_estimate: Mapped[float] = mapped_column(Float, default=0.0)
    confidence: Mapped[float] = mapped_column(Float, default=0.0)

    session: Mapped["Session"] = relationship("Session", back_populates="punches")


class CoachingTip(Base):
    __tablename__ = "coaching_tips"

    session_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("sessions.id", ondelete="CASCADE"), primary_key=True)
    tips: Mapped[list] = mapped_column(JSONB, nullable=False)  # list[str]

    session: Mapped["Session"] = relationship("Session", back_populates="coaching")
