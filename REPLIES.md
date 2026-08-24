# Replies to the three reporters

Post after the in-game test passes, not before. Saying "fixed" and shipping something broken
during a traffic spike costs more than replying a day later.

---

## 2238Dev — station timer resets to 100s, slider doesn't save on RMB release

> Both fixed in 2.1.1, thanks for the clear report.
>
> The slider one was the dial only saving when the drag crossed into a different
> direction zone — moving it within one zone changed the value but never marked the
> block dirty, so it snapped back and was gone on reload. It now saves on every release.
>
> The 100s was a nastier one: the dial remembered which row you last used *across a world
> reload*, so a world could load with it pointed at the direction bar, and the next drag
> got seeded from that bar's raw position — which sits around 100 in the middle. It no
> longer carries that across a reload, and the hold time is now clamped to its own range
> so another row's number can't land in it.

---

## universeunlimtd — multiplayer, host's track type overrides everyone

> Found it, and thank you — this was a good report.
>
> It's in the base mod rather than mine: Coasters Simulated keeps the track being placed
> in a single variable shared by the whole game, so on a server every player reads
> whoever placed last. I've worked around it in 2.1.1 for my track types by reading the
> item from the player actually doing the placing.
>
> My track types will behave in multiplayer now. The base mod's own tracks will still be
> affected until it's fixed upstream — I've reported it there with the details.
>
> And thanks re: the concrete rails 🖤

---

## Scroll-wheel report — places normal track

> Same root cause as the multiplayer bug, which is why it happens in single player too —
> that shared variable also goes stale when you change hotbar slot, so the placement uses
> the track you had a moment ago. Fixed in 2.1.1.

---

## Changelog line for 2.1.1

```
Fixed
- Station hold time no longer resets after leaving and rejoining a world.
- Direction and speed sliders now save when you release the button, including
  small adjustments.
- The right track type is now placed after switching hotbar slots, and in
  multiplayer each player places their own track instead of the host's.
  (The base mod's own track types are still affected by this; reported upstream.)
```
