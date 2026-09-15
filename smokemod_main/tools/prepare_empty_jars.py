"""Export empty variants of the native Blockbench models, preserving glass UVs.

The original shell atlases already contain the glass and wooden lids. Buds are
separate elements with separate textures; remove those elements and references.
Run from any directory with Python 3. No image processing is needed.
"""
import json
import shutil
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/smokemod"


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


for name, texture in [("jar", "jar_pixel"), ("jar_large", "jar_large")]:
    empty_name = name + "_empty"
    model_dir = ASSETS / "models/block"
    empty = read(model_dir / (name + ".json"))
    empty["elements"] = [e for e in empty["elements"]
                         if all(f.get("texture") == "#0" for f in e["faces"].values())]
    empty["textures"] = {"0": "smokemod:block/" + empty_name,
                         "particle": "smokemod:block/" + empty_name}
    empty.pop("groups", None)
    write(model_dir / (empty_name + ".json"), empty)
    shutil.copyfile(ASSETS / "textures/block" / (texture + ".png"),
                    ASSETS / "textures/block" / (empty_name + ".png"))

    native = read(model_dir / (name + ".bbmodel"))
    native["name"] = empty_name
    native["elements"] = [e for e in native["elements"]
                          if all(f.get("texture") in (None, 0) for f in e["faces"].values())]
    native["groups"] = []
    native["outliner"] = [e["uuid"] for e in native["elements"]]
    native["textures"] = native["textures"][:1]
    native["textures"][0]["name"] = empty_name + ".png"
    native["textures"][0]["path"] = str(ASSETS / "textures/block" / (empty_name + ".png"))
    native["textures"][0]["relative_path"] = "../../textures/block/" + empty_name + ".png"
    write(model_dir / (empty_name + ".bbmodel"), native)

    write(ASSETS / "blockstates" / (name + ".json"), {
        "variants": {
            "filled=false": {"model": "smokemod:block/" + empty_name},
            "filled=true": {"model": "smokemod:block/" + name},
        }
    })
    write(ASSETS / "models/item" / (name + ".json"), {"parent": "smokemod:block/" + empty_name})
    print(f"{empty_name}: {len(empty['elements'])} shell elements; no bud textures or geometry")
