# Third-party notices

Create: Coasters Extras is split-licensed — MIT code, All Rights Reserved assets. See `LICENSE.md`.

It is an addon, and it ships work derived from other MIT-licensed projects. MIT requires
their notices to travel with any copy that includes substantial portions of their work, so
they are recorded here and shipped inside the jar. That obligation is unaffected by this
mod's own license: MIT permits use inside a proprietary work precisely as long as these
notices are kept, and they are.

---

## Create: Coasters Simulated — SilverGold

<https://modrinth.com/mod/create-coasters-simulated> · MIT License

The mod this one is an addon for. The coaster physics, the anchorpoint and bezier track
system, the cart system and the original track artwork are its work, and none of this mod
runs without it.

Material derived from it and shipped here:

- **Track model sets.** Every `assets/coasters_extras/models/block/track/<name>_track/`
  folder — the segment, tie and anchorpoint geometry — began as a copy of theirs, repathed
  to our own textures. The geometry is unchanged; only the material it is drawn in differs.
- **Ponder scene structures.** The six `assets/coasters_extras/ponder/track/*.nbt` files are
  derived from their `ponder_cart.nbt`: the same capture of two anchorpoints and a curve,
  with the curve's track material rewritten and the surplus layers removed. Hand-authoring a
  valid `BezierConnection` was not practical, and starting from a known-good one was.
- **Creative tab sections.** `client/TabSections.java` is adapted from their sectioned
  creative tab. Theirs is data-driven — sections from JSON, colours from Veil, items bound
  through Registrate — and only the underlying mechanism was kept: reserve a grid row per
  section by emitting empty stacks, then draw a banner over it.
- **Ponder cart rendering.** The scenes call their `CoasterCartPonderExtras` and
  `CoasterCartExtrasElement` to draw carts, because a cart's body comes from a Flywheel
  visual and Ponder does not run those.

## Create — the Create team

<https://github.com/Creators-of-Create/Create> · MIT License

Compiled against, and its `TrackMaterialFactory`, `ScrollValueBehaviour`,
`CenteredSideValueBoxTransform` and Ponder integration are used as public API. Create is a
required runtime dependency and is not bundled.

---

## Not bundled

Create, Create: Coasters Simulated, Sable, Flywheel, Ponder, Registrate and Veil are all
`compileOnly` and declared as runtime dependencies in `neoforge.mods.toml`. None of them is
shaded or redistributed inside this jar.
