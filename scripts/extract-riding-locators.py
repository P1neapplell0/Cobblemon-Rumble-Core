#!/usr/bin/env python3
"""Extract seat locator transforms, without copying old geometry or changing riding eligibility."""
import argparse
import json
from pathlib import Path


def transform(bone):
    return {key: bone[key] for key in ("parent", "pivot", "rotation") if key in bone}


def extract(source):
    result = {}
    for path in sorted(source.rglob("*.geo.json")):
        data = json.loads(path.read_text())
        geometries = data.get("minecraft:geometry", [])
        if len(geometries) != 1:
            continue
        bones = {bone["name"]: bone for bone in geometries[0]["bones"]}
        seats = []
        for bone in bones.values():
            locators = {("seat_1" if k == "seat" else k): v
                        for k, v in bone.get("locators", {}).items() if k == "seat" or k.startswith("seat_")}
            if not locators:
                continue
            # Keep the full ancestor transform chain as a guard against a newer, different rig.
            ancestors = {}
            parent = bone.get("parent")
            while parent:
                if parent in ancestors or parent not in bones:
                    raise ValueError(f"Invalid parent chain: {path}: {parent}")
                ancestors[parent] = transform(bones[parent])
                parent = bones[parent].get("parent")
            seats.append({"bone": {"name": bone["name"], **transform(bone), "locators": locators},
                          "ancestors": ancestors})
        if seats:
            relative = path.relative_to(source)
            _, namespace, *parts = relative.parts
            result[f"{namespace}:{'/'.join(parts)}"] = seats
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Old core's packs/ride_fix_resource directory")
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    result = extract(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # One model per line keeps generated coordinates reviewable without repeating indentation.
    lines = ["  " + json.dumps(key, ensure_ascii=False) + ": "
             + json.dumps(value, ensure_ascii=False, separators=(",", ":")) for key, value in result.items()]
    args.output.write_text("{\n" + ",\n".join(lines) + "\n}\n")
    print(f"Extracted seat metadata for {len(result)} models ({args.output.stat().st_size} bytes)")
