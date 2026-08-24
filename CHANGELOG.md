# Changelog

All notable changes to Create: Coasters Extras are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2026-08-22

### Added
- **Splash Track** — a new water-themed functional track. As a cart crosses it, a big splash
  bursts off **both sides** of the rail with its own custom splash particle and a splash sound.
  Wet teal rails, so it stands apart from every other track.
  - **Water Boost dial.** Water drags a ride down, and a splash section long enough to look good
    is long enough to strand a coaster in it. Dial a speed and the section *drives* the ride at
    it instead of only taking from it, so a flume runs on its own. Left at zero it stays pure
    drag, exactly as before.
- **Launch Track** — a hydraulic-style launch. From a standstill it kicks a cart hard up to a
  high speed with fire, smoke and a launch sound — far stronger than a boost. It *will* start a
  coaster that has stopped dead, which a boost deliberately will not.
- **Reverse Track** — flips a cart's travel direction as it crosses (once per pass), turning
  any dead-end into a shuttle and enabling boomerang layouts. Its **Reverse Boost** dial sets how
  fast it sends the ride back out, so a shuttle can make it home.
- **Ponder scenes** for the Launch and Reverse tracks.
- The **Send dial is now its own control** — a small three-icon picker (Auto / Forward /
  Reverse), the same kind Create puts on a Mechanical Bearing, instead of a row on the speed
  board. It had to move to be small at all: a value board has one width shared by every row, so
  sitting next to a 0–200 speed forced a three-way choice onto a two-hundred-notch slider.
  Existing settings are migrated, so nothing already built changes direction.

### Fixed
- **Boost and brake track speed can now be set above 60.** The speed dial is drawn 0–200 but
  was silently clamping to 60 — that cap was only ever meant for a station's dwell time in
  seconds, and now applies only there. Reported on CurseForge.
- **Coasters no longer fall through plain track.** Our alias makes every one of our track ids
  compare equal to the base mod's, and the bobsled check used that comparison — so ordinary
  track was being handed the bobsled's relaxed rail guide and carts slid off it. Also reported.
- **Painting and converting track works again.** The same alias made the "is it already this
  material?" guard match any base-mod curve, so painting a plain coaster track, converting a
  section to a Brake, and converting one back to plain track were all refused with a misleading
  "Already …" message — which is most of what that feature is for.
- Track item names showed their raw translation key, and the Splash, Launch and Reverse tracks
  described themselves as "Cosmetic" in the tooltip.
- The splash sound played continuously while a cart was on the track; it now fires once per pass.
- Dial labels rendered with a blank where an icon should be — the glyph used is not in
  Minecraft's font. Each track now names its own setting: **Boost Speed**, **Launch Speed**,
  **Water Boost**, **Reverse Boost**, **Braking Sensitivity**.

### Changed
- Creative tab section order is now **Balloons → Controls → Functional Tracks → Tracks**.
- New **Balloons** banner: the real balloon items drifting over a sky that runs morning to night.
  It is a vanilla-animated GUI sprite, so its frame count comes from the image rather than from a
  number in code that could disagree with it.
- New **Tracks** banner: the track as you actually see it from above — wooden ties, side rails
  and a steel centre beam — stepping through every wood type.
- Banners that animate on hover now read their frame count from the image too. A wrong frametime
  only changes speed; a wrong frame count corrupts the frame window, and that number no longer
  has to be kept in step by hand.

## [2.1.1] - 2026-08-22

### Fixed
- The wrong track type was placed after switching hotbar slots, and on servers every player
  placed the host's track type instead of their own. Both came from the same cause — the base
  mod tracks the placing item in a single shared field — and are now fixed: you place the track
  you are holding, every time.
- Station hold time no longer resets after leaving and rejoining a world.
- The direction and speed dials now save the moment you release them, including small
  adjustments that stay within one setting.

Thanks to TeakIvy for diagnosing the track-placement fix, and to everyone who reported these.

## [2.1.0] - 2026-08-15

