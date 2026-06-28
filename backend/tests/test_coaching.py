"""
Task 6 — Coaching Tips (Claude API integration)
Acceptance criteria:
  AC1: valid JSON array from Claude → returned as list of strings
  AC2: malformed JSON from Claude → still returns a non-empty list (no crash)
  AC3: generated prompt includes all key metric fields
"""
import json
from unittest.mock import MagicMock, patch

import pytest

from ml.coaching.tips import generate_tips

_METRICS = {
    "total_punches": 120,
    "punches_per_minute": 40.0,
    "punch_breakdown": {"JAB": 60, "CROSS": 40, "LEFT_HOOK": 20},
    "total_combinations": 15,
    "max_combo_length": 4,
    "active_ratio": 0.65,
    "guard_score": 45.0,
    "footwork_score": 70.0,
    "balance_score": 80.0,
    "peak_speed": 1.2,
    "avg_speed": 0.7,
}

_PROFILE = {"stance": "ORTHODOX", "experience_level": "BEGINNER"}


def _mock_claude(response_text: str):
    """Return a mock anthropic client that yields response_text."""
    message = MagicMock()
    message.content = [MagicMock(text=response_text)]
    client = MagicMock()
    client.messages.create.return_value = message
    return client


# ── AC1 ──────────────────────────────────────────────────────────────────────

def test_valid_json_array_returned_as_list():
    tips_json = json.dumps(["Keep your guard up.", "Work on combinations.", "More lateral movement."])
    with patch("ml.coaching.tips._client", _mock_claude(tips_json)):
        result = generate_tips(_METRICS, _PROFILE)
    assert isinstance(result, list)
    assert len(result) == 3
    assert all(isinstance(t, str) for t in result)


# ── AC2 ──────────────────────────────────────────────────────────────────────

def test_malformed_json_does_not_crash():
    with patch("ml.coaching.tips._client", _mock_claude("Sorry, I cannot help with that.")):
        result = generate_tips(_METRICS, _PROFILE)
    assert isinstance(result, list)
    assert len(result) >= 1


def test_partial_json_does_not_crash():
    with patch("ml.coaching.tips._client", _mock_claude('["Keep guard up", "Work on jab')):
        result = generate_tips(_METRICS, _PROFILE)
    assert isinstance(result, list)


# ── AC3 ──────────────────────────────────────────────────────────────────────

def test_prompt_includes_key_metrics():
    captured_prompt = {}

    def capture_call(**kwargs):
        captured_prompt["messages"] = kwargs["messages"]
        message = MagicMock()
        message.content = [MagicMock(text='["tip"]')]
        return message

    mock_client = MagicMock()
    mock_client.messages.create.side_effect = capture_call

    with patch("ml.coaching.tips._client", mock_client):
        generate_tips(_METRICS, _PROFILE)

    prompt_text = captured_prompt["messages"][0]["content"]
    assert "guard_score" in prompt_text or "Guard" in prompt_text or "45" in prompt_text
    assert "40.0" in prompt_text or "punches per minute" in prompt_text.lower()
    assert "120" in prompt_text   # total_punches
