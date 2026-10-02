"""TTS adapter: text_to_speech(text, language) -> local WAV path."""
import wave
from functools import lru_cache
from pathlib import Path
from uuid import uuid4

from backend.config import AUDIO_DIR, VOICES, VOICES_DIR
from backend.models.asr import ModelUnavailable


@lru_cache(maxsize=3)
def _voice(language: str):
    voice_name = VOICES.get(language)
    if voice_name is None:
        raise ValueError(f"No voice configured for {language}.")
    model_path = VOICES_DIR / f"{voice_name}.onnx"
    config_path = VOICES_DIR / f"{voice_name}.onnx.json"
    if not model_path.is_file() or not config_path.is_file():
        raise ModelUnavailable(f"TTS voice missing: {voice_name}. Run python -m scripts.download_tts_voices.")
    try:
        from piper import PiperVoice
    except ImportError as exc:
        raise ModelUnavailable("Piper TTS is not installed. Install requirements-tts.txt.") from exc
    return PiperVoice.load(model_path, config_path=config_path)


def text_to_speech(text: str, language: str) -> Path:
    if not text.strip():
        raise ValueError("No text to speak.")
    voice = _voice(language)
    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    output = AUDIO_DIR / f"{uuid4().hex}.wav"
    try:
        with wave.open(str(output), "wb") as audio:
            voice.synthesize_wav(text, audio)
        with wave.open(str(output), "rb") as audio:
            if audio.getnframes() == 0:
                raise RuntimeError("Piper produced no audio.")
    except Exception:
        output.unlink(missing_ok=True)
        raise
    return output