### Added
- **Stations can be told which way to send a ride.** The anchorpoint dial has a third bar,
  **Send (direction)**, with three settings: *auto*, *forward* and *reverse*. Until now a
  station always launched a coaster back out the way its momentum was already pointing, which
  is fine for a shuttle and useless for a circuit — on a loop you want every dispatch to go
  the same way round regardless of which end the cart happened to roll in from, and there was
  no way to say so.

  *Auto* is the untouched setting and behaves exactly as before, so nothing you have already
  built changes. It is also deliberately the **first** zone on the bar rather than the middle
  one: a station saved before this bar existed has no value stored for it, a missing value
  reads back as zero, and zero has to mean "carry on as you were". Had auto sat in the middle,
  every station in every existing world would have quietly started launching backwards on
  load.

  Two details worth knowing if you build with it:

  - **The bar is cut into three equal thirds, and anywhere in a third selects it.** Create's
    value board only offers one kind of row — an integer drag bar — and a single maximum shared
    by every row on the board, which here is 200 because of the Launch bar next to it. A
    three-way choice on a 0–200 bar would normally mean asking you to land the handle on 0, 1
    or 2 at the extreme left. Splitting the range instead makes the whole bar live, and the
    readout shows the word rather than the number, so what you are actually doing is dragging
    between three labelled thirds.
  - **Both ends of a curve hold the same setting, not opposite ones.** Forward means *along
    the curve*, and a curve has one direction no matter which of its two anchorpoints you are
    standing at. Setting it at either end writes both, the same way the Hold and Launch bars
    already do, so it does not matter which end of your own station you walk up to.

  Goggles gained a matching **Sends** line beside *Holds for* and *Launches at*, because a
  station that always sends rides one way and a station that merely passes them through look
  identical from the outside until one of them surprises you.

- **The Coaster Controls handle moves when you drive.** Push forward and the lever swings
  forward with you, ease off and it glides back — the way Create's own Train Controls behave.
  It could not do this before because the handle was part of the block model, and a block
  model can only be swapped between fixed states: three throttle positions meant three
  models, and the lever jumped between them in 22.5-degree steps because that is the finest
  rotation Minecraft's model format allows. The handle is now drawn separately from the
  console and turned by any angle, so the motion is continuous rather than stepped.

- **Station Boost.** The anchorpoint dial is now two bars in one box rather than a single
  value: **Hold (seconds)** and **Launch (blocks/s)**. Launch speed used to come from one
  global config value shared by every station on the server, so a gentle station and a
  cannon could not coexist. Zero still means "use the config", so every station you have
  already built keeps working untouched.

### Removed
- **The chase of lights along a held station.** A row of nine dashes used to run down the track
  toward the exit while a coaster waited, speeding up as departure approached. It was meant to
  show the countdown without making you read anything; in practice it read as a dashed line
  drawn over the rails and was the first thing people asked how to turn off.

  It is gone entirely rather than made subtler, because the information it carried was never
  only there: goggles already print the exact countdown, which is precise where the sweep was
  only suggestive. The braking puff as a coaster arrives and the bell and burst on dispatch are
  untouched — those mark moments rather than drawing a permanent overlay on the track.

  Worth recording for anyone who goes looking: these were never particles. They were outline
  segments submitted through Create's outliner every client tick, which is why searching the
  code for particle spawns never found them.

### Fixed
- **The animated handle pivoted one pixel too far forward.** The hinge sat at z=8 while the
  handle's arm actually ends at z=9, which put a single pixel of arm *behind* the pivot.
  Rotation sends anything behind a pivot the opposite way to everything in front of it, so at
  full throw that pixel pushed out through the console face and swung against the lever. It is
  a small artefact but a conspicuous one — it reads as the model coming apart at the extremes
  of the throw, precisely when you are looking at the lever.

  The hinge is now taken from the model's own geometry (the arm occupies x 7–9, y 11–13,
  z 1–9, so the hinge is its centre in x and y and its rear face in z) rather than judged by
  eye, and the derivation is recorded in the code so it does not get "re-tuned" back.

