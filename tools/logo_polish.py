"""Repairs the mod icon: the patches that never took the rainbow, and the pitted gold band.

Three separate defects, three separate treatments.

1. DAMAGE -- the track's side faces around the lower left kept their original dark maroon,
   teal and olive. Each is repaired to the hue of its nearest healthy rainbow pixel, found
   by a vector distance transform so "nearest" means nearest *across the image* rather than
   nearest along a path through the damage. Path-following picked a colour from the far side
   of the loop for enclosed patches, which is what turned one wedge yellow.

2. STREAKING -- the damaged regions carried fine diagonal striations. Repairing hue alone
   kept them, so brightness is smoothed across repaired pixels only. Region-level darkness
   survives, which matters: a side face is meant to sit darker than the top face above it.

3. THE GOLD BAND -- the arc at the top runs at a third of the saturation of the rest of the
   sweep, so it reads as dirty tan, and it is pitted with dark specks. It is despeckled with
   a median and lifted back to a saturation that matches the rest of the icon.

Nothing is repainted by hand and nothing is invented: every repaired pixel takes its colour
from artwork that was already there.

Run from the repo root:  python tools_logo_polish.py
"""
import colorsys

from PIL import Image

SRC = "src/main/resources/icon.png"
OUT = "branding/icon_polished.png"
COMPARE = "build/logo_compare.png"

# Confidently part of the rainbow. The backdrop never reaches this; it sits around s=0.55.
GOOD_S, GOOD_V = 0.70, 0.80

# Carries real colour but is too dark to belong to the sweep.
BAD_S, BAD_V = 0.28, 0.72

# The backdrop, excluded by hue AND saturation together, so the saturated blue stretch of the
# rainbow -- same hue, far more colour -- is not mistaken for it.
BG_H_LO, BG_H_HI, BG_S_MAX = 185 / 360, 235 / 360, 0.70

# The gold arc: warm, undersaturated, not dark enough to count as damage.
GOLD_H_LO, GOLD_H_HI = 0.02, 0.16
GOLD_S_LO, GOLD_S_HI = 0.20, 0.70
GOLD_V_LO = 0.35
GOLD_TARGET_S = 0.64          # what the rest of the sweep reads at, once shading is allowed for

KEEP_V = 0.60                 # how much of a repaired pixel's own brightness survives
MIN_S = 0.72
SMOOTH_PASSES = 2


def load():
    im = Image.open(SRC).convert("RGBA")
    w, h = im.size
    px = im.load()
    hsv = [None] * (w * h)
    alpha = bytearray(w * h)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            i = y * w + x
            alpha[i] = 1 if a >= 200 else 0
            if a >= 200:
                hsv[i] = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return im, w, h, hsv, alpha


def classify(w, h, hsv, alpha):
    good, bad, gold = bytearray(w * h), bytearray(w * h), bytearray(w * h)
    for i in range(w * h):
        if not alpha[i]:
            continue
        hue, s, v = hsv[i]
        if s >= GOOD_S and v >= GOOD_V:
            good[i] = 1
        elif GOLD_H_LO <= hue <= GOLD_H_HI and GOLD_S_LO <= s <= GOLD_S_HI and v >= GOLD_V_LO:
            gold[i] = 1                      # the tan arc: dull, not damaged
        elif s >= BAD_S and v < BAD_V:
            if BG_H_LO <= hue <= BG_H_HI and s < BG_S_MAX:
                continue                     # backdrop
            bad[i] = 1
    return good, bad, gold


def largest_component(w, h, mask):
    """Keeps only the biggest connected run of a mask.

    The gold test is a colour test, and colour alone cannot tell the arc at the top of the
    loop from an unrelated olive wedge at the bottom that happens to share its hue. Treating
    that wedge as gold made it *more* yellow when it should have been repaired to the sweep.
    Keeping only the largest connected region separates them without hand-picking coordinates.
    """
    from collections import deque
    seen = bytearray(w * h)
    best, best_size = None, 0
    for start in range(w * h):
        if not mask[start] or seen[start]:
            continue
        comp = []
        q = deque([start])
        seen[start] = 1
        while q:
            i = q.popleft()
            comp.append(i)
            x, y = i % w, i // w
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h):
                    continue
                j = ny * w + nx
                if mask[j] and not seen[j]:
                    seen[j] = 1
                    q.append(j)
        if len(comp) > best_size:
            best, best_size = comp, len(comp)
    kept = bytearray(w * h)
    for i in (best or ()):
        kept[i] = 1
    return kept, best_size


