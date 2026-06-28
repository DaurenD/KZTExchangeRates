from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Database
    database_url: str = "postgresql+asyncpg://boxing:boxing@localhost:5432/boxing"

    # Auth
    secret_key: str = "change-me-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 60 * 24 * 7  # 7 days

    # GCP
    gcp_project_id: str = "boxing-analysis"
    gcs_bucket_name: str = "boxing-videos"
    gcs_video_upload_expiry_minutes: int = 15
    gcs_video_playback_expiry_minutes: int = 60
    cloud_tasks_queue: str = "boxing-analysis-queue"
    cloud_tasks_location: str = "us-central1"
    worker_url: str = "https://boxing-worker-xxx-uc.a.run.app"
    api_service_account: str = "boxing-api@boxing-analysis.iam.gserviceaccount.com"
    worker_service_account: str = "boxing-worker@boxing-analysis.iam.gserviceaccount.com"

    # Anthropic
    anthropic_api_key: str = ""

    # Worker (set true only in worker containers)
    is_worker: bool = False


settings = Settings()
