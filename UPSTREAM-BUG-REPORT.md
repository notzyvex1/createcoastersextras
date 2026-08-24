# Upstream report → Create: Coasters Simulated

Post to the base mod's issue tracker / Discord. This is **their** bug, not ours — we can only
work around it from an addon, and every addon that touches track placement will hit it.

Tested against **0.1.5**.

---

**Title:** `CoasterTrackPlacement.lastItem` is a global static — wrong track type placed in
multiplayer and after switching hotbar slots

---

### What happens

**Multiplayer:** the track everyone places is decided by whoever placed last. If the host
places a booster and a second player then tries to place a brake, the second player gets a
booster. It applies to every track type.

**Single player:** scroll to a different track and the first placement still uses the
previous one.

### Why

`CoasterTrackPlacement.lastItem` (line 83) is a single `static ItemStack` for the entire game:

```java
static ItemStack lastItem;
```

It is assigned on only some of the paths that reach a placement (line 309, and again around
line 1835 in `denyCoasterCrossPreview`), and it is then read to decide the material for the
curve being built.

Two consequences fall out of the one field:

- **One static, every player.** On a server there is one JVM, so every player shares that
  field. Player B's placement resolves against Player A's stack. There is no per-player
  state anywhere in the path.
- **It goes stale.** Changing the held item does not necessarily refresh it, so the next
  placement is resolved against an item the player is no longer holding.

This also means the value is written from the client preview path and read from the server
commit path, which are different threads in single player.

### Suggested fix

The information needed is already in scope at every entry point — none of these need new
plumbing:

| Method | Already has |
|---|---|
| `tryConnect(Level, Player, BlockPos, BlockState, ItemStack stack, …)` | the stack **and** the player |
| `tryConnectSecondAnchorFromCurve(Level, Player, ItemStack stack, …)` | the stack |
| `applyAnchorFirstSelection(Level, ItemStack stack, BlockPos, Player, …)` | the stack |
| `commitSecondAnchorOnServer(ServerPlayer player, InteractionHand hand, …)` | reads `player.getItemInHand(hand)` itself |

So the material can be resolved from the stack already being passed down, and `lastItem`
dropped entirely. If it has to stay for the preview cache, keying it per player (or per
thread) would fix the cross-player half.

### Reported by

Surfaced by two Create: Coasters Extras users:

- *universeunlimtd* — multiplayer via Essential; host's track type overrides everyone else's
- a second report of the scroll-wheel case, which occurs in single player too

### Note

Coasters Extras 2.1.1 works around this by capturing the real stack at those entry points and
preferring it over `lastItem`. That fixes it for our track types only — the base mod's own
track types still take the wrong value, and any other addon hooking placement will see the
same thing. Hence reporting it rather than just patching around it.
