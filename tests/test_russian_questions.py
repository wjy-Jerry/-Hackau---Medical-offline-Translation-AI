"""Russian phrase and native-review data integrity checks."""
import json

from backend.config import ROOT


def test_russian_questions_are_present_and_marked_unreviewed():
    questions = json.loads((ROOT / "data" / "emergency_questions.json").read_text(encoding="utf-8"))
    assert len(questions) == 7
    assert len({question["id"] for question in questions}) == 7
    assert {"pain", "breathing", "allergy", "medication", "consciousness", "bleeding", "chest_pain"} == {
        question["id"] for question in questions
    }
    for question in questions:
        assert all(question[language].strip() for language in ("en", "zh", "ru"))
        assert question["ru_review_status"] == "pending_native_speaker_validation"


def test_russian_review_set_contains_actual_outputs_but_no_fake_human_scores():
    review = json.loads((ROOT / "data" / "russian_review_set.json").read_text(encoding="utf-8"))
    assert review["review_status"] == "Pending native-speaker validation"
    assert len(review["review_options"]) == 4
    assert len(review["cases"]) == 6
    for case in review["cases"]:
        assert all(case[key].strip() for key in (
            "original_russian", "asr_output", "english_translation", "chinese_translation", "russian_tts_text",
        ))
        assert case["human_review_category"] is None
        assert not case["human_review_notes"]
