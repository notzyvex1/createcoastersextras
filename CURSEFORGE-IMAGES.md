![Create: Coasters Extras](https://cdn.modrinth.com/data/cached_images/1ac382fdfbd341d106604c4c5ea284ea4aa471d0.png)

**[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) ships with one balloon and one track. This addon adds 16 balloons and 287 tracks, and ten of those tracks actually do something to your coaster.**

[![Modrinth](https://cdn.modrinth.com/data/cached_images/eb50ef81187d89fcb34c061126715774f68359cc.png)](https://modrinth.com/mod/create-coasters-extras) [![GitHub](https://cdn.modrinth.com/data/cached_images/0d1ef4ebf1904cccb1815ac7e72b9ce06c139185.png)](https://github.com/notzyvex1/createcoastersextras) [![Discord](https://cdn.modrinth.com/data/cached_images/501f3a19568fd6cb4afa22682316f804d8db84c3.png)](https://discord.gg/tCu7ccjYHv) ![TikTok](https://cdn.modrinth.com/data/cached_images/1e6d5b9d433f51c54d8be820ced0438ddeeb6ba7.png) [![Ko-fi](https://cdn.modrinth.com/data/cached_images/41bd578fa915f9305277f31161472e0e7050a23c.png)](https://ko-fi.com/notzyvex)

You can boost your coaster, brake it, hold it in a station, fire redstone when it passes a sensor, or sit down at the **Coaster Controls** and drive it yourself.

> **⚠️ This is an addon and needs the base mod.** It will not load without **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** installed.

***

![Coaster track materials, top view](https://cdn.modrinth.com/data/cached_images/4e705d8c962d0d34430477d048a5f3dd4ae0fbd8.png)

***

## Never used the base mod?

Quick version: you place **anchorpoints**, then string **track** between them. The game draws a curve between the anchorpoints, and that curve is what your coaster rides on. The anchorpoints are the handles you grab to shape it.

Everything in this addon is either a new material for that curve, a new behaviour for it, or a balloon to decorate with.

If you get lost, **press W on any item in your inventory** to open a Ponder scene that shows you how to use it step by step.

***

![At a glance](https://cdn.modrinth.com/data/cached_images/9a76045bf7f75d0944dfbdbab2d41fdfa5c0c532.png)

*   **287 coaster tracks** — 277 material variants, 10 functional
*   **16 balloons** — 15 dye colours plus a Rainbow Balloon
*   **Sensor Block** — turns a passing coaster into a redstone signal
*   **Coaster Controls** — sit down and drive, with a live speedometer
*   **9 Ponder scenes** — one for each functional track
*   **Survival ready** — everything is craftable and drops when broken

***

![The functional tracks](https://cdn.modrinth.com/data/cached_images/9940802ffa0d6589ba306ab9d7abaaff8b717d45.png)

These are the main reason the addon exists. Each one is a normal coaster track that does something to your ride when it crosses.

![The functional tracks](https://cdn.modrinth.com/data/cached_images/86270c5bf03bba41081cc99561eb035cddcac86b.png)

![Boost Track](https://cdn.modrinth.com/data/cached_images/0239e2d5519fd55d90f021b289ade2decc44fad4.png)

**Accelerates a cart up to a target speed you set.** This is how a ride gets moving without a chain lift.

While it's accelerating it throws sparks and flames backwards as exhaust. The effect is strongest when launching from a standstill and fades as the cart gets up to speed. It only ever pushes a cart in the direction it's already going, so it can't reverse one.

![Powered Boost Track](https://cdn.modrinth.com/data/cached_images/08233dc02f3d7ce7ac06f0b08ab059b586912ddc.png)

**A Boost Track that only pushes while it has redstone.**

A normal boost runs all the time. Gate this one on a signal and a circuit can drive the ride instead — launch on a button, dispatch on a timer, or keep a section shut until a door opens.

![Launch Track](https://cdn.modrinth.com/data/cached_images/7cbe2054d918c54f8a3a73415746e70c7a55f9b1.png)

**A hydraulic launch.** From a dead stop it throws a cart up to speed with fire and smoke behind it, far harder than a boost, and unlike a boost it *will* start a ride that's stopped completely.

It also has a **Powered** dial. Leave it on Always and it fires the moment a cart touches it. Set it to On Redstone and it waits for your circuit. Boost and Brake have the same dial.

![Brake Track](https://cdn.modrinth.com/data/cached_images/f9bad503960626df6020932580c02c8d6db89fe8.png)

**Slows a cart down to a target speed.** Set the dial to zero and it stops the cart completely.

The braking force is calculated from how much track is left instead of being applied at a fixed rate, so a cart that comes in way too fast is still down to the target by the end of the run instead of overshooting. The wheels turn red while it's working.

Faster carts get bigger effects: above roughly **14 b/s** it throws lava particles, above **30** it adds sparks, above **43** it starts smoking, and the braking hiss gets louder the faster you hit it.

![Station Track](https://cdn.modrinth.com/data/cached_images/ba26144e1db21eeef912d13d3ce181f68972b1b3.png)

**Brings the ride in, holds it, and dispatches it again.**

It slows the cart so it stops exactly at the **last anchorpoint** no matter how fast it came in, and it freezes the whole train together instead of just the lead cart. After the wait it gently launches the ride back out.

*   **Right-click and hold the anchorpoint** to set the wait time in seconds
*   **Power it with redstone** to hold the ride in the station indefinitely
*   **Wear Engineer's Goggles** to see a live countdown. There's also a platform light that moves along the station while it waits

![Splash Track](https://cdn.modrinth.com/data/cached_images/4a92d0b9cc5bb757926d5691272874c80be8c610.png)

**Water rails.** Hit them at speed and spray comes off both side rails, water sheets down under the track, and it lands with a splash.

Water slows a ride down, so a splash section long enough to look good used to strand your coaster in the middle of it. The **Water Boost** dial fixes that — set a speed and the section drives the ride at it instead of only taking from it. Leave it at zero and it's pure drag like before.

![Reverse Track](https://cdn.modrinth.com/data/cached_images/c328a03fa2e7ee55b26821aa75010a198ef2b28d.png)

**Flips a cart's direction as it crosses.** Once per pass, so it doesn't sit there juddering back and forth.

Turns any dead end into a shuttle. The **Reverse Boost** dial sets how hard it throws the ride back out, so your boomerang can actually climb its return hill.

![Bobsled Track](https://cdn.modrinth.com/data/cached_images/cfe1c1c6c70c57e043b0040c2b7ad7d54d0ae65b.png)

**Leans the cart into corners.** The tighter the corner and the faster you take it, the further it goes over.

The lean comes from the corner itself, not an animation, and it settles back upright on the way out. The cart stays locked to the rail the whole time.

![Sensor Track](https://cdn.modrinth.com/data/cached_images/9a3ba9f88f5e80d053008947a64f6d77319b53ee.png)

**Detects every coaster that crosses it** and sparks when one does.

On its own that's just a visual. Link it to a **Sensor Block** and you get redstone out of it — see below.

![Slippery Track](https://cdn.modrinth.com/data/cached_images/3fc7003513810d351cf2ead5c62d3fe645bf383c.png)

**Removes the drag that normally bleeds speed from a coaster.** A cart leaves at the same speed it entered.

Useful for long flat runs between hills, where a ride would otherwise slow down and stall before reaching the station.

![Rainbow Track](https://cdn.modrinth.com/data/cached_images/e010cd11572b9fef5040dce3e45689fbce703bc3_0.webp)

**Rides like any other track, but leaves a trail of coloured sparkles** and plays a chime alongside them. The colour and the pitch are driven by the same value, so they stay in sync for the whole run.

The notes are locked to a major pentatonic scale. Without that, a fast curve would pick semitones at random and the whole thing would sound like a siren instead of a melody.

The chime is the original one from **1.0**. A later build had a tick gate that silenced every other note and made the tune stutter, and that's been removed, so it sounds the way it did on release again.

Crafted from **Coaster Track + 6 Dyes**. There's a matching **Rainbow Balloon** with the same colour sweep.

***

![The Sensor Block](https://cdn.modrinth.com/data/cached_images/be6fc58380ea05416923d08e9b4227e2a16f2aaa.png)

A curve is a connection between two anchorpoints, not an actual block in the world, so there's physically nothing there for a redstone signal to come out of. The Sensor Block is that missing block.

**Right-click a Sensor Track** (the track itself or either anchorpoint) to link the block to it. It can only be placed once it's linked, so you can't end up with a dead sensor and no idea why nothing fires.

After that, put it wherever you want. It doesn't need to be anywhere near the track. When a coaster crosses the linked track, the block powers, sparks, and makes an electrical snap. Hook it up to doors, dispensers, Redstone Links, note blocks, whatever you like.

***

![Coaster Controls](https://cdn.modrinth.com/data/cached_images/165578a2fd9e8e16a37f1ea9bded890c21ce4928.png)

A block you sit at, similar to Create's Train Controls.

**W and S move the lever**, and the lever actually moves with your input. Your speed shows on a custom bar in the XP slot, styled after Create's own. Stand up whenever you want and the ride carries on without you.

***

![Balloons](https://cdn.modrinth.com/data/cached_images/c801e624429f62360a74302fc864b69a3bfcc3da.png)

![The full balloon set](https://cdn.modrinth.com/data/cached_images/d5e4cba15c36d7e8e8e3a448dabfb461980ef276.png)

The base mod has one balloon, and it's red. This addon adds **15 more dye colours plus a Rainbow Balloon**, for a total of **17 balloons** between the base mod and the addon.

![All 17 balloons](https://cdn.modrinth.com/data/cached_images/01a8e11ac6272fc48f9970289ebbff2bb72b6530.png)

The added colours are **White, Light Gray, Gray, Black, Brown, Orange, Yellow, Lime, Green, Cyan, Light Blue, Blue, Purple, Magenta, and Pink**.

They all work exactly like the red one: they tether, they float, they pop, and **sneak + wrench** picks them back up.

***

![Track materials](https://cdn.modrinth.com/data/cached_images/3999f30a1e168fd69f2774420e03e9d1338b360c.png)

The same track you already build with, in 277 finishes. The material covers the full length of the curve, sleepers and slide bar both.

![Every track material](https://cdn.modrinth.com/data/cached_images/90ed2536bac5a509b7e3cfbaaeddd8c3cf3ea4b7.png)

*   **Wood** — Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson and Warped, each with planks, logs and stripped logs
*   **Stone** — Stone, Deepslate, Andesite, Granite, Diorite, Blackstone, Tuff, Calcite and their cut, polished and chiselled forms
*   **Wool (16)** — every dye colour
*   **Concrete (32)** — every dye colour, in concrete and concrete powder
*   **Copper** — every oxidation stage, cut and chiselled, plus the grate
*   **Ores** — the full set, overworld and deepslate
*   **Everything else** — terracotta, glazed terracotta, prismarine, quartz, froglights, sculk, netherite, Rose Quartz, Brass
*   **Special** — Rainbow and Copycat

The textures are built from the actual vanilla block textures with the track's shading laid over the top, so a wool track actually looks woven and a concrete track looks like concrete instead of a recoloured metal rail.

**Any track converts into any other**, so picking the wrong one isn't a trip back to the mine.

***

## Setting speeds

Speeds, wait times and directions are set on the **anchorpoint**, with a Create-style value dial: **right-click and hold, then drag**, the same way you'd set a Creative Motor. The range is **0–200 blocks/second**.

Set it on one end and the other end updates automatically, so the two anchorpoints of a curve always agree.

Depending on what's attached, an anchorpoint can carry up to three dials, sitting side by side across its face: the **speed**, the **Send** direction, and on a Boost, Brake or Launch track, **Powered**. Any anchorpoint with a dial is marked with three small brass pips, so you can tell which ones have settings without walking up to every one of them.

***

![Building your first ride](https://cdn.modrinth.com/data/cached_images/a72cb9068460ae176214410d4a981f5e856c8e6b.png)

Takes about five minutes.

1.  **Lay a loop.** Anchorpoints and plain Coaster Track from the base mod, ending back where you started.
2.  **Put a Station Track on the last curve** before the start. Right-click and hold its anchorpoint, then drag to 5 seconds.
3.  **Boost Track on the next curve out.** Set it to about 20 b/s, enough to get over the first hill.
4.  **Brake Track on the approach back in,** set to 8 b/s, so the ride comes in at a sane speed instead of shooting through the station.
5.  **Place a cart and sit in it.** The station dispatches, the boost launches you, the brake catches you, and the station takes you back in.

From there, add a **Slippery Track** on any long flat stretch where the ride is losing too much speed, and a **Sensor Track** wherever you want a door to open or a note block to fire.

***

![Crafting](https://cdn.modrinth.com/data/cached_images/bb31f5f6713282cacb5bf147bb8cc9ce89afb393.png)

Everything is craftable and everything drops when you break it. The recipes are deliberately cheap, since the point is building the ride, not grinding for parts.

*   **Boost Track** — Coaster Track + Andesite Alloy + Redstone
*   **Powered Boost Track** — Coaster Track + Andesite Alloy + Redstone Torch
*   **Launch Track** — Coaster Track + Blaze Powder + Piston
*   **Brake Track** — Coaster Track + Iron Ingot + Redstone
*   **Station Track** — Coaster Track + Redstone Torch
*   **Splash Track** — Coaster Track + Water Bucket + Prismarine Shard
*   **Reverse Track** — Coaster Track + Ender Pearl + Andesite Alloy
*   **Bobsled Track** — Coaster Track + Blue Ice + Rail
*   **Sensor Track** — Coaster Track + Observer
*   **Slippery Track** — Coaster Track + Blue Ice + Packed Ice
*   **Rainbow Track** — Coaster Track + 6 Dyes
*   **Any material track** — Coaster Track + that block (planks, wool, concrete, stone…)
*   **Any balloon** — Wool + String + Iron Nugget

**Material tracks convert freely.** The recipe accepts any track from this addon, so an Oak Track plus Blue Wool gives you a Blue Wool Track. You never have to go back to a plain track first.

***

![Requirements](https://cdn.modrinth.com/data/cached_images/f2be7907aa245869e034f564e5bb8d6d3dff70e6.png)

*   **Minecraft** 1.21.1
*   **NeoForge** 21.1.248
*   **[Create](https://www.curseforge.com/minecraft/mc-mods/create)** 6.0.x
*   **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** 0.1.2 – 0.1.x
*   **Sable** 2.0.3

Coasters Simulated already pulls in Sable, so in practice you just install **Create**, **Coasters Simulated**, and **this**.

The base mod is still pre-1.0 and its author has said there will be breaking changes before then, so the dependency is pinned below 0.2.0. If you're on a newer base mod version, you'll get a dependency screen telling you which version you need instead of a crash.

***

## Good to know

**Material variants are cosmetic.** A cherry track and a deepslate track ride identically.

**Speeds are set per curve.** A long run built from several curves is configured curve by curve.

**Everything is in the config.** All 45 values — speeds, acceleration, drag, thresholds, particle spread, sound cooldowns. If a track feels wrong on your server, it's a config line rather than a bug report. One worth knowing: `bobsled.invertBank` flips which way the bobsled leans, in case it leans out of your corners instead of into them.

**Multiplayer works**, and so do schematics. A pasted coaster relinks its anchorpoints on load, so it arrives working instead of with broken connections.

***

# MANY MORE FEATURES, BLOCKS AND ITEMS ARE PLANNED

***

![Credits](https://cdn.modrinth.com/data/cached_images/c8a6912b01b0270069a7c4da17009088cd9168f3.png)

Built on **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)** by **SilverGold**. The coaster physics, the track system, the cart and the original artwork are all theirs — this addon exists because that mod is good and I wanted more of it. The textures follow the base mod's style so everything matches in-world.

Also built on **[Create](https://www.curseforge.com/minecraft/mc-mods/create)**.

Made by **NotZyvex**. Bug reports and suggestions welcome, come say so on Discord.

[![Modrinth](https://cdn.modrinth.com/data/cached_images/eb50ef81187d89fcb34c061126715774f68359cc.png)](https://modrinth.com/mod/create-coasters-extras) [![GitHub](https://cdn.modrinth.com/data/cached_images/0d1ef4ebf1904cccb1815ac7e72b9ce06c139185.png)](https://github.com/notzyvex1/createcoastersextras) [![Discord](https://cdn.modrinth.com/data/cached_images/501f3a19568fd6cb4afa22682316f804d8db84c3.png)](https://discord.gg/tCu7ccjYHv) ![TikTok](https://cdn.modrinth.com/data/cached_images/1e6d5b9d433f51c54d8be820ced0438ddeeb6ba7.png) [![Ko-fi](https://cdn.modrinth.com/data/cached_images/41bd578fa915f9305277f31161472e0e7050a23c.png)](https://ko-fi.com/notzyvex)
