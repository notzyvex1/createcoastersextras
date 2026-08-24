# Create: Coasters Extras

An addon for **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by SilverGold. It adds 16 balloons and 23 tracks: every dye colour the base mod was missing, 18 cosmetic material variants, and five functional tracks with a Create-style speed dial.

![Create: Coasters Extras](https://cdn.modrinth.com/data/cached_images/1ac382fdfbd341d106604c4c5ea284ea4aa471d0.png)

In Coasters Simulated you place **anchorpoints** and the track is a bezier curve strung between them. That shapes most of what follows: materials apply to a whole curve, and the speed dial lives on the anchorpoint at either end.

## Install

Drop the jar into `mods/` alongside Create, Create: Coasters Simulated and Sable. See [Requirements](#requirements).

---

## Features

### Balloons

The base mod ships a red balloon. This adds the other 16.

- **15 dye colours** — white, light gray, gray, black, brown, orange, yellow, lime, green, cyan, light blue, blue, purple, magenta, pink
- **Rainbow Balloon**
- All attach, pop, and are recoverable with sneak + wrench, exactly like the base mod's red
- Create-style tooltips — hold <kbd>Shift</kbd> for a summary

The base mod's red is not re-registered here; that would duplicate the item and clash with its recipe. It is pulled into this mod's creative tab instead, so the full set of 17 sits in one place.

### Track materials

Cosmetic only. The material carries across the sleepers and the full length of the slide bar, and changes nothing about how a cart behaves.

- **11 woods** — oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, warped
- **5 stones** — stone, deepslate, andesite, granite, diorite
- **Rainbow Track** — the showpiece. It changes nothing about how a cart moves, but a cart crossing it leaves coloured sparkles and plays a chime, both driven by the same hue so they shift together. Pitches are quantised to a major pentatonic, so a ride reads as a melody rather than a sweep.

### Functional tracks

| Track | Colour | What it does |
|---|---|---|
| **Boost** | amber | Accelerates a cart to a target speed. Throws electric sparks and flame backwards as exhaust, strongest launching from a standstill and fading as the cart reaches speed. Never reverses a cart's direction. |
| **Brake** | red | Slows a cart to a target speed. Braking force scales with how fast the cart arrives, so a cart at full pelt stops hard instead of coasting. Above ~14 b/s it throws lava, past ~30 it adds sparks, past ~43 smoke, and the hiss gets louder and faster. |
| **Sensor** | yellow hazard stripes | Detects a passing cart and sparks. **No redstone output.** |

> The Sensor Track cannot emit redstone itself — a track curve is a bezier connection, not a block, so there is nothing to power. Link a **Sensor Block** to it instead: that block carries the signal, and it can sit anywhere, not just beside the track.

### Speed control

A Create-style value dial appears on the anchorpoint at either end of a boost or brake curve. Right-click and hold, then drag — the same interaction as the Creative Motor. Range **0–60 blocks per second** (b/s). The dial only appears on anchors that actually carry boost or brake track.

### Creative tab

Its own tab, split into two banner-headed sections: **Balloons** and **Tracks**.

---

## Recipes

| Item | Recipe |
|---|---|
| Balloon (15 dyes) | Wool + string + iron nugget, stacked vertically — the base mod's red balloon recipe with a different wool |
| Rainbow Balloon | Any balloon with red, green, blue and yellow dye around it |
| Wood / stone track | Coaster track + the matching planks or block (shapeless) |
| Rainbow Track | Coaster track + 6 dyes |
| Boost Track | Coaster track + andesite alloy + redstone |
| Brake Track | Coaster track + iron ingot + redstone |
| Sensor Track | Coaster track + observer |

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| [Create](https://modrinth.com/mod/create) | 6.0.10 |
| [Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) | 0.1.2 |
| [Sable](https://modrinth.com/mod/sable) | 2.0.3 |

Sable is a dependency of Coasters Simulated. Launchers that resolve dependencies will fetch it; installing by hand, you need the file yourself.

---

## Building from source

Requires **JDK 21**. Use the bundled Gradle wrapper — do not install Gradle separately.

```bash
git clone https://github.com/notzyvex1/createcoastersextras
cd createcoastersextras
# populate libs/ first — see below
./gradlew build          # Windows: gradlew.bat build
```

The jar lands in `build/libs/` as `coasters_extras-neoforge-1.21.1-<version>.jar`.

**Populate `libs/` before the first build.** Create: Coasters Simulated is not published to any Maven repository, and the build resolves the whole Create stack from a local `flatDir` rather than Maven:

```groovy
compileOnly fileTree(dir: 'libs', include: ['*.jar'])
```

So `libs/` needs the Coasters Simulated jar plus Create, Registrate, Flywheel, Ponder, Sable (Rapier and Companion included) and Veil. The sources import both `com.simibubi.create` and `dev.silvergold.simulatedcoasters`, so compilation fails without them. Everything there is `compileOnly` — none of it is bundled into the output jar; all of it is declared as a runtime dependency in `neoforge.mods.toml`.

Bugs and requests: [GitHub issues](https://github.com/notzyvex1/createcoastersextras/issues).

---

## Credits

**[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) by SilverGold.** This is an addon and exists only because that mod does. All the coaster physics, the track and cart systems, and the original artwork are its work.

Track models and textures here are derived from Create: Coasters Simulated under its MIT licence. The balloon textures are recolours of its original red balloon, so the shading and detail match upstream.

Built on [Create](https://github.com/Creators-of-Create/Create) and its public `TrackMaterialFactory` addon API. Where the addon API is not enough — cart driving, track placement, the anchorpoint speed dial, balloon attachment — it hooks the base mod through mixins. That ties it to Coasters Simulated's internals, so expect it to need an update whenever the base mod does.

Addon by **NotZyvex**.

---

## Licence

**All Rights Reserved** — see [LICENSE.md](LICENSE.md). You may play it, stream it, and put it in a free modpack that links back here. You may not reupload it or reuse its code or assets in another project. Versions published under MIT stay MIT for those releases.
