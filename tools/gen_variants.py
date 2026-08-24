"""Generates a whole family of track variants: textures, models, blockstates, recipes, lang, loot.

Thirty-two variants is roughly thirteen hundred files. Copying them by hand is not a plan, and
neither is drawing thirty-two texture sets -- so each one is recoloured from a single neutral
source, the same way the balloons were.

The recolour maps the source's BRIGHTNESS onto a ramp built from the target colour, rather than
shifting hue. Hue-shifting a grey gives grey, and tinting by multiply crushes the highlights;
mapping luminance through a ramp keeps every rivet, bolt and shadow the base texture had and
simply restates it in the new material.

Target colours are the real averages of vanilla's own wool and concrete textures, read out of
the client jar, so a Blue Wool Coaster Track actually matches blue wool.

Run from the repo root:  python tools_gen_variants.py
"""
import json
import os
import shutil

from PIL import Image

RES = "src/main/resources"
ASSETS = f"{RES}/assets/coasters_extras"
DATA = f"{RES}/data/coasters_extras"
TEX = f"{ASSETS}/textures/block/track"
MODELS = f"{ASSETS}/models/block/track"

# Neutral, so nothing of its own bleeds into the result.
SOURCE = "stone_track"

# Create is split-licensed: its code is MIT, but everything under assets/ is All Rights
# Reserved. These four came out of Create's jar, and copying or recolouring them would be
# redistributing work we have no licence to redistribute. Models point at
# `create:block/standard_track_crossing` instead, which loads from Create's own jar at
# runtime -- referencing is not redistribution. The other three were never referenced at all.
CREATE_OWNED = {
    "standard_track_crossing.png",
    "andesite_cut_polished.png",
    "portal_track.png",
    "portal_track_mip.png",
}

# How strongly the material's own surface is stamped into the track. High enough that wool
# reads as woven, and harmless on concrete because concrete has almost no variation to stamp.
DETAIL_GAIN = 2.4

# How the track's own shading is applied over the material. The floor keeps deep shadow from
# going pure black, and the range lets a lit edge brighten past the material's own tone.
SHADE_FLOOR = 0.42
SHADE_RANGE = 1.15

DYES = ["white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
        "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"]

FAMILIES = {
    "wool":     {"suffix": "wool",     "label": "Wool",     "ingredient": "minecraft:{dye}_wool"},
    "concrete": {"suffix": "concrete", "label": "Concrete", "ingredient": "minecraft:{dye}_concrete"},
}

PRETTY = {"light_gray": "Light Gray", "light_blue": "Light Blue"}


def pretty(dye):
    return PRETTY.get(dye, dye.replace("_", " ").title())


def average(path):
    im = Image.open(path).convert("RGB")
    px = list(im.getdata())
    n = len(px)
    return tuple(sum(c[i] for c in px) // n for i in range(3))


def ramp(colour):
    """A 256-entry brightness ramp from near-black through the colour to a light tint."""
    r, g, b = colour
    out = []
    for i in range(256):
        t = i / 255.0
        if t < 0.5:                      # shadows: toward black
            k = t / 0.5
            out.append((int(r * 0.28 * (1 - k) + r * k),
                        int(g * 0.28 * (1 - k) + g * k),
                        int(b * 0.28 * (1 - k) + b * k)))
        else:                            # highlights: toward white, but never all the way
            k = (t - 0.5) / 0.5
            out.append((int(r + (255 - r) * 0.72 * k),
                        int(g + (255 - g) * 0.72 * k),
                        int(b + (255 - b) * 0.72 * k)))
    return out


def surface(material_png):
    """The material's actual pixels, to build the track out of.

    Tinting the metal track and adding noise gave something the colour of wool that still
    read as metal. This uses the vanilla block itself as the surface instead, so a wool track
    is genuinely made of wool weave and a concrete track of flat concrete.

    The track's own brightness is kept as SHADING over the top -- its rivets, its dark gaps,
    the lit top edge of a rail -- so the shape still reads while the material underneath is
    real. Which is how the block would look if it were carved out of that material.
    """
    im = Image.open(material_png).convert("RGB")
    return im.load(), im.width, im.height


def detail_map(material_png):
    """The material's own surface, as a per-pixel brightness offset.

    This is what tells wool from concrete. Their average colours are only 29 apart -- closer
    than concrete is to concrete powder -- so tinting a shared source produced two families
    that looked identical, which is exactly what happened. What actually separates them is
    surface: wool's luminance varies by 6-17 across its weave, concrete's by less than 1.

    So the offset is read out of the vanilla block itself rather than invented. Wool brings
    its fibre, concrete brings nothing, and the difference falls out of the source material
    instead of being hardcoded.
    """
    im = Image.open(material_png).convert("RGB")
    px = im.load()
    lum = [[(px[x, y][0] * 299 + px[x, y][1] * 587 + px[x, y][2] * 114) // 1000
            for x in range(im.width)] for y in range(im.height)]
    mean = sum(sum(r) for r in lum) / (im.width * im.height)
    return [[(v - mean) * DETAIL_GAIN for v in row] for row in lum], im.width, im.height


def recolour(src, dst, lut, surf=None):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r * 299 + g * 587 + b * 114) // 1000
            if surf is not None:
                mat, mw, mh = surf
                mr, mg, mb = mat[x % mw, y % mh]
                # The source's brightness becomes a light/shadow multiplier rather than an
                # index into a ramp, so the material comes through as itself and the track's
                # own form is what is painted onto it.
                shade = SHADE_FLOOR + SHADE_RANGE * (lum / 255.0)
                px[x, y] = (min(255, int(mr * shade)),
                            min(255, int(mg * shade)),
                            min(255, int(mb * shade)), a)
                continue
            nr, ng, nb = lut[lum]
            px[x, y] = (nr, ng, nb, a)
    im.save(dst)


