"""Substitutes real uploaded image URLs into both store pages.

build_pages.py assumes one base URL and appends filenames to it. That works for a GitHub repo
or any host that keeps your folder structure, and it does NOT work for Modrinth or CurseForge:
both rename every upload to a content hash, so 35 uploads give 35 unrelated URLs with no shared
prefix. This maps them one at a time instead.

Input is UPLOAD-ORDER.txt from the upload folder, with the blanks filled in:

      1. 01_banner.png
         Create: Coasters Extras
         URL: https://cdn.modrinth.com/data/cached_images/abc123.png

Anything still blank is reported and left pointing at the placeholder, so a half-finished
upload produces a page with obvious gaps rather than one that looks finished and has three
silently broken images in the middle.

    python tools/apply_urls.py                  read the checklist, rewrite both pages
    python tools/apply_urls.py --check          report what is filled in, change nothing
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UPLOAD = os.path.join(os.path.expanduser("~"), "Downloads", "CoastersExtras-UPLOAD")
CHECKLIST = os.path.join(UPLOAD, "UPLOAD-ORDER.txt")
PAGES = ["CURSEFORGE-IMAGES.md", "MODRINTH-IMAGES.md"]
PLACEHOLDER = "PASTE_YOUR_IMAGE_URL_BASE_HERE"

ENTRY = re.compile(r"^\s*(\d+)\.\s+(\S+)\s*$")
URL_LINE = re.compile(r"^\s*URL:\s*(\S+)\s*$")


def read_checklist():
    """filename -> url, for every entry whose URL line has been filled in."""
    if not os.path.exists(CHECKLIST):
        sys.exit(f"No checklist at {CHECKLIST}\n"
                 f"Run tools/collect_upload_folder.py first.")
    mapping, blank, current = {}, [], None
    for line in open(CHECKLIST, encoding="utf8"):
        entry = ENTRY.match(line)
        if entry:
            current = entry.group(2)
            continue
        url = URL_LINE.match(line)
        if url and current:
            value = url.group(1)
            # The blank in the template is a run of underscores.
            if set(value) <= set("_"):
                blank.append(current)
            else:
                mapping[current] = value
            current = None
    return mapping, blank


def main():
    mapping, blank = read_checklist()
    print(f"{len(mapping)} URLs filled in, {len(blank)} still blank\n")

    if blank:
        print("still blank:")
        for name in blank:
            print(f"    {name}")
        print()

    if "--check" in sys.argv:
        for name, url in sorted(mapping.items()):
            print(f"  {name:<40}{url}")
        return

    if not mapping:
        sys.exit("Nothing to apply -- no URL lines have been filled in yet.")

    # Numbered names in the folder (01_foo.png) map back to the original (foo.png), which is
    # what the pages actually reference.
    by_original = {}
    for numbered, url in mapping.items():
        original = re.sub(r"^\d+_", "", numbered)
        by_original[original] = url

    for page in PAGES:
        path = os.path.join(ROOT, page)
        if not os.path.exists(path):
            print(f"{page:<26}missing -- skipped")
            continue
        text = open(path, encoding="utf8").read()
        swapped, missed = 0, set()

        def replace(match):
            nonlocal swapped
            url = match.group(2)
            name = url.rsplit("/", 1)[-1]
            if name in by_original:
                swapped += 1
                return f"![{match.group(1)}]({by_original[name]})"
            if PLACEHOLDER in url:
                missed.add(name)
            return match.group(0)

        text = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", replace, text)
        open(path, "w", encoding="utf8", newline="\n").write(text)

        state = "READY" if not missed else f"{len(missed)} still placeholder"
        print(f"{page:<26}{swapped} images pointed at real URLs   {state}")
        for name in sorted(missed):
            print(f"    missing: {name}")


if __name__ == "__main__":
    main()
