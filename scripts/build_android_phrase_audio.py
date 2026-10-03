"""Generate the Android pack's local audio from the installed Piper voices."""

import shutil
import wave
from pathlib import Path

from backend.config import ROOT
from backend.models.tts import text_to_speech
from backend.phrase_packs import question_rows


OUTPUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "audio"


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for question in question_rows():
        for language in ("en", "zh", "ru"):
            destination = OUTPUT / f"{question['id']}_{language}.wav"
            if destination.is_file():
                with wave.open(str(destination), "rb") as existing:
                    if existing.getnframes() > 0:
                        print(f"present {destination.name}")
                        continue
            generated = text_to_speech(question[language], language)
            shutil.copyfile(generated, destination)
            generated.unlink()
            with wave.open(str(destination), "rb") as audio:
                if audio.getnframes() == 0:
                    raise RuntimeError(f"Empty generated audio: {destination}")
            print(f"created {destination.name}")


if __name__ == "__main__":
    main()
