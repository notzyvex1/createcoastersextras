"""Builds every image the CurseForge description needs, into one upload-ready folder.

Two jobs.

1. NORMALISE THE HEADERS. The 15 hand-made headers are between 170px and 1088px wide. That
   matters because CurseForge scales a description image down to the column width and leaves
   narrow ones alone -- so "Building Your First Ride" at 1088 gets shrunk, "FAQ" at 170 does
   not, and the same font ends up rendering at wildly different sizes down the page. Padding
   them all onto one canvas width means they all scale by the same factor, so the lettering
   is finally consistent. The text is left-aligned rather than centred so every heading starts
   on the same vertical line, which is what makes a long page read as one document.

2. BUILD THE ITEM STRIPS from the mod's own textures, in the style of the sample: a row of
   sprites at a common size on a transparent background. Generated rather than screenshotted,
   so they stay correct when a texture changes -- the rainbow balloon was just relit, and a
   screenshot taken yesterday would now be a picture of the old one.

    python tools/build_description_assets.py
"""
import os
import shutil

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS = os.path.join(ROOT, "src", "main", "resources", "assets",
                     "coasters_extras", "textures", "item")
DL = os.path.join(os.path.expanduser("~"), "Downloads")
OUT = os.path.join(DL, "CoastersExtras-Description")

# In page order, with the filename the user exported and the heading it belongs to.
HEADERS = [
    ("At-a-Glance", "At a Glance"),
    ("The-Functional-Tracks", "The Functional Tracks"),
    ("Boost-Track", "Boost Track"),
    ("Brake-Track", "Brake Track"),
    ("Station-Track", "Station Track"),
    ("Sensor-Track", "Sensor Track"),
    ("Slippery-Track", "Slippery Track"),
    ("The-Sensor-Block", "The Sensor Block"),
    ("Coaster-Controls", "Coaster Controls"),
    ("Ballons", "Balloons"),
    ("Track-Materials", "Track Materials"),
    ("Building-Your-First-Ride", "Building Your First Ride"),
    ("Crafting", "Crafting"),
    ("FAQ", "FAQ"),
    ("License", "License"),
]

CANVAS_W = 1120
# Sized to match the sample showcase: big sprites, generous spacing, one clean row.
SPRITE = 112
GAP = 16


def normalise_headers():
    made, missing = [], []
    for source_name, heading in HEADERS:
        path = os.path.join(DL, source_name + ".png")
        if not os.path.exists(path):
            missing.append(source_name)
            continue
        img = Image.open(path).convert("RGBA")
        canvas = Image.new("RGBA", (CANVAS_W, max(96, img.height + 8)), (0, 0, 0, 0))
        canvas.paste(img, (0, (canvas.height - img.height) // 2), img)
        # Named after the heading, not the source file, so "Ballons.png" stops being the
        # filename for the Balloons section -- the typo is only in the file name, and it is
        # the one thing here that would be visible if anyone looked at the image URL.
        out_name = heading.lower().replace(" ", "-") + "-header.png"
        canvas.save(os.path.join(OUT, out_name))
        made.append((out_name, f"{img.width}x{img.height}", f"{CANVAS_W}x{canvas.height}"))
    return made, missing


def strip(names, out_name, per_row=None):
    """A row of sprites at a common size on transparent background."""
    loaded = []
    for name in names:
        path = os.path.join(ITEMS, name + ".png")
        if os.path.exists(path):
            loaded.append(Image.open(path).convert("RGBA"))
    if not loaded:
        return None

    per_row = per_row or len(loaded)
    rows = (len(loaded) + per_row - 1) // per_row
    canvas = Image.new("RGBA",
                       (per_row * (SPRITE + GAP) + GAP, rows * (SPRITE + GAP) + GAP),
                       (0, 0, 0, 0))
    for i, img in enumerate(loaded):
        x = GAP + (i % per_row) * (SPRITE + GAP)
        y = GAP + (i // per_row) * (SPRITE + GAP)
        canvas.paste(img.resize((SPRITE, SPRITE), Image.NEAREST), (x, y))
    canvas.save(os.path.join(OUT, out_name))
    return out_name, len(loaded), f"{canvas.width}x{canvas.height}"


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)

    made, missing = normalise_headers()
    print(f"headers normalised to {CANVAS_W}px wide\n")
    print(f"{'file':<38}{'was':<12}now")
    print("-" * 66)
    for name, was, now in made:
        print(f"{name:<38}{was:<12}{now}")
    if missing:
        print(f"\nmissing: {', '.join(missing)}")

    every = sorted(f[:-4] for f in os.listdir(ITEMS) if f.endswith(".png"))

    print("\nitem strips")
    print("-" * 66)
    # Spectrum order, not alphabetical. Sorting the dye colours by name puts black next to
    # blue next to brown and the row reads as noise; running them round the colour wheel and
    # ending on the greys is what makes a swatch row look deliberate.
    spectrum = ["rainbow", "red", "orange", "yellow", "lime", "green", "cyan",
                "light_blue", "blue", "purple", "magenta", "pink", "brown",
                "white", "light_gray", "gray", "black"]
    jobs = [
        (["boost_track", "brake_track", "station_track", "sensor_track",
          "slippery_track"], "strip-functional-tracks.png", None),
        ([c + "_balloon" for c in spectrum], "strip-balloons.png", 17),
        ([n for n in every if n.endswith("_track")
          and not any(n.startswith(f) for f in
                      ("boost", "brake", "station", "sensor", "slippery"))],
         "strip-track-materials.png", 20),
    ]
    # The sample showcase is on white, but these are left transparent on purpose: CurseForge
    # renders descriptions in both a light and a dark theme, and a white-backed PNG becomes a
    # glaring white slab on the dark one. Transparent looks identical to the sample on light
    # and correct on dark.
    for names, out_name, per_row in jobs:
        result = strip(names, out_name, per_row)
        if result:
            print(f"{result[0]:<38}{result[1]:>3} items   {result[2]}")

    print(f"\n{len(os.listdir(OUT))} files in {OUT}")


if __name__ == "__main__":
    main()