def nearest_source(w, h, good):
    """Vector distance transform: every pixel learns the coordinates of its nearest source."""
    BIG = 1 << 30
    sx = [-1] * (w * h)
    sy = [-1] * (w * h)
    dist = [BIG] * (w * h)
    for i in range(w * h):
        if good[i]:
            sx[i], sy[i], dist[i] = i % w, i // w, 0

    def relax(x, y, i, nx, ny):
        if not (0 <= nx < w and 0 <= ny < h):
            return
        j = ny * w + nx
        if sx[j] < 0:
            return
        dx, dy = x - sx[j], y - sy[j]
        d = dx * dx + dy * dy
        if d < dist[i]:
            dist[i] = d
            sx[i] = sx[j]
            sy[i] = sy[j]

    for y in range(h):                       # forward
        for x in range(w):
            i = y * w + x
            relax(x, y, i, x - 1, y)
            relax(x, y, i, x, y - 1)
            relax(x, y, i, x - 1, y - 1)
            relax(x, y, i, x + 1, y - 1)
    for y in range(h - 1, -1, -1):           # backward
        for x in range(w - 1, -1, -1):
            i = y * w + x
            relax(x, y, i, x + 1, y)
            relax(x, y, i, x, y + 1)
            relax(x, y, i, x + 1, y + 1)
            relax(x, y, i, x - 1, y + 1)
    return sx, sy


def main():
    im, w, h, hsv, alpha = load()
    good, bad, gold = classify(w, h, hsv, alpha)
    arc, arc_size = largest_component(w, h, gold)
    for i in range(w * h):
        if gold[i] and not arc[i]:
            # Same hue as the arc but nowhere near it: damage, not the band.
            gold[i] = 0
            bad[i] = 1
    gold = arc
    print(f"rainbow {sum(good)}  damaged {sum(bad)}  gold arc {arc_size}  of {w * h}")

    sx, sy = nearest_source(w, h, good)

    # --- 1. hue repair -------------------------------------------------------------
    hue_out = [None] * (w * h)
    val_out = [None] * (w * h)
    for i in range(w * h):
        if not bad[i] or sx[i] < 0:
            continue
        shue, _, sv = hsv[sy[i] * w + sx[i]]
        _, s0, v0 = hsv[i]
        hue_out[i] = shue
        val_out[i] = v0 * KEEP_V + sv * (1 - KEEP_V)

    # --- 2. brightness smoothing, repaired pixels only ------------------------------
    for _ in range(SMOOTH_PASSES):
        updated = dict()
        for i in range(w * h):
            if val_out[i] is None:
                continue
            x, y = i % w, i // w
            tot, n = 0.0, 0
            for dy in (-2, -1, 0, 1, 2):
                for dx in (-2, -1, 0, 1, 2):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < w and 0 <= ny < h):
                        continue
                    j = ny * w + nx
                    if val_out[j] is None:
                        continue
                    tot += val_out[j]
                    n += 1
            if n:
                updated[i] = tot / n
        for i, v in updated.items():
            val_out[i] = v

    out = im.copy()
    opx = out.load()
    src_px = im.load()
    for i in range(w * h):
        if val_out[i] is None:
            continue
        _, s0, _ = hsv[i]
        r, g, b = colorsys.hsv_to_rgb(hue_out[i], max(s0, MIN_S), val_out[i])
        x, y = i % w, i // w
        opx[x, y] = (int(r * 255), int(g * 255), int(b * 255), src_px[x, y][3])
    print(f"repaired {sum(1 for v in val_out if v is not None)} px")

    # --- 3. the gold band: despeckle, then bring the colour back --------------------
    tmp = out.copy()
    tpx = tmp.load()
    lifted = 0
    for i in range(w * h):
        if not gold[i]:
            continue
        x, y = i % w, i // w
        rs, gs, bs = [], [], []
        for dy in (-2, -1, 0, 1, 2):
            for dx in (-2, -1, 0, 1, 2):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h):
                    continue
                if not gold[ny * w + nx]:
                    continue                 # only ever average gold with gold
                r, g, b, _ = opx[nx, ny]
                rs.append(r); gs.append(g); bs.append(b)
        interior = len(rs) >= 14
        if len(rs) >= 5:
            rs.sort(); gs.sort(); bs.sort()
            m = len(rs) // 2
            r, g, b = rs[m], gs[m], bs[m]
        else:
            r, g, b, _ = opx[x, y]
        hue, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        # Edge pixels are part arc, part backdrop; saturating that blend of orange and blue
        # is what speckled the tail with green. Despeckle them, but leave their colour be.
        if interior and s >= 0.28:
            r, g, b = colorsys.hsv_to_rgb(hue, max(s, GOLD_TARGET_S), max(v, 0.55))
        else:
            r, g, b = colorsys.hsv_to_rgb(hue, s, v)
        tpx[x, y] = (int(r * 255), int(g * 255), int(b * 255), src_px[x, y][3])
        lifted += 1
    print(f"gold band cleaned {lifted} px")

    tmp.save(OUT)
    print("wrote", OUT)

    gap = 24
    sheet = Image.new("RGBA", (w * 2 + gap, h), (18, 18, 22, 255))
    sheet.paste(im, (0, 0))
    sheet.paste(tmp, (w + gap, 0))
    sheet.save(COMPARE)
    print("wrote", COMPARE)


if __name__ == "__main__":
    main()
