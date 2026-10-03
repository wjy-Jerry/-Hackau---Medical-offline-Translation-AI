"""Conservative, local highlighting of details explicitly stated by a patient.

These patterns do not diagnose or infer a condition. Unmatched or uncertain text
returns no structured information; the original transcript remains available.
"""
import re
import unicodedata


_SENTENCES = re.compile(r"[^.!?。！？;,，]+[.!?。！？;,，]?")
_CONJUNCTIONS = re.compile(r"\b(?:and|but)\b|而且|但是|并且|還有|还有", re.IGNORECASE)
_OTHER_PERSON_OR_UNCERTAIN = re.compile(
    r"\b(?:he|she|they|you|my (?:mother|father|friend|son|daughter|child|patient)|"
    r"if|maybe|perhaps|might|possibly|hypothetically|think|suspect|almost|nearly|"
    r"used to|stopped|no longer)\b|"
    r"(?:如果|假如|可能|也许|也許|或许|或許|差点|差點|快要|你|他|她|他们|他們|她们|她們)",
    re.IGNORECASE,
)
_NEGATIVE_EN = re.compile(
    r"\b(?:no|not|never|without|deny|denies|don't|doesn't|didn't|isn't|aren't|"
    r"haven't|do not|does not|did not|have no)\b",
    re.IGNORECASE,
)
_NEGATIVE_ZH = re.compile(r"(?:没有|沒有|没|沒|不|无|無|否认|否認)[\u4e00-\u9fff]{0,2}$")

_ALLERGY_EN = re.compile(r"\ballergic to\s+(?P<item>[a-z][a-z -]{0,35})", re.IGNORECASE)
_ALLERGY_ZH = re.compile(r"[对對](?P<item>[\u4e00-\u9fff]{1,12})[过過]敏")
_MEDICATION_EN = re.compile(
    r"\b(?:i\s+(?:am\s+)?|i'm\s+)?(?:taking|take|using|use)\s+"
    r"(?P<item>(?:medication|medicine|pills?)(?:\s+for\s+[a-z -]{1,25})?|"
    r"metformin|insulin|aspirin|warfarin|albuterol|penicillin)\b",
    re.IGNORECASE,
)
_MEDICATION_ZH = re.compile(r"我(?:正在|在)?(?:服用|吃|用)(?P<item>[\u4e00-\u9fff]{1,14})")

_LOCATIONS_EN = {
    "chest": "Chest", "head": "Head", "stomach": "Stomach", "abdomen": "Abdominal",
    "back": "Back", "neck": "Neck", "left arm": "Left arm", "right arm": "Right arm",
    "arm": "Arm", "left leg": "Left leg", "right leg": "Right leg", "leg": "Leg",
}
_LOCATION_EN = r"left arm|right arm|left leg|right leg|chest|head|stomach|abdomen|back|neck|arm|leg"
_PAIN_EN = (
    re.compile(rf"\b(?:my\s+)?(?P<location>{_LOCATION_EN})\s+(?:hurts|aches|is painful|is hurting|pain)\b", re.IGNORECASE),
    re.compile(rf"\b(?:pain|ache)\s+(?:in|on)\s+(?:my\s+|the\s+)?(?P<location>{_LOCATION_EN})\b", re.IGNORECASE),
)
_LOCATIONS_ZH = {
    "胸口": "Chest", "胸部": "Chest", "胸": "Chest", "头": "Head", "頭": "Head",
    "肚子": "Stomach", "腹部": "Abdominal", "背部": "Back", "背": "Back",
    "颈部": "Neck", "頸部": "Neck", "左手臂": "Left arm", "右手臂": "Right arm",
    "左臂": "Left arm", "右臂": "Right arm", "手臂": "Arm", "左腿": "Left leg",
    "右腿": "Right leg", "腿": "Leg",
}
_LOCATION_ZH = "|".join(sorted(_LOCATIONS_ZH, key=len, reverse=True))
_PAIN_ZH = re.compile(rf"(?P<location>{_LOCATION_ZH})(?:很|非常|有点|有點|有|偶)?(?:疼|痛)")

