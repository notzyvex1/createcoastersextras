# Create: Coasters Extras — Project State

> **Read this first after any context reset.** It holds every decision made so far.
> Last updated: 2026-08-23

---

## 0. Where things stand right now (read this before anything else)

> Last updated: 2026-08-23

**Version 1.3.0, built and installed in the Modrinth profile, not yet released.**

`build/libs/coasters_extras-neoforge-1.21.1-1.3.0.jar` is current with the source.

### The version number went DOWN on purpose

2.1.1 -> 1.3.0, at the owner's instruction. Nothing was ever published as 2.x -- there is no
2.0.0 anywhere in git or on Modrinth -- so the 2.x numbering was a local accident rather than a
shipped history. Do not "fix" this back up to 2.x.

### Functional tracks now shipping

Boost, Brake, Sensor, Station, Slippery, Bobsled, **Splash**, **Launch**, **Reverse**.

Each names its own dial setting: Boost Speed / Launch Speed / Water Boost / Reverse Boost /
Braking Sensitivity. A station's row 0 is dwell SECONDS and is clamped to 60; every other
track's row 0 is a speed and reaches 200.

### The alias mixin is a standing trap -- read this before writing any id comparison

`ResourceLocationCoasterAliasMixin` makes EVERY `coasters_extras:<x>_track` id report
`.equals()` true against `simulatedcoasters:coaster_track`, symmetrically, and every one of our
~200 track paths ends in `_track` -- so the alias fires for all of them, not a subset.

**Any `id.equals(...)` written against a track material id is a bug by construction.** Three
real bugs came from this and are now fixed; all three were reported by players as unrelated
symptoms:

- `CoasterGuideBobsledMixin` -- plain track got the bobsled's relaxed rail guide, so carts fell
  through ordinary track.
- `CurveRepaintInput` (two sites) -- the "already this material?" guard matched any base-mod
  curve, so painting plain track, converting a section to a Brake, and converting back to plain
  track were all refused.

Compare `getNamespace()` and `getPath()` as raw strings instead. `hashCode` is NOT aliased, so
map lookups are safe and `TrackMaterial.ALL.get(id)` is fine.

### The Send dial is its own behaviour now

`SendDirection` (enum, `INamedIconOptions`) + `SendDirectionBehaviour`
(`ScrollOptionBehaviour`), on its own value box on the anchorpoint's horizontal faces, sitting
0.32 blocks below the speed box.

It had to leave the speed board to be small at all: `ValueSettingsBoard` is a record with ONE
`maxValue` shared by every row, so next to a 0-200 speed the three-way choice was forced onto a
200-notch bar. `ScrollOptionBehaviour` builds its board with `maxValue = options - 1` plus an
icon formatter, which is exactly the Mechanical Bearing control.

Three things it must keep overriding, each a real bug otherwise:
- `getType()` -- behaviours live in a `Map<BehaviourType, ...>`; sharing a type evicts.
- `netId()` -- every behaviour is offered the same `ValueSettingsPacket` and the server matches
  on `netId()`, which defaults to 0 for everything. Two dials both answering 0 means edits land
  on whichever was inserted first.
- `write`/`read` -- `ScrollValueBehaviour` persists under the fixed key `"ScrollValue"` and every
  behaviour is handed the SAME tag, so two of them is last-writer-wins.

Old worlds stored direction as a raw 0-200 bar position under `StationDirection`;
`SendDirection.fromLegacyBar` decodes it, and `toLegacyBar` is written back so a downgrade does
not flip every station.

### Banners

Frame counts are no longer written in Java. The Balloons banner is a vanilla-animated GUI sprite
(`textures/gui/sprites/section/` + `.png.mcmeta`, the mechanic lifted from the owner's own
skyroutes mod); the hover-animated casing banners read their frame count out of the PNG's IHDR.
Hover-only animation is deliberate and mcmeta cannot do it, which is why both paths exist.

### Not done

- The **Create-style Station block + schedule system** was started and **deleted at the owner's
  instruction** ("its trash"). The station TRACK is untouched and still works. Do not rebuild the
  block without being asked.
- In-game verification of the Send dial's box position. It was on the top face first and was
  invisible in practice; it is now on the side faces below the speed box, and that placement has
  not been confirmed by eye.

## 1. What this is

A **NeoForge addon** for the Minecraft mod **Create: Coasters Simulated**, adding more
balloon colors and new coaster track types.

- **Mod name:** Create: Coasters Extras
- **Mod id / namespace:** `coasters_extras`
- **Project folder:** `D:\CreateCoastersExtras`
- **Logo:** user-made — coaster loop + rainbow balloon on a blueprint background