- **A stray panel floated behind the Coaster Controls.** One piece of the console sat a full
  block and a half behind where it belonged, so the block appeared to have a large slab
  attached to nothing. It had been translated out of position in the model file and left
  there by a rotation of zero degrees, which does nothing but made the offset look
  deliberate. The panel is back on the console where it started.
- **The Coaster Controls handle sat too high.** The grip and its uprights stuck up above the
  console rather than nesting into it. Lowered so the assembly reads as part of the block.
- **The station said it held for 0 seconds while holding for 3.** An untouched dial reads as
  zero, and the drive code treats zero as "use the default" -- but the goggle tooltip printed
  the raw number. So a station that worked perfectly reported that it did nothing, which is
  why it kept being reported as broken.
- **Every instruction to "scroll" an anchorpoint was wrong.** Create 6 does not set these
  values by scrolling; the dial opens a value board on **right-click and hold**. The docs, the
  Ponder scenes and the tooltips all said scroll, so everyone followed the instructions
  exactly and nothing happened. Corrected in fourteen places.
- **The Rainbow Balloon was lit as flat bands instead of a sphere.** Its vertical axis carried
  hue rather than light, so it had no highlight at all and read as a sticker beside the other
  sixteen. Relit from the same lamp as the rest of the set.

### Development
Not player-facing, recorded because both were written to stop a class of mistake recurring.

- **`tools/preview_controls_sweep.py`** draws the handle's throw in side view, straight from
  the model files, with the console behind it and the hinge marked. Checking the animation in
  game costs a build, a launch, a world and a block, and the thing being judged lasts about a
  third of a second — so the pivot had been "tuned by eye" more than once, which is how it came
  to be a pixel out. A wrong hinge now shows up as the arm cutting through the console.

  The first version of this script plotted the *inverse* rotation and confidently drew the
  throttle moving the wrong way. Mojang's `Axis.XP` puts its minus sign on the z term of y′,
  not the y term of z′, and swapping the two mirrors every angle. The correct matrix is now
  written into the file, along with the consequence that makes it counterintuitive: geometry on
  the minus-z side of a pivot **rises** for a positive angle, and this handle is entirely on the
  minus-z side.

- **`tools/preview_controls_views.py`** draws orthographic side, front and top views of the
  console and handle together, with the block's own bounds drawn in. One camera angle is not
  enough to catch a part floating clear of the body — it reads as a normal lever from the front
  and only shows up from the side.

## [1.2.0] - 2026-08-10

### Added
- **32 wool and concrete tracks** -- every dye colour, in both materials.
- **Any track crafts into any other**, so a wrong choice costs one crafting step rather than
  a fresh set of materials.
- **Create-style Shift summaries** on every block that does something, matching how Create's
  own tooltips expand.

### Fixed
- **Wool and concrete tracks rendered identically.** They were tinted metal wearing a colour
  rather than track built out of the material, and the difference only showed on the pair I
  had not tested.

### Changed
- Ponder scenes put a seat on the cart and show the Sensor Block, and say less while doing it.

## [1.1.0] - 2026-08-06

### Added
- **Sensor Block.** The Sensor Track always detected coasters; what it could not do is emit a
  signal, because a track curve is a connection between two anchorpoints rather than a block,
  and there is nothing in the world for redstone to come out of. This is that missing block.
  Right-click a Sensor Track to link the two, then place it — and only then; an unlinked one
  refuses to be placed, because a Sensor Block watching nothing is indistinguishable from a
  broken one until you have wired it up and started wondering why the door never opens.
  It does not have to be anywhere near the track it watches. Put it where the wiring is.
  Tripping throws electric sparks and closes with an audible snap.
- **Pasted coaster track now works.** Anchorpoints key their curves by the absolute position
  of the far end, so a structure, schematic or WorldEdit copy of a coaster arrived with the
  right shape and broken links. The base mod's own repair now runs on load.

