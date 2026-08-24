"""Builds the showcase structures: one of track, one of balloons.

These are plain vanilla structure templates, so they load with a Structure Block in any
world. Drop them in

    <world>/generated/minecraft/structures/

and load them by name.

The track file lays real curves between real anchorpoints, which means writing a
BezierConnection by hand. That is only safe because their format is entirely relative to the
owning block entity -- Positions[0] is always [0,0,0] and Starts are offsets from the block's
own corner -- so the geometry is correct wherever the structure is pasted. The one absolute
value is the map key naming the far anchorpoint, and AnchorpointPasteRepairMixin recomputes
that on load.

Run from the repo root:  python tools_showcase_nbt.py
"""
import os

import tools_schem as T

OUT = "showcase"

ANCHOR = "simulatedcoasters:coaster_anchorpoint"
ANCHOR_PROPS = {"chain_lift_shaft": "false", "facing": "up", "track_connected": "true"}

FLOOR = "create:andesite_casing"
TRIM = "create:brass_casing"
PLINTH = "create:industrial_iron_block"

# One lane per track, in the order they make sense to look at.
TRACKS = [
    "boost_track", "brake_track", "station_track",
    "sensor_track", "slippery_track", "rainbow_track",
]

BALLOONS = [
    "white_balloon", "light_gray_balloon", "gray_balloon", "black_balloon",
    "brown_balloon", "orange_balloon", "yellow_balloon", "lime_balloon",
    "green_balloon", "cyan_balloon", "light_blue_balloon", "blue_balloon",
    "purple_balloon", "magenta_balloon", "pink_balloon", "rainbow_balloon",
]

SPAN = 8          # blocks between the two anchorpoints of a lane
LANE_GAP = 3      # blocks between lanes


def as_long(x, y, z):
    """Minecraft's BlockPos.asLong packing."""
    v = ((x & ((1 << 26) - 1)) << 38) | ((z & ((1 << 26) - 1)) << 12) | (y & ((1 << 12) - 1))
    return v - (1 << 64) if v >= (1 << 63) else v


def vec(x, y, z):
    return {"V": T.Tag(T.LIST, (T.DOUBLE, [float(x), float(y), float(z)]))}


def pos(x, y, z):
    return {"Pos": T.Tag(T.INTS, [x, y, z])}


def bezier(dx, material, primary):
    """One end of a straight curve running dx blocks along X, relative to its own block."""
    return {
        "Girder":    T.Tag(T.BYTE, 0),
        "Primary":   T.Tag(T.BYTE, 1 if primary else 0),
        "Material":  T.Tag(T.STRING, material),
        "Positions": T.Tag(T.LIST, (T.COMPOUND, [pos(0, 0, 0), pos(dx, 0, 0)])),
        "Starts":    T.Tag(T.LIST, (T.COMPOUND,
                                    [vec(0.5, 0.75, 0.5), vec(dx + 0.5, 0.75, 0.5)])),
        "Axes":      T.Tag(T.LIST, (T.COMPOUND,
                                    [vec(1 if dx > 0 else -1, 0, 0),
                                     vec(-1 if dx > 0 else 1, 0, 0)])),
        "Normals":   T.Tag(T.LIST, (T.COMPOUND, [vec(0, 1, 0), vec(0, 1, 0)])),
    }


def anchor_nbt(peer_xyz, dx, material, primary):
    return {
        "id":                   T.Tag(T.STRING, ANCHOR),
        "Speed":                T.Tag(T.FLOAT, 0.0),
        "NeedsSpeedUpdate":     T.Tag(T.BYTE, 1),
        "JointBank":            T.Tag(T.FLOAT, 0.0),
        "AnchorPeerCurveTints": T.Tag(T.LIST, (T.END, [])),
        "AnchorPeerCurves":     T.Tag(T.LIST, (T.COMPOUND, [{
            "Peer":   T.Tag(T.LONG, as_long(*peer_xyz)),
            "HHi":    T.Tag(T.DOUBLE, 2.6666666666666665),
            "HLo":    T.Tag(T.DOUBLE, 2.6666666666666665),
            "Bezier": T.Tag(T.COMPOUND, bezier(dx, material, primary)),
        }])),
    }


