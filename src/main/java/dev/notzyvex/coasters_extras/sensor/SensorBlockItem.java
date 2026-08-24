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

public class SensorBlockItem extends BlockItem {

    private static final int SEARCH = 6;

    private static final double ON_TRACK = 1.6;

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

    private static @Nullable Link findSensorCurve(Level level, BlockPos clicked, Vec3 hit,
                                                  @Nullable Player player) {
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
                continue;
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
        return bestDist <= ON_TRACK * ON_TRACK ? best : null;
    }

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

    private static double distToSegment(Vec3 p, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.lengthSqr();
        if (len < 1.0E-9) return p.distanceToSqr(from);
        double t = Math.max(0.0, Math.min(1.0, p.subtract(from).dot(d) / len));
        return p.distanceToSqr(from.add(d.scale(t)));
    }
}
