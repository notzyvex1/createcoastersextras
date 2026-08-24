"""Adds inline icons from the two icon packs to the CurseForge description.

Icons are chosen by what they depict, not by what their filename says -- these are emoji.gg
uploads whose names are upload IDs plus a loose label, so "74240-minecraft-blue-3" tells you
nothing useful. Each mapping below was picked off a contact sheet of all 37.

They go on the At a Glance bullets and nowhere else. The functional-track headings already
carry their emoji baked into the header art, so an icon there would be a second copy of the
same symbol, and icons scattered through body text turn a spec list into a ransom note. One
consistent column of icons down the feature list is the whole effect.

Everything is normalised to a single height. The packs mix 48px, 64px and 128px sources, and
CurseForge renders each at its natural size, so unnormalised icons would stagger the line
height of every bullet they sit on.

    python tools/add_icons.py
"""
import os
import shutil

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = (r"C:\Users\Zohan\AppData\Local\Temp\claude\D--DEADLINE-SMP"
        r"\53b1c5c4-fd10-4e7b-8c38-b731994317a4\scratchpad\emojis")
OUT = os.path.join(os.path.expanduser("~"), "Downloads", "CoastersExtras-Description")
DESC = os.path.join(ROOT, "CURSEFORGE-IMAGES.md")

SIZE = 48

# Text that starts the bullet -> (source icon, exported name, why)
ICONS = [
    ("**55 coaster tracks**", "88103-minecraftcube.gif", "icon-tracks.png",
     "a block, for the material count"),
    ("**16 balloons**", "81536-rgb-shulker.gif", "icon-balloons.png",
     "the only rainbow thing in either pack, and rainbow is the signature balloon"),
    ("**Sensor Block**", "20546-end-crystal.png", "icon-sensor.png",
     "reads as a powered detector rather than a decoration"),
    ("**Coaster Controls**", "87008-hardcore.png", "icon-controls.png",
     "placeholder: no pack icon depicts a lever or seat"),
    ("**6 Ponder scenes**", "24967-enchanted-book-minecraft.png", "icon-ponder.png",
     "a book, for the in-game documentation"),
    ("**Survival ready**", "1808-iron.png", "icon-survival.png",
     "an ingot, for craftability"),
]

# The cart is the single best fit in either pack for a coaster mod, so it leads the page.
HERO = ("86744-purpletrolley.png", "icon-cart.png")


def export(source, out_name):
    path = os.path.join(WORK, source)
    if not os.path.exists(path):
        return None
    img = Image.open(path).convert("RGBA")
    ratio = SIZE / img.height
    img = img.resize((max(1, round(img.width * ratio)), SIZE), Image.NEAREST)
    img.save(os.path.join(OUT, out_name))
    return f"{img.width}x{img.height}"


def main():
    if not os.path.isdir(WORK):
        print(f"Icon packs not extracted yet: {WORK}")
        return
    os.makedirs(OUT, exist_ok=True)

    print(f"{'bullet':<26}{'icon file':<22}{'size':<10}why")
    print("-" * 96)
    exported = []
    for bullet, source, out_name, why in ICONS:
        size = export(source, out_name)
        if size:
            exported.append((bullet, out_name))
            print(f"{bullet.strip('*'):<26}{out_name:<22}{size:<10}{why}")
        else:
            print(f"{bullet.strip('*'):<26}{'MISSING':<22}{'':<10}{source}")
    hero_size = export(*HERO)
    if hero_size:
        print(f"{'(page lead)':<26}{HERO[1]:<22}{hero_size:<10}a minecart -- the best fit "
              f"in either pack")

    text = open(DESC, encoding="utf8").read()
    base = "PASTE_YOUR_IMAGE_URL_BASE_HERE"
    added = 0
    for bullet, out_name in exported:
        needle = f"- {bullet}"
        if needle in text and out_name not in text:
            # Alt text left empty on purpose. These are decorative -- the bullet already says
            # "16 balloons", and a screen reader announcing "balloons image, 16 balloons"
            # reads every line twice.
            text = text.replace(needle, f"- ![]({base}/{out_name}) {bullet}", 1)
            added += 1
    open(DESC, "w", encoding="utf8", newline="\n").write(text)
    print(f"\n{added} icons placed in {os.path.basename(DESC)}")
    for name in ("icon-cart.png",):
        print(f"{name} exported but not placed -- drop it beside the title if you want it")


if __name__ == "__main__":
    main()
