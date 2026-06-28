"""Shared fixtures for API tests."""
import uuid
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.models.user import User
from app.services.auth import create_access_token


def _make_user(user_id: str | None = None) -> User:
    u = User()
    u.id = uuid.UUID(user_id) if user_id else uuid.uuid4()
    u.email = "test@example.com"
    u.name = "Test User"
    u.stance = "ORTHODOX"
    u.experience_level = "BEGINNER"
    u.hashed_password = "hashed"
    return u


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def auth_user():
    return _make_user()


@pytest.fixture
def auth_headers(auth_user):
    token = create_access_token(str(auth_user.id))
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def mock_current_user(auth_user):
    """Override the current_user dependency with a fake user."""
    from app.dependencies import current_user
    app.dependency_overrides[current_user] = lambda: auth_user
    yield auth_user
    app.dependency_overrides.clear()
