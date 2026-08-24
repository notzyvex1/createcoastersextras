"""Generates the Ponder scene structures for our functional tracks.

Ponder structures are vanilla structure templates: palette + blocks + size, gzipped.
Hand-synthesising one would mean hand-synthesising a BezierConnection, and a curve
carries eight coupled vectors that all have to agree -- Starts, Axes, Normals and
Positions -- with no feedback if they do not.

So this starts from Create: Coasters Simulated's own `ponder_cart` structure, which is
a known-good capture of two anchorpoints joined by one curve with a cart on it, and
changes only what has to change: the curve's Material, and a prop or two per scene.
Their curve data is entirely relative to the owning block entity (Positions[0] is
always [0,0,0]), so it stays geometrically valid wherever Ponder pastes it.

Run from the repo root:  python tools_ponder_nbt.py
"""
import os
import shutil
import tools_schem as T

SRC = "build/scponder/ponder_cart.nbt"
OUT = "src/main/resources/assets/coasters_extras/ponder/track"

# Scene file name -> the track material its curve should use.
SCENES = {
    "boost":    "coasters_extras:boost_track",
    "brake":    "coasters_extras:brake_track",
    "station":  "coasters_extras:station_track",
    "sensor":   "coasters_extras:sensor_track",
    "slippery": "coasters_extras:slippery_track",
    "rainbow":  "coasters_extras:rainbow_track",
}

# Belongs to a mod we do not depend on; it would resolve to air and leave an orphan
# block entity behind. The scenes label themselves with Ponder text instead.
DROP_BLOCKS = ("simulated:red_nameplate",)

# The sensor scene needs the thing it talks about. It describes a Sensor Block turning a
# crossing into redstone, and until now showed neither the block nor a lamp -- which is
# exactly the scene someone opens when asking how to do that.
#
# The block is placed unlinked and the scene drives its state directly. A real link stores
# absolute anchor positions and would be wrong the moment Ponder pasted the structure
# somewhere else; a scripted demo has no need to actually be wired up.
SENSOR_PROPS = [
    ("coasters_extras:sensor_block", {"powered": "false"}, (7, 1, 5)),
    ("minecraft:redstone_lamp",      {"lit": "false"},     (7, 1, 4)),
]

# Their capture stacks six takes of the same cart on top of each other, one per beat of
# their multi-part scene. We want one cart on one track, so everything above the track
# layer goes.
KEEP_BELOW_Y = 3

# ...and the surviving cart starts at the near end so it has somewhere to travel to.
CART_FROM = (7, 2, 7)
CART_TO = (5, 2, 7)

# A seat on the cart, in every scene. The capture had one and it was thrown away with the
# stacked layers. Without it the cart is a box on a rail; with it, it reads immediately as
# something you get into -- which is most of what these scenes are trying to say.
SEAT = ("create:red_seat", {"waterlogged": "false"}, (5, 3, 7))


def val(t):
    return t.val if hasattr(t, "val") else t


def items(tag):
    """Payload list out of a TAG_List, which the reader stores as (itemTypeId, [..])."""
    x = val(tag)
    return x[1] if isinstance(x, tuple) else x


def palette_names(root):
    return [val(val(p)["Name"]) for p in items(val(root)["palette"])]


def retint(root, material):
    """Points every bezier in the structure at our track material."""
    changed = 0
    for block in items(val(root)["blocks"]):
        b = val(block)
        if "nbt" not in b:
            continue
        be = val(b["nbt"])
        if "AnchorPeerCurves" not in be:
            continue
        for curve in items(be["AnchorPeerCurves"]):
            bez = val(val(curve)["Bezier"])
            if "Material" in bez:
                bez["Material"] = T.Tag(T.STRING, material)
                changed += 1
    return changed


