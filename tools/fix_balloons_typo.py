"""Fixes "Ballons" -> "Balloons" by duplicating the word's own 'o' glyph.

Re-typesetting is not an option: the pixel font that made these headers is not on this machine,
and a substitute would sit visibly wrong beside the other fourteen. But the missing letter is a
second 'o' and the word already has one, so copying those pixels is exact by construction --
same font, size, colour and drop shadow, because they are the same pixels.

The glyph bounds are hard-coded because they had to be read off a ruler. Automatic splitting
failed twice: the letters share a drop shadow, so there is no transparent column between them
and no real density valley either. Deriving them "cleverly" would be guessing dressed up as
detection.

    python tools/fix_balloons_typo.py            preview, writes nothing
    python tools/fix_balloons_typo.py --write    write Balloons.png
"""
import os
import sys

from PIL import Image

DL = os.path.join(os.path.expanduser("~"), "Downloads")
SRC = os.path.join(DL, "Ballons.png")
OUT = os.path.join(DL, "Balloons.png")
PREVIEW = (r"C:\Users\Zohan\AppData\Local\Temp\claude\D--DEADLINE-SMP"
           r"\53b1c5c4-fd10-4e7b-8c38-b731994317a4\scratchpad\balloons_fixed.png")

# Read off tools' ruler overlay of Ballons.png (330x78).
O_START, O_END = 168, 221      # the 'o', inclusive
N_START = 228                  # where 'n' begins, for the inter-letter gap


def main():
    if not os.path.exists(SRC):
        sys.exit(f"Not found: {SRC}")
    img = Image.open(SRC).convert("RGBA")

    width = O_END - O_START + 1
    gap = N_START - O_END - 1
    step = width + gap
    print(f"{SRC}\n{img.width}x{img.height}")
    print(f"'o' at columns {O_START}-{O_END} ({width}px), gap to 'n' {gap}px, "
          f"shifting the tail {step}px right")

    out = Image.new("RGBA", (img.width + step, img.height), (0, 0, 0, 0))
    # up to and including the original 'o'
    out.alpha_composite(img.crop((0, 0, O_END + 1, img.height)), (0, 0))
    # the duplicate, one letter-pitch along
    out.alpha_composite(img.crop((O_START, 0, O_END + 1, img.height)), (O_START + step, 0))
    # the rest of the word, pushed right by the same amount
    out.alpha_composite(img.crop((O_END + 1, 0, img.width, img.height)),
                        (O_END + 1 + step, 0))

    # Preview on dark, since the lettering is white on transparency and invisible on white.
    dark = Image.new("RGBA", (out.width, out.height + 16), (30, 31, 34, 255))
    dark.alpha_composite(out, (0, 8))
    dark.resize((out.width * 2, (out.height + 16) * 2), Image.NEAREST).save(PREVIEW)
    print(f"preview: {PREVIEW}")

    if "--write" not in sys.argv:
        print("\nPreview only. Add --write to save Balloons.png")
        return
    out.save(OUT)
    print(f"\nwrote {OUT}  ({out.width}x{out.height})")


if __name__ == "__main__":
    main()
