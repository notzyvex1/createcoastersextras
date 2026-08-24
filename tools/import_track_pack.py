"""Turns a texture resource pack into a real track variant.

The rose quartz and brass packs are resource packs: they override the BASE mod's
`simulatedcoasters:` track textures, so installing one repaints every track in the world and you
can only ever have one at a time. A variant is the opposite -- it is its own material with its own
id, so rose quartz and brass can sit side by side on the same coaster.

The textures are used as they are, not recoloured. Elsewhere in this project a variant is baked by
tinting a greyscale source, because there is no artwork for 172 blocks; here somebody has actually
drawn these, so the only work is putting them where a variant expects to find them.

    py tools/import_track_pack.py "C:/Users/Zohan/Downloads/rose quartz.zip" rose_quartz "Rose Quartz"
    py tools/import_track_pack.py "C:/Users/Zohan/Downloads/brass.zip"       brass        Brass

Prints the enum constant to paste into TrackVariant.
"""
import io
import json
import os
import shutil
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "coasters_extras")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "coasters_extras")
TEX = os.path.join(ASSETS, "textures", "block", "track")
MODELS = os.path.join(ASSETS, "models", "block", "track")

# The variant every new one is cloned from: its models are already repathed correctly and its
# texture folder lists exactly what a variant needs.
SOURCE = "stone_track"

NEEDED = [
    "standard_track.png", "standard_track_dyed.png", "standard_track_mip.png",
    "middle_beam.png", "middle_beam_dyed.png",
    "track_anchorpoint_connection.png", "track_anchorpoint_connection_side.png",
    "track_anchorpoint_girder.png", "track_anchorpoint_girder_chainlift.png",
]


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)
    pack, name, display = sys.argv[1], sys.argv[2], sys.argv[3]
    folder = name + "_track"

    z = zipfile.ZipFile(pack)
    inside = {os.path.basename(n): n for n in z.namelist()
              if "/block/coaster_track/" in n and n.endswith(".png")}
    item_src = next((n for n in z.namelist()
                     if n.endswith("textures/item/coaster_track.png")), None)

    out_tex = os.path.join(TEX, folder)
    os.makedirs(out_tex, exist_ok=True)

    used, borrowed = [], []
    for want in NEEDED:
        if want in inside:
            with open(os.path.join(out_tex, want), "wb") as f:
                f.write(z.read(inside[want]))
            used.append(want)
        else:
            # Fall back to the source variant rather than skipping. A missing texture is not a
            # gap in the model -- it is a missing-texture checkerboard on part of every rail.
            fallback = os.path.join(TEX, SOURCE, want)
            if want == "standard_track_mip.png" and "standard_track.png" in inside:
                # The mip is just the base at a smaller size; the pack's own base is a far better
                # source than an unrelated variant's.
                with open(os.path.join(out_tex, want), "wb") as f:
                    f.write(z.read(inside["standard_track.png"]))
                borrowed.append(want + " (from its own standard_track)")
            elif os.path.exists(fallback):
                shutil.copy2(fallback, os.path.join(out_tex, want))
                borrowed.append(want + " (from " + SOURCE + ")")
            else:
                borrowed.append(want + " MISSING")

    # Models: copy the source's, rewriting every reference to point at this variant.
    out_models = os.path.join(MODELS, folder)
    os.makedirs(out_models, exist_ok=True)
    for f in os.listdir(os.path.join(MODELS, SOURCE)):
        s = os.path.join(MODELS, SOURCE, f)
        d = os.path.join(out_models, f)
        if f.endswith((".json", ".mtl")):
            io.open(d, "w", encoding="utf8", newline="\n").write(
                io.open(s, encoding="utf8").read().replace(SOURCE, folder))
        else:
            shutil.copy2(s, d)

    # Item icon straight from the pack.
    if item_src:
        with open(os.path.join(ASSETS, "textures", "item", folder + ".png"), "wb") as f:
            f.write(z.read(item_src))

    for path, body in (
        (os.path.join(ASSETS, "models", "item", folder + ".json"),
         {"parent": "minecraft:item/generated",
          "textures": {"layer0": f"coasters_extras:item/{folder}"}}),
        (os.path.join(ASSETS, "blockstates", folder + "_material.json"),
         {"variants": {"": {"model": f"coasters_extras:block/{folder}_material"}}}),
        (os.path.join(ASSETS, "models", "block", folder + "_material.json"),
         {"parent": f"coasters_extras:block/track/{folder}/segment_left"}),
        (os.path.join(DATA, "loot_table", "blocks", folder + "_material.json"),
         {"type": "minecraft:block",
          "pools": [{"rolls": 1,
                     "entries": [{"type": "minecraft:item",
                                  "name": f"coasters_extras:{folder}"}],
                     "conditions": [{"condition": "minecraft:survives_explosion"}]}]}),
    ):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with io.open(path, "w", encoding="utf8", newline="\n") as f:
            json.dump(body, f, indent=2)
            f.write("\n")

    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(io.open(lang_path, encoding="utf8"))
    lang[f"block.coasters_extras.{folder}_material"] = display + " Track"
    lang[f"item.coasters_extras.{folder}"] = display + " Track"
    io.open(lang_path, "w", encoding="utf8", newline="\n").write(
        json.dumps(lang, indent=2, ensure_ascii=False) + "\n")

    print(f"{display} Track -> {folder}")
    print(f"  {len(used)} textures from the pack")
    for b in borrowed:
        print(f"  filled in: {b}")
    print(f"\n  paste into TrackVariant:")
    print(f'    {name.upper()}("{name}", "{display}"),')


if __name__ == "__main__":
    main()
