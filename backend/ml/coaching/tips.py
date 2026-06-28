"""Generate coaching tips via Claude API given session metrics."""
from __future__ import annotations

import anthropic

from app.config import settings

_client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

_SYSTEM = (
    "You are an experienced boxing coach. "
    "Given a boxer's session metrics, provide 3–5 concise, actionable coaching tips. "
    "Focus on the weakest areas first. Be specific and encouraging. "
    "Return only a JSON array of strings — no other text."
)


def generate_tips(metrics: dict, profile: dict) -> list[str]:
    prompt = (
        f"Boxer profile: stance={profile.get('stance', 'ORTHODOX')}, "
        f"experience={profile.get('experience_level', 'BEGINNER')}.\n\n"
        f"Session metrics:\n"
        f"- Total punches: {metrics['total_punches']}\n"
        f"- Punches per minute: {metrics['punches_per_minute']:.1f}\n"
        f"- Punch breakdown: {metrics['punch_breakdown']}\n"
        f"- Combinations: {metrics['total_combinations']} (max length: {metrics['max_combo_length']})\n"
        f"- Active ratio: {metrics['active_ratio']:.0%}\n"
        f"- Guard score: {metrics['guard_score']:.0f}/100\n"
        f"- Footwork score: {metrics['footwork_score']:.0f}/100\n"
        f"- Balance score: {metrics['balance_score']:.0f}/100\n"
        f"- Peak speed: {metrics['peak_speed']:.3f}, avg speed: {metrics['avg_speed']:.3f}\n\n"
        "Provide coaching tips as a JSON array of strings."
    )

    message = _client.messages.create(
        model="claude-haiku-4-5-20251001",
        max_tokens=512,
        system=_SYSTEM,
        messages=[{"role": "user", "content": prompt}],
    )

    import json
    text = message.content[0].text.strip()
    try:
        tips = json.loads(text)
        return tips if isinstance(tips, list) else [text]
    except json.JSONDecodeError:
        return [text]
