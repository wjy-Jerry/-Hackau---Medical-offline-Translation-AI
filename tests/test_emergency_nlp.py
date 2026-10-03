"""The extractor highlights statements; it must not infer or diagnose."""
import pytest

from backend.models.emergency_nlp import extract_key_information


@pytest.mark.parametrize(
    ("text", "expected"),
    [
        ("I am allergic to penicillin and my chest hurts.", {"allergy": "Penicillin", "symptom": "Chest pain"}),
        ("I am allergic to penicillin because I had a reaction.", {"allergy": "Penicillin"}),
        ("I am taking medication for asthma.", {"medication": "Medication for asthma"}),
        ("I take metformin.", {"medication": "Metformin"}),
        ("I have pain in my left arm.", {"symptom": "Left arm pain"}),
        ("I cannot breathe normally.", {"breathing_difficulty": "Difficulty breathing"}),
        ("I have trouble breathing.", {"breathing_difficulty": "Difficulty breathing"}),
        ("I am bleeding.", {"bleeding": "Bleeding"}),
        ("I fainted.", {"loss_of_consciousness": "Reported fainting or loss of consciousness"}),
        ("我对青霉素过敏，而且胸口疼。", {"allergy": "青霉素", "symptom": "Chest pain"}),
        ("我胸口有疼而且我无法正常呼吸", {"symptom": "Chest pain", "breathing_difficulty": "Difficulty breathing"}),
        ("我胸口偶疼而且我无法正常呼吸", {"symptom": "Chest pain", "breathing_difficulty": "Difficulty breathing"}),
        ("我正在服用治疗哮喘的药物。", {"medication": "治疗哮喘的药物"}),
        ("我不能呼吸。", {"breathing_difficulty": "Difficulty breathing"}),
        ("我无法正常呼吸。", {"breathing_difficulty": "Difficulty breathing"}),
        ("我在流血。", {"bleeding": "Bleeding"}),
        ("我晕倒了。", {"loss_of_consciousness": "Reported fainting or loss of consciousness"}),
        ("I feel dizzy.", {"other_symptom": "Dizziness"}),
        ("I am nauseous.", {"other_symptom": "Nausea"}),
        ("I have a fever.", {"other_symptom": "Fever"}),
        ("我头晕。", {"other_symptom": "Dizziness"}),
        ("我觉得恶心。", {"other_symptom": "Nausea"}),
        ("我发烧。", {"other_symptom": "Fever"}),
    ],
)
def test_explicit_patient_statements(text, expected):
    assert extract_key_information(text) == expected


@pytest.mark.parametrize(
    "text",
    [
        "Please help me.",
        "I have asthma.",
        "Penicillin and metformin.",
        "I am not allergic to penicillin.",
        "I have no chest pain.",
        "I can breathe normally.",
        "I have no difficulty breathing.",
        "I am not bleeding.",
        "I did not faint.",
        "I almost fainted.",
        "I used to take metformin.",
        "I think I am allergic to penicillin.",
        "Are you allergic to penicillin?",
        "My mother has chest pain.",
        "If I faint, call for help.",
        "我没有胸痛。",
        "我没有出血。",
        "我没有呼吸困难。",
        "我差点晕倒了。",
        "你对青霉素过敏吗？",
        "如果我晕倒了怎么办？",
        "I am not dizzy.",
        "I don't feel dizzy.",
        "My mother is nauseous.",
        "I might have a fever.",
        "我不头晕。",
        "我没发烧。",
    ],
)
def test_unstated_negated_or_other_person_information_is_empty(text):
    assert extract_key_information(text) == {}


def test_negated_item_does_not_hide_a_separate_positive_statement():
    assert extract_key_information("I am not allergic to penicillin, but my chest hurts.") == {
        "symptom": "Chest pain"
    }


def test_multiple_explicit_symptoms_are_kept_without_inference():
    assert extract_key_information("My chest hurts and I feel dizzy. I am bleeding.") == {
        "symptom": "Chest pain", "other_symptom": "Dizziness", "bleeding": "Bleeding"
    }
