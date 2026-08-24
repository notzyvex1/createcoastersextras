package dev.notzyvex.coasters_extras.sensor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.notzyvex.coasters_extras.CreateTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The Sensor Block as an item: link it to a Sensor Track first, then place it.
 *
 * <p>It deliberately cannot be placed until it is linked. A Sensor Block that watches nothing
 * is just a block that never turns on, and the only way to discover that is to wire it up and
 * wonder why the door never opens. Refusing the placement puts the mistake where it can be
 * understood.
 *
 * <p>Linking accepts a click anywhere along the track, not just on an anchorpoint. Aiming at a
 * curve makes the base mod retarget the hit to a block along it, so the position that arrives
 * here is somewhere near the curve rather than on either end -- this looks around it for the
 * nearest sensor curve and binds to that.
 */
public class SensorBlockItem extends BlockItem {

    /** How far from the click to look for anchorpoints that might own a sensor curve. */
    private static final int SEARCH = 6;

    /**
     * How close the click has to land to the track itself, in blocks.
     *
     * <p>Searching a box around the click and taking the nearest curve was wrong: standing
     * beside a sensor track and clicking the FLOOR reported "Linked", because a curve was
     * within reach even though it was nowhere near what you were pointing at. Being near a
     * thing is not the same as clicking it, so the click point is now measured against the
     * curve's own geometry.
     */
    private static final double ON_TRACK = 1.6;

    /**
     * How close the player's line of sight has to pass to the rail, in blocks.
     *
     * <p>A curve is not a block, so right-clicking one produces no block hit on the track --
     * the click lands on whatever is behind it, or nothing at all. Testing where the click
     * landed therefore only ever worked when you clicked an anchorpoint. What actually means
     * "I am pointing at that track" is the look ray passing through it, so that is what is
     * measured. Tighter than {@link #ON_TRACK} because aiming is precise in a way that
     * clicking a block face is not.
     */
    private static final double ON_AIM = 0.8;
    private static final double REACH = 6.0;

    public SensorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        Link found = findSensorCurve(level, clicked, context.getClickLocation(), player);
        if (found != null) {
            if (!level.isClientSide()) {
                // Through vanilla's helper, not by setting the component directly. The
                // BLOCK_ENTITY_DATA codec demands an "id" naming the block entity type and
                // throws on save without it -- which meant a linked Sensor Block sitting in
                // an inventory crashed the game on quit, when the player was written out.
                BlockItem.setBlockEntityData(stack, SensorRegistry.SENSOR_BLOCK_ENTITY.get(),
                                             SensorBlockEntity.linkTag(found.a, found.b));
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable("coasters_extras.sensor.linked")
                                     .withStyle(ChatFormatting.GREEN), true);
                }
                level.playSound(null, clicked, SoundEvents.NOTE_BLOCK_BELL.value(),
                                SoundSource.BLOCKS, 0.6F, 1.6F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        if (!SensorBlockEntity.hasLink(tag)) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("coasters_extras.sensor.not_linked")
                                 .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        InteractionResult result = super.place(context);
        // Confirm on the way down as well as on the way in. Linking and placing are separate
        // steps, and without this the second one is silent -- you are left guessing whether
        // the block you just put down is the one you linked or a fresh unlinked spare.
        if (result.consumesAction()) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide()) {
                BlockPos a = SensorBlockEntity.readA(tag);
                player.displayClientMessage(
                        Component.translatable("coasters_extras.sensor.placed",
                                a.getX() + ", " + a.getY() + ", " + a.getZ())
                                 .withStyle(ChatFormatting.GREEN), true);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip,
                                TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        boolean linked = SensorBlockEntity.hasLink(tag);

        // The link state goes above the fold. It is the one thing you need to know before
        // deciding whether this stack is ready to place, and burying it behind shift would
        // mean holding shift every time you picked one up.
        if (linked) {
            BlockPos a = SensorBlockEntity.readA(tag);
            tooltip.add(Component.translatable("coasters_extras.sensor.tip.linked",
                            a.getX() + ", " + a.getY() + ", " + a.getZ())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("coasters_extras.sensor.tip.unlinked")
                    .withStyle(ChatFormatting.GRAY));
        }

        if (CreateTooltip.collapsed(tooltip)) {
            return;
        }

        CreateTooltip.title(tooltip, "Sensor Block", ChatFormatting.RED);
        CreateTooltip.summary(tooltip,
                "Turns a coaster crossing a _Sensor Track_ into a _redstone signal_");
        CreateTooltip.pair(tooltip, "When Right-Clicked on a Sensor Track",
                "_Links_ to that track. Aim at the rail or at either _anchorpoint_");
        CreateTooltip.pair(tooltip, "Before it is Linked",
                "Cannot be _placed_. One watching nothing looks exactly like one that is "
              + "_broken_");
        CreateTooltip.pair(tooltip, "While a Coaster is on the Track",
                "Emits _full redstone_ from every side, and through the block it sits on");
        CreateTooltip.pair(tooltip, "Placement",
                "Does _not_ have to be near the track. Put it where the _wiring_ is");
    }

