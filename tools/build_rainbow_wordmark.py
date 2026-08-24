"""Sweeps a spectrum across the light face of a pixel wordmark, leaving its outline alone.

The heading images are white pixel type with a black keyline and a darker drop shadow. Tinting
the whole image sweeps the hue across the keyline too and the letters lose their edge -- the
same mistake the rainbow balloon made, where hue was applied down the whole texture and the
result read as flat bands rather than a lit object.

So the split here is by BRIGHTNESS, not by alpha: pixels above a threshold are the lit face and
take the spectrum, everything below is structure and is left exactly as it was. Each recoloured
pixel keeps its own value, so whatever shading the face already had survives the recolour.

    python tools/build_rainbow_wordmark.py Rainbow-Track.png
    python tools/build_rainbow_wordmark.py Rainbow-Track.png --out Rainbow-Track-rainbow.png

Reads from and writes to Downloads, which is where build_icon_headers.py expects the text images.
"""
import argparse
import colorsys
import os

from PIL import Image

DL = os.path.join(os.path.expanduser("~"), "Downloads")

# The band of the spectrum to sweep. Stopping short of a full turn keeps the last letter from
# landing back on the colour of the first, which reads as a mistake rather than a rainbow.
HUE_START = 0.00
HUE_END = 0.85

# Above this value a pixel counts as the lit face. The keyline is near-black and the drop shadow
# is dark grey, so a high threshold keeps both.
FACE_VALUE = 0.62

# Faces are white, so their own saturation is nothing to preserve -- this is the saturation the
# spectrum is drawn at. Slightly under full so it reads as paint rather than as a test pattern.
SATURATION = 0.82


def rainbow(img):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size

    # Sweep across the INKED width, not the canvas width. Padding either side would otherwise eat
    # part of the spectrum and the first and last letters would come out nearly the same colour.
    box = img.getbbox()
    x0, x1 = (box[0], box[2]) if box else (0, w)
    span = max(1, x1 - x0 - 1)

    changed = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            _, _, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if v < FACE_VALUE:
                continue                      # keyline and shadow: structure, left alone
            hue = HUE_START + (HUE_END - HUE_START) * ((x - x0) / span)
            nr, ng, nb = colorsys.hsv_to_rgb(hue % 1.0, SATURATION, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
            changed += 1
    return img, changed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("name", help="file in Downloads, e.g. Rainbow-Track.png")
    ap.add_argument("--out", help="output name (default: overwrite in place)")
    args = ap.parse_args()

    src = args.name if os.path.isabs(args.name) else os.path.join(DL, args.name)
    if not os.path.exists(src):
        raise SystemExit(f"no such file: {src}")

    img = Image.open(src)
    print(f"{os.path.basename(src)}  {img.size[0]}x{img.size[1]}")

    # Keep the original once, so re-running never compounds a sweep onto an already-swept image.
    # The suffix goes BEFORE the extension: Pillow picks its writer from the extension, and a
    # trailing ".orig" leaves it with nothing to go on.
    stem, ext = os.path.splitext(src)
    backup = f"{stem}.orig{ext}"
    if not os.path.exists(backup):
        Image.open(src).save(backup)
        print(f"  original kept as {os.path.basename(backup)}")
    img = Image.open(backup)

    out_img, changed = rainbow(img)
    dst = os.path.join(DL, args.out) if args.out else src
    out_img.save(dst)
    print(f"  {changed} face pixels swept hue {HUE_START:g}..{HUE_END:g}")
    print(f"  wrote {dst}")


if __name__ == "__main__":
    main()
