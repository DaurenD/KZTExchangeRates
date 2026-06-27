import json

from google.cloud import tasks_v2
from google.protobuf import duration_pb2

from app.config import settings

_client = tasks_v2.CloudTasksClient()
_queue_path = _client.queue_path(
    settings.gcp_project_id,
    settings.cloud_tasks_location,
    settings.cloud_tasks_queue,
)


def enqueue_analysis(session_id: str) -> None:
    """Push an HTTP task that calls the worker's /analyze endpoint."""
    payload = json.dumps({"session_id": session_id}).encode()
    task = {
        "http_request": {
            "http_method": tasks_v2.HttpMethod.POST,
            "url": f"{settings.worker_url}/internal/analyze",
            "headers": {"Content-Type": "application/json"},
            "body": payload,
            # Cloud Run service-to-service auth via OIDC
            "oidc_token": {"service_account_email": f"boxing-worker@{settings.gcp_project_id}.iam.gserviceaccount.com"},
        },
        "dispatch_deadline": duration_pb2.Duration(seconds=1800),  # 30 min max per task
    }
    _client.create_task(parent=_queue_path, task=task)
