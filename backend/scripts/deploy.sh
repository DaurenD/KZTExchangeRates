#!/usr/bin/env bash
# Builds Docker images, pushes to Artifact Registry, and deploys both
# Cloud Run services (API + Worker).
#
# Prerequisites:
#   Run setup_gcp.sh first.
#   export PROJECT_ID=<YOUR_PROJECT>   (or set it below)

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-boxing-analysis}"
REGION="${REGION:-us-central1}"
DB_INSTANCE="${DB_INSTANCE:-boxing-db}"
REGISTRY="$REGION-docker.pkg.dev/$PROJECT_ID/boxing-docker"

API_IMAGE="$REGISTRY/boxing-api:latest"
WORKER_IMAGE="$REGISTRY/boxing-worker:latest"

API_SA="boxing-api@${PROJECT_ID}.iam.gserviceaccount.com"
WORKER_SA="boxing-worker@${PROJECT_ID}.iam.gserviceaccount.com"

CONNECTION_NAME=$(gcloud sql instances describe "$DB_INSTANCE" \
  --project="$PROJECT_ID" --format="value(connectionName)")

BACKEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# ── Authenticate Docker to Artifact Registry ──────────────────────────────────
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# ── Build & push API image ────────────────────────────────────────────────────
echo "Building API image..."
docker build -t "$API_IMAGE" -f "$BACKEND_DIR/docker/Dockerfile.api" "$BACKEND_DIR"
docker push "$API_IMAGE"

# ── Build & push Worker image ─────────────────────────────────────────────────
echo "Building Worker image..."
docker build -t "$WORKER_IMAGE" -f "$BACKEND_DIR/docker/Dockerfile.worker" "$BACKEND_DIR"
docker push "$WORKER_IMAGE"

# ── Deploy Worker first (API enqueues tasks pointing at Worker URL) ───────────
echo "Deploying Worker..."
gcloud run deploy boxing-worker \
  --image="$WORKER_IMAGE" \
  --platform=managed \
  --region="$REGION" \
  --service-account="$WORKER_SA" \
  --add-cloudsql-instances="$CONNECTION_NAME" \
  --set-secrets="DATABASE_URL=boxing-database-url:latest,SECRET_KEY=boxing-secret-key:latest,ANTHROPIC_API_KEY=boxing-anthropic-key:latest" \
  --set-env-vars="IS_WORKER=true,GCP_PROJECT_ID=$PROJECT_ID,GCS_BUCKET_NAME=boxing-videos,CLOUD_TASKS_LOCATION=$REGION,WORKER_SERVICE_ACCOUNT=$WORKER_SA" \
  --no-allow-unauthenticated \
  --min-instances=0 \
  --max-instances=5 \
  --timeout=1800 \
  --memory=2Gi \
  --cpu=2 \
  --project="$PROJECT_ID"

WORKER_URL=$(gcloud run services describe boxing-worker \
  --region="$REGION" --project="$PROJECT_ID" --format="value(status.url)")

# ── Deploy API ────────────────────────────────────────────────────────────────
echo "Deploying API..."
gcloud run deploy boxing-api \
  --image="$API_IMAGE" \
  --platform=managed \
  --region="$REGION" \
  --service-account="$API_SA" \
  --add-cloudsql-instances="$CONNECTION_NAME" \
  --set-secrets="DATABASE_URL=boxing-database-url:latest,SECRET_KEY=boxing-secret-key:latest" \
  --set-env-vars="GCP_PROJECT_ID=$PROJECT_ID,GCS_BUCKET_NAME=boxing-videos,CLOUD_TASKS_LOCATION=$REGION,CLOUD_TASKS_QUEUE=boxing-analysis-queue,WORKER_URL=$WORKER_URL,API_SERVICE_ACCOUNT=$API_SA,WORKER_SERVICE_ACCOUNT=$WORKER_SA" \
  --allow-unauthenticated \
  --min-instances=0 \
  --max-instances=10 \
  --timeout=60 \
  --memory=512Mi \
  --project="$PROJECT_ID"

API_URL=$(gcloud run services describe boxing-api \
  --region="$REGION" --project="$PROJECT_ID" --format="value(status.url)")

echo ""
echo "=== Deploy complete ==="
echo "API URL:    $API_URL"
echo "Worker URL: $WORKER_URL"
echo ""
echo "Update mobile app base URL to: $API_URL"