## 2. Build target (LOCKED — must match base mod exactly)

| Thing | Version |
|---|---|
| Minecraft | **1.21.1** |
| Loader | **NeoForge** (NOT Fabric, NOT Forge) |
| Create | **6.0.10+mc1.21.1** |
| Create: Coasters Simulated | **0.1.2 – 0.1.4** |
| Java | **21** |

Base mod jar (for reference/decompile) is cached at:
`<scratchpad>/coasters.jar` — re-downloadable from Modrinth `create-coasters-simulated`.

## 3. Base mod facts (verified by inspecting the jar)

- **License: MIT** — forking/editing/redistribution is legal with attribution.
- No public source repo listed on Modrinth.
- Package root: `dev.silvergold.simulatedcoasters`
- 467 classes, **51 mixins**, uses the **Sable** physics library (by the Create: Aeronautics team).
- Creative tab lang key: `itemGroup.simulatedcoasters` = "Simulated Coasters"

### Registered content (only ONE balloon, ONE track!)
- Blocks: `coaster_anchorpoint`, `coaster_cart`, `coaster_track_material`, `red_balloon`, `rivet`
- Items: those + `coaster_track`, `incomplete_coaster_track`

### Balloon internals
- Single block class `RedBalloonBlock`; logic lives in
  `BalloonTetherController`, `BalloonCascadePop`, `BalloonResolve`, `BalloonPlacement`,
  `BalloonIntegrity`, `BalloonSpawner` (21 classes in `balloon/`).
- **Physics is DATA-DRIVEN** — this is the key enabler:
  `data/simulatedcoasters/physics_block_properties/red_balloon.json`
  ```json
  { "selector": "simulatedcoasters:red_balloon",
    "properties": { "sable:mass": 0.1, "sable:restitution": 0.5,
      "sable:floating_material": "simulatedcoasters:balloon_drag",
      "sable:floating_scale": 0.33 } }
  ```
- Recipe: `<color>_wool` + `string` + `iron_nugget` (vertical), yields 1 balloon.

### Track internals
- Built on **Create's public addon API**:
  `com.simibubi.create.content.trains.track.TrackMaterialFactory`
  → `.make("coaster_track").standardModels().noRecipeGen().build()`
  Registering new track materials needs **no mixins**.
- Geometry is **OBJ files**, not JSON cubes, loaded via `"loader": "neoforge:obj"`:
  `segment_center_beam.obj`, `segment_left.obj`, `segment_right.obj`, `tie.obj`
  (made in Blockbench 5.1.4; coords normalized 0–1; segments are HALF a block long)
- Textures are two-layer: `standard_track.png` (base) + `standard_track_dyed.png`
  (light grayscale, tinted at render time — this is how dyeable tracks work).
- Cart driving lives in `CoasterCartChainLiftDrive` + 23 `chainlift/` classes
  (span registration, edge index, path caching, network payloads). **Do not reimplement.**

## 4. Decisions made

| # | Decision | Reasoning |
|---|---|---|
| 1 | **Addon, not a fork** | Base mod is 0.1.2 and actively developed; a fork means re-merging forever |
| 2 | **15 new dye colors + 1 rainbow** | Completes vanilla's 16; red already exists upstream |
| 3 | **Do NOT register our own red balloon** | Would create a duplicate item AND a conflicting recipe (same wool+string+nugget). Instead, add THEIR `simulatedcoasters:red_balloon` into OUR creative tab — one item, shown in both tabs |
| 4 | **Own creative tab** | `itemGroup.coasters_extras` = "Create: Coasters Extras"; icon = **rainbow balloon**. Own tab won't break if they restructure theirs |
| 5 | Optional later: also inject our balloons into THEIR tab | Discoverability; must be config-gated + defensive (log warning if tab key missing) |
| 6 | **Boost track = powered-rail behavior** | Cart enters segment → block entity applies force along travel direction. Small UI for editing boost power (right-click w/ wrench) |
| 7 | Boost track reuses THEIR rail/tie textures | Only the centre beam changes → reads as same family, no duplicate assets |
| 8 | Track build order | Boost + Brake together (one system, inverted sign) → Station (near-free) → Launch (needs state machine) |
| 9 | **Own a full copy of their track assets** | All 13 textures + 22 model/OBJ/MTL files copied into `coasters_extras:` with refs rewritten. Zero `simulatedcoasters:` references remain → we can retexture anything and nothing breaks if they restructure. MIT allows this; attribution goes in the README |
| 10 | Our copies keep THEIR original filenames | `middle_beam.png`, not `boost_middle_beam.png` — so the retexture lands where their models already look, and the preview resource pack is a straight drop-in override |

