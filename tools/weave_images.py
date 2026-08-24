"""Rewrites CURSEFORGE.md with the header images in place of text headings.

Each `## Heading` becomes the matching header image, and the three showcase strips are
inserted under the sections they illustrate.

The heading text is kept as the image's alt text rather than thrown away. That is not a
detail: CurseForge readers on slow connections, anyone with images blocked, and every search
engine indexing the page see the alt text and nothing else. A page whose headings are images
with empty alt attributes has, to all of them, no headings at all.

One base URL is used throughout, so after uploading the folder you replace a single string
rather than fifteen.

    python tools/weave_images.py                 write CURSEFORGE-IMAGES.md
    python tools/weave_images.py --base <url>    set the image base URL
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "CURSEFORGE.md")
OUT = os.path.join(ROOT, "CURSEFORGE-IMAGES.md")

DEFAULT_BASE = "PASTE_YOUR_IMAGE_URL_BASE_HERE"

# Heading text as it appears in the file -> header image filename.
HEADERS = {
    "at a glance": "at-a-glance-header.png",
    "the functional tracks": "the-functional-tracks-header.png",
    "boost track": "boost-track-header.png",
    "brake track": "brake-track-header.png",
    "station track": "station-track-header.png",
    "sensor track": "sensor-track-header.png",
    "slippery track": "slippery-track-header.png",
    "the sensor block": "the-sensor-block-header.png",
    "coaster controls": "coaster-controls-header.png",
    "balloons": "balloons-header.png",
    "track materials": "track-materials-header.png",
    "building your first ride": "building-your-first-ride-header.png",
    "crafting": "crafting-header.png",
    "requirements": "faq-header.png",
    "credits": "license-header.png",
}

# Strip images, dropped in after the first paragraph of the named section.
STRIPS = {
    "the functional tracks": ("strip-functional-tracks.png",
                              "The five functional tracks"),
    "balloons": ("strip-balloons.png", "All 17 balloon colours"),
    "track materials": ("strip-track-materials.png", "Every track material"),
}


def main():
    base = DEFAULT_BASE
    if "--base" in sys.argv:
        base = sys.argv[sys.argv.index("--base") + 1].rstrip("/")

    lines = open(SRC, encoding="utf8").read().split("\n")
    out, used, pending = [], set(), None

    for line in lines:
        heading = re.match(r"^(#{1,3})\s+(?:[^\w\s]+\s+)?(.+?)\s*$", line)
        key = heading.group(2).strip().lower().rstrip("?") if heading else None

        if key in HEADERS:
            # An emoji prefix on the sub-headings ("⚡ Boost Track") is dropped: it is baked
            # into the header art already, and a duplicate in the alt text reads as a stutter
            # to a screen reader.
            out.append(f"![{heading.group(2).strip()}]({base}/{HEADERS[key]})")
            used.add(key)
            pending = STRIPS.get(key)
            continue

        out.append(line)
        # Wait for the section's opening paragraph, so the strip sits under the explanation
        # rather than between the heading and its own first sentence.
        if pending and line.strip() == "" and out[-2].strip() and not out[-2].startswith("!"):
            image, caption = pending
            out += [f"![{caption}]({base}/{image})", ""]
            pending = None

    open(OUT, "w", encoding="utf8", newline="\n").write("\n".join(out))

    print(f"wrote {OUT}\n")
    print(f"{len(used)} of {len(HEADERS)} headers placed")
    for key in HEADERS:
        mark = "ok " if key in used else "MISS"
        print(f"  {mark} {key:<28}{HEADERS[key]}")
    if base == DEFAULT_BASE:
        print(f"\nImage URLs are still the placeholder. Upload the folder, then rerun:")
        print(f"  python tools/weave_images.py --base https://your-host/path")


if __name__ == "__main__":
    main()
