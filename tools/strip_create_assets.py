"""Removes Create's All-Rights-Reserved textures from our own asset folders.

Create is split-licensed: its CODE is MIT, but everything under `assets/` is All Rights
Reserved. Four of its textures had been copied into every track folder -- inherited from the
source folder the variant generator copies from, and multiplied 55 times by it.

Three of them were never referenced by any model at all. The fourth, `standard_track_crossing`,
is used, so instead of shipping a copy the models are repointed at Create's own resource
location. The texture is loaded from Create's jar at runtime, which is not redistribution --
the same thing our Coaster Controls model already does with `create:block/andesite_casing`.

Run from the repo root:  python tools_strip_create_assets.py
"""
import json
import os
import re

TEX = "src/main/resources/assets/coasters_extras/textures/block/track"
MODELS = "src/main/resources/assets/coasters_extras/models/block/track"

# Copied in, never referenced by a single model. Dead weight and a licence problem at once.
UNUSED = ("andesite_cut_polished.png", "portal_track.png", "portal_track_mip.png")

# Referenced, so it is not enough to delete it -- the models have to point somewhere valid.
SHARED = "standard_track_crossing"
SHARED_PNG = SHARED + ".png"
CREATE_REF = "create:block/" + SHARED

REF = re.compile(r'"coasters_extras:block/track/[^"/]+/' + SHARED + '"')


def main():
    removed = 0
    for folder in sorted(os.listdir(TEX)):
        d = os.path.join(TEX, folder)
        if not os.path.isdir(d):
            continue
        for name in UNUSED + (SHARED_PNG,):
            p = os.path.join(d, name)
            if os.path.exists(p):
                os.remove(p)
                removed += 1

    repointed = 0
    for root, _, files in os.walk(MODELS):
        for fn in files:
            if not fn.endswith(".json"):
                continue
            p = os.path.join(root, fn)
            text = open(p, encoding="utf8").read()
            new = REF.sub('"' + CREATE_REF + '"', text)
            if new != text:
                open(p, "w", encoding="utf8", newline="\n").write(new)
                repointed += 1

    print(f"deleted {removed} copied textures")
    print(f"repointed {repointed} models at {CREATE_REF}")

    # Nothing may still reference a file we just deleted, or the track renders untextured.
    dangling = 0
    for root, _, files in os.walk(MODELS):
        for fn in files:
            if not fn.endswith(".json"):
                continue
            body = open(os.path.join(root, fn), encoding="utf8").read()
            for stem in [u[:-4] for u in UNUSED] + [SHARED]:
                if f'coasters_extras:block/track/' in body and f'/{stem}"' in body:
                    print(f"  DANGLING {os.path.join(root, fn)} -> {stem}")
                    dangling += 1
    print(f"dangling references: {dangling}")


if __name__ == "__main__":
    main()
