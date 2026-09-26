"""Render a trip Markdown export from pack + visited ids. Mirrors TripExport.kt."""
import json
import sys


def main(pack_path, date, *visited):
    with open(pack_path, encoding="utf-8") as f:
        pack = json.load(f)
    pois = {p["id"]: p for p in pack["pois"]}
    lines = [f"# Trip — {pack['city_name']} ({date})", ""]
    for i, pid in enumerate(visited, 1):
        p = pois[pid]
        lines += [f"## {i}. {p['name']}", "", p["summary"], ""]
    out = "\n".join(lines)
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2], *sys.argv[3:]))
