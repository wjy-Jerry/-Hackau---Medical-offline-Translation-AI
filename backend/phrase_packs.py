"""Load local question packs without changing the quick-question API shape."""

import json
from backend.config import ROOT


PACKS_DIR = ROOT / "data" / "phrase_packs"


def load_phrase_pack(name: str = "ambulance") -> dict:
    pack_dir = PACKS_DIR / name
    manifest = json.loads((pack_dir / "manifest.json").read_text(encoding="utf-8"))
    languages = {}
    ordered_ids = manifest["question_order"]
    if not ordered_ids or len(ordered_ids) != len(set(ordered_ids)):
        raise ValueError(f"Invalid question order in phrase pack {name}")
    for code in manifest["languages"]:
        language_pack = json.loads((pack_dir / f"{code}.json").read_text(encoding="utf-8"))
        if language_pack["language"] != code or set(language_pack["questions"]) != set(ordered_ids):
            raise ValueError(f"Incomplete {code} phrases in pack {name}")
        if not all(isinstance(value, str) and value.strip() for value in language_pack["questions"].values()):
            raise ValueError(f"Blank {code} phrase in pack {name}")
        languages[code] = language_pack
    return {"manifest": manifest, "languages": languages}


def question_rows(name: str = "ambulance") -> list[dict]:
    pack = load_phrase_pack(name)
    rows = []
    for question_id in pack["manifest"]["question_order"]:
        row = {"id": question_id}
        for code, language_pack in pack["languages"].items():
            row[code] = language_pack["questions"][question_id]
        row["ru_review_status"] = pack["languages"]["ru"]["review_status"]
        rows.append(row)
    return rows
