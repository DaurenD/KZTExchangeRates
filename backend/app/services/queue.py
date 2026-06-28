import json
from functools import lru_cache

from google.cloud import tasks_v2
from google.protobuf import duration_pb2

from app.config import settings


@lru_cache(maxsize=1)
def _client() -> tasks_v2.CloudTasksClient:
    return tasks_v2.CloudTasksClient()


def _queue_path() -> str:
    return _client().queue_path(
        settings.gcp_project_id,
        settings.cloud_tasks_location,
        settings.cloud_tasks_queue,
    )


def enqueue_analysis(session_id: str) -> None:
    payload = json.dumps({"session_id": session_id}).encode()
    task = {
        "http_request": {
            "http_method": tasks_v2.HttpMethod.POST,
            "url": f"{settings.worker_url}/internal/analyze",
            "headers": {"Content-Type": "application/json"},
            "body": payload,
            "oidc_token": {
                "service_account_email": settings.worker_service_account,
            },
        },
        "dispatch_deadline": duration_pb2.Duration(seconds=1800),
    }
    _client().create_task(parent=_queue_path(), task=task)
