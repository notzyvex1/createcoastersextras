"""Collects every image either page uses into one numbered upload folder, with a manifest.

Numbered in the order they appear on the page. That is the whole point: uploading three dozen
images and then matching each returned CDN URL back to the right slot is the tedious, easy-to-
get-wrong part of this job, and doing it in page order means you work straight down the list
without hunting.

Images already hosted on the Modrinth CDN are downloaded and included too. They are the banner
and two showcase shots the description already links to -- leaving them out would mean a folder
that is "all the images" except the three most prominent ones, and a page still depending on
URLs from an older upload.
"""
import json
import os
import re
import shutil
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GENERATED = os.path.join(os.path.expanduser("~"), "Downloads", "CoastersExtras-Description")
OUT = os.path.join(os.path.expanduser("~"), "Downloads", "CoastersExtras-UPLOAD")
SOURCES = ["CURSEFORGE-IMAGES.md", "MODRINTH-IMAGES.md"]

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) coaster-tooling/1.0"
IMAGE = re.compile(r"!\[([^\]]*)\]\(([^)]+)\)")


def describe(name):
    """Plain-English label, so the manifest is readable without opening every file."""
    if name.endswith("-header.png"):
        return "section header: " + name[:-11].replace("-", " ")
    if name.startswith("strip-"):
        return "showcase strip: " + name[6:-4].replace("-", " ")
    if name.startswith("social-"):
        return "social icon: " + name[7:-4]
    if name.startswith("icon-"):
        return "bullet icon: " + name[5:-4]
    return "existing image"


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)

    # Page order, first appearance wins. Both pages are scanned because the two differ
    # slightly and an image used only by one still has to be uploaded.
    order, seen = [], set()
    for source in SOURCES:
        path = os.path.join(ROOT, source)
        if not os.path.exists(path):
            continue
        for alt, url in IMAGE.findall(open(path, encoding="utf8").read()):
            key = url.rsplit("/", 1)[-1].split("?")[0]
            if key not in seen:
                seen.add(key)
                order.append((alt, url, key))

    print(f"{len(order)} distinct images referenced across both pages\n")
    print(f"{'#':<4}{'file':<38}{'source':<12}what")
    print("-" * 92)

    manifest, failed = [], []
    for i, (alt, url, key) in enumerate(order, 1):
        # Numeric prefix so the upload UI lists them in page order rather than alphabetically.
        out_name = f"{i:02d}_{key}"
        target = os.path.join(OUT, out_name)

        if url.startswith("http"):
            try:
                req = urllib.request.Request(url, headers={"User-Agent": UA})
                with urllib.request.urlopen(req, timeout=45) as r, open(target, "wb") as fh:
                    fh.write(r.read())
                origin = "downloaded"
            except Exception as e:
                failed.append((key, str(e)[:60]))
                print(f"{i:<4}{out_name:<38}{'FAILED':<12}{e}")
                continue
        else:
            local = os.path.join(GENERATED, key)
            if not os.path.exists(local):
                failed.append((key, "not found in the generated folder"))
                print(f"{i:<4}{out_name:<38}{'MISSING':<12}{local}")
                continue
            shutil.copyfile(local, target)
            origin = "generated"

        what = alt if alt else describe(key)
        manifest.append({"n": i, "file": out_name, "original": key,
                         "alt": alt, "what": what, "origin": origin})
        print(f"{i:<4}{out_name:<38}{origin:<12}{what}")

    # Social icons whose URL is not filled in yet are not referenced by either page, so the
    # scan above never sees them. They are included anyway: the alternative is uploading now,
    # adding a Discord link next week, and having to come back and upload one more file.
    extras = sorted(f for f in os.listdir(GENERATED)
                    if f.startswith("social-") and f not in seen)
    for i, name in enumerate(extras, len(manifest) + 1):
        out_name = f"{i:02d}_{name}"
        shutil.copyfile(os.path.join(GENERATED, name), os.path.join(OUT, out_name))
        manifest.append({"n": i, "file": out_name, "original": name, "alt": "",
                         "what": describe(name) + "  (no URL yet -- upload for later)",
                         "origin": "generated"})
        print(f"{i:<4}{out_name:<38}{'spare':<12}{describe(name)}  (not linked yet)")

    with open(os.path.join(OUT, "MANIFEST.json"), "w", encoding="utf8") as fh:
        json.dump(manifest, fh, indent=2)

    # A plain checklist to tick off while uploading, since the JSON is for the next script
    # and not for a human working through a web form.
    with open(os.path.join(OUT, "UPLOAD-ORDER.txt"), "w", encoding="utf8") as fh:
        fh.write("Upload in this order. Paste each returned URL beside its number.\n")
        fh.write("=" * 78 + "\n\n")
        for row in manifest:
            fh.write(f"{row['n']:>3}. {row['file']}\n")
            fh.write(f"     {row['what']}\n")
            fh.write(f"     URL: ________________________________________\n\n")

    total = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT))
    print(f"\n{len(manifest)} files, {total // 1024} KiB -> {OUT}")
    if failed:
        print(f"\n{len(failed)} failed:")
        for name, why in failed:
            print(f"    {name:<44}{why}")


if __name__ == "__main__":
    main()
