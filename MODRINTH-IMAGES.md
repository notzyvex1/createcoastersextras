# Create: Coasters Extras

![bannerthxdolphin](https://cdn.modrinth.com/data/cached_images/1ac382fdfbd341d106604c4c5ea284ea4aa471d0.png)

[![CurseForge](https://cdn.modrinth.com/data/cached_images/cd1376d27fd5d3ca02cf6822aa859d8b5650a492.png)](https://www.curseforge.com/minecraft/mc-mods/create-coasters-extras) [![GitHub](https://cdn.modrinth.com/data/cached_images/0d1ef4ebf1904cccb1815ac7e72b9ce06c139185.png)](https://github.com/notzyvex1/createcoastersextras) [![Discord](https://cdn.modrinth.com/data/cached_images/501f3a19568fd6cb4afa22682316f804d8db84c3.png)](https://discord.gg/tCu7ccjYHv) ![TikTok](https://cdn.modrinth.com/data/cached_images/1e6d5b9d433f51c54d8be820ced0438ddeeb6ba7.png) [![Ko-fi](https://cdn.modrinth.com/data/cached_images/41bd578fa915f9305277f31161472e0e7050a23c.png)](https://ko-fi.com/notzyvex)

[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) provides the coaster physics and a single track to run them on. This addon provides everything you would build around that: a station that runs itself, a driver's seat with a working speedometer, ten tracks that change what a cart does when it crosses them, and 277 further materials that are purely cosmetic.

> ### This is an addon and requires the base mod
> It does nothing on its own and will not load without **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** installed.

---

![Coaster track materials, top view](https://cdn.modrinth.com/data/cached_images/4e705d8c962d0d34430477d048a5f3dd4ae0fbd8.png)

---

## If you have not used the base mod before

You place anchorpoints, then run track between them. The game draws a curve through those points, and that curve is your ride. The anchorpoints are the handles you use to reshape it afterwards.

Everything in this addon is one of three things: a material for that curve, a behaviour for it, or decoration to place around it.

Pressing `W` on any item opens its Ponder scene, which builds the thing in front of you step by step.

---

![At a glance](https://cdn.modrinth.com/data/cached_images/9a76045bf7f75d0944dfbdbab2d41fdfa5c0c532.png)

| | |
|---|---|
| **Coaster tracks** | **287** total: 10 functional, 277 cosmetic |
| **Station Track** | Brings the whole train in, holds it on the platform, dispatches it again |
| **Coaster Controls** | A block you sit at and drive, with a live speedometer |
| **Balloons** | **17**: 15 dye colours, a Rainbow Balloon and a Copycat Balloon |
| **Sensor Block** | Turns a coaster passing overhead into a redstone signal |
| **Ponder scenes** | **9** |
| **Config options** | **52**, covering every speed, rate and threshold in the mod |
| **Survival ready** | Every block craftable, every block drops when broken |

---

![The functional tracks](https://cdn.modrinth.com/data/cached_images/9940802ffa0d6589ba306ab9d7abaaff8b717d45.png)

Ten tracks that do something to a cart. Each is configured from the anchorpoints at either end: look at one, hold right-click, and scroll, which is the same interaction as Create's Creative Motor. Setting either end writes both, so it does not matter which one you are standing next to.

A dial you have never touched reads **Default** and uses the config value for that track.

![The functional tracks](https://cdn.modrinth.com/data/cached_images/86270c5bf03bba41081cc99561eb035cddcac86b.png)

![Boost Track](https://cdn.modrinth.com/data/cached_images/0239e2d5519fd55d90f021b289ade2decc44fad4.png)

Accelerates a cart up to the target speed you set. This is how a ride moves if you do not want a chain lift, and for most layouts it is the only powered track needed.

It throws sparks and flame backwards as exhaust, strongest from a standstill and fading as the cart reaches speed. It only ever pushes a cart in the direction it is already travelling and will never reverse one.

![Powered Boost Track](https://cdn.modrinth.com/data/cached_images/08233dc02f3d7ce7ac06f0b08ab059b586912ddc.png)

Identical to the Boost Track, except that it only pushes while its anchorpoint is receiving a redstone signal.

An ordinary boost runs constantly, which makes it something you design around rather than something you operate. Gating it on a signal lets a circuit drive the ride: launch from a button, dispatch on a timer, or hold a section closed until a door opens. Its speed and acceleration are separate config values from the plain boost, so a signal-driven launch can be made aggressive without affecting every boost on the map.

![Launch Track](https://cdn.modrinth.com/data/cached_images/7cbe2054d918c54f8a3a73415746e70c7a55f9b1.png)

A hydraulic-style launch. From a standstill it accelerates a cart considerably harder than a boost does, with fire and smoke behind it, and unlike a boost it will start a ride that has come to a complete stop.

The **Powered** dial controls when it fires. Left on *Always*, it triggers on contact. Set to *On Redstone*, it holds the train until your circuit releases it, which is what you want as soon as there is a station in front of it. Boost and Brake carry the same dial.

![Brake Track](https://cdn.modrinth.com/data/cached_images/f9bad503960626df6020932580c02c8d6db89fe8.png)

Slows a cart to a target speed, or stops it completely if the dial is set to zero.

Braking is calculated from the track remaining rather than applied at a fixed rate, so a cart arriving at full speed is still at the target by the end of the section instead of overshooting it. The wheels turn red while braking, and the faster it arrives the larger the effect: past roughly 14 b/s it throws lava, past 30 it adds sparks, and past 43 it begins smoking.

![Station Track](https://cdn.modrinth.com/data/cached_images/ba26144e1db21eeef912d13d3ce181f68972b1b3.png)

Brings a ride in, holds it, and dispatches it again.

It eases the cart to a halt at the last anchorpoint regardless of arrival speed, and arrests the entire train together rather than letting each cart stop independently. It then waits for the dwell time and launches gently back out.

- Right-click and hold the anchorpoint to set the dwell time in seconds.
- Power it with redstone to hold the ride in the station indefinitely.
- Wear Engineer's Goggles for a live countdown, and watch the platform light march `>----` → `->---` → `-->--` as it waits.

![Splash Track](https://cdn.modrinth.com/data/cached_images/4a92d0b9cc5bb757926d5691272874c80be8c610.png)

Water rails. A coaster crossing at speed throws spray from both side rails, drops a curtain of water beneath the track, and lands with a splash. The rails themselves are animated: light moves across the water and bubbles surface and pop.

Water slows a ride down, which meant a splash section long enough to look convincing was previously long enough to strand a coaster in the middle of it. The **Water Boost** dial addresses that. Set a speed and the section drives the ride at it rather than only taking from it, so a flume can run continuously. Left at zero it behaves as pure drag.

![Reverse Track](https://cdn.modrinth.com/data/cached_images/c328a03fa2e7ee55b26821aa75010a198ef2b28d.png)

Reverses a cart's direction of travel as it crosses, once per pass so that it does not oscillate in place.

This turns any dead end into a shuttle and makes boomerang layouts possible. The **Reverse Boost** dial sets how hard it sends the ride back out, which matters more than it may appear: a boomerang that cannot climb its own return hill leaves the cart sitting in the valley.

![Sensor Track](https://cdn.modrinth.com/data/cached_images/9a3ba9f88f5e80d053008947a64f6d77319b53ee.png)

Detects every coaster that crosses it, and sparks to mark the moment.

Pair it with the Sensor Block described below to get a redstone signal out of it.

![Slippery Track](https://cdn.modrinth.com/data/cached_images/3fc7003513810d351cf2ead5c62d3fe645bf383c.png)

Cancels the drag a coaster normally loses speed to, so it leaves at the speed it entered.

Intended for long flat runs between hills, where a ride would otherwise stall short of the station.

![Bobsled Track](https://cdn.modrinth.com/data/cached_images/cfe1c1c6c70c57e043b0040c2b7ad7d54d0ae65b.png)

Leans the cart into corners. The harder the corner and the faster you take it, the further it goes over, up to 35 degrees by default.

The lean is worked out from the corner itself rather than animated: the track measures how tight the curve is and how fast the cart is moving, converts that into the sideways force a rider would feel, and rolls the cart to the angle that would cancel it. Coming out of the corner it settles upright again on its own. The cart stays locked to the rail throughout and cannot come off it.

---

![The Sensor Block](https://cdn.modrinth.com/data/cached_images/be6fc58380ea05416923d08e9b4227e2a16f2aaa.png)

A curve is a connection between two anchorpoints rather than a block in the world, so there is physically nothing there for a redstone signal to come out of. The Sensor Block is that missing block.

Right-click a Sensor Track, either the track itself or one of its anchorpoints, to link it. The block confirms the link, and it cannot be placed until it has one, so you will not end up with a dead sensor and no indication why.

It can then be placed anywhere and does not need to be near the track. When a coaster crosses, it powers, sparks and gives an electrical snap, which is enough to drive doors, dispensers, Redstone Links or note blocks.

---

![Coaster Controls](https://cdn.modrinth.com/data/cached_images/165578a2fd9e8e16a37f1ea9bded890c21ce4928.png)

A block you sit at, in the manner of Create's Train Controls.

`W` and `S` move the throttle lever, and the lever animates with them. Your speed reads out on a custom bar in the experience slot, styled after Create's own. Standing up leaves the ride behind.

---

![Balloons](https://cdn.modrinth.com/data/cached_images/c801e624429f62360a74302fc864b69a3bfcc3da.png)

![The full balloon set](https://cdn.modrinth.com/data/cached_images/d5e4cba15c36d7e8e8e3a448dabfb461980ef276.png)

The base mod ships one balloon and it is red. This adds the other fifteen dye colours (White, Light Gray, Gray, Black, Brown, Orange, Yellow, Lime, Green, Cyan, Light Blue, Blue, Purple, Magenta and Pink) along with a Rainbow Balloon.

![All 17 balloon colours](https://cdn.modrinth.com/data/cached_images/01a8e11ac6272fc48f9970289ebbff2bb72b6530.png)

The **Copycat Balloon** is a single balloon that wears any block in the game. Sneak and right-click a block to copy it, then place the balloon and it takes that block's texture, redrawn as it renders rather than selected from a list of prepared variants.

All of them behave as the red one does: they tether, float and pop, and sneak plus wrench recovers them.

---

![Track materials](https://cdn.modrinth.com/data/cached_images/3999f30a1e168fd69f2774420e03e9d1338b360c.png)

The same track you already build with, in 277 finishes. The material runs the full length of the curve, sleepers and slide bar both.

![Every track material](https://cdn.modrinth.com/data/cached_images/90ed2536bac5a509b7e3cfbaaeddd8c3cf3ea4b7.png)

| Family | Variants |
|---|---|
| **Wood** | Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson and Warped, each with planks, logs and stripped logs |
| **Stone** | Stone, Deepslate, Andesite, Granite, Diorite, Blackstone, Tuff, Calcite and their cut, polished and chiselled forms |
| **Wool** | All 16 colours |
| **Concrete** | All 16 colours, plus all 16 concrete powders |
| **Copper** | Every oxidation stage, cut and chiselled, plus the grate |
| **Ores** | The full set in both overworld and deepslate form |
| **Other** | Terracotta and glazed terracotta, prismarine, quartz, froglights, sculk, netherite, Rose Quartz, Brass and more |
| **Special** | Rainbow and Copycat |

Tracks are built from the actual pixels of the vanilla block with the track's own shading laid over the top, so a wool track reads as woven and a concrete track as flat concrete, rather than one recoloured metal rail pretending to be both.

---

![Rainbow Track](https://cdn.modrinth.com/data/cached_images/e010cd11572b9fef5040dce3e45689fbce703bc3_0.webp)

Rides like any other track, but leaves a trail of coloured sparkles and plays a chime that climbs with them. Colour and pitch are driven by the same value, so they stay in step for the whole run.

The notes are quantised to a major pentatonic, which is the difference between a ride that sounds like a melody and one that sounds like a siren. On a free scale a fast curve picks semitones at random and the result is noise.

It pairs with the Rainbow Balloon, which is the same sweep drawn on a balloon.

---

![Copycat Track](None)

Coaster track that takes the appearance of any block you show it. Point it at oak planks and you get oak track; point it at deepslate and you get deepslate track. It rides exactly like every other track, as the material is purely cosmetic.

| Gesture | What happens |
|---|---|
| Right-click a block | The whole stack becomes track built from that block |
| Sneak + right-click a placed curve | That section changes to the track you are holding |

The second gesture works with every track in the addon, not only Copycat. You can convert one section of a finished ride into a Brake Track without breaking and relaying it, and every adjustment you made to the curve survives, because only the material changes and the geometry is never touched.

---

## Setting speeds

Speeds, dwell times and directions live on the anchorpoint as a Create-style value dial. Right-click and hold, then drag, exactly as with a Creative Motor. The range is **0–200 blocks per second**.

Setting one end updates the other automatically, so the two anchorpoints of a curve always agree.

Depending on what is attached, an anchorpoint can carry up to three dials, laid out side by side across its face: the speed, the **Send** direction, and on a Boost, Brake or Launch track, **Powered**. An anchorpoint that carries a dial is marked with three small brass pips, so you can see at a glance which ones have settings without walking up to every single one.

---

![Building your first ride](https://cdn.modrinth.com/data/cached_images/a72cb9068460ae176214410d4a981f5e856c8e6b.png)

1. Lay a loop from anchorpoints and plain Coaster Track, bringing the end back round to the start.
2. Put a Station Track on the last curve before the start, and set its dwell to 5 seconds.
3. Put a Boost Track on the next curve out, set to about 20 b/s, which is enough to carry the first hill.
4. Put a Brake Track on the approach back in, set to 8 b/s, so the ride arrives at a reasonable speed.
5. Place a cart and sit in it. The station dispatches, the boost launches you, the brake catches you, and the station takes you back.

From there, add a Slippery Track on any long flat stretch where the ride is losing too much speed, and a Sensor Track wherever you want a door to open or a note block to fire.

---

![Crafting](https://cdn.modrinth.com/data/cached_images/bb31f5f6713282cacb5bf147bb8cc9ce89afb393.png)

Everything is craftable and everything drops when broken. Recipes are deliberately cheap, as the intent is for the building to be the difficult part rather than the gathering.

| Item | Recipe |
|---|---|
| **Boost Track** | Coaster Track + Andesite Alloy + Redstone |
| **Powered Boost Track** | Coaster Track + Andesite Alloy + Redstone Torch |
| **Launch Track** | Coaster Track + Blaze Powder + Piston |
| **Brake Track** | Coaster Track + Iron Ingot + Redstone |
| **Station Track** | Coaster Track + Redstone Torch |
| **Splash Track** | Coaster Track + Water Bucket + Prismarine Shard |
| **Reverse Track** | Coaster Track + Ender Pearl + Andesite Alloy |
| **Sensor Track** | Coaster Track + Observer |
| **Slippery Track** | Coaster Track + Blue Ice + Packed Ice |
| **Bobsled Track** | Coaster Track + Blue Ice + Rail |
| **Rainbow Track** | Coaster Track + 6 Dyes |
| **Any material track** | Coaster Track + that block |
| **Any balloon** | Wool + String + Iron Nugget |

Material tracks convert freely. The recipe accepts any track from this addon, so an Oak Track plus Blue Wool gives a Blue Wool Track without starting from a plain one.

---

![Requirements](https://cdn.modrinth.com/data/cached_images/f2be7907aa245869e034f564e5bb8d6d3dff70e6.png)

| | Version |
|---|---|
| Minecraft | **1.21.1** |
| Loader | **NeoForge** 21.1.248 |
| [Create](https://modrinth.com/mod/create) | 6.0.0 – 6.x |
| [Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) | 0.1.2 – 0.1.x |
| [Sable](https://modrinth.com/mod/sable) | pulled in by Coasters Simulated |

In practice you install Create, Coasters Simulated, and this.

The base mod is pre-1.0 and its author has said he intends significant changes before release, so the dependency is pinned below 0.2.0. On a newer base mod you will get a dependency screen naming the version required rather than a crash, which is deliberate.

---

## Configuration

All 52 tuning values are exposed in the config: target speeds, acceleration rates, drag coefficients, activation thresholds, particle spread, sound cooldowns, and the distances a splash throws sideways and falls beneath the track. If a track feels wrong for your server, it is a config change rather than a bug report.

The config reloads from disk without a restart.

---

## Good to know

Material variants are cosmetic. A cherry track and a deepslate track ride identically.

Speeds are set per curve, so a long run built from several curves is configured curve by curve.

Multiplayer works, and so do schematics. A pasted coaster relinks its anchorpoints on load rather than arriving with the correct shape and broken connections.

---

## Frequently asked

**Does this work on Fabric?**
No. NeoForge only, because the base mod is NeoForge only.

**Do I need this on the server as well as the client?**
Yes, both. Track behaviour is server-side and the effects and Ponder scenes are client-side.

**Will it work with Create: Steam 'n' Rails or other Create addons?**
Yes. It registers through Create's public track API and does not touch train tracks.

**My coaster crawls to a stop halfway round.**
That is drag, and it is the base mod working as intended. Put a Slippery Track on the flat sections, or a small Boost mid-course.

**Can I use these textures in my modpack or video?**
Modpacks yes, no permission needed. See the licence: the code is MIT, the artwork is not.

---

![Credits](https://cdn.modrinth.com/data/cached_images/c8a6912b01b0270069a7c4da17009088cd9168f3.png)

Built on **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by **SilverGold**. The coaster physics, the track system, the cart and the original artwork are all theirs. This addon exists because that mod is good and I wanted more of it, and the textures here follow the base mod's own so that everything matches in-world.

Also built on **[Create](https://modrinth.com/mod/create)**.

Made by **NotZyvex**. Bug reports and suggestions are welcome on Discord.

[![CurseForge](https://cdn.modrinth.com/data/cached_images/cd1376d27fd5d3ca02cf6822aa859d8b5650a492.png)](https://www.curseforge.com/minecraft/mc-mods/create-coasters-extras) [![GitHub](https://cdn.modrinth.com/data/cached_images/0d1ef4ebf1904cccb1815ac7e72b9ce06c139185.png)](https://github.com/notzyvex1/createcoastersextras) [![Discord](https://cdn.modrinth.com/data/cached_images/501f3a19568fd6cb4afa22682316f804d8db84c3.png)](https://discord.gg/tCu7ccjYHv) ![TikTok](https://cdn.modrinth.com/data/cached_images/1e6d5b9d433f51c54d8be820ced0438ddeeb6ba7.png) [![Ko-fi](https://cdn.modrinth.com/data/cached_images/41bd578fa915f9305277f31161472e0e7050a23c.png)](https://ko-fi.com/notzyvex)
