"""Orthographic side, front and top views of the Coaster Controls, from the model JSON.

The sweep preview answers "does the handle rotate correctly". This answers the different
question of "does the block look right at all", which is what screenshots kept showing it did
not. Three orthographic views make a misplaced element obvious in a way a screenshot from one
angle does not: a box floating in front of the console reads as a normal lever from the front
and as a paddle on a stick from the side.

    py tools/preview_controls_views.py

Console elements are drawn filled, handle elements outlined on top, and the block's own 0..16
bounds are drawn as a box so anything sticking out of the block is visible as such.
"""
import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets",
                      "coasters_extras", "models", "block")
OUT = os.path.join(HERE, "controls_views.png")

SCALE = 16
PAD = 34
LO, HI = -4.0, 20.0          # a margin either side so overhang is visible

BG = (22, 24, 30)
GRID = (38, 42, 52)
BOUNDS = (86, 94, 112)
CONSOLE_FILL = (52, 58, 72)
CONSOLE_EDGE = (110, 120, 142)
HANDLE_EDGE = (120, 220, 140)
HANDLE_FILL = (30, 62, 40)
LABEL = (215, 218, 228)
WARN = (255, 140, 120)


def load(name):
    with open(os.path.join(ASSETS, name), encoding="utf8") as fh:
        return json.load(fh)["elements"]


# Each view maps a 3D box to a 2D rect: (title, horizontal axis, vertical axis, flip-h)
VIEWS = [
    ("SIDE   (looking along +X)", 2, 1, False),   # z across, y up
    ("FRONT  (looking along +Z)", 0, 1, True),    # x across, y up -- flipped so left is left
    ("TOP    (looking down -Y)", 2, 0, False),    # z across, x down
]


def panel_size():
    span = int((HI - LO) * SCALE)
    return span + PAD * 2, span + PAD * 2


def draw_view(img, ox, title, ha, va, fliph):
    d = ImageDraw.Draw(img)
    span = int((HI - LO) * SCALE)

    def px(h, v):
        hh = (HI - h) if fliph else (h - LO)
        return (ox + PAD + hh * SCALE, PAD + (HI - v) * SCALE)

    def rect(h0, v0, h1, v1, **kw):
        """Corners in model space, ordered for PIL after any flip."""
        (xa, ya), (xb, yb) = px(h0, v0), px(h1, v1)
        d.rectangle([min(xa, xb), min(ya, yb), max(xa, xb), max(ya, yb)], **kw)

    for i in range(-4, 21, 4):
        d.line([px(i, LO), px(i, HI)], fill=GRID)
        d.line([px(LO, i), px(HI, i)], fill=GRID)
    rect(0, 0, 16, 16, outline=BOUNDS, width=2)

    def box(e, fill, edge):
        f, t = e["from"], e["to"]
        rect(f[ha], f[va], t[ha], t[va], fill=fill, outline=edge)

    for e in load("coaster_controls.json"):
        box(e, CONSOLE_FILL, CONSOLE_EDGE)
    for e in load("coaster_controls_handle.json"):
        box(e, HANDLE_FILL, HANDLE_EDGE)

    d.text((ox + PAD, 12), title, fill=LABEL)
    return span


def main():
    w, h = panel_size()
    img = Image.new("RGB", (w * 3, h), BG)
    for i, (title, ha, va, fliph) in enumerate(VIEWS):
        draw_view(img, i * w, title, ha, va, fliph)

    d = ImageDraw.Draw(img)
    # Say the measured numbers rather than leaving them to be eyeballed off the picture.
    handle = load("coaster_controls_handle.json")
    console = load("coaster_controls.json")
    console_front = min(e["from"][2] for e in console)
    grip_front = min(e["from"][2] for e in handle)
    grip_width = max(e["to"][0] for e in handle) - min(e["from"][0] for e in handle)
    d.text((PAD, h - 46),
           f"console front face at z={console_front:g}   "
           f"handle reaches z={grip_front:g}   "
           f"gap in front of console = {console_front - grip_front:.2f}px",
           fill=WARN)
    d.text((PAD, h - 30),
           f"handle spans {grip_width:g}px in X (block is 16)   "
           f"-- green is the handle, grey is the console",
           fill=WARN)

    img.save(OUT)
    print("wrote", OUT)
    print(f"console front z={console_front:g}, handle front z={grip_front:g}, "
          f"overhang {console_front - grip_front:.2f}px, handle X span {grip_width:g}px")


if __name__ == "__main__":
    main()
