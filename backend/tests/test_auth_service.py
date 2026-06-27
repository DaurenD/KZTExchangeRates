"""
Task 1 — Auth Service
Acceptance criteria:
  AC1: hash+verify roundtrip returns True
  AC2: wrong password returns False
  AC3: create_access_token → decode_token returns same user_id
  AC4: tampered token raises JWTError
"""
import uuid

import pytest
from jose import JWTError

from app.services.auth import create_access_token, decode_token, hash_password, verify_password


# AC1
def test_correct_password_verifies():
    hashed = hash_password("my-secret")
    assert verify_password("my-secret", hashed) is True


# AC2
def test_wrong_password_rejected():
    hashed = hash_password("my-secret")
    assert verify_password("wrong", hashed) is False


# AC3
def test_token_roundtrip_preserves_user_id():
    user_id = str(uuid.uuid4())
    token = create_access_token(user_id)
    assert decode_token(token) == user_id


# AC4
def test_tampered_token_raises():
    token = create_access_token(str(uuid.uuid4()))
    bad_token = token[:-4] + "xxxx"
    with pytest.raises(JWTError):
        decode_token(bad_token)


def test_different_users_get_different_tokens():
    t1 = create_access_token("user-a")
    t2 = create_access_token("user-b")
    assert t1 != t2
