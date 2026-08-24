"""Rebuilds the section headers as [icon] [gap] [text], matching About.png.

The reference is 196x52: a 46px icon at the left, a 28px gap, then the heading in white pixel
type on transparency. Those proportions are what is copied here -- icon 88% of the height, gap
54% -- rather than numbers that merely look close, so the result sits at the same rhythm as the
original.

Icons are the mod's own item textures wherever the mod has one, which is the whole point: a
Boost Track heading marked with the actual Boost Track item says something an emoji never can.
Five headings have no matching item because the block is a 3D model with no flat icon, or the
concept is not an item at all; those borrow from Create, which this mod is an addon for and
already credits. Nothing is invented and nothing decorative is used where a real item exists.

"The Functional Tracks" gets all five tracks in a row rather than one of them, because it
introduces the set -- and using the Boost Track there would repeat the icon of the very next
heading.

    python tools/build_icon_headers.py
    python tools/build_icon_headers.py --preview     write a contact sheet, change nothing
"""
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(ROOT, "src", "main", "resources", "assets",
                   "coasters_extras", "textures", "item")
CREATE = (r"C:\Users\Zohan\OneDrive\Documents"
          r"\create-1.21.1-6.0.10.jar_Decompiler.com\assets\create\textures")
DL = os.path.join(os.path.expanduser("~"), "Downloads")
OUT = os.path.join(DL, "CoastersExtras-Description")

# Proportions read off About.png (196x52, icon 0-45, text starts at 74).
HEIGHT = 96
ICON = round(HEIGHT * 0.88)
GAP = round(HEIGHT * 0.54)
WIDTH = 1280

# Four, not all five. Five forces a 3+2 layout at 28px a tile, which at the size a heading
# actually renders is unreadable mush. Four tiles at 42px in a 2x2 stay legible and still read
# as "a set of tracks", which is all this icon has to say.
FOUR = ["boost_track", "brake_track", "station_track", "sensor_track"]

# source header file -> (icon spec, output name)
# A string is a mod item; "create:x" is a Create item; "create:block/x" a Create block
# texture; a list is composited side by side.
HEADERS = [
    ("At-a-Glance",              "create:goggles",           "at-a-glance-header.png"),
    ("The-Functional-Tracks",    FOUR,                       "the-functional-tracks-header.png"),
    # The riveted cap face off Create's fluid tank. Picked over the four-track composite because
    # a 2x2 of shrunken track items is mush at heading size, while this reads as one clean plate
    # -- and it says "machinery" without naming any single track, which is what a section that
    # introduces the whole set should do.
    ("The-Functional-Tracks",    "create:block/fluid_tank_top",
                                                             "functional-tracks-plate-header.png"),
    ("Boost-Track",              "boost_track",              "boost-track-header.png"),
    ("Brake-Track",              "brake_track",              "brake-track-header.png"),
    ("Station-Track",            "station_track",            "station-track-header.png"),
    ("Sensor-Track",             "sensor_track",             "sensor-track-header.png"),
    ("Slippery-Track",           "slippery_track",           "slippery-track-header.png"),
    # The Rainbow Track's wordmark is itself swept through the spectrum by
    # tools/build_rainbow_wordmark.py, so this heading is the only one where the type carries
    # the meaning rather than just naming it.
    #
    # The track alone. It had the rainbow balloon beside it, on the reasoning that the heading
    # was the one place the two halves of the rainbow set met -- but the section is about the
    # track, and a second item next to it reads as "these two things", which is a claim the
    # heading should not be making.
    ("Rainbow-Track",            "rainbow_track",            "rainbow-track-header.png"),
    ("Copycat-Track",            "copycat_track",            "copycat-track-header.png"),
    # The Sensor Block is a Blockbench model with no flat item icon; contact_side is the
    # texture its own model uses for the sensing face, so this is the block's real material.
    ("The-Sensor-Block",         "create:block/contact_side", "the-sensor-block-header.png"),
    # Create's linked_controller and wrench both came out of that decompile broken -- the
    # wrench file is a wooden door texture and the controller is a misaligned sprite. Checked
    # by rendering them large rather than trusting the filenames.
    ("Coaster-Controls",         "create:brass_hand",        "coaster-controls-header.png"),
    ("Balloons",                  "rainbow_balloon",          "balloons-header.png"),
    ("Track-Materials",          "oak_track",                "track-materials-header.png"),
    ("Building-Your-First-Ride", "tape_measure",             "building-your-first-ride-header.png"),
    ("Crafting",                 "rivet",                    "crafting-header.png"),
    ("FAQ",                      "create:clipboard",         "faq-header.png"),
    ("License",                  "create:schematic",         "license-header.png"),
]


