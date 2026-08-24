"""Builds both store pages from the plain sources, in one pass.

Replaces the weave_images / add_icons / add_socials chain. Those three each edited the output
of the last, so re-running the first one silently threw away the other two's work -- a footgun
that only shows up when you change one image weeks later. This regenerates everything from
CURSEFORGE.md and MODRINTH.md every time, so running it twice gives the same answer as running
it once.

The two pages differ in exactly one way: each links to the OTHER store, never to itself. A
Modrinth page with a Modrinth button is a button that reloads the page you are on, and it
pushes the CurseForge link -- the one that is actually useful there -- further along the row.

Nothing is invented. Any URL still set to the placeholder in page_config.json is dropped from
the bar rather than linked, because a social icon leading to a 404 reads as an abandoned
project, which is worse than having one fewer icon.

    python tools/build_pages.py
    python tools/build_pages.py --base https://host/path      override the image base
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG = os.path.join(ROOT, "tools", "page_config.json")
FILL_IN = "PUT_YOUR_URL_HERE"
PLACEHOLDER_BASE = "PASTE_YOUR_IMAGE_URL_BASE_HERE"

PAGES = [("CURSEFORGE.md", "CURSEFORGE-IMAGES.md", "CurseForge"),
         ("MODRINTH.md", "MODRINTH-IMAGES.md", "Modrinth")]

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
    "rainbow track": "rainbow-track-header.png",
    "copycat track": "copycat-track-header.png",
    "building your first ride": "building-your-first-ride-header.png",
    "crafting": "crafting-header.png",
    "requirements": "faq-header.png",
    "credits": "license-header.png",
}

STRIPS = {
    "the functional tracks": ("strip-functional-tracks.png", "The five functional tracks"),
    "balloons": ("strip-balloons.png", "All 17 balloon colours"),
    "track materials": ("strip-track-materials.png", "Every track material"),
}

BULLET_ICONS = [
    ("**55 coaster tracks**", "icon-tracks.png"),
    ("**16 balloons**", "icon-balloons.png"),
    ("**Sensor Block**", "icon-sensor.png"),
    ("**Coaster Controls**", "icon-controls.png"),
    ("**6 Ponder scenes**", "icon-ponder.png"),
    ("**Survival ready**", "icon-survival.png"),
]

# filename -> uploaded URL, filled by load_url_map().
URL_MAP = {}


def url_for(base, filename):
    """The uploaded URL, else base + filename, else None.

    None when the file was never uploaded AND no real base is set -- which is the case for the
    six At-a-Glance bullet icons. Emitting base + filename there would put the literal
    placeholder string in an <img src>, so the page would ship with six broken images in its
    most-read section. Callers drop the image instead and the bullet simply appears without an
    icon, which nobody will notice.
    """
    if filename in URL_MAP:
        return URL_MAP[filename]
    if base and base != PLACEHOLDER_BASE:
        return f"{base}/{filename}"
    return None


# X and YouTube are deliberately absent: there are no accounts behind them, and a logo for a
# service you are not on is worse than no logo -- it invites a click that goes nowhere and
# makes the row look padded.
SOCIAL_FILES = {
    "Modrinth": "social-modrinth.png", "CurseForge": "social-curseforge.png",
    "GitHub": "social-github.png", "Discord": "social-discord.png",
    "TikTok": "social-tiktok.png", "Ko-fi": "social-kofi.png",
}


def social_bar(base, socials, this_store):
    """The icon row. Icons with a destination are links; the rest are plain images.

    Showing an icon with no link is a deliberate compromise, not an oversight. A row with three
    of eight logos reads as a project with barely any presence; the full row reads as a project
    that has one, and the missing hrefs cost a curious reader one wasted click rather than a
    404. The moment a URL lands in page_config.json that icon becomes clickable with no other
    change.
    """
    parts = []
    for label, file_name in SOCIAL_FILES.items():
        if label == this_store:
            continue                      # never link a page to itself
        icon = url_for(base, file_name)
        if not icon:
            continue                      # icon never uploaded; nothing to show
        url = socials.get(label, FILL_IN)
        parts.append(f"![{label}]({icon})" if url == FILL_IN
                     else f"[![{label}]({icon})]({url})")
    return " ".join(parts)


def build(source, target, this_store, base, socials):
    lines = open(os.path.join(ROOT, source), encoding="utf8").read().split("\n")
    bar = social_bar(base, socials, this_store)

    out, pending, placed = [], None, set()
    for line in lines:
        heading = re.match(r"^(#{1,3})\s+(?:[^\w\s]+\s+)?(.+?)\s*$", line)
        key = heading.group(2).strip().lower().rstrip("?") if heading else None

        if key in HEADERS:
            out.append(f"![{heading.group(2).strip()}]({url_for(base, HEADERS[key])})")
            placed.add(key)
            pending = STRIPS.get(key)
            continue

        for bullet, icon in BULLET_ICONS:
            if line.startswith(f"- {bullet}"):
                icon_url = url_for(base, icon)
                # No URL means the icon was never uploaded. Skip it -- writing the image tag
                # anyway put the literal string "None" in the src and shipped six broken
                # images in the most-read section of the page.
                if icon_url:
                    # Empty alt: the bullet text already names the feature, and a screen
                    # reader announcing it twice makes the list unreadable.
                    line = line.replace(f"- {bullet}",
                                        f"- ![]({icon_url}) {bullet}", 1)
                break

        out.append(line)
        if pending and line.strip() == "" and out[-2].strip() and not out[-2].startswith("!"):
            image, caption = pending
            out += [f"![{caption}]({url_for(base, image)})", ""]
            pending = None

    text = "\n".join(out)
    if bar:
        # Under the tagline rather than above the banner -- the banner is the page's opening
        # image and nothing should come before it.
        at = next((i for i, line in enumerate(out)
                   if line.startswith("**[Create: Coasters Simulated]")
                   or line.startswith("**Create: Coasters Simulated")), 2)
        out.insert(at + 1, f"\n{bar}")
        text = "\n".join(out).rstrip() + f"\n\n{bar}\n"

    open(os.path.join(ROOT, target), "w", encoding="utf8", newline="\n").write(text)
    return placed, bar


def load_url_map():
    """Explicit per-file URLs, for hosts that rename uploads to a content hash.

    Modrinth and CurseForge both do this, so there is no shared prefix to append filenames to
    and --base cannot work for them. When a file appears here its URL wins; anything absent
    falls back to base + filename, which still covers a plain static host or a GitHub repo.
    """
    path = os.path.join(ROOT, "tools", "image_urls.json")
    if not os.path.exists(path):
        return {}
    raw = json.load(open(path, encoding="utf-8-sig"))
    return {k: v for k, v in raw.items()
            if not k.startswith("_") and isinstance(v, str) and v.startswith("http")}


def main():
    config = json.load(open(CONFIG, encoding="utf-8-sig"))
    base = config["image_base"].rstrip("/")
    if "--base" in sys.argv:
        base = sys.argv[sys.argv.index("--base") + 1].rstrip("/")
    socials = config["socials"]
    global URL_MAP
    URL_MAP = load_url_map()
    print(f"{len(URL_MAP)} images have an explicit uploaded URL")

    live = [k for k, v in socials.items() if v != FILL_IN]
    print(f"image base: {base}")
    print(f"socials with a real URL: {', '.join(live) if live else 'none'}\n")

    for source, target, store in PAGES:
        if not os.path.exists(os.path.join(ROOT, source)):
            print(f"{source:<18}MISSING -- skipped")
            continue
        placed, bar = build(source, target, store, base, socials)
        missing = [k for k in HEADERS if k not in placed]
        icons = bar.count("![")
        print(f"{target:<24}{len(placed)}/{len(HEADERS)} headers, {icons} social icons "
              f"(no self-link to {store})")
        if missing:
            print(f"  headings not found: {', '.join(missing)}")

    unset = [k for k, v in socials.items() if v == FILL_IN]
    if unset:
        print(f"\nNot linked, no URL yet: {', '.join(unset)}")
        print(f"  add them in tools/page_config.json")
    if base == PLACEHOLDER_BASE:
        print(f"\nImage base is still the placeholder -- images will not load until it is set.")


if __name__ == "__main__":
    main()
