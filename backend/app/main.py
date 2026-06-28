from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import auth, progress, sessions, worker

app = FastAPI(title="Boxing Analysis API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(sessions.router)
app.include_router(progress.router)
app.include_router(worker.router)


@app.get("/health")
def health():
    return {"status": "ok"}