class Build:
    """Collects blocks and interns palette entries."""

    def __init__(self):
        self.palette = []
        self.index = {}
        self.blocks = []

    def state(self, name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in self.index:
            entry = {"Name": T.Tag(T.STRING, name)}
            if props:
                entry["Properties"] = T.Tag(T.COMPOUND, {
                    k: T.Tag(T.STRING, v) for k, v in props.items()})
            self.palette.append(entry)
            self.index[key] = len(self.palette) - 1
        return self.index[key]

    def put(self, x, y, z, name, props=None, nbt=None):
        b = {"pos": T.Tag(T.LIST, (T.INT, [x, y, z])),
             "state": T.Tag(T.INT, self.state(name, props))}
        if nbt:
            b["nbt"] = T.Tag(T.COMPOUND, nbt)
        self.blocks.append(b)

    def save(self, path, size):
        root = T.Tag(T.COMPOUND, {
            "size":        T.Tag(T.LIST, (T.INT, list(size))),
            "entities":    T.Tag(T.LIST, (T.END, [])),
            "blocks":      T.Tag(T.LIST, (T.COMPOUND, self.blocks)),
            "palette":     T.Tag(T.LIST, (T.COMPOUND, self.palette)),
            "DataVersion": T.Tag(T.INT, 3955),
        })
        T.save(path, "", root)
        print(f"{os.path.basename(path):24s} {size[0]}x{size[1]}x{size[2]}  "
              f"{len(self.blocks):4d} blocks  {len(self.palette)} states")


def floor(b, w, d, y=0):
    """Andesite deck with a brass border, so the platform reads as built rather than dumped."""
    for x in range(w):
        for z in range(d):
            edge = x in (0, w - 1) or z in (0, d - 1)
            b.put(x, y, z, TRIM if edge else FLOOR)


def build_tracks():
    b = Build()
    w = SPAN + 5
    d = len(TRACKS) * LANE_GAP + 2
    floor(b, w, d)

    for i, track in enumerate(TRACKS):
        z = 2 + i * LANE_GAP
        ax, bx, y = 2, 2 + SPAN, 1
        material = f"coasters_extras:{track}"
        # A plinth under each end, so the anchorpoints sit on something deliberate.
        b.put(ax, 0, z, PLINTH)
        b.put(bx, 0, z, PLINTH)
        b.put(ax, y, z, ANCHOR, ANCHOR_PROPS,
              anchor_nbt((bx, y, z), SPAN, material, True))
        b.put(bx, y, z, ANCHOR, ANCHOR_PROPS,
              anchor_nbt((ax, y, z), -SPAN, material, False))

    b.save(f"{OUT}/coasters_extras_tracks.nbt", (w, 3, d))


def build_balloons():
    b = Build()
    cols = 4
    rows = (len(BALLOONS) + cols - 1) // cols
    w, d = cols * 3 + 1, rows * 3 + 1
    floor(b, w, d)

    for i, balloon in enumerate(BALLOONS):
        cx = 2 + (i % cols) * 3
        cz = 2 + (i // cols) * 3
        b.put(cx, 0, cz, PLINTH)
        b.put(cx, 1, cz, f"coasters_extras:{balloon}")

    b.save(f"{OUT}/coasters_extras_balloons.nbt", (w, 3, d))


def verify(path):
    """Read it straight back: a structure that fails to parse fails silently in game."""
    _, root = T.load(path)
    d = root.val
    size = d["size"].val[1]
    blocks = d["blocks"].val[1]
    curves = 0
    for blk in blocks:
        nbt = blk.get("nbt")
        if nbt and "AnchorPeerCurves" in nbt.val:
            curves += len(nbt.val["AnchorPeerCurves"].val[1])
    print(f"   verified: size={size} blocks={len(blocks)} curve-ends={curves}")


def main():
    os.makedirs(OUT, exist_ok=True)
    build_tracks()
    verify(f"{OUT}/coasters_extras_tracks.nbt")
    build_balloons()
    verify(f"{OUT}/coasters_extras_balloons.nbt")


if __name__ == "__main__":
    main()
