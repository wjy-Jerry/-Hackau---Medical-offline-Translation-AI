"""ASR adapter: speech_to_text(audio_path, language=None) -> ASRResult."""
import logging
import time
from functools import lru_cache
from pathlib import Path

from backend.config import ASR_DIR, LANGUAGES
from backend.schemas import ASRResult

logger = logging.getLogger("fieldtalk.asr")


class ModelUnavailable(RuntimeError):
    pass


@lru_cache(maxsize=1)
def _model():
    if not (ASR_DIR / "model.bin").is_file():
        raise ModelUnavailable(f"ASR model missing at {ASR_DIR}. Run python -m scripts.download_asr_model.")
    try:
        from faster_whisper import WhisperModel
    except ImportError as exc:
        raise ModelUnavailable("faster-whisper is not installed. Install requirements-asr.txt.") from exc
    started = time.perf_counter()
    model = WhisperModel(str(ASR_DIR), device="cpu", compute_type="int8", local_files_only=True)
    logger.info("ASR model load_ms=%.1f", (time.perf_counter() - started) * 1000)
    return model


def speech_to_text(audio_path: str | Path, language: str | None = None) -> ASRResult:
    path = Path(audio_path)
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError("No audio recorded. Please record again.")
    if language is not None and language not in LANGUAGES:
        raise ValueError(f"Unsupported ASR language: {language}.")
    try:
        model = _model()
        started = time.perf_counter()
        segments, info = model.transcribe(str(path), language=language, beam_size=5, vad_filter=False)
        text = " ".join(segment.text.strip() for segment in segments).strip()
        logger.info("ASR inference_ms=%.1f", (time.perf_counter() - started) * 1000)
    except ModelUnavailable:
        raise
    except Exception as exc:
        raise RuntimeError(f"Speech recognition failed: {exc}") from exc
    if not text:
        raise ValueError("No speech recognized. Please record again.")
    # Whisper does not expose a calibrated utterance confidence score.
    return ASRResult(text=text, language=language or info.language, confidence=None)
