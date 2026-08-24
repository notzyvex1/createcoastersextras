"""Cross-check every asset reference in the built jar actually resolves."""
import zipfile, json, sys, re

import sys, glob
# take the jar as an argument; a hardcoded version silently checked a stale build
JAR = sys.argv[1] if len(sys.argv) > 1 else sorted(glob.glob('build/libs/coasters_extras-*.jar'))[-1]
NS  = "coasters_extras"
z   = zipfile.ZipFile(JAR)
N   = set(z.namelist())
problems = []

def res(ref, kind):
    """resource id -> jar path. kind: 'textures' or 'models'"""
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    ext = ".png" if kind == "textures" else ".json"
    if kind == "textures":
        return f"assets/{ns}/textures/{path}{ext}"
    return f"assets/{ns}/models/{path}{ext}"

# ---------------------------------------------------------------- blockstates
bs = [n for n in N if n.startswith(f"assets/{NS}/blockstates/") and n.endswith(".json")]
for b in bs:
    d = json.loads(z.read(b).decode())
    for variant, v in d.get("variants", {}).items():
        entries = v if isinstance(v, list) else [v]
        for e in entries:
            m = res(e["model"], "models")
            if m not in N:
                problems.append(f"blockstate {b.split('/')[-1]} -> missing model {e['model']}")

# ---------------------------------------------------------------- models
models = [n for n in N if n.startswith(f"assets/{NS}/models/") and n.endswith(".json")]
for m in models:
    d = json.loads(z.read(m).decode())
    for slot, ref in (d.get("textures") or {}).items():
        if ref.startswith("#"):
            continue
        p = res(ref, "textures")
        # vanilla/create textures live in other jars - only check our own
        if p.startswith(f"assets/{NS}/") and p not in N:
            problems.append(f"model {m.split('/')[-1]} slot {slot} -> missing texture {ref}")
    par = d.get("parent")
    if par and par.startswith(NS + ":"):
        if res(par, "models") not in N:
            problems.append(f"model {m.split('/')[-1]} -> missing parent {par}")
    obj = d.get("model")
    if obj and obj.startswith(NS + ":"):
        p = "assets/" + obj.split(":")[0] + "/" + obj.split(":")[1]
        if p not in N:
            problems.append(f"model {m.split('/')[-1]} -> missing OBJ {obj}")

# ---------------------------------------------------------------- lang
lang = json.loads(z.read(f"assets/{NS}/lang/en_us.json").decode())

BALLOONS = ["white","orange","magenta","light_blue","yellow","lime","pink","gray",
            "light_gray","cyan","purple","blue","brown","green","black","rainbow"]
TRACKS   = ["oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry",
            "bamboo","crimson","warped","stone","deepslate","andesite","granite","diorite",
            "boost","sensor","brake"]

for c in BALLOONS:
    for k in (f"block.{NS}.{c}_balloon", f"item.{NS}.{c}_balloon"):
        if k not in lang: problems.append(f"lang missing {k}")
    for p in (f"assets/{NS}/blockstates/{c}_balloon.json",
              f"assets/{NS}/models/block/balloon/{c}_balloon.json",
              f"assets/{NS}/models/item/{c}_balloon.json",
              f"assets/{NS}/textures/block/balloon/{c}_balloon.png",
              f"assets/{NS}/textures/item/{c}_balloon.png",
              f"data/{NS}/loot_table/blocks/{c}_balloon.json"):
        if p not in N: problems.append(f"balloon {c}: missing {p.split('/')[-2]}/{p.split('/')[-1]}")

for t in TRACKS:
    for k in (f"item.{NS}.{t}_track", f"block.{NS}.{t}_track_material"):
        if k not in lang: problems.append(f"lang missing {k}")
    for p in (f"assets/{NS}/blockstates/{t}_track_material.json",
              f"assets/{NS}/models/block/{t}_track_material.json",
              f"assets/{NS}/models/item/{t}_track.json",
              f"assets/{NS}/textures/item/{t}_track.png",
              f"assets/{NS}/textures/block/track/{t}_track/standard_track.png",
              f"assets/{NS}/models/block/track/{t}_track/segment_left.obj"):
        if p not in N: problems.append(f"track {t}: missing {p.split('/')[-1]}")

# ---------------------------------------------------------------- mixins
mx = json.loads(z.read(f"{NS}.mixins.json").decode())
for m in mx["mixins"]:
    p = f"dev/notzyvex/{NS}/mixin/{m}.class"
    if p not in N: problems.append(f"mixin class missing: {m}")

# ---------------------------------------------------------------- report
print(f"jar entries: {len(N)}")
print(f"blockstates: {len(bs)} | models: {len(models)} | lang keys: {len(lang)}")
print(f"mixins: {mx['mixins']}")
print()
if problems:
    print(f"!!! {len(problems)} PROBLEMS")
    for p in problems[:40]: print("   ", p)
else:
    print("ALL REFERENCES RESOLVE - no missing models, textures, lang or mixins")
