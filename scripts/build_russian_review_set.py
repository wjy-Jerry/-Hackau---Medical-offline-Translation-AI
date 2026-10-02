"""Generate synthetic speech and machine outputs for later native-speaker review.

The human review fields remain empty. Piper-generated speech is a technical test,
not evidence of recognition quality on native-speaker or patient recordings.
"""
import json
import time
from datetime import datetime, timezone
from pathlib import Path

from backend.config import ROOT
from backend.models.asr import speech_to_text
from backend.models.translation import translate_and_extract
from backend.models.tts import text_to_speech


CASES = [
    ("chest_pain", "У меня болит грудь."),
    ("breathing", "Я не могу нормально дышать."),
    ("allergy", "У меня аллергия на пенициллин."),
    ("bleeding", "У меня кровотечение."),
    ("medication", "Я принимаю лекарства."),
    ("dizziness", "У меня кружится голова."),
]
REVIEW_OPTIONS = [
    "Correct and natural",
    "Understandable but awkward",
    "Incorrect",
    "Potentially dangerous meaning change",
]


def main():
    rows = []
    for case_id, original in CASES:
        audio_path = text_to_speech(original, "ru")
        try:
            started = time.perf_counter()
            recognized = speech_to_text(audio_path, "ru")
            asr_ms = round((time.perf_counter() - started) * 1000, 1)
        finally:
            audio_path.unlink(missing_ok=True)
        started = time.perf_counter()
        english = translate_and_extract(recognized.text, "ru", "en").translation
        english_ms = round((time.perf_counter() - started) * 1000, 1)
        started = time.perf_counter()
        chinese = translate_and_extract(recognized.text, "ru", "zh").translation
        chinese_ms = round((time.perf_counter() - started) * 1000, 1)
        rows.append({
            "id": case_id,
            "original_russian": original,
            "asr_output": recognized.text,
            "english_translation": english,
            "chinese_translation": chinese,
            "russian_tts_text": original,
            "asr_ms": asr_ms,
            "english_translation_ms": english_ms,
            "chinese_translation_ms": chinese_ms,
            "human_review_category": None,
            "human_review_notes": "",
            "notes": "Synthetic Piper speech; pending native-speaker validation.",
        })
    document = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "review_status": "Pending native-speaker validation",
        "test_audio_source": "Local Piper ru_RU-irina-medium synthetic voice",
        "review_options": REVIEW_OPTIONS,
        "cases": rows,
    }
    output = Path(ROOT) / "data" / "russian_review_set.json"
    output.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(rows)} unreviewed Russian cases to {output}")


if __name__ == "__main__":
    main()
