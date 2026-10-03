"""A standalone Android phrase workflow needs complete bundled local audio."""

import wave

from backend.config import ROOT
from backend.phrase_packs import question_rows


def test_all_android_quick_questions_have_local_audio():
    audio_dir = ROOT / "android" / "app" / "src" / "main" / "assets" / "audio"
    expected = {f"{row['id']}_{code}.wav" for row in question_rows() for code in ("en", "zh", "ru")}
    assert {path.name for path in audio_dir.glob("*.wav")} == expected
    for filename in expected:
        with wave.open(str(audio_dir / filename), "rb") as audio:
            assert audio.getnframes() > 0
            assert audio.getnchannels() >= 1
            assert audio.getsampwidth() == 2


def test_android_project_reads_shared_phrase_pack():
    gradle = (ROOT / "android" / "app" / "build.gradle").read_text(encoding="utf-8")
    assert "../../data/phrase_packs" in gradle
    assert "com.google.mlkit:translate" in gradle
    manifest = (ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml").read_text(encoding="utf-8")
    assert 'android:usesCleartextTraffic="false"' in manifest