    private record Link(BlockPos a, BlockPos b) {}

    /**
     * The sensor curve nearest the clicked position, or null if there is none in reach.
     *
     * <p>Reflective, because the anchorpoint block entity belongs to the base mod and a
     * missing method should cost a link rather than crash a right-click.
     */
    private static @Nullable Link findSensorCurve(Level level, BlockPos clicked, Vec3 hit,
                                                  @Nullable Player player) {
        // Where the player is looking, as a segment. Curves are checked against this as well
        // as against the click, so aiming at a track binds even though the click itself
        // landed on the ground behind it.
        Vec3 eye = player == null ? null : player.getEyePosition();
        Vec3 tip = eye == null ? null : eye.add(player.getLookAngle().scale(REACH));

        Link best = null;
        double bestDist = Double.MAX_VALUE;
        boolean bestByAim = false;

        for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-SEARCH, -SEARCH, -SEARCH),
                                                   clicked.offset(SEARCH, SEARCH, SEARCH))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            Map<?, ?> curves;
            try {
                Object view = be.getClass().getMethod("getAnchorPeerCurvesView").invoke(be);
                if (!(view instanceof Map<?, ?> m)) continue;
                curves = m;
            } catch (Throwable ignored) {
                continue;               // not an anchorpoint
            }

            for (Map.Entry<?, ?> e : curves.entrySet()) {
                if (!(e.getKey() instanceof BlockPos peer)) continue;
                if (!(e.getValue() instanceof BezierConnection bc)) continue;
                if (bc.getMaterial() == null) continue;
                var id = bc.getMaterial().id;
                if (!"coasters_extras".equals(id.getNamespace())
                        || !"sensor_track".equals(id.getPath())) {
                    continue;
                }
                // Aimed at wins outright over merely clicked near.
                double aim = eye == null ? Double.MAX_VALUE
                                         : distanceToCurve(bc, pos, peer, eye, tip);
                if (aim <= ON_AIM * ON_AIM) {
                    if (!bestByAim || aim < bestDist) {
                        bestDist = aim;
                        best = new Link(pos.immutable(), peer.immutable());
                        bestByAim = true;
                    }
                    continue;
                }
                if (bestByAim) continue;

                double d = distanceToCurve(bc, pos, peer, hit, null);
                if (d < bestDist) {
                    bestDist = d;
                    best = new Link(pos.immutable(), peer.immutable());
                }
            }
        }
        if (bestByAim) return best;
        // Near a sensor track is not the same as pointing at one.
        return bestDist <= ON_TRACK * ON_TRACK ? best : null;
    }

    /**
     * Shortest distance, squared, from a point to the curve's rail.
     *
     * <p>Sampled rather than solved: the exact nearest point on a cubic bezier is a quartic,
     * and twenty-odd samples are far more precision than a click needs.
     *
     * <p>The samples are not guaranteed to be in world space -- some curves store their
     * points relative to the owning block entity -- so rather than assume, this probes t=0
     * against the two anchorpoints and offsets everything if it landed nowhere near either.
     */
    private static double distanceToCurve(BezierConnection bc, BlockPos a, BlockPos b,
                                          Vec3 from, @Nullable Vec3 to) {
        Vec3 wa = Vec3.atCenterOf(a);
        Vec3 wb = Vec3.atCenterOf(b);
        Vec3 probe = bc.getPosition(0);
        if (probe == null) return Double.MAX_VALUE;

        Vec3 base = Vec3.ZERO;
        if (probe.distanceToSqr(wa) > 64 && probe.distanceToSqr(wb) > 64) {
            base = Vec3.atLowerCornerOf(bc.bePositions.getFirst());
        }

        double best = Double.MAX_VALUE;
        for (int i = 0; i <= 24; i++) {
            Vec3 p = bc.getPosition(i / 24.0);
            if (p == null) continue;
            Vec3 w = p.add(base);
            best = Math.min(best, to == null ? w.distanceToSqr(from) : distToSegment(w, from, to));
        }
        return best;
    }

    /** Squared distance from a point to the segment from-to. */
    private static double distToSegment(Vec3 p, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.lengthSqr();
        if (len < 1.0E-9) return p.distanceToSqr(from);
        double t = Math.max(0.0, Math.min(1.0, p.subtract(from).dot(d) / len));
        return p.distanceToSqr(from.add(d.scale(t)));
    }
}
