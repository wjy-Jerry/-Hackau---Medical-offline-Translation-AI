"""Translation adapter: translate_and_extract(text, source, target)."""
import logging
import time

from backend.config import LANGUAGES
from backend.models.asr import ModelUnavailable
from backend.models.emergency_nlp import extract_key_information
from backend.schemas import TranslationResult

logger = logging.getLogger("fieldtalk.translation")


def _keep_stanza_offline():
    """Argos 1.11 otherwise checks for Stanza resource updates on first use."""
    from argostranslate import sbd, settings

    if getattr(sbd.StanzaSentencizer.lazy_pipeline, "_fieldtalk_offline", False):
        return

    def local_pipeline(self):
        if self.stanza_pipeline is None:
            self.stanza_pipeline = sbd.stanza.Pipeline(
                lang=self.stanza_lang_code,
                dir=str(self.pkg.package_path / "stanza"),
                processors="tokenize",
                use_gpu=settings.device == "cuda",
                logging_level="WARNING",
                download_method=None,
            )
        return self.stanza_pipeline

    local_pipeline._fieldtalk_offline = True
    sbd.StanzaSentencizer.lazy_pipeline = local_pipeline


def translate_and_extract(text: str, source_language: str, target_language: str) -> TranslationResult:
    if not text.strip():
        raise ValueError("No text to translate.")
    if source_language not in LANGUAGES or target_language not in LANGUAGES or source_language == target_language:
        raise ValueError("Unsupported translation pair. Select English and Chinese in opposite directions.")
    try:
        from argostranslate import package, translate
    except ImportError as exc:
        raise ModelUnavailable("Argos Translate is not installed. Install requirements-translation.txt.") from exc
    installed = package.get_installed_packages()
    if not any(p.from_code == source_language and p.to_code == target_language for p in installed):
        raise ModelUnavailable(
            f"Translation model {source_language}→{target_language} is missing. Run python -m scripts.download_translation_models."
        )
    try:
        _keep_stanza_offline()
        started = time.perf_counter()
        translated_text = translate.translate(text, source_language, target_language)
        logger.info("Translation inference_ms=%.1f", (time.perf_counter() - started) * 1000)
    except Exception as exc:
        raise RuntimeError(f"Translation failed: {exc}") from exc
    if not translated_text.strip():
        raise RuntimeError("Translation returned empty text.")
    return TranslationResult(translation=translated_text, key_information=extract_key_information(text))