def variant(name, colour, family, dye, material_png):
    """Everything one variant needs."""
    lut = ramp(colour)
    surf = surface(material_png)

    os.makedirs(f"{TEX}/{name}_track", exist_ok=True)
    for f in os.listdir(f"{TEX}/{SOURCE}"):
        if f.endswith(".png") and f not in CREATE_OWNED:
            recolour(f"{TEX}/{SOURCE}/{f}", f"{TEX}/{name}_track/{f}", lut, surf)

    # models are pure geometry: copy and repoint at our own texture folder
    os.makedirs(f"{MODELS}/{name}_track", exist_ok=True)
    for f in os.listdir(f"{MODELS}/{SOURCE}"):
        s = f"{MODELS}/{SOURCE}/{f}"
        d = f"{MODELS}/{name}_track/{f}"
        if f.endswith((".json", ".mtl")):
            text = open(s, encoding="utf8").read().replace(SOURCE, f"{name}_track")
            open(d, "w", encoding="utf8", newline="\n").write(text)
        else:
            shutil.copyfile(s, d)

    item_src = f"{ASSETS}/textures/item/{SOURCE}.png"
    if os.path.exists(item_src):
        recolour(item_src, f"{ASSETS}/textures/item/{name}_track.png", lut, surf)

    for path, body in (
        (f"{ASSETS}/models/item/{name}_track.json",
         {"parent": "minecraft:item/generated",
          "textures": {"layer0": f"coasters_extras:item/{name}_track"}}),
        (f"{ASSETS}/blockstates/{name}_track_material.json",
         {"variants": {"": {"model": f"coasters_extras:block/{name}_track_material"}}}),
        (f"{ASSETS}/models/block/{name}_track_material.json",
         {"parent": f"coasters_extras:block/track/{name}_track/segment_left"}),
        (f"{DATA}/recipe/{name}_track.json",
         {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": [{"tag": "coasters_extras:coaster_tracks"},
                          {"item": family["ingredient"].format(dye=dye)}],
          "result": {"count": 1, "id": f"coasters_extras:{name}_track"}}),
        (f"{DATA}/loot_table/blocks/{name}_track_material.json",
         {"type": "minecraft:block",
          "pools": [{"rolls": 1,
                     "entries": [{"type": "minecraft:item",
                                  "name": f"coasters_extras:{name}_track"}],
                     "conditions": [{"condition": "minecraft:survives_explosion"}]}]}),
    ):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf8", newline="\n") as f:
            json.dump(body, f, indent=2)
            f.write("\n")


def main():
    lang_path = f"{ASSETS}/lang/en_us.json"
    lang = json.load(open(lang_path, encoding="utf8"))

    made = []
    for key, family in FAMILIES.items():
        for dye in DYES:
            name = f"{dye}_{family['suffix']}"
            material = f"build/vanilla/{dye}_{key}.png"
            colour = average(material)
            variant(name, colour, family, dye, material)
            title = f"{pretty(dye)} {family['label']} Coaster Track"
            lang[f"block.coasters_extras.{name}_track_material"] = title
            lang[f"item.coasters_extras.{name}_track"] = title
            made.append((name, colour))

    with open(lang_path, "w", encoding="utf8", newline="\n") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"generated {len(made)} variants")
    for n, c in made[:4]:
        print(f"   {n:22s} #{c[0]:02X}{c[1]:02X}{c[2]:02X}")
    print("   ...")
    print("\nenum entries to add to TrackVariant:")
    for key, family in FAMILIES.items():
        for dye in DYES:
            name = f"{dye}_{family['suffix']}"
            const = name.upper()
            print(f'    {const:22s}("{name}", "{pretty(dye)} {family["label"]}"),')


if __name__ == "__main__":
    main()
