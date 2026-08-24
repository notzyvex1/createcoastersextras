"""Adds a linked social icon bar to the top and bottom of the CurseForge description.

Uses the 128x set from the pack. Not the 512x downscaled and not the 16x upscaled: these are
pixel art, so resampling in either direction destroys the hard pixel edges that make them match
the pixel-font headers. 128x is close enough to the display size to be used as-is.

The bar is repeated at the top and in Credits on purpose. The top one catches people deciding
whether to download; the bottom one catches people who read the whole page and now want the
Discord -- and Credits already ends with "come say so on Discord" with no link to say it on.

URLs that are not known are left as an obvious placeholder rather than guessed. A social icon
linking to a 404 is worse than no icon: it looks like the project is abandoned.

    python tools/add_socials.py
    python tools/add_socials.py --discord <url> --modrinth <url> --youtube <url>
"""
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = (r"C:\Users\Zohan\AppData\Local\Temp\claude\D--DEADLINE-SMP"
        r"\53b1c5c4-fd10-4e7b-8c38-b731994317a4\scratchpad\social\128x")
OUT = os.path.join(os.path.expanduser("~"), "Downloads", "CoastersExtras-Description")
DESC = os.path.join(ROOT, "CURSEFORGE-IMAGES.md")
BASE = "PASTE_YOUR_IMAGE_URL_BASE_HERE"

FILL_IN = "PUT_YOUR_URL_HERE"

# label, source file in the pack, exported name, default URL
# "cruseforge" is the pack's own spelling; it is their typo, not one to fix on disk.
#
# The _bg variants -- the boxed ones. Each sits on its own filled square, so every icon in the
# row occupies an identical rectangle. The plain marks do not: GitHub's cat is tall, YouTube's
# play button is wide and short, and a row of them sits at visibly different heights with
# ragged gaps. The box also gives each mark its own background, which matters because
# CurseForge renders the page in both a light and a dark theme and GitHub's white cat would
# vanish on the light one.
SOCIALS = [
    ("Modrinth",   "modrinth_icon_bg.png",   "social-modrinth.png",   FILL_IN),
    ("CurseForge", "cruseforge_icon_bg.png", "social-curseforge.png", FILL_IN),
    ("GitHub",     "github_icon_bg.png",     "social-github.png",
     "https://github.com/notzyvex1/create-coasters-extras"),
    ("Discord",    "discord_icon_bg.png",    "social-discord.png",    FILL_IN),
    ("YouTube",    "youtube_icon_bg.png",    "social-youtube.png",    FILL_IN),
    ("TikTok",     "tiktok_icon_bg.png",     "social-tiktok.png",     FILL_IN),
    ("X",          "xtwitter_icon_bg.png",   "social-x.png",          FILL_IN),
    ("Ko-fi",      "kofi_icon_bg.png",       "social-kofi.png",       FILL_IN),
]

MARK_TOP = "<!-- socials-top -->"
MARK_END = "<!-- socials-credits -->"


def main():
    urls = {label.lower(): default for label, _, _, default in SOCIALS}
    for i, arg in enumerate(sys.argv):
        key = arg.lstrip("-").lower()
        if key in urls and i + 1 < len(sys.argv):
            urls[key] = sys.argv[i + 1]

    if not os.path.isdir(PACK):
        print(f"Icon pack not extracted: {PACK}")
        return
    os.makedirs(OUT, exist_ok=True)

    print(f"{'service':<13}{'file':<24}{'size':<10}link")
    print("-" * 92)
    bar = []
    for label, source, out_name, _ in SOCIALS:
        path = os.path.join(PACK, source)
        if not os.path.exists(path):
            print(f"{label:<13}{'MISSING':<24}{'':<10}{source}")
            continue
        img = Image.open(path).convert("RGBA")
        img.save(os.path.join(OUT, out_name))
        url = urls[label.lower()]
        # Alt text is the service name, so a reader with images off still sees a usable row
        # of link names rather than eight empty brackets.
        bar.append(f"[![{label}]({BASE}/{out_name})]({url})")
        shown = "needs your URL" if url == FILL_IN else url
        print(f"{label:<13}{out_name:<24}{f'{img.width}x{img.height}':<10}{shown}")

    row = " ".join(bar)
    text = open(DESC, encoding="utf8").read()

    if MARK_TOP in text:
        print("\nBar already present -- rewriting it in place.")
        head, _, rest = text.partition(MARK_TOP)
        text = head + MARK_TOP + "\n" + row + "\n" + rest.split("\n", 2)[2]
    else:
        # Under the tagline, not above the banner: the banner is what the page opens with.
        lines = text.split("\n")
        at = next((i for i, line in enumerate(lines)
                   if line.startswith("**[Create: Coasters Simulated]")), 2)
        lines.insert(at + 1, f"\n{MARK_TOP}\n{row}")
        text = "\n".join(lines)

    if MARK_END not in text:
        text = text.rstrip() + f"\n\n{MARK_END}\n{row}\n"

    open(DESC, "w", encoding="utf8", newline="\n").write(text)
    shutil.copyfile(DESC, os.path.join(OUT, os.path.basename(DESC)))

    missing = [label for label, _, _, _ in SOCIALS if urls[label.lower()] == FILL_IN]
    print(f"\n{len(bar)} icons, bar placed at top and in Credits")
    if missing:
        print(f"\n{len(missing)} still need URLs: {', '.join(missing)}")
        print("  python tools/add_socials.py --discord <url> --modrinth <url> ...")


if __name__ == "__main__":
    main()