def drop(root, names):
    """Removes blocks whose palette entry is one of `names`, leaving the palette alone.

    Reindexing the palette would mean rewriting every block's state index for no gain;
    an unreferenced palette entry costs a few bytes and loads fine.
    """
    pal = palette_names(root)
    doomed = {i for i, n in enumerate(pal) if n in names}
    if not doomed:
        return 0
    blocks = val(root)["blocks"]
    kept = [b for b in items(blocks) if val(val(b)["state"]) not in doomed]
    removed = len(items(blocks)) - len(kept)
    blocks.val = (T.COMPOUND, kept)
    return removed


def trim_height(root, limit):
    """Drops every block at or above `limit` and shrinks the recorded size to match."""
    d = val(root)
    blocks = d["blocks"]
    kept = [b for b in items(blocks) if val(items(val(b)["pos"])[1]) < limit]
    removed = len(items(blocks)) - len(kept)
    blocks.val = (T.COMPOUND, kept)

    size = items(d["size"])
    size[1] = limit
    return removed


def move_cart(root, src, dst):
    """Slides the one surviving cart to the near end of the track."""
    for block in items(val(root)["blocks"]):
        b = val(block)
        pos = [val(v) for v in items(b["pos"])]
        if tuple(pos) == tuple(src):
            b["pos"] = T.Tag(T.LIST, (T.INT, list(dst)))
            return True
    return False


def add_block(root, name, props, pos):
    """Appends one block, interning its state in the palette."""
    d = val(root)
    pal = items(d["palette"])
    entry = {"Name": T.Tag(T.STRING, name)}
    if props:
        entry["Properties"] = T.Tag(T.COMPOUND,
                {k: T.Tag(T.STRING, v) for k, v in props.items()})
    pal.append(entry)
    items(d["blocks"]).append({
        "pos":   T.Tag(T.LIST, (T.INT, list(pos))),
        "state": T.Tag(T.INT, len(pal) - 1),
    })
    # the seat sits a layer above the cart, so the structure has to be tall enough for it
    size = items(d["size"])
    size[1] = max(size[1], pos[1] + 1)


def add_lamps(root):
    """Puts the Sensor Block and its lamp beside the track."""
    d = val(root)
    pal = items(d["palette"])
    blocks = items(d["blocks"])

    for name, props, (x, y, z) in SENSOR_PROPS:
        entry = {"Name": T.Tag(T.STRING, name)}
        if props:
            entry["Properties"] = T.Tag(T.COMPOUND,
                    {k: T.Tag(T.STRING, v) for k, v in props.items()})
        pal.append(entry)
        blocks.append({
            "pos":   T.Tag(T.LIST, (T.INT, [x, y, z])),
            "state": T.Tag(T.INT, len(pal) - 1),
        })
    return len(SENSOR_PROPS)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, material in SCENES.items():
        _, root = T.load(SRC)          # reload per scene; edits are destructive
        curves = retint(root, material)
        gone = drop(root, DROP_BLOCKS)
        gone += trim_height(root, KEEP_BELOW_Y)
        moved = move_cart(root, CART_FROM, CART_TO)
        add_block(root, *SEAT)
        lamps = add_lamps(root) if name == "sensor" else 0

        path = os.path.join(OUT, name + ".nbt")
        T.save(path, "", root)

        # Read it straight back. A structure that fails to parse does not crash Ponder,
        # it silently renders an empty scene -- so the check has to happen here.
        _, check = T.load(path)
        mats = set()
        for block in items(val(check)["blocks"]):
            b = val(block)
            if "nbt" not in b:
                continue
            be = val(b["nbt"])
            for curve in items(be.get("AnchorPeerCurves", T.Tag(T.LIST, (T.COMPOUND, [])))):
                mats.add(val(val(val(curve)["Bezier"])["Material"]))
        size = [val(v) for v in items(val(check)["size"])]
        ok = mats == {material}
        carts = sum(1 for b in items(val(check)["blocks"])
                    if val(items(val(b)["pos"])[1]) == 2)
        print(f"{name:9s} curves={curves} dropped={gone} cart_moved={moved} lamps={lamps} "
              f"size={size} layer2={carts} materials={sorted(mats)} "
              f"{'OK' if ok else 'MISMATCH'}")


if __name__ == "__main__":
    main()
