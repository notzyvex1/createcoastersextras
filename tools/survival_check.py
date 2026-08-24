"""Makes sure everything the mod adds is obtainable and recoverable in survival.

Two things break a survival playthrough and neither shows up in creative:

  * a block with no loot table drops NOTHING when broken. You place it, you mine it, it is
    gone. Only the balloons had one.
  * an item with no recipe cannot be got at all.

So this generates the missing loot tables and then audits every registered block and item,
reporting anything still unobtainable rather than silently passing.

Track blocks are the awkward case: the block is `<name>_track_material` but the thing you
hold is `<name>_track`, so the loot table has to name the item, not the block. Naming the
block would drop something that has no item form and therefore nothing at all.

Run from the repo root:  python tools_survival_check.py
"""
import json
import os
import re

RES = "src/main/resources"
LOOT = f"{RES}/data/coasters_extras/loot_table/blocks"
RECIPE = f"{RES}/data/coasters_extras/recipe"
SRC = "src/main/java/dev/notzyvex/coasters_extras"


def registered():
    """Every block we register, mapped to the item it should drop."""
    blocks = {}

    # the cosmetic variants come from the enum, not from a register() call
    enum = open(f"{SRC}/track/TrackVariant.java", encoding="utf8").read()
    for name in re.findall(r'^\s{4}([A-Z_]+)\s*\("([a-z_]+)"', enum, re.M):
        blocks[f"{name[1]}_track_material"] = f"{name[1]}_track"

    for path in ("track/ModTracks.java", "sensor/SensorRegistry.java"):
        s = open(f"{SRC}/{path}", encoding="utf8").read()
        for b in re.findall(r'BLOCKS\.register\("([a-z_]+)"', s):
            # a *_track_material block is mined into its *_track item
            blocks[b] = b[:-9] if b.endswith("_material") else b

    balloons = open(f"{SRC}/BalloonColor.java", encoding="utf8").read()
    for c in re.findall(r'^\s{4}([A-Z_]+)\s*\(', balloons, re.M):
        n = c.lower()
        if n.endswith("_wool"):
            continue
        blocks[f"{n}_balloon"] = f"{n}_balloon"
    return blocks


def write_loot(block, item):
    table = {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"coasters_extras:{item}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }
    with open(f"{LOOT}/{block}.json", "w", encoding="utf8", newline="\n") as f:
        json.dump(table, f, indent=2)
        f.write("\n")


def main():
    os.makedirs(LOOT, exist_ok=True)
    blocks = registered()

    added = []
    for block, item in sorted(blocks.items()):
        if not os.path.exists(f"{LOOT}/{block}.json"):
            write_loot(block, item)
            added.append(block)

    print(f"blocks registered: {len(blocks)}")
    print(f"loot tables written: {len(added)}")
    for b in added:
        print("   +", b)

    # --- audit: can every item actually be obtained? ---
    recipes = set()
    for f in os.listdir(RECIPE):
        d = json.load(open(f"{RECIPE}/{f}", encoding="utf8"))
        res = d.get("result")
        if isinstance(res, dict):
            rid = res.get("id") or res.get("item")
            if rid:
                recipes.add(rid.split(":", 1)[-1])

    items = sorted(set(blocks.values()))
    missing = [i for i in items if i not in recipes]
    print(f"\nitems: {len(items)}   with a recipe: {len(items) - len(missing)}")
    if missing:
        print("NO RECIPE — cannot be obtained in survival:")
        for m in missing:
            print("   -", m)
    else:
        print("every item has a recipe")

    orphans = [b for b in blocks if not os.path.exists(f"{LOOT}/{b}.json")]
    if orphans:
        print("STILL NO LOOT TABLE:", orphans)


if __name__ == "__main__":
    main()
