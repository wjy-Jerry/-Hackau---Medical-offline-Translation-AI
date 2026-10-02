"""API contract tests with local ASR replaced only inside the test."""
import os
import subprocess
import sys
import wave

import pytest

os.environ["FIELDTALK_MODE"] = "mock"

from fastapi.testclient import TestClient

from backend import main
from backend.main import app
from backend.models import asr
from backend.models.translation import translate_and_extract
from backend.models.tts import text_to_speech
from backend.schemas import ASRResult


client = TestClient(app)


def test_asr_reports_missing_local_model(tmp_path, monkeypatch):
    path = tmp_path / "sample.wav"
    path.write_bytes(b"mock audio")
    monkeypatch.setattr(asr, "ASR_DIR", tmp_path)
    asr._model.cache_clear()
    with pytest.raises(asr.ModelUnavailable, match="ASR model missing"):
        asr.speech_to_text(path, "en")
    asr._model.cache_clear()


@pytest.mark.parametrize(
    ("source", "expected"),
    [
        ("I am allergic to penicillin.", "青霉素"),
        ("My chest hurts.", "胸"),
        ("I cannot breathe normally.", "呼吸"),
        ("I am taking medication for asthma.", "哮喘"),
    ],
)
def test_translation_contract(source, expected):
    result = translate_and_extract(source, "en", "zh")
    assert expected in result.translation
    assert result.key_information == {}


@pytest.mark.parametrize(
    ("source", "expected"),
    [
        ("我对青霉素过敏。", "penicillin"),
        ("我的胸口疼。", "chest"),
        ("我无法正常呼吸。", "breathe"),
        ("我正在服用治疗哮喘的药物。", "asthma"),
    ],
)
def test_reverse_translation_contract(source, expected):
    result = translate_and_extract(source, "zh", "en")
    assert expected in result.translation.lower()


def test_translation_cold_start_has_no_network():
    script = """
import socket
from backend.models.translation import translate_and_extract
def deny(*args, **kwargs):
    raise AssertionError('Translation attempted a network connection')
socket.create_connection = deny
socket.socket.connect = deny
socket.socket.connect_ex = deny
assert '青霉素' in translate_and_extract('I am allergic to penicillin.', 'en', 'zh').translation
assert 'penicillin' in translate_and_extract('我对青霉素过敏。', 'zh', 'en').translation.lower()
"""
    result = subprocess.run([sys.executable, "-c", script], capture_output=True, text=True)
    assert result.returncode == 0, result.stderr


def test_tts_contract():
    path = text_to_speech("我对青霉素过敏。", "zh")
    with wave.open(str(path), "rb") as audio:
        assert audio.getnframes() > 0
    path.unlink()


def test_real_translation_and_mock_tts_end_to_end(monkeypatch):
    monkeypatch.setattr(
        main,
        "speech_to_text",
        lambda audio_path, language: ASRResult(text="I am allergic to penicillin.", language=language, confidence=None),
    )
    response = client.post(
        "/process_audio",
        data={"source_language": "en", "target_language": "zh"},
        files={"audio": ("sample.wav", b"mock audio", "audio/wav")},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["original_text"] == "I am allergic to penicillin."
    assert "青霉素" in body["translation"]
    assert body["confidence"] is None
    assert body["mode"] == "mock"
    assert client.get(body["audio_url"]).headers["content-type"] == "audio/wav"


def test_rejects_unsupported_pair():
    response = client.post(
        "/process_audio",
        data={"source_language": "en", "target_language": "en"},
        files={"audio": ("sample.wav", b"mock audio", "audio/wav")},
    )
    assert response.status_code == 400
