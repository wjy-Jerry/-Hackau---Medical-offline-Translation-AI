"""Translation adapter: translate_and_extract(text, source, target)."""
import logging
import re
import time

from backend.config import ASR_LANGUAGES
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

    # The published ru↔en packages bundle tokenizers but their Stanza
    # resources files lack the `packages` key required by our Stanza version.
    # Emergency utterances are short, so split those inputs on punctuation
    # locally; keep the packaged Stanza path for the EN/ZH packages.
    original_split = sbd.StanzaSentencizer.split_sentences

    def local_split(self, text):
        if "ru" in (self.pkg.from_code, self.pkg.to_code):
            return [part for part in re.split(r"(?<=[.!?])\s+", text.strip()) if part]
        return original_split(self, text)

    sbd.StanzaSentencizer.split_sentences = local_split


def translate_and_extract(text: str, source_language: str, target_language: str) -> TranslationResult:
    if not text.strip():
        raise ValueError("No text to translate.")
    if source_language not in ASR_LANGUAGES or target_language not in ASR_LANGUAGES or source_language == target_language:
        raise ValueError("Unsupported translation pair. Select two different available languages.")
    try:
        from argostranslate import package, translate
    except ImportError as exc:
        raise ModelUnavailable("Argos Translate is not installed. Install requirements-translation.txt.") from exc
    installed = package.get_installed_packages()
    route = ([(source_language, "en"), ("en", target_language)]
             if {source_language, target_language} == {"ru", "zh"}
             else [(source_language, target_language)])
    available = {(p.from_code, p.to_code) for p in installed}
    if not all(pair in available for pair in route):
        raise ModelUnavailable(
            f"Translation model for {source_language}→{target_language} is missing. Run python -m scripts.download_translation_models."
        )
    try:
        _keep_stanza_offline()
        started = time.perf_counter()
        translated_text = text
        for source, target in route:
            translated_text = translate.translate(translated_text, source, target)
        logger.info("Translation inference_ms=%.1f", (time.perf_counter() - started) * 1000)
    except Exception as exc:
        raise RuntimeError(f"Translation failed: {exc}") from exc
    if not translated_text.strip():
        raise RuntimeError("Translation returned empty text.")
    return TranslationResult(translation=translated_text, key_information=extract_key_information(text))