## 4b. Track roadmap — TWO separate axes

Do not conflate these; they need different work:

**Axis A — behaviour tracks** (need Java + likely mixins)
Boost → Brake → Station → Launch.

**Axis B — material/cosmetic variants** (texture work + one `TrackMaterialFactory` call each,
NO behaviour code). User wants these in a **later update**:
- Stone (+ likely deepslate/andesite/granite/diorite variants)
- **All wood types** (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry,
  bamboo, crimson, warped = 11)

That's 15-20+ variants. **Generate them with a script**, exactly like the 16 balloons —
recolour/retexture from the copied base textures, emit models + blockstates + lang in a loop.
Precedent: **Create: Steam 'n' Rails** ships ~20 track variants via the same Create API —
it's open-source, read it before building this axis.

## 5. Assets — DONE ✅

Path: `D:\CreateCoastersExtras\assets\coasters_extras\`

```
textures/block/balloon/     16 x 32x32   (15 dyes + rainbow)
textures/item/              16 x 16x16
models/block/balloon/       16 JSON
models/item/                16 JSON
blockstates/                16 JSON
textures/block/coaster_track/  boost_track.png, boost_track_dyed.png,
                               boost_middle_beam.png, boost_middle_beam_dyed.png
models/block/track/boost_track/  *.obj (copied verbatim) + *.json + *_dyed.json
```

**How textures were made:** recolored from the real `red_balloon.png` (not redrawn), so
shading/highlight/knot detail match upstream exactly. Hue+saturation shift to target;
brightness ramp remapped into a range guaranteeing contrast (needed for black).

**Generator scripts** (in scratchpad — regenerate anytime):
`gen_balloons.py`, `gen_bbmodel.py`, `gen_boost_track.py`, `gen_boost_model.py`

**Gotchas already solved — don't regress these:**
- Block texture is a **UV ATLAS**: sides `x0-14,y14-28` | top `x0-14,y0-14` |
  bottom `x14-28,y0-14` | knot+string `x≥14,y≥14`. Sweeping hue across the whole
  image gives each face a different slice — must sweep PER REGION.
- Rainbow gradient must span the **balloon body only**. Detect it as the *contiguous*
  run of wide rows from the top — a plain width filter includes the string (it's
  several px wide) and the spectrum dies at cyan.
- Black needs a minimum brightness range (~0.30) or it collapses to a flat silhouette.

## 6. NOT DONE ❌ — everything Java

- [ ] JDK 21 (machine has JRE only — **no `javac`**). Portable extract to `.tools`.
      Download from Adoptium has failed twice; retry or user installs manually.
- [ ] NeoForge 1.21.1 MDK scaffold + Gradle wrapper
- [ ] Register 16 balloon blocks/items (+ physics JSONs, recipes, loot tables)
- [ ] Creative tab w/ rainbow icon + their red balloon
- [ ] Boost track `TrackMaterial` registration
- [ ] Boost block entity (apply force to carts)
- [ ] Boost power UI (screen + network packet)

### The ONE open technical question
Do the balloon systems match by `instanceof RedBalloonBlock` (→ our blocks just
subclass it, clean) or against the specific red block instance (→ needs a mixin)?
Same question for whether cart velocity is reachable from outside (Sable physics body).
**User says they have a decompiled copy — path still not provided.**

## 7. Related context

- User also runs a Paper 1.21.11 lifesteal server, "DEADLINE SMP"
  (`game.blockbyte.host`, server `3ea841b3`). It is **online and populated** again as of
  2026-08-14; the outage was container DNS on the host's side, not a server fault. Tooling
  lives in `D:\DEADLINE SMP` (`srv.py` for SFTP + panel, `panel.py` for power/commands).
  That's a **separate project** from this mod.
- Base mod's own roadmap already lists "more colors for balloons" and "hydraulic
  launches" — worth contacting the author (MIT, they may welcome contribution).

## Red wheels on the Brake Track -- BUILT, needs in-game verification

All four pieces are in and the project compiles:

1. `CoasterCartDriveMixin`, `brake_track` case -- calls `BrakingTracker.report(x,y,z)` once
   the overspeed passes 2.0 b/s.
2. `net/BrakingTracker` -- collects per tick, republishes only when the block-rounded set
   changes, so a cart sitting on a brake is not a packet per tick.
3. `net/BrakingPayload` + `net/ModNetwork` + `client/BrakingCarts` -- positions on the wire,
   cached client-side with a 400 ms TTL so the glow fades rather than sticking.
4. `mixin/BrakingWheelTintMixin` -- the render hook. In the **client** list of
   `coasters_extras.mixins.json`.

**Positions, not cart ids.** The earlier plan was to sync a `Set<UUID>` of sub levels, but
`renderOneAxis` is handed no cart identity to match a UUID against -- only a `worldOrigin`.
Threading an id down to it would mean editing eight call sites in someone else's renderer.
A distance check against a handful of points costs the same and touches nothing.

### The lambda worry was wrong -- do not re-derive this

An earlier note in this file guessed the `ModelBlockRenderer.renderModel` call sat inside a
lambda (the `invokedynamic` that builds a `WheelPlacementConsumer`) and that a mixin therefore
could not reach it. Checked against 0.1.4 with `javap -c`: it does not. There is exactly **one**
`renderModel` call in the whole class, at offset 191 in `renderOneAxis` itself. The
`invokedynamic` is in `renderStandingAt`, and the lambda it builds (`lambda$renderStandingAt$0`)
only forwards to `renderOneAxis`. No synthetic method has to be targeted.

Two other things confirmed there and worth not re-checking:

- It is the **NeoForge** `renderModel` overload, the one ending `ModelData, RenderType` -- not
  vanilla's nine-argument form. Both exist on `ModelBlockRenderer`; the wrong one will not match.
- `Vec3 worldOrigin` is **parameter 10 of `renderOneAxis`**, so it is live at the call site and
  is reached with `@Local(argsOnly = true)`. It is the only `Vec3` argument, so no ordinal.

`@ModifyArgs` rather than `@Redirect`: a redirect would have to re-issue eleven arguments by
hand and would claim the call site exclusively, which conflicts with any other mod touching it.

**Still unverified:** nobody has watched a cart brake yet. The tint multipliers
(`COASTERS_EXTRAS$TINT_*`) and the 3.5-block match radius in `BrakingCarts` are guesses.

## After the first release

Everything below was designed or half-explored during development and deliberately left out
of the initial release. Recorded here so none of it has to be worked out twice.

### Coaster operations (the big one)
Modelled on Create's train system, deliberately smaller.

- **Station Block** -- a real block, bound by right-clicking a Station Track. Opens a
  Create-style menu: name the stop, set dwell, dispatch.
- **Coaster Controls** -- placed on the front of a cart. Right-click to sit and drive it by
  hand: throttle, direction, max speed.
- **Coaster Schedule** -- an item slotted *into the Controls*, not carried by a driver. With
  one inserted the coaster runs its route unattended. This choice removes driver-mob AI and
  pathfinding entirely, which is where most of the cost in Create's version sits.
- A third creative tab section, **Controls**, for these three.

Rough order and cost: Station Block + menu (~4h, first real GUI, teaches the plumbing the
rest needs) -> Controls with a rideable seat (~5h) -> Schedule format and autopilot (~6h).

### Ponder scenes -- DONE in 0.9.3
Six scenes, one per functional track, in `ponder/TrackScenes.java`, registered by
`ponder/CoastersExtrasPonderPlugin` from `ClientSetup#onClientSetup`.

