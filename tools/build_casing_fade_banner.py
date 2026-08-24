"""Builds the Controls banner: andesite casing fading slowly through brass and back.

The banner strip itself is not invented here. Rebuilding a casing banner from scratch was tried
four separate ways and never matched -- tiling the whole casing repeats its border, tiling the
interior repeats its bevel, and nine-slice smears the pattern into bands. What works is the
opposite: take one casing texture, strip its left and right frame columns so only the top and
bottom rails survive, and tile that horizontally. Nothing repeats because there is nothing left
to repeat except the rails, which are meant to run the full width.

The transition is a dissolve, not a crossfade: every pixel is either fully andesite or fully
brass, and they flip over one at a time. A crossfade blends the two into a muddy in-between that
is neither material, which on a 16-colour block texture reads as a blurry mess; a dissolve keeps
every pixel a real colour from a real casing the whole way through.

Which pixel flips when comes from an 8x8 Bayer matrix rather than random noise. Random dissolve
clumps -- patches go early while others linger -- and at eighteen pixels tall that clumping is
the whole image. Bayer spreads the flips evenly, so the change reads as the material turning
over rather than as the texture corroding.

It runs the whole casing set as a chain -- andesite, brass, copper, railway, shadow steel,
refined radiance, creative -- and dissolves from the last back into the first, so the loop
closes on itself with no seam and no ping-pong needed. Every leg alternates the matrix's
direction, so consecutive dissolves do not all wipe in with the same pixels going first.

    py tools/build_casing_fade_banner.py
"""
import io
import os
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(ROOT, "libs", "create-1.21.1-6.0.10.jar")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "coasters_extras",
                   "textures", "gui", "section", "casing_banner.png")

BANNER_W = 162
BANNER_H = 18
BORDER = 2           # thickness of the casing's frame, measured off andesite_casing
PER_LEG = 8          # frames spent dissolving one casing into the next

# Andesite first because that is the frame shown when nobody is hovering, and Controls should
# look like plain andesite at rest. The rest run cheapest to fanciest, which is also roughly
# Create's own progression, so the cycle reads as going somewhere rather than shuffling.
CASINGS = [
    "andesite_casing",
    "brass_casing",
    "copper_casing",
    "railway_casing",
    "shadow_steel_casing",
    "refined_radiance_casing",
    "creative_casing",
]


def casing(name):
    """One 16x16 casing face out of Create's jar, first frame if it is animated."""
    with zipfile.ZipFile(JAR) as z:
        hit = [n for n in z.namelist() if n.endswith("textures/block/" + name + ".png")]
        if not hit:
            raise SystemExit("no such Create texture: " + name)
        img = Image.open(io.BytesIO(z.read(hit[0]))).convert("RGBA")
    return img.crop((0, 0, img.width, img.width)) if img.height > img.width else img


def rails_only(tile):
    """Remove the left and right frame columns, keeping the top and bottom rails.

    The side columns are what produce a vertical seam every sixteen pixels once the tile is
    repeated. They are painted over with an interior column rather than mirrored, because
    mirroring reintroduces the bevel it was supposed to remove. The rails are then restored at
    full width so they run unbroken across the finished strip.
    """
    out = tile.copy()
    px, src = out.load(), tile.load()
    interior = tile.width // 2          # a column well clear of either frame
    for x in list(range(BORDER)) + list(range(tile.width - BORDER, tile.width)):
        for y in range(tile.height):
            px[x, y] = src[interior, y]
    for y in list(range(BORDER)) + list(range(tile.height - BORDER, tile.height)):
        for x in range(tile.width):
            px[x, y] = src[x, y]
    return out


def strip(tile):
    """The 16px tile stretched and repeated into one banner-width row."""
    out = Image.new("RGBA", (BANNER_W, BANNER_H), (0, 0, 0, 0))
    # Rails at native size, only the middle stretched, so the frame stays crisp at 18px tall.
    top = tile.crop((0, 0, tile.width, BORDER))
    bottom = tile.crop((0, tile.height - BORDER, tile.width, tile.height))
    middle = tile.crop((0, BORDER, tile.width, tile.height - BORDER)) \
                 .resize((tile.width, BANNER_H - 2 * BORDER), Image.NEAREST)
    for x in range(0, BANNER_W, tile.width):
        out.alpha_composite(top, (x, 0))
        out.alpha_composite(middle, (x, BORDER))
        out.alpha_composite(bottom, (x, BANNER_H - BORDER))
    return out


def bayer(n=8):
    """The n x n ordered-dither matrix, normalised to 0..1."""
    m = [[0]]
    size = 1
    while size < n:
        m = [[4 * v for v in row] + [4 * v + 2 for v in row] for row in m] \
            + [[4 * v + 3 for v in row] + [4 * v + 1 for v in row] for row in m]
        size *= 2
    return [[v / (n * n) for v in row] for row in m]


def dissolve(a, b, t, matrix, invert):
    """`a` with the fraction `t` of its pixels replaced by `b`, chosen by threshold order."""
    out = a.copy()
    op, bp = out.load(), b.load()
    n = len(matrix)
    for y in range(a.height):
        row = matrix[y % n]
        for x in range(a.width):
            threshold = row[x % n]
            if invert:
                threshold = 1.0 - threshold
            if t > threshold:
                op[x, y] = bp[x, y]
    return out


def main():
    strips = [strip(rails_only(casing(c))) for c in CASINGS]
    matrix = bayer()
    frames = len(strips) * PER_LEG

    sheet = Image.new("RGBA", (BANNER_W, BANNER_H * frames), (0, 0, 0, 0))
    for leg in range(len(strips)):
        a = strips[leg]
        b = strips[(leg + 1) % len(strips)]      # last leg wraps back to andesite
        for step in range(PER_LEG):
            # step 0 is the pure casing; the dissolve completes on the next leg's step 0.
            f = dissolve(a, b, step / PER_LEG, matrix, leg % 2 == 1)
            sheet.paste(f, (0, (leg * PER_LEG + step) * BANNER_H))

    sheet.save(OUT)
    print(" -> ".join(CASINGS) + " -> " + CASINGS[0])
    print(f"  {frames} frames  {sheet.size[0]}x{sheet.size[1]}  ->  {OUT}")
    print(f"  set TabSections.Banner frames to {frames}")

    # One column of every frame, so the whole cycle can be checked at a glance.
    preview = Image.new("RGBA", (BANNER_W * 2, BANNER_H * frames * 2), (30, 31, 34, 255))
    preview.alpha_composite(sheet.resize(
        (BANNER_W * 2, BANNER_H * frames * 2), Image.NEAREST))
    p = os.path.join(os.path.expanduser("~"), "Downloads", "casing_cycle_preview.png")
    preview.save(p)
    print(f"  preview -> {p}")


if __name__ == "__main__":
    main()
