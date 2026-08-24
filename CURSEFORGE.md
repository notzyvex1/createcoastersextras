![Create: Coasters Extras](https://cdn.modrinth.com/data/cached_images/1ac382fdfbd341d106604c4c5ea284ea4aa471d0.png)

**Build the whole ride, not just the track.** A **station** that pulls the train in, holds it on the platform and dispatches it again. A **control stand** you sit down at and drive, with a live speedometer. **281 track materials**, **17 balloons**, and a Ponder scene for every block that teaches itself.

Then the tracks that do the work: **boost** to launch a cart, **brake** to trim it or stop it dead, a **sensor** that fires redstone as a coaster passes, and a **slippery** track for the long drops. Five functional tracks in all — but the station and the driver's seat are what turn a fast minecart into an actual coaster.

Built for **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)**, which gives you real coaster physics and one track to run them on. This is everything you build around that.

> **⚠️ This is an addon — it needs the base mod.**
> It does nothing on its own and will not load without **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** installed.

---

![Coaster track materials, top view](https://cdn.modrinth.com/data/cached_images/4e705d8c962d0d34430477d048a5f3dd4ae0fbd8.png)

---

## Never used the base mod?

You place **anchorpoints**, then string **track** between them — the game draws a curve, and that curve is your ride. The anchorpoints are the handles you grab to shape it.

Everything here is one of three things: a **new material** for that curve, a **new behaviour** for it, or a **balloon** to hang around it.

**Lost? Press W on any item in your inventory** for a Ponder scene that builds the whole thing in front of you, step by step.

---

## At a glance

- **Station Track** — brings the whole train in, holds it on the platform, dispatches it again
- **Coaster Controls** — a block you sit down at and drive, with a live speedometer
- **281 coaster tracks** — 21 hand-made, 32 wool & concrete, 223 built from vanilla blocks, 5 functional
- **17 balloons** — 15 dye colours, a Rainbow Balloon and a Copycat Balloon
- **Sensor Block** — turns a coaster passing overhead into a redstone signal
- **6 Ponder scenes** — press W and the block builds itself in front of you
- **Survival ready** — every block craftable, every block drops when broken

---

## The functional tracks

These are the reason the addon exists. Each one is a normal coaster track that changes what your ride does when it crosses.

### ⚡ Boost Track

**Accelerates a cart up to a target speed you set.** This is how a ride moves without a chain lift.

It throws sparks and flame backwards as exhaust — strongest launching from a standstill, fading as the cart comes up to speed. It only ever pushes a cart the way it is already going; it will never reverse one.

### 🛑 Brake Track

**Slows a cart to a target speed, or stops it dead.**

Braking is calculated from the *track remaining*, not applied at a fixed rate — so a cart arriving at full pelt is still down to target by the end of the run, instead of sailing off the end. Set the dial to zero and it comes to a complete stop.

The faster it hits, the bigger the show: past roughly **14 b/s** it throws lava, past **30** it adds sparks, past **43** it starts smoking, and the hiss climbs the whole way up.

### 🚉 Station Track

**Brings a ride in, holds it, and dispatches it again.** The one that makes a coaster feel like a coaster.

It eases the cart to a halt at the **last anchorpoint** — however fast it came in — and **freezes the entire train** together, not just the lead cart. Then it waits, and launches gently back out.

- **Right-click and hold the anchorpoint** to set the dwell time in seconds
- **Power it with redstone** to hold the ride in the station indefinitely
- **Wear Engineer's Goggles** for a live countdown, and watch the platform light march across as it waits

### 📡 Sensor Track

**Notices every coaster that crosses it,** and sparks to mark the moment.

Pair it with the **Sensor Block** and you get redstone out of it — see below.

### 🧊 Slippery Track

**Cancels the drag a coaster normally bleeds speed to.** Comes out as fast as it went in.

Made for long flat runs between hills, where a ride would otherwise stall out short of the station.

---

## The Sensor Block

A curve is a *connection between two anchorpoints*, not a block sitting in the world — so there is physically nothing there for a redstone signal to come out of. The Sensor Block is that missing block.

**Right-click a Sensor Track** (the track itself or either anchorpoint) to link it. The block tells you it's linked, and only then can it be placed — so you can't end up with a dead sensor wondering why nothing fires.

Then **put it anywhere you like.** It does not need to be near the track. When a coaster crosses, it powers, sparks, and gives an electrical *snap* — doors, dispensers, Redstone Links, note blocks, anything.

---

## Coaster Controls

A block you sit at, like Create's Train Controls.

**W and S move the lever** — and the lever visibly moves with them. Your speed reads out on a custom bar in the XP slot, styled after Create's own. Stand up and the ride is yours to walk away from.

---

## Balloons

![The full balloon set](https://cdn.modrinth.com/data/cached_images/d5e4cba15c36d7e8e8e3a448dabfb461980ef276.png)

The base mod ships one balloon, and it's red. Here are the other fifteen — **White, Light Gray, Gray, Black, Brown, Orange, Yellow, Lime, Green, Cyan, Light Blue, Blue, Purple, Magenta, Pink** — plus a **Rainbow Balloon**.

🎭 **The Copycat Balloon** is one balloon that wears any block in the game. Sneak and right-click a block to copy it, then place it — the balloon takes that block's texture, re-drawn as it renders rather than picked from a list of prepared ones. It floats and pops like every other balloon.

They all behave exactly like the red one: they tether, they float, they pop, and **sneak + wrench** gets them back.

---

## Track materials

The same track you already build with, in every finish. The material runs the **full length of the curve** — sleepers and slide bar both.

- **Wood (11)** — Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson, Warped
- **Stone (6)** — Stone, Deepslate, Andesite, Granite, Diorite, Rose Quartz
- **Wool (16)** — every dye colour
- **Concrete (16)** — every dye colour
- **Metal** — Rusted, Brass
- **Special** — Rainbow, Copycat
- **Vanilla blocks (223)** — every glazed terracotta, the whole ore set, planks, logs, deepslate, blackstone, prismarine, copper, terracotta, mushroom blocks, froglights and more

The wool and concrete tracks are built from the **actual pixels of the vanilla block**, with the track's own shading laid over the top — so a wool track genuinely reads as woven and a concrete track as flat concrete, rather than as one recoloured metal rail pretending to be both.

**The Rainbow Track** is the showpiece. It rides like any other track, but leaves a trail of coloured sparkles and plays a chime that climbs with them — colour and pitch driven by the same value so they stay in step. The notes are quantised to a major pentatonic, so a ride comes out sounding like a melody instead of a siren.

---

## 🌈 Rainbow Track

**The showpiece.** It rides like any other track, but leaves a trail of coloured sparkles and plays a chime that climbs with them — colour and pitch driven by the same value, so they stay in step for the whole run.

The notes are quantised to a major pentatonic. That one detail is the difference between a ride that sounds like a melody and one that sounds like a siren: on a free scale, a fast curve picks semitones at random and the result is noise.

Pairs with the **Rainbow Balloon**, which is the same sweep drawn on a balloon.

---

## 🎭 Copycat Track

**Coaster track that takes the look of any block you show it.** Point it at oak planks and you get oak track. Point it at deepslate and you get deepslate track. It rides exactly like every other track — the material is purely cosmetic.

| Gesture | What happens |
|---|---|
| **Right-click a block** | The whole stack becomes track built from that block |
| **Sneak + right-click a placed curve** | That section changes to the track you are holding |

That second one works with **every** track, not just Copycat — a brake, a station, rainbow, plain coaster track, anything. Change one section of a finished ride into a Brake Track without destroying and relaying it, and every micro-adjustment you made to the curve survives, because only the material changes and the geometry is never touched.

---

## Setting speeds

Boost, brake and station timings live on the **anchorpoint**, as a Create-style value dial: **right-click and hold, then drag**, exactly like a Creative Motor. Range is **0–60 blocks/second**.

Set it on one end and **the other end follows automatically** — the two anchorpoints of a curve always agree.

The dial only appears on anchorpoints that actually carry a functional track.

---

## Building your first ride

Five minutes, start to finish.

1. **Lay a loop.** Anchorpoints and plain Coaster Track from the base mod. Bring the end back round to the start.
2. **Put a Station Track on the last curve** before the start. Right-click and hold its anchorpoint, then drag to 5 seconds.
3. **Boost Track on the next curve out.** Right-click and hold it, then drag to about 20 b/s — enough to carry the first hill.
4. **Brake Track on the approach back in,** set to 8 b/s, so the ride arrives at a sane speed rather than shooting through.
5. **Place a cart, sit in it.** The station dispatches, the boost launches you, the brake catches you, the station takes you back.

Then add a **Slippery Track** on any long flat stretch where the ride is dying, and a **Sensor Track** wherever you want a door to open or a note block to fire.

---

## Crafting

Everything is craftable, and everything drops when you break it. Recipes are deliberately cheap — the fun is in building the ride, not in farming for it.

- **Boost Track** — Coaster Track + Andesite Alloy + Redstone
- **Brake Track** — Coaster Track + Iron Ingot + Redstone
- **Station Track** — Coaster Track + Redstone Torch
- **Sensor Track** — Coaster Track + Observer
- **Slippery Track** — Coaster Track + Ice
- **Rainbow Track** — Coaster Track + 6 Dyes
- **Any material track** — Coaster Track + that block (planks, wool, concrete, stone…)
- **Any balloon** — Wool + String + Iron Nugget

**Material tracks convert freely.** The recipe takes *any* track from this addon, so an Oak Track plus Blue Wool gives you a Blue Wool Track — no need to start over from a plain one every time.

---

## Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.248
- **[Create](https://www.curseforge.com/minecraft/mc-mods/create)** 6.0.10
- **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** 0.1.2 – 0.1.4
- **Sable** 2.0.3

Coasters Simulated already pulls in Sable, so in practice: install **Create**, **Coasters Simulated**, and **this**.

The base mod is pre-1.0 and its author intends breaking changes before then, so the dependency is pinned. On a newer base mod you get a dependency screen naming the version you need, rather than a crash.

---

## Good to know

**Material variants are cosmetic.** A cherry track and a deepslate track ride identically.

**Speeds are set per curve.** A long run built from several curves is configured curve by curve.

**Pick-block on a placed curve** currently returns the base mod's plain track rather than the variant. Known, and on the list.

**Multiplayer works**, and so do schematics — a pasted coaster relinks its anchorpoints on load rather than arriving with the right shape and broken connections.

---

# MANY MORE FEATURES, BLOCKS AND ITEMS ARE PLANNED

---

## Credits

Built on **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by **SilverGold** — the coaster physics, the track system, the cart and the original artwork are all theirs. This addon exists because that mod is good and I wanted more of it. The textures here follow the base mod's own so everything matches in-world.

Also built on **[Create](https://www.curseforge.com/minecraft/mc-mods/create)**.

Made by **NotZyvex**. Bug reports and suggestions welcome — come say so on Discord.
