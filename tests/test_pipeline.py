"""API contract tests with local ASR replaced only inside the test."""
import io
import subprocess
import sys
import wave

import pytest

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
    ("source", "expected", "key_information"),
    [
        ("I am allergic to penicillin.", "青霉素", {"allergy": "Penicillin"}),
        ("My chest hurts.", "胸", {"symptom": "Chest pain"}),
        ("I cannot breathe normally.", "呼吸", {"breathing_difficulty": "Difficulty breathing"}),
        ("I am taking medication for asthma.", "哮喘", {"medication": "Medication for asthma"}),
    ],
)
def test_translation_contract(source, expected, key_information):
    result = translate_and_extract(source, "en", "zh")
    assert expected in result.translation
    assert result.key_information == key_information


@pytest.mark.parametrize(
    ("source", "expected", "key_information"),
    [
        ("我对青霉素过敏。", "penicillin", {"allergy": "青霉素"}),
        ("我的胸口疼。", "chest", {"symptom": "Chest pain"}),
        ("我无法正常呼吸。", "breathe", {"breathing_difficulty": "Difficulty breathing"}),
        ("我正在服用治疗哮喘的药物。", "asthma", {"medication": "治疗哮喘的药物"}),
    ],
)
def test_reverse_translation_contract(source, expected, key_information):
    result = translate_and_extract(source, "zh", "en")
    assert expected in result.translation.lower()
    assert result.key_information == key_information


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


@pytest.mark.parametrize(("language", "text"), [
    ("en", "My chest hurts."),
    ("zh", "我对青霉素过敏。"),
    ("ru", "У меня болит грудь."),
])
def test_tts_contract(language, text):
    path = text_to_speech(text, language)
    with wave.open(str(path), "rb") as audio:
        assert audio.getnframes() > 0
        assert audio.getframerate() > 0
    path.unlink()


def test_real_translation_and_tts_end_to_end(monkeypatch):
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
    assert body["key_information"] == {"allergy": "Penicillin"}
    assert body["confidence"] is None
    assert body["mode"] == "local"
    assert client.get(body["audio_url"]).headers["content-type"] == "audio/wav"


def test_tts_failure_keeps_translated_text(monkeypatch):
    monkeypatch.setattr(
        main,
        "speech_to_text",
        lambda audio_path, language: ASRResult(text="My chest hurts.", language=language, confidence=None),
    )
    def fail_tts(text, language):
        raise RuntimeError("Test voice failure")
    monkeypatch.setattr(main, "text_to_speech", fail_tts)
    response = client.post(
        "/process_audio",
        data={"source_language": "en", "target_language": "zh"},
        files={"audio": ("sample.wav", b"mock audio", "audio/wav")},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["original_text"] == "My chest hurts."
    assert "胸" in body["translation"]
    assert body["key_information"] == {"symptom": "Chest pain"}
    assert body["audio_url"] == ""
    assert "Speech unavailable" in body["warning"]


def test_rejects_unsupported_pair():
    response = client.post(
        "/process_audio",
        data={"source_language": "en", "target_language": "en"},
        files={"audio": ("sample.wav", b"mock audio", "audio/wav")},
    )
    assert response.status_code == 400


@pytest.mark.parametrize("question_id", ["pain", "breathing", "allergy", "medication", "consciousness", "bleeding"])
@pytest.mark.parametrize("language", ["en", "zh"])
def test_predefined_question_audio(question_id, language, monkeypatch):
    monkeypatch.setattr(main, "speech_to_text", lambda *args: pytest.fail("Quick questions must bypass ASR"))
    response = client.get(f"/quick_question/{question_id}/audio", params={"target_language": language})
    assert response.status_code == 200, response.text
    assert response.headers["content-type"] == "audio/wav"
    assert response.headers["cache-control"] == "no-store"
    with wave.open(io.BytesIO(response.content), "rb") as audio:
        assert audio.getnframes() > 0


def test_predefined_question_rejects_unknown_id_and_language():
    assert client.get("/quick_question/unknown/audio", params={"target_language": "en"}).status_code == 404
    assert client.get("/quick_question/pain/audio", params={"target_language": "ru"}).status_code == 400


def test_predefined_question_audio_failure_keeps_written_prompt_available(monkeypatch):
    def fail_tts(*args):
        raise RuntimeError("Test voice failure")
    monkeypatch.setattr(main, "text_to_speech", fail_tts)
    response = client.get("/quick_question/pain/audio", params={"target_language": "zh"})
    assert response.status_code == 503
    assert main.QUESTIONS["pain"]["zh"] == "哪里疼？"
