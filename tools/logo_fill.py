"""Fills the holes bitten out of the icon's track. Changes nothing else.

Earlier attempts recoloured things. They should not have: the colours were right, it is the
missing chunks that were wrong. So this only ever writes pixels that are currently backdrop
and should not be, and every other pixel comes out byte-identical.

Telling damage apart from design is the whole problem. The artwork is full of deliberate
gaps -- the spaces between the ladder rungs, the notched cut-outs down the loop -- and those
must survive. The tell is the white outline: every intentional gap in this artwork is traced
with one, and the eroded bites are raw backdrop sitting straight against raw track. So a
pixel is only filled if it is well enclosed by track AND has no outline anywhere near it.

Filled pixels take the median colour of the track around them, so they match whatever part
of the gradient they landed in.

Run from the repo root:  python tools_logo_fill.py
"""
import colorsys

from PIL import Image

SRC = "branding/icon_before_polish.png"
OUT = "branding/icon_filled.png"
COMPARE = "build/logo_fill_compare.png"

OUTLINE_S, OUTLINE_V = 0.30, 0.72
BG_H_LO, BG_H_HI, BG_S_MAX = 180 / 360, 240 / 360, 0.72

# Of the 24 neighbours in a 5x5, how many must be track before a backdrop pixel counts as a
# hole. High, because a genuine hole is surrounded on nearly every side.
ENCLOSED = 14
PASSES = 6                      # each pass eats one ring off the edge of a larger hole

# Radius of the closing that repairs chewed EDGES. Enclosure alone cannot fix those: a bite
# taken out of the side of the band is open to the backdrop, so it is never surrounded. A
# closing fills any notch narrower than twice this and leaves the silhouette otherwise
# untouched. Kept small so the intentional gaps -- the rung spaces on the arc, the notched
# cut-outs down the loop -- are far too wide to be bridged.
CLOSE_R = 2
# Repeated rather than done once at a bigger radius: a wide closing bridges anything narrow,
# including the rung gaps on the arc. Small closings applied in turn follow the band's shape
# and only ever eat into a notch from its own walls.
CLOSE_ITERS = 3


def main():
    im = Image.open(SRC).convert("RGBA")
    w, h = im.size
    px = im.load()
    n = w * h

    track = bytearray(n)
    outline = bytearray(n)
    backdrop = bytearray(n)
    for y in range(h):
        for x in range(w):
            i = y * w + x
            r, g, b, a = px[x, y]
            if a < 200:
                continue
            hue, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < OUTLINE_S and v > OUTLINE_V:
                outline[i] = 1
            elif BG_H_LO <= hue <= BG_H_HI and s < BG_S_MAX:
                backdrop[i] = 1
            else:
                track[i] = 1

    out = im.copy()
    opx = out.load()
    total = 0

    def outline_near(x, y, r=2):
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and outline[ny * w + nx]:
                    return True
        return False

    # ---- closing: dilate the track, erode it back, and keep what that recovered ----
    disc = [(dx, dy) for dy in range(-CLOSE_R, CLOSE_R + 1)
                     for dx in range(-CLOSE_R, CLOSE_R + 1)
            if dx * dx + dy * dy <= CLOSE_R * CLOSE_R]

    for it in range(CLOSE_ITERS):
        dil = bytearray(n)
        for i in range(n):
            if not track[i]:
                continue
            x, y = i % w, i // w
            for dx, dy in disc:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    dil[ny * w + nx] = 1

        closed = []
        for i in range(n):
            if track[i] or not backdrop[i]:
                continue
            x, y = i % w, i // w
            if outline_near(x, y):
                continue                     # a drawn gap, not damage
            ok = True
            for dx, dy in disc:
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h) or not dil[ny * w + nx]:
                    ok = False
                    break
            if ok:
                closed.append(i)

        if not closed:
            break
        for i in closed:
            x, y = i % w, i // w
            cols = [opx[x + dx, y + dy]
                    for dy in (-2, -1, 0, 1, 2) for dx in (-2, -1, 0, 1, 2)
                    if 0 <= x + dx < w and 0 <= y + dy < h and track[(y + dy) * w + (x + dx)]]
            if not cols:
                continue
            rs = sorted(c[0] for c in cols); gs = sorted(c[1] for c in cols)
            bs = sorted(c[2] for c in cols); m = len(rs) // 2
            opx[x, y] = (rs[m], gs[m], bs[m], 255)
            backdrop[i] = 0
            track[i] = 1
            total += 1
        print(f"closing {it + 1}: recovered {len(closed)} px")

    for p in range(PASSES):
        candidates = []
        for i in range(n):
            if not backdrop[i]:
                continue
            x, y = i % w, i // w
            near = 0
            touches_outline = False
            for dy in (-2, -1, 0, 1, 2):
                for dx in (-2, -1, 0, 1, 2):
                    if dx == 0 and dy == 0:
                        continue
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < w and 0 <= ny < h):
                        continue
                    j = ny * w + nx
                    if outline[j]:
                        touches_outline = True
                        break
                    if track[j]:
                        near += 1
                if touches_outline:
                    break
            # An outline anywhere close means this gap was drawn on purpose. Leave it.
            if touches_outline or near < ENCLOSED:
                continue
            candidates.append(i)

        if not candidates:
            break

        for i in candidates:
            x, y = i % w, i // w
            cols = []
            for dy in (-2, -1, 0, 1, 2):
                for dx in (-2, -1, 0, 1, 2):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < w and 0 <= ny < h):
                        continue
                    j = ny * w + nx
                    if track[j]:
                        cols.append(opx[nx, ny])
            if not cols:
                continue
            rs = sorted(c[0] for c in cols)
            gs = sorted(c[1] for c in cols)
            bs = sorted(c[2] for c in cols)
            m = len(rs) // 2
            opx[x, y] = (rs[m], gs[m], bs[m], 255)
            backdrop[i] = 0
            track[i] = 1
            total += 1
        print(f"pass {p + 1}: filled {len(candidates)}")

    print(f"filled {total} px in total")

    # Prove nothing else moved.
    src = im.load()
    touched = 0
    for y in range(h):
        for x in range(w):
            if src[x, y] != opx[x, y]:
                touched += 1
    print(f"pixels differing from the original: {touched}")

    out.save(OUT)
    gap = 24
    sheet = Image.new("RGBA", (w * 2 + gap, h), (18, 18, 22, 255))
    sheet.paste(im, (0, 0))
    sheet.paste(out, (w + gap, 0))
    sheet.save(COMPARE)
    print("wrote", OUT, "and", COMPARE)


if __name__ == "__main__":
    main()
