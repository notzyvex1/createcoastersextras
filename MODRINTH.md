# Create: Coasters Extras

![bannerthxdolphin](https://cdn.modrinth.com/data/cached_images/1ac382fdfbd341d106604c4c5ea284ea4aa471d0.png)

An addon for [Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated). The base mod handles the physics and gives you one track to run on. This adds the rest of the park: a station that runs itself, a driver's seat, nine tracks that change what a cart does when it crosses them, and 277 more that are purely cosmetic.

> This mod requires **[Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated)**. It will not load without it.

---

![Coaster track materials, top view](https://cdn.modrinth.com/data/cached_images/4e705d8c962d0d34430477d048a5f3dd4ae0fbd8.png)

---

## If you haven't used the base mod before

You place anchorpoints, then run track between them. The game draws a curve through those points, and that curve is your ride. Anchorpoints are the handles you grab to reshape it.

Everything in this mod is one of three things: a material for that curve, a behaviour for it, or decoration to put around it.

Press `W` on any item to open its Ponder scene, which builds the thing in front of you.

---

## At a glance

| | |
|---|---|
| Coaster tracks | 286 total: 9 functional, 277 cosmetic |
| Balloons | 17, including a Rainbow and a Copycat |
| Ponder scenes | 9 |
| Config options | 45 |
| Requires | Minecraft 1.21.1, NeoForge, Create, Create: Coasters Simulated |

---

## Functional tracks

There are nine. Each one is configured from the anchorpoints at either end: look at one, hold right-click, and scroll, the same interaction as Create's Creative Motor. Whichever end you set, both are written, so it doesn't matter which one you happen to be standing next to.

A dial you have never touched displays "Default" and falls back to the config value for that track.

### Boost Track

Accelerates a coaster up to the speed you set. For most layouts this is the only powered track you need, and it is how a ride moves if you don't want a chain lift. It will not reverse anything; it pushes in whatever direction the cart is already travelling.

### Powered Boost Track

Identical to the Boost Track, except that it only pushes while its anchorpoint is receiving a redstone signal.

An ordinary boost runs constantly, which makes it something you design around rather than something you operate. Gating it on a signal means a circuit can drive the ride: launch from a button, dispatch on a timer, or keep a section shut until a door opens. It keeps its own speed and acceleration values in the config, separate from the plain boost, so you can make a signal-driven launch aggressive without affecting every boost on the map.

### Launch Track

A hydraulic-style launch. From a standstill it accelerates a cart far harder than a boost does, with fire and smoke behind it, and unlike a boost it will start a ride that has come to a complete stop.

The **Launch On** dial decides when it fires. Left on *Always* it triggers on contact. Set to *On Redstone* it holds until your circuit releases it, which is what you want as soon as there is a station in front of it.

### Brake Track

Slows a coaster to a target speed, or stops it completely if you set the dial to zero.

It brakes according to how much track is left rather than at a fixed rate, so a train arriving much too fast is still at the target speed by the end of the section instead of overshooting it. The wheels turn red while braking.

### Station Track

Brings a train in, holds it, and dispatches it again.

It eases the whole train to a halt at the platform regardless of arrival speed, and it arrests every cart together rather than letting each one stop independently. The dial sets the dwell time in seconds. A redstone signal on the anchorpoint holds the train indefinitely, which is how you gate a station. Engineer's Goggles show a live countdown.

### Splash Track

Water rails. A coaster crossing at speed throws spray from both side rails, drops a curtain of water beneath the track, and lands with a splash.

Water slows a ride down, so a splash section long enough to look convincing was previously long enough to strand a coaster in the middle of it. The **Water Boost** dial fixes that: set a speed and the section drives the ride at it instead of only taking from it. Left at zero it behaves as pure drag.

### Reverse Track

Reverses a cart's direction of travel as it crosses, once per pass so that it does not oscillate in place.

This turns a dead end into a shuttle. The **Reverse Boost** dial controls how hard it sends the ride back out, which matters more than it might sound: a boomerang layout that cannot climb its own return hill just leaves the cart sitting in the valley.

### Sensor Track

Detects every coaster that passes over it and sparks as it does.

Pair it with a **Sensor Block** and that block emits a redstone signal on each pass. The block does not need to be adjacent to the track, so you can put it behind a wall, under the platform, or in a control room some distance away.

### Slippery Track

Removes the drag a coaster normally loses speed to, so it leaves at the speed it entered. Intended for long flat runs between hills, where a ride would otherwise stall short of the station.

---

## Coaster Controls

A block you place on a cart and sit at to drive. The lever animates with your throttle input, and the speedometer occupies the experience bar's slot, matching the way Create's own train controls behave, so you can read your speed without looking away from the track.

---

## Balloons

![The full balloon set](https://cdn.modrinth.com/data/cached_images/d5e4cba15c36d7e8e8e3a448dabfb461980ef276.png)

Seventeen in total: fifteen dye colours, a Rainbow Balloon, and a Copycat Balloon that adopts the texture of whatever block you give it. They are decorative, and they are most of the difference between a coaster that looks like a fairground and one that looks like a rollercoaster in an empty field.

---

## Cosmetic tracks

277 materials, built from the actual block rather than a metal track tinted to approximately the right colour. That distinction is the reason wool and concrete tracks are now visually distinct from each other.

The set covers every wood along with its planks, logs and stripped logs; all sixteen wools and concretes; the ores in both overworld and deepslate form; every stage of copper oxidation, cut and chiselled; and terracotta, glazed terracotta, prismarine, quartz, froglights, sculk and netherite.

Any track can be crafted into any other, so choosing the wrong one does not mean another trip to the mine.

**Rainbow Track** cycles colour along its length and chimes as you ride over it. It has no effect on speed.

**Copycat Track** takes on the appearance of any block you give it.

---

## Building your first ride

1. Place two anchorpoints and run Boost Track between them.
2. Add a Station Track section where the platform should be.
3. Put Coaster Controls on the cart and sit down.
4. Run Brake Track into the station so the train arrives slowly.
5. The rest is shaping the curve.

---

## Configuration

All 45 tuning values are exposed: target speeds, acceleration rates, drag coefficients, activation thresholds, particle spread, sound cooldowns, and the distances a splash throws sideways and falls beneath the track. If a track feels wrong for your server, it is a config change rather than a bug report.

Every block in the mod is craftable, and every block drops itself when broken. Nothing here is creative-only.

---

## Known gaps

A **Bobsled Track** exists in the files but is not in the creative menu. The physics half is implemented (the cart is released from the rail so it can slide across the trough and bank), but the trough itself is not, so a cart currently slides off the side. It will be added to the menu once it is a ride rather than a hazard.

---

## Credits

Built on [Create: Coasters Simulated](https://modrinth.com/mod/create-coasters-simulated) by Silvergold. Creative tab sections adapted from Create Simulated (MIT). Banner art by dolphin.
