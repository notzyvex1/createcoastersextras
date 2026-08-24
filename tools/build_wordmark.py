"""Composes a new heading wordmark out of the letters in the existing ones.

Every heading in the description is hand-made art in one pixel font, and a new section needs a
matching one. Rather than guess at the font, the letters are taken from the headings that already
exist -- they are the font, by definition.

Two things make this work that were not obvious:

The glyphs look separated but scan as one solid run, because each wordmark carries a drop shadow
offset down-right that fills every gap between letters. Thresholding to the bright face pixels
first drops the shadow and the letters fall apart cleanly. Segmenting the raw image finds exactly
two runs -- one per word -- which is what sent the first attempt at this into the weeds.

The font is monospaced, so composing is a matter of stepping a fixed pitch rather than kerning
pair by pair. Pitch is measured off a source wordmark rather than assumed: it is the distance
between the left edges of consecutive glyph cells, which stays constant even though the ink
inside a cell does not.

The shadow is then rebuilt the same way the originals have it, so the result sits beside them
without looking pasted in.

    py tools/build_wordmark.py "Copycat Track" Copycat-Track.png
"""
import os
import sys

from PIL import Image

DL = os.path.join(os.path.expanduser("~"), "Downloads")

# Wordmarks to harvest letters from, and what each one says. Between them these cover every
# letter needed so far; add another line if a heading needs one they do not have.
SOURCES = [
    ("Coaster-Controls.png", "Coaster Controls"),
    ("Slippery-Track.png",   "Slippery Track"),
    ("Boost-Track.png",      "Boost Track"),
    ("Station-Track.png",    "Station Track"),
    ("Sensor-Track.png",     "Sensor Track"),
    ("Brake-Track.png",      "Brake Track"),
    ("Track-Materials.png",  "Track Materials"),
]

# Above this, a pixel is the letter's face; below it, the drop shadow. The two are far apart --
# the face is white or a saturated colour, the shadow is near black -- so the exact value does
# not matter much.
FACE = 110


def face_mask(img):
    """Columns that contain face ink, with the drop shadow excluded."""
    px = img.load()
    out = []
    for x in range(img.width):
        hit = False
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a > 40 and max(r, g, b) >= FACE:
                hit = True
                break
        out.append(hit)
    return out


def runs_of(mask):
    runs = []
    start = None
    for x in range(len(mask) + 1):
        on = mask[x] if x < len(mask) else False
        if on and start is None:
            start = x
        elif not on and start is not None:
            runs.append((start, x))
            start = None
    return runs


def harvest(glyphs, filename, text):
    """Cut one wordmark into glyphs and file them under their letters."""
    path = os.path.join(DL, filename)
    if not os.path.exists(path):
        return None
    img = Image.open(path).convert("RGBA")
    letters = [c for c in text if c != " "]
    found = runs_of(face_mask(img))
    if len(found) != len(letters):
        # Not fatal: a wordmark whose letters touch even without their shadow is simply skipped
        # rather than silently mis-assigned, which would put the wrong glyph under a letter.
        print(f"  {filename:<24}{len(found)} glyphs for {len(letters)} letters -- skipped")
        return None

    for letter, (x0, x1) in zip(letters, found):
        if letter not in glyphs:
            glyphs[letter] = img.crop((x0, 0, x1, img.height))
    print(f"  {filename:<24}{len(found)} glyphs")
    return img, found, text


def pitch(sample):
    """Cell pitch, measured off a source rather than assumed.

    Only pairs of letters that sit next to each other WITHIN a word are measured. A pair either
    side of a space is two cells apart, not one, and averaging those in drags the pitch wide.

    A space is exactly one cell -- that is what monospaced means -- so there is nothing separate
    to measure for it. Deriving it from the visible gap instead was the first attempt and gives a
    nonsense answer, because the gap runs from the END of one glyph's ink to the START of the
    next one's, while the pitch runs left edge to left edge.
    """
    _, found, text = sample
    steps = []
    i = 0
    for a, b in zip(text, text[1:]):
        if a != " ":
            i += 1
        if a != " " and b != " ":
            steps.append(found[i][0] - found[i - 1][0])
    return round(sum(steps) / len(steps))


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    text, out_name = sys.argv[1], sys.argv[2]

    glyphs = {}
    samples = []
    print("harvesting letters:")
    for filename, source_text in SOURCES:
        got = harvest(glyphs, filename, source_text)
        if got:
            samples.append(got)
    if not samples:
        raise SystemExit("no usable wordmarks found in " + DL)

    missing = sorted({c for c in text if c != " "} - set(glyphs))
    if missing:
        raise SystemExit("no glyph for: " + " ".join(missing)
                         + "\nadd a wordmark containing them to SOURCES")

    step = pitch(samples[0])
    height = samples[0][0].height
    print(f"\npitch {step}px, height {height}px")

    # Lay the face out first; the shadow is added underneath afterwards.
    width = 0
    placed = []
    for ch in text:
        if ch == " ":
            width += step          # a space is one cell, like every other character
            continue
        placed.append((width, glyphs[ch]))
        width += step

    face = Image.new("RGBA", (width + 8, height), (0, 0, 0, 0))
    for x, glyph in placed:
        face.alpha_composite(glyph, (x, 0))

    # Same drop shadow the originals carry: one pixel-block down and right, in near black.
    shadow = Image.new("RGBA", face.size, (0, 0, 0, 0))
    alpha = face.split()[3]
    solid = Image.new("RGBA", face.size, (26, 26, 26, 255))
    shadow.paste(solid, (0, 0), alpha)

    out = Image.new("RGBA", (face.width + SHADOW, face.height + SHADOW), (0, 0, 0, 0))
    out.alpha_composite(shadow, (SHADOW, SHADOW))
    out.alpha_composite(face, (0, 0))

    path = os.path.join(DL, out_name)
    out.save(path)
    print(f"{text!r} -> {path}  {out.size[0]}x{out.size[1]}")


# Measured off the originals: the shadow sits this far down and right of the face.
SHADOW = 5


if __name__ == "__main__":
    main()
