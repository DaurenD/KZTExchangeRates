from datetime import timedelta
from functools import lru_cache

from google.cloud import storage
from google.oauth2 import service_account

from app.config import settings


@lru_cache(maxsize=1)
def _client() -> storage.Client:
    # On Cloud Run, credentials come from the attached service account.
    # The service account must have roles/iam.serviceAccountTokenCreator on
    # itself so it can sign URLs without a key file.
    return storage.Client(project=settings.gcp_project_id)


def _bucket() -> storage.Bucket:
    return _client().bucket(settings.gcs_bucket_name)


def presigned_upload_url(gcs_path: str) -> str:
    """Presigned PUT URL — mobile client uploads video directly to GCS."""
    blob = _bucket().blob(gcs_path)
    return blob.generate_signed_url(
        version="v4",
        expiration=timedelta(minutes=settings.gcs_video_upload_expiry_minutes),
        method="PUT",
        content_type="video/mp4",
        service_account_email=settings.api_service_account,
    )


def presigned_playback_url(gcs_path: str) -> str:
    blob = _bucket().blob(gcs_path)
    return blob.generate_signed_url(
        version="v4",
        expiration=timedelta(minutes=settings.gcs_video_playback_expiry_minutes),
        method="GET",
        service_account_email=settings.api_service_account,
    )


def download_to_file(gcs_path: str, local_path: str) -> None:
    _bucket().blob(gcs_path).download_to_filename(local_path)


def video_exists(gcs_path: str) -> bool:
    return _bucket().blob(gcs_path).exists()
