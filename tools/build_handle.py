"""Generates handle models for the Coaster Controls.

The first handle was a single angled bar. It read as a plank lying on the console because it
had no grip to hold and no visible hinge -- nothing said which end moves. Each design here
fixes that the same way: a pivot boss at the bottom so the rotation has an obvious centre, and
something at the top a hand could actually close around.

Every design hinges about **y=9, z=3**, matching PIVOT_Y and PIVOT_Z in CoasterControlsRenderer.
Move one and the other has to move with it, or the handle swings around a point in mid-air.

    py tools/build_handle.py            writes the T-bar (the default)
    py tools/build_handle.py wheel      writes the brake wheel instead
    py tools/build_handle.py quadrant   writes the twin-lever quadrant

UVs are computed from each box rather than typed. A wrong UV is invisible in the file and
obvious in game, which is the worst combination to debug by eye.
"""
import io
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                   "coasters_extras", "models", "block")

IRON = "#7"          # create:block/industrial_iron_block
GAUGE = "#6"         # create:block/gauge -- the brass-ish accent

TEXTURES = {
    "4": "create:block/controls",
    "5": "create:block/controls_frame",
    "6": "create:block/gauge",
    "7": "create:block/industrial_iron_block",
    "1_7": "create:block/controls",
    "particle": "create:block/bogey/particle",
}


def box(name, frm, to, tex=IRON, rotation=None):
    """One element, with all six faces UV-mapped from its own bounds.

    Deriving the UVs from the box means a face always samples a patch the same size as the
    face itself, so the metal grain stays the same scale on a 2px post and a 10px grip.
    """
    x0, y0, z0 = frm
    x1, y1, z1 = to
    faces = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "west":  [z0, 16 - y1, z1, 16 - y0],
        "east":  [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "up":    [x0, z0, x1, z1],
        "down":  [x0, 16 - z1, x1, 16 - z0],
    }
    el = {
        "name": name,
        "from": list(frm),
        "to": list(to),
        "faces": {f: {"uv": [round(max(0.0, min(16.0, v)), 2) for v in uv],
                      "texture": tex}
                  for f, uv in faces.items()},
    }
    if rotation:
        el["rotation"] = rotation
    return el


def tbar():
    """A T-bar: post up from a hinge boss, horizontal grip across the top, capped ends.

    This is the shape a ride operator actually pulls -- two hands on a crossbar. It also reads
    as a coaster control rather than a train throttle, which a plain lever does not.
    """
    return [
        # Hinge boss. Wider than the post and centred on the pivot, so the eye is told where
        # the rotation happens before anything moves.
        box("handle_pivot", [6, 8, 1], [10, 11, 5], GAUGE),
        # Shaft.
        box("handle_post", [7, 10, 2], [9, 15, 4]),
        # Crossbar, and the caps that stop it looking like a floating stick.
        box("handle_grip", [3, 15, 2], [13, 17, 4]),
        box("handle_cap_left", [2, 14, 1], [4, 18, 5], GAUGE),
        box("handle_cap_right", [12, 14, 1], [14, 18, 5], GAUGE),
    ]


def wheel():
    """A spoked brake wheel, built from four rim segments and a hub.

    A wheel spins continuously instead of sweeping a small arc, which suits the block-entity
    renderer far better than a lever: there is no end of travel to hit and no snap back.
    Approximated with straight segments -- Minecraft's model format has no curves, and an
    octagon at this size reads as round.
    """
    return [
        box("handle_hub", [7, 8, 1], [9, 10, 5], GAUGE),
        # Rim, as a square ring standing in the XY plane.
        box("handle_rim_top", [4, 14, 2], [12, 16, 4]),
        box("handle_rim_bottom", [4, 2, 2], [12, 4, 4]),
        box("handle_rim_left", [2, 4, 2], [4, 14, 4]),
        box("handle_rim_right", [12, 4, 2], [14, 14, 4]),
        # Spokes, crossing at the hub.
        box("handle_spoke_v", [7, 4, 2], [9, 14, 4], GAUGE),
        box("handle_spoke_h", [4, 8, 2], [12, 10, 4], GAUGE),
    ]


def quadrant():
    """Two small levers side by side in a slotted plate -- throttle and brake, aircraft style.

    Worth it only now that the brake is a real separate control on the spacebar: two levers
    that always move together would be a lie about how the block works.
    """
    return [
        box("handle_plate", [3, 8, 1], [13, 10, 5], GAUGE),
        box("handle_lever_a", [5, 9, 2], [7, 16, 4]),
        box("handle_knob_a", [4, 15, 1], [8, 17, 5], GAUGE),
        box("handle_lever_b", [9, 9, 2], [11, 16, 4]),
        box("handle_knob_b", [8, 15, 1], [12, 17, 5], GAUGE),
    ]


DESIGNS = {"tbar": tbar, "wheel": wheel, "quadrant": quadrant}


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else "tbar"
    if which not in DESIGNS:
        raise SystemExit(f"unknown design {which!r}. one of: {', '.join(DESIGNS)}")

    os.makedirs(OUT, exist_ok=True)

    # Write every design to a preview file so they can be compared in Blockbench, and the
    # chosen one over the model the renderer actually loads.
    for name, fn in DESIGNS.items():
        model = {"parent": "minecraft:block/block", "textures": TEXTURES,
                 "elements": fn()}
        path = os.path.join(OUT, f"coaster_controls_handle_{name}.json")
        io.open(path, "w", encoding="utf8", newline="\n").write(
            json.dumps(model, indent=2) + "\n")
        print(f"  {name:9s} {len(model['elements'])} elements -> {os.path.basename(path)}")

    chosen = {"parent": "minecraft:block/block", "textures": TEXTURES,
              "elements": DESIGNS[which]()}
    live = os.path.join(OUT, "coaster_controls_handle.json")
    io.open(live, "w", encoding="utf8", newline="\n").write(
        json.dumps(chosen, indent=2) + "\n")
    print(f"\nactive handle: {which} -> coaster_controls_handle.json")
    print("pivot is y=9 z=3 -- keep CoasterControlsRenderer.PIVOT_Y/PIVOT_Z in step")


if __name__ == "__main__":
    main()