### Changed
- Verified against **Create: Coasters Simulated 0.1.4**, and the dependency range is pinned
  to `[0.1.2,0.2.0)`. The base mod is pre-1.0 and its author has said he intends dramatic
  changes; pinned, a newer base mod gives you a dependency screen rather than a crash.

## [1.0.0] - 2026-08-05

First public release.

Create: Coasters Extras is an addon for **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by **SilverGold** — the coaster physics, the track system and the original artwork are that mod's, and this one does not run without it.

| Requires | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Create | 6.0.10 |
| Create: Coasters Simulated | 0.1.2 – 0.1.4 |

### Tracks that do something
- **Boost Track** — drives a coaster up to a speed you set. This is how you make a ride move without a chain lift.
- **Brake Track** — slows a coaster to the speed you set, braking by how much track is left rather than at a fixed rate, so even a coaster at full speed is down to target before the run ends. Set it to zero for a dead stop.
- **Station Track** — eases an arriving coaster to a halt at the *last* anchorpoint, whatever speed it came in at, freezes the whole train together, waits, then eases it away under its own power. A redstone signal holds it. Engineer's Goggles show a live countdown, and lights run along the platform while it waits.
- **Sensor Track** — sparks as a coaster crosses, marking the moment on the ride.
- **Slippery Track** — cancels drag, so a coaster crosses at the speed it arrived with.

Boost, Brake and Station take their setting from a Create-style dial on either anchorpoint; both ends stay in sync.

### Driving
- **Coaster Controls** — sit on a coaster holding them and W and S drive it, with a speedometer while you do.

### Decoration
- **18 cosmetic track variants** — 11 woods, 5 stones, rusted steel, and a Rainbow Track that sweeps a full spectrum along its length.
- **16 balloons**, to go with the base mod's red one.

### Learning it
- **Ponder scenes for six tracks** — the five functional ones and Rainbow. Press **W** over any of them.

## [0.9.3] - 2026-08-05 (development build)

### Added
- **Ponder scenes for all six functional tracks** — Boost, Brake, Station, Sensor, Slippery
  and Rainbow. Press **W** over any of them. The Boost scene opens on the question people
  actually arrive with: plain track carries a cart but never pushes one, and this is what
  makes a coaster move.
- **Rusted Coaster Track.** The textures and models had been finished for a while; the
  material was simply never registered. It is now the 17th variant.
- **Goggles readout on station anchorpoints.** Shows a live countdown and a progress bar
  while a coaster is held, and rewrites itself as the station moves through arriving,
  waiting, held by redstone and departing. With no coaster present it shows the dwell the
  anchorpoint is set to.
- **A light that runs along a held station platform**, chasing toward the exit — a bright
  head with a trail filling in behind it.

### Changed
- **Station Track now stops a coaster at the far anchorpoint**, not wherever it happened to
  slow down. Deceleration is recomputed every tick from the distance remaining, so a coaster
  arriving at any speed comes to rest at the same place: gently if it can, harder if it must.
  A coaster that trickles in too slowly is nudged along rather than parking mid-platform.
- **Dispatch is a gentle ramp** rather than an impulse — it leans on the coaster until it is
  clear of the platform instead of firing it out of a cannon.
- **The speed dial now writes both anchorpoints.** A curve has one at each end and each
  carried its own value, while the drive hook read whichever answered first — so editing the
  "wrong" end silently did nothing. Setting either now sets both.

### Notes
- Sensor Track detects a coaster and sparks, but does not emit a redstone signal. A curve is
  a bezier connection rather than a block, so it has nothing to emit from. Its Ponder scene
  says what it does rather than promising an output that does not exist.

## [0.7.3] - 2026-08-05

First public release: 16 balloons and 20 coaster tracks.

Create: Coasters Extras is an addon for **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by **SilverGold** — the coaster physics, the track system and the original artwork are that mod's, and this one does not run without it.

The base mod ships one balloon and one track. Counting its red balloon alongside these, that makes 17 balloons in the tab; all 20 tracks are new.

