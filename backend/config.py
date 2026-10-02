"""Configuration shared by all three model adapters."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = Path(os.getenv("FIELDTALK_MODEL_DIR", ROOT / "models_local"))
AUDIO_DIR = Path(os.getenv("FIELDTALK_AUDIO_DIR", ROOT / "generated_audio"))
MODE = "local"
ASR_DIR = Path(os.getenv("FIELDTALK_ASR_DIR", MODEL_DIR / "whisper-base"))
TRANSLATION_DIR = Path(os.getenv("ARGOS_PACKAGES_DIR", MODEL_DIR / "argos"))
os.environ.setdefault("ARGOS_PACKAGES_DIR", str(TRANSLATION_DIR))
VOICES_DIR = Path(os.getenv("FIELDTALK_VOICES_DIR", MODEL_DIR / "voices"))
VOICES = {"en": "en_US-lessac-medium", "zh": "zh_CN-huayan-medium", "ru": "ru_RU-irina-medium"}
LANGUAGES = {"en", "zh"}
ASR_LANGUAGES = LANGUAGES | {"ru"}