Things worth not relearning:

- **Implement `PonderPlugin` directly; do not extend `CreatePonderPlugin`.** Its
  `registerSharedText` is namespaced by the *calling* mod, so inheriting it mints a copy of
  Create's whole shared-text table under our id. Create's level-restore handling still
  applies, because Ponder runs the restore hook for every registered plugin -- which is also
  why the base mod's `CoasterPonderRestore` revives *our* curves.
- **Register by ITEM id.** The `*_track_material` blocks have no block item, so a scene keyed
  to the block id can never be opened.
- **Structures are vanilla structure templates, gzipped**, at
  `assets/coasters_extras/ponder/<path>.nbt`. A missing or corrupt one does not crash --
  Ponder logs and renders an empty scene, so check the log, not the screen.
- **Do not hand-synthesise a bezier.** `tools_ponder_nbt.py` starts from the base mod's own
  `ponder_cart.nbt` capture and rewrites only the curve `Material`. Their curve data is
  entirely block-entity-relative (`Positions[0]` is always `[0,0,0]`), so it stays valid at
  any paste origin.
- **Move the cart with `moveSection`, not physics.** Whether Sable ticks inside a
  `PonderLevel` is unverified; a moved section is deterministic and lets each beat land on
  its caption.
- Text lang keys are positional (`text_1`, `text_2`, ...) in program order. Inserting a
  `showText` in the middle silently renumbers everything after it.

