"""Draws the Coaster Controls handle sweep in side view, straight from the model JSON.

The handle's motion is decided by three numbers in CoasterControlsRenderer (the pivot) and two
in CoasterControlsBlockEntity (the throw and the ease). Checking them in game means a build, a
launch, a world and a block, and the thing you are judging lasts about a third of a second.

This reads the same model files the game bakes and plots the arm at each throttle position
against the console outline, so a wrong pivot is visible as the arm cutting through the
console or swinging off its mount.

    py tools/preview_controls_sweep.py

Writes controls_sweep.png beside itself. Side view: +Z to the right (into the block, away from
the player), +Y up. Everything is in the model's own 0-16 pixel units.
"""
import json
import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets",
                      "coasters_extras", "models", "block")
OUT = os.path.join(HERE, "controls_sweep.png")

# Keep these in step with the Java. They are the whole point of the preview.
PIVOT_Y = 12.0
PIVOT_Z = 11.0
THROW = 24.0
POSITIONS = [("BACK", -THROW, (255, 110, 110)),
             ("NEUTRAL", 0.0, (235, 235, 235)),
             ("FORWARD", THROW, (120, 220, 140))]

SCALE = 26
PAD = 60
# The model's z runs a little past both ends of the block once the grip is included.
Z0, Z1 = -3.0, 17.0
# Up to 26, not 17: the lever's 45-degree rest pose already puts the grab bar above the block,
# and full forward throw takes it higher again. A canvas that stops at the block's own height
# crops off the exact part of the sweep worth looking at.
Y0, Y1 = -1.0, 26.0

BG = (22, 24, 30)
GRID = (40, 44, 54)
AXIS = (70, 76, 92)
CONSOLE = (96, 104, 124)
CONSOLE_FILL = (46, 51, 64)


def load(name):
    with open(os.path.join(ASSETS, name), encoding="utf8") as fh:
        return json.load(fh)["elements"]


def px(z, y):
    """Model space -> image space. Y is flipped; +Z runs right."""
    return (PAD + (z - Z0) * SCALE, PAD + (Y1 - y) * SCALE)


def rot(z, y, deg):
    """Rotate about the pivot in the ZY plane, matching Axis.XP in the renderer.

    Mojang's Axis.XP is JOML's rotationX, whose matrix puts the minus sign on the Z TERM OF Y,
    not on the Y term of Z:

        y' = y*cos - z*sin
        z' = y*sin + z*cos

    Getting that backwards inverts the whole drawing, which is exactly what happened the first
    time this ran: it plotted the inverse rotation and made FORWARD look like it dipped the
    handle when in game it lifts it. The consequence of the sign living on y' is that geometry
    on the MINUS-Z side of the pivot rises for a positive angle -- and this handle is entirely
    on the minus-Z side, since the grip sits at z 0.43 and the hinge at z 9.
    """
    a = math.radians(deg)
    dz, dy = z - PIVOT_Z, y - PIVOT_Y
    return (PIVOT_Z + dy * math.sin(a) + dz * math.cos(a),
            PIVOT_Y + dy * math.cos(a) - dz * math.sin(a))


def baked(z, y, element):
    """The element's own rotation from the model file, about its own origin.

    Only X-axis rotations move anything in this plane; a Y or Z rotation leaves the side view
    unchanged, so they are ignored here rather than wrongly flattened into it.
    """
    r = element.get("rotation")
    if not r or r.get("axis") != "x" or not r.get("angle"):
        return (z, y)
    a = math.radians(r["angle"])
    oz, oy = r["origin"][2], r["origin"][1]
    dz, dy = z - oz, y - oy
    return (oz + dy * math.sin(a) + dz * math.cos(a),
            oy + dy * math.cos(a) - dz * math.sin(a))


def main():
    w = int(PAD * 2 + (Z1 - Z0) * SCALE)
    h = int(PAD * 2 + (Y1 - Y0) * SCALE)
    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)

    for i in range(-4, 20, 2):
        d.line([px(i, Y0), px(i, Y1)], fill=GRID)
        d.line([px(Z0, i), px(Z1, i)], fill=GRID)
    # The block's own bounds, so "sticking out of the block" is obvious.
    d.rectangle([px(0, 16), px(16, 0)], outline=AXIS, width=2)

    # Console body, drawn as its silhouette in this plane.
    for e in load("coaster_controls.json"):
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        d.rectangle([px(z0, y1), px(z1, y0)], fill=CONSOLE_FILL, outline=CONSOLE)

    handle = load("coaster_controls_handle.json")
    for label, angle, colour in POSITIONS:
        for e in handle:
            (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
            corners = [(z0, y0), (z1, y0), (z1, y1), (z0, y1)]
            # Apply the element's OWN baked rotation first, then the runtime swing. Skipping the
            # baked one is what made an earlier version of this preview draw a lever that lies
            # flat -- which is exactly how the bug got into the model in the first place, so a
            # preview that ignores it cannot catch it.
            corners = [baked(z, y, e) for z, y in corners]
            corners = [rot(z, y, angle) for z, y in corners]
            d.polygon([px(z, y) for z, y in corners], outline=colour)
        # Label at the grip end, which is the far end from the hinge.
        gz, gy = rot(0.43, 12.0, angle)
        lx, ly = px(gz, gy)
        d.text((lx - 46, ly - 16), label, fill=colour)

    # The hinge itself.
    hz, hy = px(PIVOT_Z, PIVOT_Y)
    d.ellipse([hz - 5, hy - 5, hz + 5, hy + 5], outline=(255, 210, 80), width=2)
    d.text((hz + 10, hy - 6), f"pivot z={PIVOT_Z:g} y={PIVOT_Y:g}", fill=(255, 210, 80))

    d.text((PAD, 16), "Coaster Controls handle sweep - side view", fill=(220, 220, 230))
    d.text((PAD, 32), f"throw +/-{THROW:g} deg   +Z right (into block)   player stands left",
           fill=(150, 156, 170))

    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
