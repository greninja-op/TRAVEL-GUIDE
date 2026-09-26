"""Validate a POI pack. Mirrors shared-core/PackValidator.kt. Stdlib only."""
import json
import sys


def validate(pack):
    errors = []
    pois = pack.get("pois", [])
    if not pois:
        errors.append("pack has no pois")
    ids = [p.get("id", "") for p in pois]
    if len(ids) != len(set(ids)):
        errors.append("duplicate poi ids")
    for p in pois:
        pid = p.get("id", "?")
        lat, lng = p.get("lat"), p.get("lng")
        if not isinstance(lat, (int, float)) or not (-90 <= lat <= 90):
            errors.append(f"{pid}: bad lat")
        if not isinstance(lng, (int, float)) or not (-180 <= lng <= 180):
            errors.append(f"{pid}: bad lng")
        r = p.get("radius_m")
        if not isinstance(r, (int, float)) or not (10 <= r <= 500):
            errors.append(f"{pid}: radius_m out of 10..500")
        if not p.get("sources"):
            errors.append(f"{pid}: missing sources (SPEC 1.5)")
        if len(p.get("summary", "")) > 140:
            errors.append(f"{pid}: summary > 140 chars")
        if p.get("layer", "heritage") not in ("heritage", "food", "stay"):
            errors.append(f"{pid}: bad layer")
        for key in ("name", "history", "fun_facts", "see_list", "category"):
            if key not in p:
                errors.append(f"{pid}: missing {key}")
    known = set(ids)
    for r in pack.get("routes", []):
        missing = [i for i in r.get("ordered_poi_ids", []) if i not in known]
        if missing:
            errors.append(f"route {r.get('id')}: unknown pois {missing}")
    return errors


def main(path):
    with open(path, encoding="utf-8") as f:
        pack = json.load(f)
    errors = validate(pack)
    print(f"pois={len(pack.get('pois', []))} routes={len(pack.get('routes', []))} "
          f"version={pack.get('pack_version')}")
    if errors:
        print("INVALID:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print("VALID")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