_BREATHING_EN = re.compile(
    r"\b(?:can'?t|cannot|unable to)\s+breathe\b|"
    r"\b(?:difficulty|trouble)\s+breathing\b|\bshort(?:ness)? of breath\b",
    re.IGNORECASE,
)
_BREATHING_ZH = re.compile(r"(?:不能|无法|無法|难以|難以)(?:正常)?呼吸|呼吸困难|呼吸困難|喘不过气|喘不過氣")
_BLEEDING_EN = re.compile(r"\bbleed(?:ing|s)?\b", re.IGNORECASE)
_BLEEDING_ZH = re.compile(r"流血|出血")
_FAINTING_EN = re.compile(r"\b(?:fainted|passed out|blacked out|lost consciousness)\b", re.IGNORECASE)
_FAINTING_ZH = re.compile(r"晕倒|暈倒|昏倒|失去意识|失去意識|昏过去|昏過去")
_OTHER_SYMPTOMS_EN = (
    (re.compile(r"\bi\s+(?:feel|am)\s+(?:very\s+)?dizzy\b", re.IGNORECASE), "Dizziness"),
    (re.compile(r"\bi\s+(?:feel|am)\s+(?:very\s+)?nauseous\b", re.IGNORECASE), "Nausea"),
    (re.compile(r"\bi\s+(?:have|have got)\s+(?:a\s+)?fever\b", re.IGNORECASE), "Fever"),
)
_OTHER_SYMPTOMS_ZH = (
    (re.compile(r"我(?:感到|感觉|覺得|觉得|很|有点|有點)?头晕|我(?:感到|感觉|覺得|觉得|很|有点|有點)?頭暈"), "Dizziness"),
    (re.compile(r"我(?:感到|感觉|覺得|觉得|很|有点|有點)?恶心|我(?:感到|感觉|覺得|觉得|很|有点|有點)?噁心"), "Nausea"),
    (re.compile(r"我(?:在|正)?发烧|我(?:在|正)?發燒"), "Fever"),
)


def _negated_before(clause: str, start: int) -> bool:
    before = clause[max(0, start - 35):start]
    return bool(_NEGATIVE_EN.search(before) or _NEGATIVE_ZH.search(before))


def _add(result: dict, key: str, value: str) -> None:
    if key not in result:
        result[key] = value
    elif value not in result[key].split(", "):
        result[key] += f", {value}"


def _item_from_match(match: re.Match) -> str:
    item = re.split(r"\b(?:because|since|when|after|which|that)\b", match.group("item"), maxsplit=1, flags=re.IGNORECASE)[0]
    return item.strip().strip(" -")


def extract_key_information(text: str) -> dict:
    result = {}
    normalized = unicodedata.normalize("NFKC", text).replace("’", "'")
    for sentence in _SENTENCES.findall(normalized):
        if "?" in sentence or "？" in sentence:
            continue
        for clause in _CONJUNCTIONS.split(sentence):
            clause = clause.strip(" \t\r\n.,;，。；")
            if not clause or _OTHER_PERSON_OR_UNCERTAIN.search(clause):
                continue

            match = _ALLERGY_EN.search(clause) or _ALLERGY_ZH.search(clause)
            if match and not _negated_before(clause, match.start()):
                item = _item_from_match(match)
                if item:
                    _add(result, "allergy", item.capitalize() if item.isascii() else item)

            match = _MEDICATION_EN.search(clause) or _MEDICATION_ZH.search(clause)
            if match and not _negated_before(clause, match.start()):
                item = _item_from_match(match)
                if item:
                    _add(result, "medication", item.capitalize() if item.isascii() else item)

            for pattern in _PAIN_EN + (_PAIN_ZH,):
                match = pattern.search(clause)
                if match and not _negated_before(clause, match.start()):
                    location = match.group("location")
                    name = _LOCATIONS_EN.get(location.lower()) or _LOCATIONS_ZH.get(location)
                    _add(result, "symptom", f"{name} pain")
                    break

            match = _BREATHING_EN.search(clause) or _BREATHING_ZH.search(clause)
            if match and (re.search(r"can'?t|cannot|unable to|不能|无法|無法|难以|難以|喘不", match.group(), re.IGNORECASE)
                          or not _negated_before(clause, match.start())):
                _add(result, "breathing_difficulty", "Difficulty breathing")

            match = _BLEEDING_EN.search(clause) or _BLEEDING_ZH.search(clause)
            if match and not _negated_before(clause, match.start()):
                _add(result, "bleeding", "Bleeding")

            match = _FAINTING_EN.search(clause) or _FAINTING_ZH.search(clause)
            if match and not _negated_before(clause, match.start()):
                _add(result, "loss_of_consciousness", "Reported fainting or loss of consciousness")

            for pattern, label in _OTHER_SYMPTOMS_EN + _OTHER_SYMPTOMS_ZH:
                match = pattern.search(clause)
                if match and not _negated_before(clause, match.start()):
                    _add(result, "other_symptom", label)
    return result
