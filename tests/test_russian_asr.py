"""Technical Russian ASR checks with locally synthesized speech.

These fixtures are useful for regression checks but do not replace native-speaker
review or testing with actual patient speech.
"""
import re
import wave

import pytest

from backend.config import ASR_DIR, VOICES_DIR
from backend.models.asr import speech_to_text


PHRASES = [
    "У меня болит грудь.",
    "Я не могу нормально дышать.",
    "У меня аллергия на пенициллин.",
    "У меня кровотечение.",
    "Я принимаю лекарства.",
    "У меня кружится голова.",
]


def test_russian_asr_accepts_distinct_spoken_phrases(tmp_path):
    voice_path = VOICES_DIR / "ru_RU-irina-medium.onnx"
    if not (ASR_DIR / "model.bin").is_file() or not voice_path.is_file():
        pytest.skip("Install the local multilingual ASR model and Russian Piper voice")
    from piper import PiperVoice

    voice = PiperVoice.load(voice_path, config_path=voice_path.with_suffix(".onnx.json"))
    outputs = []
    for index, phrase in enumerate(PHRASES):
        audio_path = tmp_path / f"russian-{index}.wav"
        with wave.open(str(audio_path), "wb") as wav:
            voice.synthesize_wav(phrase, wav)
        result = speech_to_text(audio_path, "ru")
        assert result.language == "ru"
        assert re.search(r"[А-Яа-яЁё]", result.text), result.text
        outputs.append(result.text)
    assert len(set(outputs)) == len(PHRASES), outputs
