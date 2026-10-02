"""One-time setup of only missing local model assets.

Runtime adapters never invoke this module or download models.
"""
from scripts.download_asr_model import main as download_asr
from scripts.download_translation_models import main as download_translation
from scripts.download_tts_voices import main as download_voices


def main():
    download_asr()
    download_translation()
    download_voices()
    print("All available English, Chinese, and Russian local models are ready.")


if __name__ == "__main__":
    main()
