"""Builds a resource pack that overrides the Coaster Controls models, for fast iteration.

Rebuilding the jar to move a handle two pixels costs about four minutes of Gradle. A resource
pack loaded above the mod overrides the same model paths, so the loop becomes: edit the JSON,
press F3+T, look. Seconds instead of minutes, and no build at all.

Drops straight into the Modrinth test profile's resourcepacks folder. Enable it once in
Options -> Resource Packs and leave it enabled; regenerating the pack does not un-enable it.

    py tools/build_devpack.py

Delete the pack (or move it below the mod) before judging how the shipped jar looks -- an
override left enabled is an easy way to spend an afternoon fixing a file the game is ignoring.
"""
import io
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "resources", "assets", "coasters_extras")
PROFILE = os.path.join(os.environ["APPDATA"], "ModrinthApp", "profiles",
                       "Create Coasters Extras")
PACK = os.path.join(PROFILE, "resourcepacks", "CoastersExtras-Dev")

# 1.21.1 resource packs are format 34. A wrong number here shows the pack as incompatible
# rather than failing loudly, so it is worth stating why.
PACK_FORMAT = 34

MODELS = [
    "coaster_controls.json",
    "coaster_controls_handle.json",
    "coaster_controls_handle_tbar.json",
    "coaster_controls_handle_wheel.json",
    "coaster_controls_handle_quadrant.json",
]


def main():
    if os.path.isdir(PACK):
        shutil.rmtree(PACK)
    dst_models = os.path.join(PACK, "assets", "coasters_extras", "models", "block")
    os.makedirs(dst_models, exist_ok=True)

    io.open(os.path.join(PACK, "pack.mcmeta"), "w", encoding="utf8", newline="\n").write(
        json.dumps({"pack": {
            "pack_format": PACK_FORMAT,
            "description": "Coasters Extras - model overrides for iteration",
        }}, indent=2) + "\n")

    copied = 0
    for name in MODELS:
        src = os.path.join(SRC, "models", "block", name)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(dst_models, name))
            copied += 1
        else:
            print(f"  (skipped {name}, not built yet)")

    # The blockstate too, so a handle can be swapped without touching Java: point the block
    # at a different model here and reload.
    bs_src = os.path.join(SRC, "blockstates", "coaster_controls.json")
    if os.path.exists(bs_src):
        bs_dst = os.path.join(PACK, "assets", "coasters_extras", "blockstates")
        os.makedirs(bs_dst, exist_ok=True)
        shutil.copy2(bs_src, os.path.join(bs_dst, "coaster_controls.json"))
        copied += 1

    print(f"copied {copied} file(s)")
    print(f"pack: {PACK}")
    print("\nEnable once in Options -> Resource Packs, then:")
    print("  edit  src/main/resources/.../models/block/coaster_controls_handle.json")
    print("  run   py tools/build_devpack.py")
    print("  press F3+T in game")


if __name__ == "__main__":
    main()