| Requires | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Create | 6.0.10 |
| Create: Coasters Simulated | 0.1.2 – 0.1.4 |
| Sable | 2.0.3 (pulled in by Coasters Simulated) |

Speeds below are in blocks per second, written **b/s**.

### Added



- Centre beam now draws from the curve's own material. The base mod takes it from a single
  hardcoded model, which left a grey stripe down the middle of every coloured track.
- Brake force scales to a fixed stopping distance rather than a flat rate, so a cart arriving
  at 100 blocks/second stops in about 12 blocks instead of 74.
- Animated Balloons banner in the creative tab.
#### Balloons

- **15 dye colours** the base mod was missing: white, light gray, gray, black, brown, orange, yellow, lime, green, cyan, light blue, blue, purple, magenta, pink. With the base mod's red, that covers all 16 vanilla dyes.
- **Rainbow Balloon** — a hue sweep across the balloon body.
- All of them behave like the red balloon: they attach, they pop, and sneaking with a **Create wrench** recovers one as an item.
- Textures are recolours of the base mod's own red balloon rather than redraws, so the shading, highlight and knot detail match it.
- **Create-style tooltips** — hold **Shift** for a summary.
- Recipes match the base mod's: wool, string and an iron nugget stacked vertically. The Rainbow Balloon is any balloon surrounded by red, yellow, green and blue dye.

#### Tracks — material variants

Cosmetic. Each carries its material across the sleepers and the full slide bar, and none of them changes how a cart moves.

- **11 woods:** oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, warped.
- **5 stones:** stone, deepslate, andesite, granite, diorite.
- **Rainbow Track** — six dyes and a coaster track. It has no effect on a cart. It is there to look good.
- **Rainbow Track** — cosmetic. A cart crossing it emits coloured dust and a chime, both
  derived from one hue so they stay in step. Notes are quantised to a major pentatonic
  (C D E G A C). Fires every third tick so it does not bury the brake's hiss.
- Each is crafted from a coaster track plus the matching block: oak planks for oak, deepslate for deepslate, and so on.

#### Tracks — functional

Boost and brake take their target speed from the dial on the anchorpoints at either end of the curve — see Interface, below.

- **Boost Track** (amber) — accelerates a cart up to the target speed along its current direction of travel. It never reverses a cart. Throws electric sparks and flame backwards as exhaust, strongest launching from a standstill and fading as the cart reaches speed. Crafted from a coaster track, andesite alloy and redstone.
- **Brake Track** (red) — slows a cart down to the target speed. Braking force scales with how fast the cart arrives, so one at full pelt stops hard instead of coasting through. Above roughly **14 b/s** it throws lava, past **30 b/s** it adds sparks, past **43 b/s** smoke, and the hiss gets louder and faster with each. Crafted from a coaster track, an iron ingot and redstone.
- **Sensor Track** (yellow hazard stripes) — sparks where a passing cart touches it. Crafted from a coaster track and an observer.

  **It does not emit a redstone signal.** A track curve is the bezier connection drawn between two anchorpoints, not a block, so there is nothing there to power. The detection and the spark are real; there is no output.

#### Interface

- **Speed dial on anchorpoints.** A boost or brake curve needs somewhere to keep its target speed and is not itself a block; the anchorpoints at either end are. A Create-style scroll dial sits on them — click and hold, then scroll, the same interaction as the Creative Motor. Range **0–60 b/s**, default 22.
- The dial only appears on anchorpoints that actually carry boost or brake track. Anchors holding plain or cosmetic track do not show it.
- **Creative tab** — "Create: Coasters Extras", Rainbow Balloon icon, split into two banner-headed sections: **Balloons** and **Tracks**.
- The base mod's red balloon is shown in the Balloons section so the colour set is complete in one place. It is *not* re-registered — there is still exactly one red balloon in the game, and no duplicate recipe.
- Nothing the base mod registers is replaced or altered, and the boost and brake behaviour applies only to this mod's own track materials.

[0.7.3]: https://github.com/notzyvex1/create-coasters-extras/releases/tag/v0.7.3
