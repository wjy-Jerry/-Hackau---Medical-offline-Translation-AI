"""Run with FIELDTALK_ASR_TEST_AUDIO_DIR pointing to spoken WAV fixtures."""
import os
import re
from pathlib import Path

import pytest

from backend.config import ASR_DIR
from backend.models.asr import speech_to_text


def test_three_distinct_spoken_sentences():
    audio_dir = os.getenv("FIELDTALK_ASR_TEST_AUDIO_DIR")
    if not audio_dir or not (ASR_DIR / "model.bin").is_file():
        pytest.skip("Set FIELDTALK_ASR_TEST_AUDIO_DIR and download the local ASR model")
    paths = [Path(audio_dir) / name for name in ("allergy.wav", "chest.wav", "breathing.wav")]
    assert all(path.is_file() for path in paths), "Expected allergy.wav, chest.wav, and breathing.wav"
    texts = [speech_to_text(path, "en").text.lower() for path in paths]
    normalized = [re.sub(r"[^a-z ]", "", text) for text in texts]
    assert len(set(normalized)) == 3, texts
    assert "penicillin" in normalized[0], texts
    assert "chest" in normalized[1], texts
    assert "breathe" in normalized[2] or "breathing" in normalized[2], texts


def test_chinese_speech_when_fixture_is_available():
    audio_dir = os.getenv("FIELDTALK_ASR_TEST_AUDIO_DIR")
    if not audio_dir or not (ASR_DIR / "model.bin").is_file():
        pytest.skip("Set FIELDTALK_ASR_TEST_AUDIO_DIR and download the local ASR model")
    path = Path(audio_dir) / "allergy_zh.wav"
    if not path.is_file():
        pytest.skip("Optional Chinese spoken fixture is not present")
    result = speech_to_text(path, "zh")
    assert result.language == "zh"
    assert re.search(r"[\u4e00-\u9fff]", result.text)
