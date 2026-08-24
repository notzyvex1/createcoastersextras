"""Rebuilds the rainbow balloon so it is lit like the other 16.

The 16 solid balloons are shaded as spheres: a dark rim, mid-tones across the body, and a
bright low-saturation specular up and to the left. The rainbow one was built the other way
round -- vertical position carries HUE, so its top is dark because it is deep red and its
bottom is dark because it is indigo, not because of where the light is. It has no specular at
all; its brightest pixels are the yellow band, which is a property of yellow rather than of
lighting. Next to the other 16 it reads as a flat sticker.

Method: keep the hue of every pixel exactly where it already is, and take the LUMINANCE and
saturation from a solid balloon, pixel for pixel.

Luminance, specifically -- not HSV's "value". Value is not perceptual: yellow at V=0.9 is more
than twice as bright to the eye as red at V=0.9, so copying V across hues flattens the contrast
instead of transplanting it. The first attempt at this did exactly that and came out flatter
than the original. Copying luma makes the output's brightness map identical to the reference's
by construction, which is the whole point: same lamp, same sphere, different paint.

A consequence worth expecting: the shadowed part of the yellow band goes olive, and the lit
part of the blue band goes pale. That is correct. A real rainbow ball in real light does that;
bands of constant brightness are what made it look flat.

    python tools/fix_rainbow_balloon.py            write the new texture
    python tools/fix_rainbow_balloon.py --dry      report without writing
"""
import colorsys
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS = os.path.join(ROOT, "src", "main", "resources", "assets",
                     "coasters_extras", "textures", "item")

TARGET = "rainbow_balloon.png"
# Red is the reference because it is the most saturated of the 16, so its shading ramp spans
# the widest range and survives being transplanted onto every hue in the rainbow.
REFERENCE = "red_balloon.png"


def luma(r, g, b):
    """Rec. 709 luminance. Green dominates because the eye is most sensitive to it, which is
    exactly the weighting HSV's `value` throws away."""
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def load(name):
    return Image.open(os.path.join(ITEMS, name)).convert("RGBA")


def relight(source, reference, spec_strength=0.85):
    """Applies the reference's light falloff as a multiplier, then its specular as a whiten.

    Two stages because they are two different physical things and one operation cannot do both.

    Shading is multiplicative: a surface turned away from the light reflects a FRACTION of what
    it would face-on, whatever colour it is. So the reference's brightness is used as a ratio
    around its own mean, not as an absolute target. Copying absolute luminance instead was the
    first attempt and it was wrong -- red is a naturally dark hue, so forcing the yellow and
    green bands down to red's brightness turned them olive and the whole sprite went muddy.

    A specular is additive and white: it is the light source reflected off the surface, so it
    is not the surface's colour at all. Multiplication can never produce it -- scaling a
    saturated blue up only gives a brighter blue, never the near-white hotspot the other 16
    balloons have. It is found where the reference loses saturation relative to its own body,
    which is what a highlight physically is, and applied by blending toward white.
    """
    opaque = [(x, y) for y in range(source.height) for x in range(source.width)
              if source.getpixel((x, y))[3] > 0]

    ref_luma = {p: luma(*reference.getpixel(p)[:3]) for p in opaque}
    ref_sat = {p: colorsys.rgb_to_hsv(*[c / 255 for c in reference.getpixel(p)[:3]])[1]
               for p in opaque}
    top_sat = max(ref_sat.values())

    # Separating the string from the specular. Both are pale and desaturated, so saturation
    # alone cannot tell them apart -- treating every desaturated pixel as a highlight blew the
    # string out to near-white, which is what the first version of this did.
    #
    # What actually distinguishes them is topology. A specular is a bright patch ENCLOSED by
    # the balloon; the string runs off to the edge of the sprite. So flooding inward from the
    # transparent border through pale pixels reaches the string and cannot reach the specular.
    pale = {p for p in opaque if ref_sat[p] < 0.5 * top_sat}
    seen, queue = set(), []
    for y in range(source.height):
        for x in range(source.width):
            if source.getpixel((x, y))[3] == 0:
                queue.append((x, y))
                seen.add((x, y))
    while queue:
        x, y = queue.pop()
        for nxt in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if nxt in seen or not (0 <= nxt[0] < source.width
                                   and 0 <= nxt[1] < source.height):
                continue
            if source.getpixel(nxt)[3] == 0 or nxt in pale:
                seen.add(nxt)
                queue.append(nxt)
    string = {p for p in pale if p in seen}
    body = [p for p in opaque if p not in string]

    mean = sum(ref_luma[p] for p in body) / len(body)

    out = Image.new("RGBA", source.size, (0, 0, 0, 0))
    clipped = 0
    for p in opaque:
        src = source.getpixel(p)
        # The string is already right in the original and is not part of the sphere, so it is
        # carried through untouched rather than shaded by the balloon's lighting.
        if p in string:
            out.putpixel(p, src)
            continue

        shade = ref_luma[p] / mean
        channels = [c * shade for c in src[:3]]
        if max(channels) > 255.5:
            clipped += 1

        spec = max(0.0, (top_sat - ref_sat[p]) / top_sat) * spec_strength
        channels = [c + (255 - c) * spec for c in channels]

        out.putpixel(p, (min(255, round(channels[0])), min(255, round(channels[1])),
                         min(255, round(channels[2])), src[3]))
    print(f"  {len(body)} body pixels relit, {len(string)} string pixels left alone")
    return out, clipped


def report(label, img):
    vals = [luma(*img.getpixel((x, y))[:3])
            for y in range(img.height) for x in range(img.width)
            if img.getpixel((x, y))[3] > 0]
    mean = sum(vals) / len(vals)
    # A lit sphere puts its brightest pixel well above its mean. A flat one does not, which
    # makes this ratio the one number that says whether the sprite reads as round.
    print(f"  {label:<12}mean {mean:5.0f}   peak {max(vals):5.0f}   "
          f"floor {min(vals):5.0f}   peak/mean {max(vals) / mean:.2f}")


def main():
    source, reference = load(TARGET), load(REFERENCE)
    out, clipped = relight(source, reference)

    print("brightness structure")
    report("before", source)
    report("after", out)
    report("reference", reference)
    if clipped:
        # Only possible where a hue cannot reach the reference's brightness at that
        # saturation -- a fully saturated blue cannot be as bright as a pale pink.
        print(f"\n  {clipped} pixels hit the 255 ceiling and were clamped")

    if "--dry" in sys.argv:
        print("\n--dry, nothing written")
        return

    backup = os.path.join(ITEMS, TARGET + ".orig")
    if not os.path.exists(backup):
        shutil.copyfile(os.path.join(ITEMS, TARGET), backup)
        print(f"\noriginal kept at {TARGET}.orig")
    out.save(os.path.join(ITEMS, TARGET))
    print(f"wrote {os.path.join(ITEMS, TARGET)}")


if __name__ == "__main__":
    main()
