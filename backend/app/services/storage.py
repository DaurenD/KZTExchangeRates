from datetime import timedelta

from google.cloud import storage

from app.config import settings

_client = storage.Client(project=settings.gcp_project_id)
_bucket = _client.bucket(settings.gcs_bucket_name)


def presigned_upload_url(gcs_path: str) -> str:
    """Returns a V4 signed URL the mobile client can PUT the video to directly."""
    blob = _bucket.blob(gcs_path)
    return blob.generate_signed_url(
        version="v4",
        expiration=timedelta(minutes=settings.gcs_video_upload_expiry_minutes),
        method="PUT",
        content_type="video/mp4",
    )


def presigned_playback_url(gcs_path: str) -> str:
    blob = _bucket.blob(gcs_path)
    return blob.generate_signed_url(
        version="v4",
        expiration=timedelta(minutes=settings.gcs_video_playback_expiry_minutes),
        method="GET",
    )


def download_to_file(gcs_path: str, local_path: str) -> None:
    _bucket.blob(gcs_path).download_to_filename(local_path)


def video_exists(gcs_path: str) -> bool:
    return _bucket.blob(gcs_path).exists()
