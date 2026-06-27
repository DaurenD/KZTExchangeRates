-- Boxing Analysis DB — initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR     UNIQUE NOT NULL,
    hashed_password VARCHAR     NOT NULL,
    name            VARCHAR     NOT NULL,
    stance          VARCHAR     NOT NULL DEFAULT 'ORTHODOX',
    weight_kg       FLOAT,
    height_cm       FLOAT,
    experience_level VARCHAR    NOT NULL DEFAULT 'BEGINNER',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_type    VARCHAR     NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL,
    ended_at        TIMESTAMPTZ,
    duration_ms     BIGINT,
    gcs_video_path  VARCHAR,
    analysis_state  VARCHAR     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX sessions_user_id_idx ON sessions(user_id);
CREATE INDEX sessions_started_at_idx ON sessions(started_at DESC);

CREATE TABLE session_metrics (
    session_id          UUID    PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,
    total_punches       INT     NOT NULL DEFAULT 0,
    punches_per_minute  FLOAT   NOT NULL DEFAULT 0,
    punch_breakdown     JSONB   NOT NULL DEFAULT '{}',
    total_combinations  INT     NOT NULL DEFAULT 0,
    max_combo_length    INT     NOT NULL DEFAULT 0,
    avg_combo_length    FLOAT   NOT NULL DEFAULT 0,
    active_ratio        FLOAT   NOT NULL DEFAULT 0,
    guard_score         FLOAT   NOT NULL DEFAULT 0,
    footwork_score      FLOAT   NOT NULL DEFAULT 0,
    balance_score       FLOAT   NOT NULL DEFAULT 0,
    peak_speed          FLOAT   NOT NULL DEFAULT 0,
    avg_speed           FLOAT   NOT NULL DEFAULT 0
);

CREATE TABLE punches (
    id              BIGSERIAL   PRIMARY KEY,
    session_id      UUID        NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    timestamp_ms    BIGINT      NOT NULL,
    punch_type      VARCHAR     NOT NULL,
    hand            VARCHAR     NOT NULL,
    speed_estimate  FLOAT       NOT NULL DEFAULT 0,
    confidence      FLOAT       NOT NULL DEFAULT 0
);

CREATE INDEX punches_session_id_idx ON punches(session_id);

CREATE TABLE coaching_tips (
    session_id  UUID    PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,
    tips        JSONB   NOT NULL
);
