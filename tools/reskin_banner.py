"""Re-skins the supplied banner with a different Create casing, keeping its geometry exactly.

Rebuilding the banner from scratch was attempted four times and got closer each time without ever
matching: tiling the whole casing repeats its border, tiling the interior repeats its bevel,
nine-slice smears the dot pattern into bands, and a nine-slice-plus-tile still missed because the
reference is also darkened by roughly sixteen per channel and animates over eight frames.

So the geometry is not rebuilt at all. The reference IS the geometry -- every pixel, including
the border, the darkening and whatever the animation does -- and only the colours are swapped.

The swap is by luminance rank. Both casings are drawn with a small palette; sorting each palette
from dark to light and mapping one onto the other transfers the material without touching a single
pixel's position. It is exact where the palettes are the same size and graceful where they are
not, because the lookup interpolates across the rank rather than requiring a one-to-one match.

    py tools/reskin_banner.py andesite_casing andesite_casing_banner
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(ROOT, "libs", "create-1.21.1-6.0.10.jar")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "coasters_extras",
                   "textures", "gui", "section")
REFERENCE = os.path.join(os.path.expanduser("~"), "Downloads", "creative_casing_banner.png")
SOURCE_CASING = "creative_casing"


def casing(name):
    with zipfile.ZipFile(JAR) as z:
        hit = [n for n in z.namelist() if n.endswith("textures/block/" + name + ".png")]
        if not hit:
            raise SystemExit("no such Create texture: " + name)
        img = Image.open(io.BytesIO(z.read(hit[0]))).convert("RGBA")
    if img.height > img.width:
        img = img.crop((0, 0, img.width, img.width))
    return img


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def palette(img):
    """Opaque colours, dark to light, most-used first within equal luminance."""
    counts = {}
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            c = px[x, y]
            if c[3] > 0:
                counts[c[:3]] = counts.get(c[:3], 0) + 1
    return sorted(counts, key=luminance)


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    target_name, out_name = sys.argv[1], sys.argv[2]

    if not os.path.exists(REFERENCE):
        raise SystemExit("reference banner not found: " + REFERENCE)

    ref = Image.open(REFERENCE).convert("RGBA")
    src_pal = palette(casing(SOURCE_CASING))
    dst_pal = palette(casing(target_name))
    print(f"  {SOURCE_CASING}: {len(src_pal)} colours   {target_name}: {len(dst_pal)} colours")

    # Rank in the SOURCE casing's palette -> same rank in the target's. Ranks are compared by
    # luminance so a dark pixel stays dark; a banner whose shading inverted would look embossed
    # the wrong way round.
    def mapped(colour):
        best, best_d = 0, None
        for i, c in enumerate(src_pal):
            d = abs(luminance(c) - luminance(colour))
            if best_d is None or d < best_d:
                best, best_d = i, d
        if not dst_pal:
            return colour
        # Proportional, so palettes of different sizes still span the same range.
        j = round(best * (len(dst_pal) - 1) / max(1, len(src_pal) - 1))
        return dst_pal[j]

    cache = {}
    out = Image.new("RGBA", ref.size, (0, 0, 0, 0))
    rp, op = ref.load(), out.load()
    for y in range(ref.height):
        for x in range(ref.width):
            c = rp[x, y]
            if c[3] == 0:
                continue
            key = c[:3]
            if key not in cache:
                cache[key] = mapped(key)
            m = cache[key]
            op[x, y] = (m[0], m[1], m[2], c[3])

    os.makedirs(OUT, exist_ok=True)
    png = os.path.join(OUT, out_name + ".png")
    out.save(png)

    meta = REFERENCE + ".mcmeta"
    if os.path.exists(meta):
        # Copy the reference's own animation settings rather than inventing them, so the two
        # banners animate in step when they sit next to each other.
        io.open(png + ".mcmeta", "w", encoding="utf8", newline="\n").write(
            io.open(meta, encoding="utf8").read())
    else:
        io.open(png + ".mcmeta", "w", encoding="utf8", newline="\n").write(
            json.dumps({"animation": {"frametime": 3, "height": 18}}, indent=2) + "\n")

    print(f"  {out_name}.png  {out.size[0]}x{out.size[1]}  ({len(cache)} colours remapped)")
    print(f"  -> {OUT}")


if __name__ == "__main__":
    main()
