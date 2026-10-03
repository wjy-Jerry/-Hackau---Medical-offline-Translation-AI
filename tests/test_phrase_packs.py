"""The local phrase packs must stay complete and preserve the existing questions."""

import json

from backend.config import ROOT
from backend.main import QUESTIONS
from backend.phrase_packs import load_phrase_pack, question_rows


def test_ambulance_pack_is_complete_and_matches_existing_quick_questions():
    pack = load_phrase_pack()
    rows = question_rows()
    legacy = json.loads((ROOT / "data" / "emergency_questions.json").read_text(encoding="utf-8"))
    assert rows == legacy
    assert list(QUESTIONS) == pack["manifest"]["question_order"]
    assert set(pack["languages"]) == {"en", "zh", "ru"}
    assert len(rows) == 7


def test_review_labels_do_not_claim_unperformed_human_validation():
    languages = load_phrase_pack()["languages"]
    assert languages["en"]["review_status"] == "source_text_pending_team_review"
    assert languages["zh"]["review_status"] == "pending_team_review"
    assert languages["ru"]["review_status"] == "pending_native_speaker_validation"
    assert languages["ru"]["review_label"] == "Pending native-speaker validation"
