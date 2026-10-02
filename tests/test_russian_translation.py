"""Offline translation regression cases; Russian wording awaits native review."""
import re
import subprocess
import sys

import pytest

from backend.models.translation import translate_and_extract


@pytest.mark.parametrize(
    ("source", "expected"),
    [
        ("У меня болит грудь.", "chest"),
        ("Я не могу нормально дышать.", "breathe"),
        ("У меня аллергия на пенициллин.", "penicillin"),
        ("У меня кровотечение.", "bleeding"),
        ("Я принимаю лекарства.", "medication"),
        ("У меня кружится голова.", "dizzy"),
    ],
)
def test_russian_to_english_is_distinct_and_relevant(source, expected):
    result = translate_and_extract(source, "ru", "en")
    assert expected in result.translation.lower()


def test_english_to_russian_distinct_outputs():
    sources = ["My chest hurts.", "I am allergic to penicillin.", "I cannot breathe normally."]
    outputs = [translate_and_extract(text, "en", "ru").translation for text in sources]
    assert len(set(outputs)) == len(sources)
    assert all(re.search(r"[А-Яа-яЁё]", text) for text in outputs)


def test_russian_chinese_pivot_both_directions():
    zh = translate_and_extract("У меня аллергия на пенициллин.", "ru", "zh").translation
    ru = translate_and_extract("我对青霉素过敏。", "zh", "ru").translation
    assert "青霉素" in zh
    assert "пенициллин" in ru.lower()


def test_russian_translation_cold_start_has_no_network():
    script = """
import socket
from backend.models.translation import translate_and_extract
def deny(*args, **kwargs):
    raise AssertionError('Translation attempted a network connection')
socket.create_connection = deny
socket.socket.connect = deny
socket.socket.connect_ex = deny
assert 'chest' in translate_and_extract('У меня болит грудь.', 'ru', 'en').translation.lower()
assert 'грудь' in translate_and_extract('My chest hurts.', 'en', 'ru').translation.lower()
"""
    result = subprocess.run([sys.executable, "-c", script], capture_output=True, text=True)
    assert result.returncode == 0, result.stderr
