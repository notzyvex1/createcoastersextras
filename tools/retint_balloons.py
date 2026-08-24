"""Retints the sixteen dye balloons from one master, so the whole set is one family.

The artwork is not redrawn. Every balloon in this mod is the same sprite -- same outline, same
highlight, same curly string -- differing only in the colour of the body. So the body is lifted
off one balloon as a light-to-dark ramp and re-tinted per colour, which gives every balloon
identical shading instead of sixteen near-misses that were each tinted separately.

Red is the master. It is the base mod's own balloon and the leftmost on the reference sheet, so
it is the drawing the rest of the set is meant to match.

Two things the obvious version gets wrong:

The string must not be tinted. On the reference sheet every balloon carries the same pale string
whatever colour the body is. Which pixels are string is found by SATURATION against a saturated
master -- body is saturated, string is not. Diffing two finished balloons was tried first and
returns nothing, because our white balloon's string is already a different grey from our black
one's; they were never a shared layer.

The shading must be re-spread, not applied absolutely. The master's own brightness range is
measured and each pixel's position within it is what carries over, so a dark target keeps its
highlights and a light one keeps its shadows. Multiplying the master's brightness by the target's
instead -- the obvious version -- crushes every shade of black together and blows every shade of
white out.

    py tools/retint_balloons.py --preview     contact sheet only, writes nothing into the mod
    py tools/retint_balloons.py               retint for real
"""
import colorsys
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "coasters_extras",
                   "textures", "item")

MASTER = "red"
# Below this saturation a pixel is string, knot or outline rather than body. Well above the
# greys in the string and well below anything in a red balloon, so it is not a close call.
STRING_SATURATION = 0.25

# How much lighter the brightest body pixel is than the darkest, as a fraction of the target's
# own brightness. Measured off the master rather than invented: it is what makes the balloon
# read as a sphere instead of a disc.
SHADE_FLOOR = 0.52
# How much of the target's saturation every body pixel keeps regardless of the master's gloss.
SATURATION_FLOOR = 0.8

# The reference sheet's sixteen, in its own order. Read off the sheet rather than taken from
# Minecraft's dye table -- the sheet is brighter than the dye colours, particularly in the
# greens and the light blue, and matching the dyes would miss it.
ORDER = [
    ("orange",     0xE8912E), ("yellow",     0xE8D44F),
    ("lime",       0x7BC44A), ("green",      0x4E8B2C),
    ("cyan",       0x2C93B8), ("light_blue", 0x86ACE4),
    ("blue",       0x3A4FB8), ("purple",     0x8B3CC4),
    ("magenta",    0xC34FC4), ("pink",       0xE88AAE),
    ("brown",      0x8B5A2E), ("white",      0xE4E4E8),
    ("light_gray", 0xA8A8A8), ("gray",       0x6B6B6B),
    ("black",      0x2B2B3B),
]


def load(name):
    return Image.open(os.path.join(TEX, name + "_balloon.png")).convert("RGBA")


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def split(master):
    """(body pixels, string pixels) of the master, by saturation."""
    p = master.load()
    body, string = [], []
    for y in range(master.height):
        for x in range(master.width):
            c = p[x, y]
            if c[3] == 0:
                continue
            (string if hsv(c)[1] < STRING_SATURATION else body).append((x, y))
    return body, string


def retint(master, body, rgb):
    """The master's shading wearing `rgb`. Everything not in `body` passes through untouched."""
    th, ts, tv = hsv(((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255))

    p = master.load()
    vs = [hsv(p[x, y])[2] for x, y in body]
    ss = [hsv(p[x, y])[1] for x, y in body]
    lo, hi = min(vs), max(vs)
    span = max(1e-6, hi - lo)
    smax = max(1e-6, max(ss))

    out = master.copy()
    op = out.load()
    for x, y in body:
        c = p[x, y]
        _, s, v = hsv(c)
        t = (v - lo) / span                          # 0 darkest, 1 brightest in the master
        nv = min(1.0, tv * (SHADE_FLOOR + (1 - SHADE_FLOOR) * t))
        # Highlights are less saturated than shadows -- that gloss is why a balloon looks
        # inflated. The master already has it, so carry its ratio rather than inventing one.
        #
        # Only a fifth of the saturation is handed to that ratio though. Carrying it in full
        # was tried and drains the whole set: most body pixels sit below the master's peak, so
        # every balloon comes out chalky. The gloss is a highlight, not an overall wash.
        ns = min(1.0, ts * (SATURATION_FLOOR + (1 - SATURATION_FLOOR) * (s / smax)))
        r, g, b = colorsys.hsv_to_rgb(th, ns, nv)
        op[x, y] = (round(r * 255), round(g * 255), round(b * 255), c[3])
    return out


def main():
    preview_only = "--preview" in sys.argv

    master = load(MASTER)
    body, string = split(master)
    print(f"master {MASTER}: {len(body)} body pixels retinted, "
          f"{len(string)} left alone (string, knot, outline)")

    built = [(name, retint(master, body, rgb)) for name, rgb in ORDER]

    # Red is the master and rainbow is hand-drawn -- neither is ours to regenerate.
    before = [load("red")] + [load(n) for n, _ in ORDER] + [load("rainbow")]
    after = [load("red")] + [img for _, img in built] + [load("rainbow")]

    cell = 48
    sheet = Image.new("RGBA", (cell * len(after), cell * 2 + 8), (255, 255, 255, 255))
    for i, img in enumerate(before):
        sheet.alpha_composite(img.resize((cell, cell), Image.NEAREST), (i * cell, 0))
    for i, img in enumerate(after):
        sheet.alpha_composite(img.resize((cell, cell), Image.NEAREST), (i * cell, cell + 8))
    out = os.path.join(os.path.expanduser("~"), "Downloads", "balloon_retint_preview.png")
    sheet.save(out)
    print(f"top row = now, bottom row = retinted  ->  {out}")

    if preview_only:
        print("preview only; nothing written into the mod")
        return

    for name, img in built:
        img.save(os.path.join(TEX, name + "_balloon.png"))
    print(f"{len(built)} balloons retinted in {TEX}")


if __name__ == "__main__":
    main()