## The base mod is pre-1.0 and will break us

SilverGold said this in Discord on 2026-08-05, unprompted:

> you can make add-ons if you'd like to decompile the mod yourself, but just know that I
> make dramatic changes to the mod before it's 1.0 release

and that a public API is deliberately being held back until he has cleaned up and shipped
hotfixes, so addons do not pin him down early.

That is explicit permission to build this, and an explicit warning about what happens next.
Thirteen mixin files target ten of his internal classes, and `injectors.defaultRequire` is 1,
so a single renamed method is a hard crash at startup -- with our mod named in the report.

**The dependency range is therefore pinned to `[0.1.2,0.2.0)`.**

Verified against **0.1.4** (2026-08-06) on the day it shipped: all ten target classes are
signature-identical to 0.1.2, and the bytecode of every method we inject into --
`onPrePhysicsTick`, `apply`, `useSplineRails`, `CurveBerMaterials.from`, `addBehaviours`,
`onLoad` -- is unchanged instruction for instruction. `libs/` now holds 0.1.4 and the build
is made against it; 0.1.2 is kept under `libs/superseded/` for diffing the next one. It was `[0.1.2,)`, which
would have let our mod load against a version it cannot possibly work with and crash there.
Pinned, a user on a newer base mod gets a dependency screen naming the version they need,
which is a support question rather than a bug report for two people.

When he does ship an update, expect a compatibility pass rather than a patch: re-run javap
over the new jar, re-check every `targets =` and every `@At` descriptor, then widen the range
to the new version once it builds and runs. Do not widen the range hopefully.

### Pick-block returns THEIR track for OUR curves -- deferred, cause confirmed
Middle-clicking one of our curves puts `simulatedcoasters:coaster_track` in the hotbar
instead of ours. `PickBlockOurTrackMixin` was written to correct this and does not work.

Cause, read out of the bytecode of `MinecraftPickBlockFakeTrackMixin`:

```
AnchorPeerCurveHit.curve().getMaterial().id .equals( CoasterTrackMaterials.COASTER.id )
   -> new ItemStack(SimulatedCoasters.COASTER_TRACK.get())
```

and a second branch doing the same via `TrackBlockOutline.result`. Both hardcode their item.
Their check passes for our curves **because `ResourceLocationCoasterAliasMixin` deliberately
makes our ids compare equal to theirs** -- so this is self-inflicted, and the alias mixin is
load-bearing elsewhere, so it cannot simply be dropped.

Two things to fix when picking this up:
1. **We are correcting at the wrong point.** Their mixin sets the picked stack itself. Rather
   than injecting at `Minecraft#pickBlock` RETURN and overwriting afterwards, redirect the
   `SimulatedCoasters.COASTER_TRACK` GETSTATIC in *their* mixin -- the same technique that
   fixed the Ponder material bug in `CurveBerMaterialsMixin`, which is known to work.
2. **The correction never reaches the server.** `inv.setItem(inv.selected, stack)` is
   client-only. Vanilla pick-block in creative goes through
   `gameMode.handleCreativeModeItemAdd(stack, slot)`; without that the stack is a ghost and
   reverts. This alone would explain the symptom even if the item were chosen correctly.

### More rail materials -- concrete and wool
Requested: the 16 concrete colours and the 16 wool colours, on top of the current 17 variants.

The enum drives everything -- material, block, item, beam model, creative tab -- so the Java
side is one line per entry (see how `RUST` was added). The work is assets: each variant needs
a `models/block/track/<name>_track/` folder (22 files, copied and repathed), an item model, a
blockstate, a recipe, lang, and a `standard_track.png` tinted to the colour.

All of that is mechanical and should be **generated by a script**, not copied by hand -- 32
variants is 700+ files. Tint the existing greyscale track texture per colour rather than
drawing 32 textures. Check `TabSections` still reads well at that length; 49 variants is five
and a half rows, and the section banner maths pads to whole rows already.

### Smaller items
- **Config file** -- boost target, brake stopping distance, particle counts, chime pitch,
  slippery recovery. Every one of those has been changed by rebuilding the mod, repeatedly.
- ~~**Red wheels on the brake**~~ -- built; see the section above. Only the tint values and
  match radius still want an in-game look.
- **Sensor redstone** -- currently particles only. A curve is not a block so it cannot emit;
  powering its anchorpoints is the available route.

### Ruled out
- **Switch Track.** An anchorpoint is capped at two curves
  (`legCount() < 2`), so a three-way junction cannot be represented at all. The steering
  approach worked; the shape it needed does not exist in the base mod's data model.
