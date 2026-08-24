"""Corrects every instruction that tells players to "scroll" an anchorpoint.

Create 6 does not set these values by scrolling. Traced through its own source:

    ValueSettingsInputHandler, on right-click, reaches
        if (valueSettingsBehaviour.acceptsValueSettings() && !fakePlayer)
            ... VALUE_SETTINGS_HANDLER.startInteractionWith(pos, type, hand, direction)

    ScrollValueBehaviour does not override acceptsValueSettings(), and the interface default
    returns true -- so the dial always takes the value-settings branch, which opens a board you
    RIGHT-CLICK AND HOLD, then drag. The mouse wheel is not part of that interaction.

So the mod's docs, its Ponder scenes and its tooltips have all been telling players to perform
a gesture that does nothing, on a control that works perfectly. That is why the reports are
"scrolling is broken" rather than "the station is broken": people were doing exactly what they
were told.

Two things are NOT the cause, checked and ruled out first:
  * A wrench requirement. needsWrench defaults to false and the mod never calls
    requiresWrench(), so the box is visible bare-handed.
  * A missing injection. CoasterAnchorpointBlockEntity declares addBehaviours itself
    (descriptor (Ljava/util/List;)V), so the mixin has a real target and the dial is registered.

    python tools/fix_scroll_wording.py            show every change
    python tools/fix_scroll_wording.py --write    apply them
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(ROOT, "src", "main", "resources", "assets",
                    "coasters_extras", "lang", "en_us.json")
DOCS = ["CURSEFORGE.md", "MODRINTH.md", "README.md"]

# Ponder lines are on a strict width budget -- they render as a caption over the scene, and an
# overlong one wraps into the diagram. "Hold right-click" is shorter than "Right-click and
# hold" and says the same thing.
LANG_FIXES = {
    "coasters_extras.ponder.boost_track.text_5":
        "Hold right-click on an anchorpoint to set the speed.",
    "coasters_extras.ponder.station_track.text_4":
        "Then it waits. Hold right-click to set how long.",
}

# Prose replacements, longest first so a short pattern cannot eat part of a long one.
DOC_FIXES = [
    ("**click and hold, then scroll**, exactly like a Creative Motor",
     "**right-click and hold, then drag**, exactly like a Creative Motor"),
    ("**Scroll the anchorpoint** to set the dwell time in seconds",
     "**Right-click and hold the anchorpoint** to set the dwell time in seconds"),
    ("Scroll its anchorpoint to 5 seconds.",
     "Right-click and hold its anchorpoint, then drag to 5 seconds."),
    ("Scroll it to about 20 b/s",
     "Right-click and hold it, then drag to about 20 b/s"),
    ("as a Create-style scroll dial",
     "as a Create-style value dial"),
    # MODRINTH.md keeps backticks around numbers; CURSEFORGE.md has them stripped because
    # CurseForge's editor mangles code spans. Same sentence, two spellings.
    ("Scroll its anchorpoint to `5` seconds",
     "Right-click and hold its anchorpoint, then drag to `5` seconds"),
    ("Scroll it to about `20` b/s",
     "Right-click and hold it, then drag to about `20` b/s"),
    ("A Create-style scroll dial appears",
     "A Create-style value dial appears"),
    ("Click and hold, then scroll — the same interaction as the Creative Motor",
     "Right-click and hold, then drag — the same interaction as the Creative Motor"),
]


def main():
    write = "--write" in sys.argv
    changed = 0

    data = json.load(io.open(LANG, encoding="utf8"))
    print("lang/en_us.json")
    for key, new in LANG_FIXES.items():
        old = data.get(key)
        if old is None:
            print(f"    MISSING KEY  {key}")
            continue
        if old == new:
            print(f"    already ok   {key}")
            continue
        print(f"    {key}\n        - {old}\n        + {new}")
        data[key] = new
        changed += 1
    if write and changed:
        with io.open(LANG, "w", encoding="utf8", newline="\n") as fh:
            json.dump(data, fh, indent=2, ensure_ascii=False)
            fh.write("\n")

    for doc in DOCS:
        path = os.path.join(ROOT, doc)
        if not os.path.exists(path):
            continue
        text = io.open(path, encoding="utf8").read()
        before = text
        hits = []
        for old, new in DOC_FIXES:
            if old in text:
                hits.append(f"        - {old}\n        + {new}")
                text = text.replace(old, new)
        # Anything still telling people to scroll an anchorpoint is a miss worth reporting
        # rather than silently leaving behind.
        leftover = re.findall(r"[^.\n]*[Ss]croll[^.\n]*", text)
        print(f"\n{doc}")
        if hits:
            print("\n".join(hits))
            changed += len(hits)
        else:
            print("    no matches")
        for line in leftover:
            print(f"    STILL MENTIONS SCROLL: {line.strip()[:90]}")
        if write and text != before:
            io.open(path, "w", encoding="utf8", newline="\n").write(text)

    print(f"\n{changed} changes" + ("" if write else " -- dry run, add --write"))


if __name__ == "__main__":
    main()
