#!/usr/bin/env bash
# Provisions all GCP resources needed to run the boxing analysis backend.
# Run once per project. Idempotent — safe to re-run.
#
# Prerequisites:
#   gcloud auth login && gcloud config set project <YOUR_PROJECT>
#   export PROJECT_ID=<YOUR_PROJECT>   (or set it below)

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-boxing-analysis}"
REGION="${REGION:-us-central1}"
BUCKET="${BUCKET:-boxing-videos}"
DB_INSTANCE="${DB_INSTANCE:-boxing-db}"
DB_NAME="${DB_NAME:-boxing}"
DB_USER="${DB_USER:-boxing}"
QUEUE="${QUEUE:-boxing-analysis-queue}"
API_SA="boxing-api"
WORKER_SA="boxing-worker"

echo "=== Project: $PROJECT_ID  Region: $REGION ==="

# ── Enable APIs ───────────────────────────────────────────────────────────────
gcloud services enable \
  sqladmin.googleapis.com \
  storage.googleapis.com \
  cloudtasks.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iam.googleapis.com \
  --project="$PROJECT_ID"

# ── GCS bucket ────────────────────────────────────────────────────────────────
if ! gsutil ls -b "gs://$BUCKET" &>/dev/null; then
  gsutil mb -p "$PROJECT_ID" -l "$REGION" "gs://$BUCKET"
fi
gsutil cors set "$(dirname "$0")/gcs_cors.json" "gs://$BUCKET"
# Retain videos for 90 days then auto-delete
gsutil lifecycle set /dev/stdin "gs://$BUCKET" <<'EOF'
{"rule":[{"action":{"type":"Delete"},"condition":{"age":90}}]}
EOF

# ── Cloud SQL (PostgreSQL 15) ─────────────────────────────────────────────────
if ! gcloud sql instances describe "$DB_INSTANCE" --project="$PROJECT_ID" &>/dev/null; then
  gcloud sql instances create "$DB_INSTANCE" \
    --database-version=POSTGRES_15 \
    --tier=db-f1-micro \
    --region="$REGION" \
    --storage-type=SSD \
    --storage-size=10 \
    --project="$PROJECT_ID"
fi

if ! gcloud sql databases describe "$DB_NAME" --instance="$DB_INSTANCE" --project="$PROJECT_ID" &>/dev/null; then
  gcloud sql databases create "$DB_NAME" --instance="$DB_INSTANCE" --project="$PROJECT_ID"
fi

DB_PASSWORD=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
gcloud sql users create "$DB_USER" \
  --instance="$DB_INSTANCE" \
  --password="$DB_PASSWORD" \
  --project="$PROJECT_ID" 2>/dev/null || \
  gcloud sql users set-password "$DB_USER" \
    --instance="$DB_INSTANCE" \
    --password="$DB_PASSWORD" \
    --project="$PROJECT_ID"

CONNECTION_NAME=$(gcloud sql instances describe "$DB_INSTANCE" \
  --project="$PROJECT_ID" --format="value(connectionName)")
DATABASE_URL="postgresql+asyncpg://$DB_USER:$DB_PASSWORD@/$DB_NAME?host=/cloudsql/$CONNECTION_NAME"

# ── Cloud Tasks queue ─────────────────────────────────────────────────────────
if ! gcloud tasks queues describe "$QUEUE" --location="$REGION" --project="$PROJECT_ID" &>/dev/null; then
  gcloud tasks queues create "$QUEUE" \
    --location="$REGION" \
    --max-dispatches-per-second=10 \
    --max-concurrent-dispatches=5 \
    --project="$PROJECT_ID"
fi

# ── Service accounts ──────────────────────────────────────────────────────────
for SA in "$API_SA" "$WORKER_SA"; do
  if ! gcloud iam service-accounts describe "${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
       --project="$PROJECT_ID" &>/dev/null; then
    gcloud iam service-accounts create "$SA" \
      --display-name="Boxing ${SA^}" \
      --project="$PROJECT_ID"
  fi
done

API_SA_EMAIL="${API_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
WORKER_SA_EMAIL="${WORKER_SA}@${PROJECT_ID}.iam.gserviceaccount.com"

# API SA needs: GCS object read/write, Cloud Tasks enqueue, Cloud SQL client
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$API_SA_EMAIL" \
  --role="roles/storage.objectAdmin" --condition=None
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$API_SA_EMAIL" \
  --role="roles/cloudtasks.enqueuer" --condition=None
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$API_SA_EMAIL" \
  --role="roles/cloudsql.client" --condition=None

# API SA needs to sign its own tokens (for presigned GCS URLs without a key file)
gcloud iam service-accounts add-iam-policy-binding "$API_SA_EMAIL" \
  --member="serviceAccount:$API_SA_EMAIL" \
  --role="roles/iam.serviceAccountTokenCreator" \
  --project="$PROJECT_ID"

# Worker SA needs: GCS object read, Cloud SQL client, Anthropic secret
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$WORKER_SA_EMAIL" \
  --role="roles/storage.objectViewer" --condition=None
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$WORKER_SA_EMAIL" \
  --role="roles/cloudsql.client" --condition=None
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$WORKER_SA_EMAIL" \
  --role="roles/secretmanager.secretAccessor" --condition=None

# Cloud Tasks needs to invoke the Worker Cloud Run service
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$WORKER_SA_EMAIL" \
  --role="roles/run.invoker" --condition=None

# ── Secret Manager ────────────────────────────────────────────────────────────
store_secret() {
  local name="$1" value="$2"
  if ! gcloud secrets describe "$name" --project="$PROJECT_ID" &>/dev/null; then
    printf '%s' "$value" | gcloud secrets create "$name" \
      --data-file=- --replication-policy=automatic --project="$PROJECT_ID"
  else
    printf '%s' "$value" | gcloud secrets versions add "$name" \
      --data-file=- --project="$PROJECT_ID"
  fi
}

SECRET_KEY=$(python3 -c "import secrets; print(secrets.token_hex(32))")
store_secret "boxing-database-url" "$DATABASE_URL"
store_secret "boxing-secret-key" "$SECRET_KEY"

# Anthropic key must be set manually — prompt the user
echo ""
echo "=== ACTION REQUIRED ==="
echo "Store your Anthropic API key in Secret Manager:"
echo "  echo -n 'sk-ant-...' | gcloud secrets create boxing-anthropic-key --data-file=- --replication-policy=automatic --project=$PROJECT_ID"
echo ""
echo "Then grant the Worker SA access:"
echo "  gcloud secrets add-iam-policy-binding boxing-anthropic-key --member=serviceAccount:$WORKER_SA_EMAIL --role=roles/secretmanager.secretAccessor --project=$PROJECT_ID"
echo ""

# ── Artifact Registry ─────────────────────────────────────────────────────────
if ! gcloud artifacts repositories describe boxing-docker \
     --location="$REGION" --project="$PROJECT_ID" &>/dev/null; then
  gcloud artifacts repositories create boxing-docker \
    --repository-format=docker \
    --location="$REGION" \
    --project="$PROJECT_ID"
fi

echo "=== GCP provisioning complete ==="
echo "API SA:    $API_SA_EMAIL"
echo "Worker SA: $WORKER_SA_EMAIL"
echo "Bucket:    gs://$BUCKET"
echo "DB:        $CONNECTION_NAME"
echo "Queue:     $QUEUE ($REGION)"
