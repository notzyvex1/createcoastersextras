# Create: Coasters Extras 1.2

Five new functional tracks, 232 new materials, and a stack of fixes for things that were
quietly breaking rides people had already built.

## New tracks

**Splash Track.** Water rails. A coaster crossing at speed throws spray from both side rails and
sheets water down beneath the track, with a splash on the way in. The rails are animated, so
light moves across the water and bubbles surface and pop.

Water slows a ride down, which meant a splash section long enough to look convincing was long
enough to strand a coaster in the middle of it. The Water Boost dial handles that: set a speed
and the section drives the ride at it instead of only taking from it, so a flume runs
continuously. Left at zero it is pure drag, as before.

**Launch Track.** A hydraulic-style launch that accelerates a cart far harder than a boost, with
fire and smoke behind it. Unlike a boost it will start a ride that has stopped completely.

It also has a Powered dial, shared with Boost and Brake. Left on Always it fires on contact;
set to On Redstone it holds the train until a circuit releases it, which is what you want as
soon as there is a station in front of it.

**Reverse Track.** Reverses a cart's direction as it crosses, once per pass so it does not
oscillate in place. This turns a dead end into a shuttle. The Reverse Boost dial sets how hard it
sends the ride back out, which matters because a boomerang that cannot climb its own return hill
just leaves the cart in the valley.

**Powered Boost Track.** A boost that only pushes while its anchorpoint has redstone. An ordinary
boost runs constantly, which makes it something you design around rather than something you
operate; gating it on a signal means a circuit can drive the ride. Its speed and acceleration are
separate config values from the plain boost, so a signal-driven launch can be aggressive without
affecting every boost on the map.

**Bobsled Track.** Leans the cart into corners, up to 35 degrees by default. The lean is
calculated rather than animated: the track measures how tight the curve is and how fast the cart
is going, converts that to the sideways force a rider would feel, and rolls the cart to the angle
that cancels it. Coming out of the corner it settles upright on its own.

## 232 more materials

The track list went from 55 to 287. Every wood with its planks, logs and stripped logs; all
sixteen wools, concretes and concrete powders; the ores in both overworld and deepslate form;
every stage of copper oxidation; terracotta, prismarine, quartz, froglights, sculk and netherite.

Tracks are now built from the actual block rather than a metal rail tinted to approximately the
right colour, which is why wool and concrete used to be indistinguishable from each other.

Any track can also be crafted into any other, so picking the wrong one is no longer a trip back to
the mine.

## Fixes

**Carts fell through plain track.** Every one of our track IDs compares equal to the base mod's,
which is deliberate and is how the tracks stay compatible. The bobsled check used that comparison,
so ordinary coaster track was being handed the bobsled's loosened rail guide and carts slid off
it. The same bug was breaking two other things silently: painting a track and converting a section
to a Brake both refused with "Already that material" when they were nothing of the kind.

**Speed could not be set above 60.** The dial is drawn 0 to 200 and was clamping at 60. That cap
was only ever meant for a station's dwell time in seconds, and now applies only there.

**Pick-block returned the wrong track.** Middle-clicking one of these tracks gave you the base
mod's plain track instead of the one you were looking at.

**The splash sound looped** for as long as a cart was on the track. It now plays once per pass.

**Track items showed their raw translation key**, and Splash, Launch and Reverse all described
themselves as "Cosmetic" in the tooltip.

**Dial labels had a blank square** where an icon should have been, because the glyph is not in
Minecraft's font. Every dial now names its own setting, and a dial nobody has touched reads
"Default" rather than a clipped "config default".

**Splash, Launch, Reverse and Powered Boost had no crafting recipes at all** and were obtainable
only in creative. All four are craftable now, as is the Bobsled Track.

**A redstone-held station froze every coaster that arrived afterwards.** The arrest flag a
station sets when a ride reaches the platform is only cleared by its dispatch, and a station
held by redstone never reaches that dispatch. So the flag stayed set forever, and from then on
any cart touching that track was pinned where it stood instead of rolling to the end. The
station now records when each cart turned up, so a coaster that arrives after the hold still
drives to the platform end and joins the train there. Trains still stop together. Reported by
a new bacteria may Hi.

**Anchorpoints that carry a dial are marked.** Three small brass pips, so you can see which
anchorpoints have settings without opening each one. Only the seven tracks that actually have
dials are marked.

**The three dials sit side by side** across the anchorpoint face rather than stacked down it.
An anchorpoint is half a block tall and a full block wide, so stacking them ran out of room.

**The Rainbow Track's chime has somewhere to go.** It was six notes inside one octave, so a
cart could cross most of a rainbow section without the tune moving enough to hear. It is now
eleven notes across two, and the colour and pitch travel about two and a half times faster
along the track. Both are one config value.

## Other changes

The Send dial is its own control now, a small three-way picker like the one on a Mechanical
Bearing, rather than a row on the speed board. It had to move to be small at all: a value board
shares one width across every row, so sitting next to a 0-200 speed forced a three-way choice onto
a two-hundred-notch slider. Anything already built keeps its direction.

Brake wheels turn red while braking.

The Tracks tab header is now a scrolling strip of track: every wood species with its planks, logs
and stripped logs, then all sixteen wools. A full lap takes about a minute and a half.

The Functional Tracks section is labelled in purple so it does not read as another row of Create.

52 config options, covering every speed, rate, threshold, particle spread and sound cooldown in
the mod. One worth knowing about is `bobsled.invertBank`: which way the lean should go depends on
the base mod's handedness, so if carts lean out of corners rather than into them, set it true.

Ponder scenes for the new tracks.
