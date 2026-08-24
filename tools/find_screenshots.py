"""Finds every Minecraft screenshot across all Modrinth App profiles, newest first.

Sorted by date rather than by profile, because what matters when picking gallery images is
which are recent enough to show the current build -- a screenshot from a fortnight ago may show
textures, models or a UI that no longer exist.

Also builds a contact sheet, since a list of timestamps says nothing about what is in them.
"""
import os
from datetime import datetime

from PIL import Image

ROOTS = [
    os.path.join(os.environ["APPDATA"], "ModrinthApp", "profiles"),
    os.path.join(os.environ["APPDATA"], ".minecraft", "screenshots"),
]
OUT = os.path.join(os.path.expanduser("~"), "Downloads", "AllScreenshots-Preview.png")

shots = []
for root in ROOTS:
    if not os.path.isdir(root):
        continue
    if root.endswith("screenshots"):
        for name in os.listdir(root):
            if name.lower().endswith((".png", ".jpg")):
                p = os.path.join(root, name)
                shots.append(("(.minecraft)", p, os.path.getmtime(p)))
        continue
    for profile in os.listdir(root):
        folder = os.path.join(root, profile, "screenshots")
        if not os.path.isdir(folder):
            continue
        for name in os.listdir(folder):
            if name.lower().endswith((".png", ".jpg")):
                p = os.path.join(folder, name)
                shots.append((profile, p, os.path.getmtime(p)))

shots.sort(key=lambda s: s[2], reverse=True)
print(f"{len(shots)} screenshots across all profiles\n")
print(f"{'when':<18}{'profile':<26}{'size':<12}file")
print("-" * 92)
for profile, path, when in shots:
    try:
        w, h = Image.open(path).size
    except OSError:
        w = h = 0
    stamp = datetime.fromtimestamp(when).strftime("%Y-%m-%d %H:%M")
    print(f"{stamp:<18}{profile[:24]:<26}{f'{w}x{h}':<12}{os.path.basename(path)}")

if not shots:
    raise SystemExit

# Contact sheet, newest first, so a gallery choice can be made by looking.
COLS, CELL, LABEL = 4, 320, 30
rows = (min(len(shots), 24) + COLS - 1) // COLS
sheet = Image.new("RGB", (COLS * CELL, rows * (int(CELL * 0.5625) + LABEL)), (26, 27, 32))
from PIL import ImageDraw
draw = ImageDraw.Draw(sheet)
thumb_h = int(CELL * 0.5625)

for i, (profile, path, when) in enumerate(shots[:24]):
    try:
        img = Image.open(path).convert("RGB")
    except OSError:
        continue
    img.thumbnail((CELL, thumb_h))
    x = (i % COLS) * CELL
    y = (i // COLS) * (thumb_h + LABEL)
    sheet.paste(img, (x, y))
    stamp = datetime.fromtimestamp(when).strftime("%m-%d %H:%M")
    draw.text((x + 4, y + thumb_h + 4), f"{i + 1}. {stamp}  {profile[:20]}",
              fill=(200, 202, 212))

sheet.save(OUT)
print(f"\ncontact sheet of the newest {min(len(shots), 24)} -> {OUT}")
