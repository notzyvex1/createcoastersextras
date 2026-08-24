"""Adds the Copycat Track and Rainbow Track sections to both store descriptions.

Both are cosmetic, so they go after Track materials rather than under The Functional Tracks.
Filing Copycat under "functional" would actively mislead -- it does nothing to a cart, and the
whole point of the section is that everything in it changes how a ride behaves.

The Rainbow paragraph already existed as one line buried at the end of Track materials. It is
moved rather than duplicated: it is the showpiece of the mod and a line in someone else's table
is not where a showpiece goes. Its header image has existed since the icon headers were built and
has never been placed, because there was no heading for build_pages.py to attach it to.

    py tools/add_cosmetic_sections.py
"""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGETS = ["MODRINTH.md", "CURSEFORGE.md"]

# The line currently inside Track materials. Lifted out, not copied.
RAINBOW_OLD = "🌟 **The Rainbow Track** is the showpiece."

SPECIAL_ROW_OLD = "| 🌈 **Special** | Rainbow |"
SPECIAL_ROW_NEW = "| 🌈 **Special** | Rainbow, Copycat, Rose Quartz, Brass |"

SECTIONS = """## 🌈 Rainbow Track

**The showpiece.** It rides like any other track, but leaves a trail of coloured sparkles and \
plays a chime that climbs with them — colour and pitch driven by the same value, so they stay in \
step for the whole run.

The notes are quantised to a major pentatonic. That one detail is the difference between a ride \
that sounds like a melody and one that sounds like a siren: on a free scale, a fast curve picks \
semitones at random and the result is noise.

Pairs with the **Rainbow Balloon**, which is the same sweep drawn on a balloon.

---

## 🎭 Copycat Track

**Coaster track that takes the look of any block you show it.** Point it at oak planks and you \
get oak track. Point it at deepslate and you get deepslate track. It rides exactly like every \
other track — the material is purely cosmetic.

| Gesture | What happens |
|---|---|
| **Right-click a block** | The whole stack becomes track built from that block |
| **Sneak + right-click a placed curve** | That section changes to the track you are holding |

That second one works with **every** track, not just Copycat — a brake, a station, rainbow, plain \
coaster track, anything. Change one section of a finished ride into a Brake Track without \
destroying and relaying it, and every micro-adjustment you made to the curve survives, because \
only the material changes and the geometry is never touched.

---

"""


def main():
    for name in TARGETS:
        path = os.path.join(ROOT, name)
        if not os.path.exists(path):
            print(f"{name:<18}not found, skipped")
            continue

        text = io.open(path, encoding="utf8").read()
        before = text

        # 1. Widen the Special row so the materials table stops claiming Rainbow is the only one.
        text = text.replace(SPECIAL_ROW_OLD, SPECIAL_ROW_NEW)

        # 2. Lift the old Rainbow line out of Track materials.
        text = re.sub(r"\n" + re.escape(RAINBOW_OLD) + r"[^\n]*\n", "\n", text)

        # 3. Insert both sections after Track materials, which is the section they belong beside.
        #    Anchored on the NEXT heading rather than on a line count, so this survives the
        #    section above it being edited.
        marker = re.search(r"\n## [^\n]*Setting speeds[^\n]*\n", text)
        if marker is None:
            marker = re.search(r"\n## [^\n]*Building your first ride[^\n]*\n", text)
        if marker is None:
            print(f"{name:<18}could not find an insertion point, skipped")
            continue
        at = marker.start() + 1
        text = text[:at] + SECTIONS + text[at:]

        if text == before:
            print(f"{name:<18}already up to date")
            continue

        io.open(path, "w", encoding="utf8", newline="\n").write(text)
        print(f"{name:<18}Rainbow Track + Copycat Track added")


if __name__ == "__main__":
    main()
