"""Converts the community-made coastercontrols.bbmodel into the mod's split models.

The renderer needs the handle as its own model so it can be rotated independently; if the
console model still contained it, the handle would be drawn twice -- once static, once
animated -- and the two would z-fight.

The split is by texture, not by index: the arm elements are exactly the ones that reference
the custom `controlsarm.png`. That survives the author reordering cubes, which an index list
would not.

Blockbench texture ids are per-file integers that faces reference; those are rewritten to the
`#name` slots a Minecraft model uses. Two entries in this file share the name controlsarm.png,
so slots are keyed by id and only the resolved path is deduplicated.

    py tools/import_friend_controls.py
"""
import base64
import io
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(os.path.expanduser("~"), "Downloads", "coastercontrols.bbmodel")
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets",
                      "coasters_extras", "models", "block")
TEXOUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                      "coasters_extras", "textures", "block")

# Blockbench texture name -> where the model should point. Create's own textures are
# referenced rather than copied; only the author's new arm texture ships with us.
MAP = {
    "controls.png": "create:block/controls",
    "controls_frame.png": "create:block/controls_frame",
    "train_controller_base.png": "create:block/train_controller_base",
    "controlsarm.png": "coasters_extras:block/controls_arm",
}

ARM_TEX = "controlsarm.png"
LEGAL = [-45, -22.5, 0, 22.5, 45]
FACES = ["north", "south", "east", "west", "up", "down"]


def snap(a):
    return min(LEGAL, key=lambda x: abs(x - a))


def main():
    m = json.load(io.open(SRC, encoding="utf8"))

    # Blockbench faces reference a texture by its INDEX in this array, not by the `id` field.
    # Keying on `id` looked right and was wrong: in this file indices 0-4 carry ids 4, 6, 8, 3, 5,
    # so every lookup missed except index 4, which matched by coincidence. Faces pointing at
    # index 2 (create:block/controls) fell through the fallback below and were written as
    # #controls_frame -- a texture with fully transparent bands, so those faces sampled dead
    # pixels and rendered as holes. That is where the black void in the console came from.
    #
    # Both keys are recorded. Index is what faces use; id is kept only so a file that really does
    # reference by id still resolves rather than silently falling back.
    slots, textures = {}, {}
    for index, t in enumerate(m.get("textures", [])):
        name = t.get("name", "")
        path = MAP.get(name, "create:block/controls_frame")
        key = os.path.splitext(name)[0]
        slots[str(index)] = key
        slots.setdefault(str(t.get("id")), key)
        textures[key] = path
        if name == ARM_TEX and t.get("source", "").startswith("data:image"):
            os.makedirs(TEXOUT, exist_ok=True)
            raw = base64.b64decode(t["source"].split(",", 1)[1])
            io.open(os.path.join(TEXOUT, "controls_arm.png"), "wb").write(raw)

    # Same index-not-id rule as above: this decides which elements are the HANDLE rather than the
    # console, so getting it wrong splits the model in the wrong place.
    arm_ids = {str(i) for i, t in enumerate(m["textures"]) if t.get("name") == ARM_TEX}
    arm_ids |= {str(t["id"]) for t in m["textures"] if t.get("name") == ARM_TEX}

    console, handle = [], []
    for e in m["elements"]:
        faces = {}
        uses_arm = False
        for fname, f in (e.get("faces") or {}).items():
            if fname not in FACES or f.get("texture") is None:
                continue
            tid = str(f["texture"])
            if tid in arm_ids:
                uses_arm = True
            faces[fname] = {"uv": [round(v, 2) for v in f.get("uv", [0, 0, 16, 16])],
                            "texture": "#" + slots.get(tid, "controls_frame")}
            if f.get("rotation"):
                faces[fname]["rotation"] = f["rotation"]
        if not faces:
            continue

        el = {"from": [round(v, 4) for v in e["from"]],
              "to": [round(v, 4) for v in e["to"]],
              "faces": faces}
        # Blockbench stores an element's rotation as a LIST of Euler degrees [x, y, z], and keeps
        # the pivot in the element's own `origin` field rather than inside `rotation`. The dict
        # test below never matched, so every rotation in the file was thrown away in silence --
        # which is why the 45-degree lever came out lying flat across the front of the block, and
        # why the -22.5-degree dashboard came out horizontal and opened the cavity behind it.
        rot = e.get("rotation")
        if isinstance(rot, (list, tuple)) and any(abs(v) > 1e-6 for v in rot):
            if sum(1 for v in rot if abs(v) > 1e-6) > 1:
                raise SystemExit(f"element {e.get('from')} has multi-axis rotation {rot}; "
                                 "a Minecraft model element may rotate about exactly one axis")
            axis = "x" if rot[0] else ("y" if rot[1] else "z")
            el["rotation"] = {"angle": snap(rot["xyz".index(axis)]),
                              "axis": axis,
                              "origin": [round(v, 4) for v in e.get("origin", [8, 8, 8])]}
        elif isinstance(rot, dict) and rot.get("angle"):
            el["rotation"] = {"angle": snap(rot["angle"]),
                              "axis": rot.get("axis", "x"),
                              "origin": [round(v, 4) for v in rot.get("origin", [8, 8, 8])]}
        (handle if uses_arm else console).append(el)

    os.makedirs(MODELS, exist_ok=True)
    used = {k: v for k, v in textures.items()}
    used["particle"] = "create:block/controls_frame"

    for fname, els in (("coaster_controls.json", console),
                       ("coaster_controls_handle.json", handle)):
        io.open(os.path.join(MODELS, fname), "w", encoding="utf8", newline="\n").write(
            json.dumps({"parent": "minecraft:block/block",
                        "textures": used, "elements": els}, indent=2) + "\n")

    print(f"console: {len(console)} elements")
    print(f"handle : {len(handle)} elements")
    if handle:
        ys = [e["from"][1] for e in handle] + [e["to"][1] for e in handle]
        zs = [e["from"][2] for e in handle] + [e["to"][2] for e in handle]
        print(f"handle spans y {min(ys)}..{max(ys)}  z {min(zs)}..{max(zs)}")
        print("  -> set CoasterControlsRenderer PIVOT_Y/PIVOT_Z near the LOW end of those")
    print("wrote coaster_controls.json + coaster_controls_handle.json")


if __name__ == "__main__":
    main()