def load_icon(spec):
    if spec.startswith("create:block/"):
        path = os.path.join(CREATE, "block", spec.split("/", 1)[1] + ".png")
    elif spec.startswith("create:"):
        path = os.path.join(CREATE, "item", spec.split(":", 1)[1] + ".png")
    else:
        path = os.path.join(MOD, spec + ".png")
    if not os.path.exists(path):
        return None
    img = Image.open(path).convert("RGBA")
    # Animated textures are a vertical strip of frames; frame 0 is the icon.
    if img.height > img.width and img.height % img.width == 0:
        img = img.crop((0, 0, img.width, img.width))
    return img


def build_icon(spec):
    """One square icon, or several composited into the same square footprint."""
    if isinstance(spec, str):
        img = load_icon(spec)
        return img.resize((ICON, ICON), Image.NEAREST) if img else None

    loaded = [load_icon(s) for s in spec]
    loaded = [i for i in loaded if i]
    if not loaded:
        return None
    # Square-ish grid, so the group keeps the same footprint as a single icon and every
    # heading's text still starts at the same x.
    per_row = math.ceil(math.sqrt(len(loaded)))
    cell = ICON // per_row
    rows = math.ceil(len(loaded) / per_row)
    canvas = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    for i, img in enumerate(loaded):
        x = (i % per_row) * cell
        y = (ICON - rows * cell) // 2 + (i // per_row) * cell
        canvas.paste(img.resize((cell, cell), Image.NEAREST), (x, y))
    return canvas


def main():
    os.makedirs(OUT, exist_ok=True)
    made, missing = [], []
    previews = []

    print(f"canvas {WIDTH}x{HEIGHT}, icon {ICON}px, gap {GAP}px "
          f"(About.png proportions)\n")
    print(f"{'heading':<28}{'icon':<30}status")
    print("-" * 74)

    for source, spec, out_name in HEADERS:
        text_path = os.path.join(DL, source + ".png")
        if not os.path.exists(text_path):
            missing.append(f"{source}.png (your text image)")
            print(f"{source:<28}{'':<30}NO TEXT IMAGE")
            continue

        icon = build_icon(spec)
        label = " + ".join(spec) if isinstance(spec, list) else spec
        if icon is None:
            missing.append(f"icon for {source}: {label}")
            print(f"{source:<28}{label[:28]:<30}ICON NOT FOUND")
            continue

        text = Image.open(text_path).convert("RGBA")
        # Scale the text down only if icon + gap + text would overflow the canvas.
        room = WIDTH - ICON - GAP
        if text.width > room:
            scale = room / text.width
            text = text.resize((room, max(1, round(text.height * scale))), Image.LANCZOS)

        canvas = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        canvas.alpha_composite(icon, (0, (HEIGHT - ICON) // 2))
        canvas.alpha_composite(text, (ICON + GAP, (HEIGHT - text.height) // 2))
        canvas.save(os.path.join(OUT, out_name))
        made.append(out_name)
        previews.append(canvas)
        print(f"{source:<28}{label[:28]:<30}ok")

    if previews:
        sheet = Image.new("RGBA", (WIDTH, HEIGHT * len(previews)), (30, 31, 34, 255))
        for i, img in enumerate(previews):
            sheet.alpha_composite(img, (0, i * HEIGHT))
        sheet.save(os.path.join(os.path.dirname(OUT), "header_preview.png"))
        print(f"\npreview -> {os.path.join(os.path.dirname(OUT), 'header_preview.png')}")

    print(f"\n{len(made)} headers rebuilt into {OUT}")
    if missing:
        print(f"{len(missing)} problems:")
        for m in missing:
            print(f"    {m}")


if __name__ == "__main__":
    main()

