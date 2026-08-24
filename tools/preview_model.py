"""Renders a Minecraft block model as an isometric preview, so it can be checked without launching the game.

Every element is an axis-aligned box, so an isometric projection needs no 3D library: project
the eight corners of each box, draw the three visible faces as filled polygons, and shade each
by which way it points. Painter's algorithm handles occlusion -- sorting boxes by depth is
correct here precisely because they are axis-aligned and mostly non-overlapping.

Element rotations ARE applied, which is the whole point: the three throttle models differ only
by a rotation, and a preview that ignored it would draw three identical pictures.
"""
import io
import json
import math
import os
import sys

from PIL import Image, ImageDraw

MODELS = (r"D:\CreateCoastersExtras\src\main\resources\assets"
          r"\coasters_extras\models\block")
OUT = os.path.join(os.path.expanduser("~"), "Downloads", "CoasterControls-Preview.png")

SCALE = 22
# Flat colours per texture slot; the point is to read the SHAPE, not the skin.
COLOURS = {
    "#4": (150, 150, 158), "#1_7": (92, 96, 104),
    "#knob": (196, 58, 52), "#missing": (255, 0, 220),
}
DEFAULT = (130, 130, 140)
# Isometric: x goes right-down, z goes left-down, y goes up.
COS30, SIN30 = math.cos(math.radians(30)), math.sin(math.radians(30))


def rotate(point, rotation):
    """Rotate a point about an axis-aligned origin, as Minecraft does."""
    if not rotation:
        return point
    angle = math.radians(rotation.get("angle", 0))
    if angle == 0:
        return point
    axis = rotation.get("axis", "y")
    ox, oy, oz = rotation.get("origin", [8, 8, 8])
    x, y, z = point[0] - ox, point[1] - oy, point[2] - oz
    c, s = math.cos(angle), math.sin(angle)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return [x + ox, y + oy, z + oz]


def raw_project(p):
    """Isometric projection in model units, before any fitting."""
    x, y, z = p
    return ((x - z) * COS30, -y - (x + z) * SIN30 * 0.5)


def fit(points, w, h, margin=40):
    """Scale and offset so the whole model lands inside the canvas.

    Computed from the model's own projected bounds rather than a fixed zoom -- a hardcoded
    scale was cropping the block to a corner, and a preview that does not show the whole
    thing is worse than no preview because it looks like geometry is missing.
    """
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    span_x, span_y = max(xs) - min(xs), max(ys) - min(ys)
    scale = min((w - 2 * margin) / max(span_x, 0.001),
                (h - 2 * margin) / max(span_y, 0.001))
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    return lambda p: (w / 2 + (p[0] - cx) * scale, h / 2 + (p[1] - cy) * scale)


def render(path, title):
    model = json.load(io.open(path, encoding="utf8"))
    W, H = 460, 460
    img = Image.new("RGBA", (W, H), (30, 31, 36, 255))
    draw = ImageDraw.Draw(img)

    # Project everything first, so the fit can be computed from real bounds.
    raw = []
    for e in model.get("elements", []):
        rot = e.get("rotation")
        x0, y0, z0 = e["from"]
        x1, y1, z1 = e["to"]
        corners = {name: raw_project(rotate(list(pt), rot))
                   for name, pt in {
                       "000": (x0, y0, z0), "100": (x1, y0, z0), "010": (x0, y1, z0),
                       "110": (x1, y1, z0), "001": (x0, y0, z1), "101": (x1, y0, z1),
                       "011": (x0, y1, z1), "111": (x1, y1, z1),
                   }.items()}
        tex = next((f.get("texture") for f in e.get("faces", {}).values()), None)
        # Painter's order. In this projection larger x+z sits further from the camera, so the
        # far boxes must be drawn FIRST -- hence the negation. Height is deliberately NOT part
        # of the distance: subtracting it (as this first did) buried the lever behind the
        # console, because the lever is tall and the console is deep.
        # Height only breaks ties, so a box stacked on another draws after it.
        depth = (-((x0 + x1) / 2 + (z0 + z1) / 2), (y0 + y1) / 2)
        raw.append((depth, corners, COLOURS.get(tex, DEFAULT), e.get("name", "")))

    every_point = [p for _, c, _, _ in raw for p in c.values()]
    place = fit(every_point, W, H)
    boxes = [(d, {k: place(v) for k, v in c.items()}, col, n) for d, c, col, n in raw]

    for _, c, base, _ in sorted(boxes, key=lambda b: b[0]):
        top = tuple(min(255, int(v * 1.25)) for v in base)
        left = tuple(int(v * 0.72) for v in base)
        right = base
        draw.polygon([c["010"], c["110"], c["111"], c["011"]], fill=top,
                     outline=(20, 20, 24))
        draw.polygon([c["001"], c["011"], c["111"], c["101"]], fill=right,
                     outline=(20, 20, 24))
        draw.polygon([c["000"], c["010"], c["011"], c["001"]], fill=left,
                     outline=(20, 20, 24))

    draw.text((12, 10), title, fill=(235, 236, 244))
    draw.text((12, 26), f"{len(model.get('elements', []))} elements",
              fill=(150, 152, 164))
    return img


def main():
    wanted = [("coaster_controls_back.json", "THROTTLE 0  (back)"),
              ("coaster_controls.json", "THROTTLE 1  (neutral)"),
              ("coaster_controls_fwd.json", "THROTTLE 2  (forward)")]
    frames = [render(os.path.join(MODELS, f), t) for f, t in wanted]
    sheet = Image.new("RGBA", (sum(f.width for f in frames), frames[0].height))
    x = 0
    for f in frames:
        sheet.paste(f, (x, 0))
        x += f.width
    sheet.save(OUT)
    print(f"wrote {OUT}  ({sheet.width}x{sheet.height})")


if __name__ == "__main__":
    main()
