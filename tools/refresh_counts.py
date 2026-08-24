"""Brings the store descriptions back in line with what the mod actually ships.

Counts in a description rot silently. Nothing fails to build when a track is added, so the page
goes on claiming a number that was true two releases ago -- and a wrong count on the front page
is the first thing a reader can check and the first thing that costs them trust.

The numbers here are not typed in from memory. They are the ones read out of TrackVariant and
BalloonColor: 53 visible track variants plus 5 functional materials, and 17 balloons.

    py tools/refresh_counts.py
"""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGETS = ["MODRINTH.md", "CURSEFORGE.md"]

# old -> new. Every one is exact text, so a replacement that silently matches nothing is
# reported rather than assumed to have worked.
EDITS = [
    # Lead paragraph.
    ("**55 track materials**, **16 balloons**",
     "**58 track materials**, **17 balloons**"),

    # At a glance.
    ("| **55** — 18 cosmetic, 32 wool & concrete, 5 functional |",
     "| **58** — 21 cosmetic, 32 wool & concrete, 5 functional |"),
    ("| **16** — 15 dye colours plus a Rainbow Balloon |",
     "| **17** — 15 dye colours, a Rainbow Balloon and a Copycat Balloon |"),

    # Materials table. Rose Quartz sits with the stones and Brass with the metal, because that
    # is what they are -- filing them under Special because they are new would leave Special
    # meaning "recently added" rather than "not an ordinary material".
    ("| 🪨 **Stone** | Stone, Deepslate, Andesite, Granite, Diorite |",
     "| 🪨 **Stone** | Stone, Deepslate, Andesite, Granite, Diorite, Rose Quartz |"),
    ("| 🦺 **Metal** | Rusted |",
     "| 🦺 **Metal** | Rusted, Brass |"),
    ("| 🌈 **Special** | Rainbow, Copycat, Rose Quartz, Brass |",
     "| 🌈 **Special** | Rainbow, Copycat |"),

    # CurseForge says the same things as bullet lists rather than tables, so it needs its own
    # pairs. Kept in one list rather than split per file: a pair that does not appear in a file
    # is simply skipped, and one list means a future count change cannot be applied to one store
    # and forgotten on the other.
    ("- **55 coaster tracks** — 18 cosmetic, 32 wool & concrete, 5 functional",
     "- **58 coaster tracks** — 21 cosmetic, 32 wool & concrete, 5 functional"),
    ("- **16 balloons** — 15 dye colours plus a Rainbow Balloon",
     "- **17 balloons** — 15 dye colours, a Rainbow Balloon and a Copycat Balloon"),
    ("- **Stone (5)** — Stone, Deepslate, Andesite, Granite, Diorite",
     "- **Stone (6)** — Stone, Deepslate, Andesite, Granite, Diorite, Rose Quartz"),
    ("- **Metal** — Rusted",
     "- **Metal** — Rusted, Brass"),
    ("- **Special** — Rainbow",
     "- **Special** — Rainbow, Copycat"),
]

# Appended to the Balloons section, which currently ends with the list of the fifteen colours.
BALLOON_ANCHOR = ("The base mod ships one balloon, and it's red. Here are the other fifteen")
BALLOON_EXTRA = """
🎭 **The Copycat Balloon** is one balloon that wears any block in the game. Sneak and right-click \
a block to copy it, then place it — the balloon takes that block's texture, re-drawn as it \
renders rather than picked from a list of prepared ones. It floats and pops like every other \
balloon.
"""


def main():
    for name in TARGETS:
        path = os.path.join(ROOT, name)
        if not os.path.exists(path):
            print(f"{name:<18}not found, skipped")
            continue

        text = io.open(path, encoding="utf8").read()
        done = 0
        missed = []
        for old, new in EDITS:
            if old in text:
                text = text.replace(old, new)
                done += 1
            elif new not in text:
                missed.append(old[:52])

        if BALLOON_EXTRA.strip() not in text:
            at = text.find(BALLOON_ANCHOR)
            if at >= 0:
                end = text.find("\n\n", at)
                text = text[:end + 1] + BALLOON_EXTRA + text[end + 1:]
                done += 1
            else:
                missed.append("balloons section anchor")

        io.open(path, "w", encoding="utf8", newline="\n").write(text)
        print(f"{name:<18}{done} updated")
        for m in missed:
            print(f"    no match: {m}")


if __name__ == "__main__":
    main()
