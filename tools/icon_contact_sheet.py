"""Builds one labelled contact sheet of every icon from all three packs.

Grouped by pack and captioned with the filename, because the whole difficulty with these packs
is that the names are upload IDs -- "74240-minecraft-blue-3" tells you nothing about what the
picture is, so choosing one means either opening 37 files or having them all in front of you.

Sized so each icon is legible at 100%: 96px cells with the label under each, rather than a
dense grid that has to be zoomed to be useful.
"""
import os
import zipfile

from PIL import Image, ImageDraw

DL = os.path.join(os.path.expanduser("~"), "Downloads")
WORK = (r"C:\Users\Zohan\AppData\Local\Temp\claude\D--DEADLINE-SMP"
        r"\53b1c5c4-fd10-4e7b-8c38-b731994317a4\scratchpad\allicons")
OUT = os.path.join(DL, "IconPacks-Preview.png")

PACKS = [
    ("Minecraft emoji (stoner pack)", "563832-stoner-emojigg-pack.zip", None),
    ("Nexor stickers", "882664-nexor-studios-stickers-emojigg-pack.zip", None),
    # Only the boxed 128px variants of the social pack: the same 47 marks exist at three sizes
    # and in plain form, and showing all four would be 200 near-identical tiles.
    ("Social icons (boxed, 128px)", "Social Icons Pack PNG.zip", "128x/_bg"),
]

CELL, LABEL, COLS, PAD = 96, 26, 10, 10
BG = (28, 28, 34, 255)
HEADER = 34


def extract(zip_name):
    path = os.path.join(DL, zip_name)
    if not os.path.exists(path):
        return []
    target = os.path.join(WORK, zip_name[:-4])
    os.makedirs(target, exist_ok=True)
    out = []
    with zipfile.ZipFile(path) as z:
        for entry in z.namelist():
            if entry.endswith("/") or not entry.lower().endswith((".png", ".gif")):
                continue
            dest = os.path.join(target, entry.replace("/", "_"))
            if not os.path.exists(dest):
                with open(dest, "wb") as fh:
                    fh.write(z.read(entry))
            out.append((entry, dest))
    return out


groups = []
for title, zip_name, want in PACKS:
    files = extract(zip_name)
    if want:
        folder, suffix = want.split("/")
        files = [(e, p) for e, p in files
                 if e.startswith(folder + "/") and suffix in os.path.basename(e)]
    # Deduplicate by basename: several packs ship the same mark twice.
    seen, unique = set(), []
    for entry, path in sorted(files, key=lambda t: os.path.basename(t[0]).lower()):
        base = os.path.basename(entry)
        if base in seen:
            continue
        seen.add(base)
        unique.append((base, path))
    if unique:
        groups.append((title, unique))
        print(f"{title:<34}{len(unique)} icons")

height = PAD
for _, items in groups:
    rows = (len(items) + COLS - 1) // COLS
    height += HEADER + rows * (CELL + LABEL) + PAD
width = COLS * CELL + PAD * 2

sheet = Image.new("RGBA", (width, height), BG)
draw = ImageDraw.Draw(sheet)

y = PAD
for title, items in groups:
    draw.rectangle([PAD, y, width - PAD, y + HEADER - 8], fill=(44, 46, 54, 255))
    draw.text((PAD + 8, y + 8), f"{title}   ({len(items)})", fill=(235, 235, 245, 255))
    y += HEADER

    for i, (name, path) in enumerate(items):
        try:
            img = Image.open(path).convert("RGBA")
        except OSError:
            continue
        img.thumbnail((CELL - 16, CELL - 16), Image.NEAREST)
        cx = PAD + (i % COLS) * CELL
        cy = y + (i // COLS) * (CELL + LABEL)
        # A faint tile behind each icon, so pale marks are visible against the dark ground.
        draw.rectangle([cx + 3, cy + 3, cx + CELL - 5, cy + CELL - 5],
                       fill=(38, 39, 46, 255))
        sheet.alpha_composite(img, (cx + (CELL - img.width) // 2,
                                    cy + (CELL - img.height) // 2))
        label = name.rsplit(".", 1)[0]
        label = label.split("-", 1)[-1] if label[:1].isdigit() else label
        draw.text((cx + 4, cy + CELL - 2), label[:15], fill=(170, 172, 182, 255))

    rows = (len(items) + COLS - 1) // COLS
    y += rows * (CELL + LABEL) + PAD

sheet.save(OUT)
print(f"\n{sum(len(i) for _, i in groups)} icons across {len(groups)} packs")
print(f"{sheet.width}x{sheet.height} -> {OUT}")
