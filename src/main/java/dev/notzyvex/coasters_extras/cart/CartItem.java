package dev.notzyvex.coasters_extras.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;

public class CartItem extends Item {

    private final CartPrefab prefab;

    public CartItem(Properties properties, CartPrefab prefab) {
        super(properties);
        this.prefab = prefab;
    }

    public CartPrefab prefab() {
        return prefab;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        Vec3 at = Vec3.atCenterOf(context.getClickedPos()).add(0, 1, 0);
        ServerSubLevel cart = CoasterCartSpawner.spawnMinimalContraption(
                server, at, new Quaterniond());
        if (cart == null) {
            say(player, "Place this on or near coaster track.", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        build(cart);

        say(player, prefab.display() + " placed.", ChatFormatting.GREEN);
        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private void build(ServerSubLevel cart) {
        LevelAccessor plot = cart.getPlot().getEmbeddedLevelAccessor();
        BlockPos origin = cart.getPlot().getCenterBlock();
        for (CartPrefab.Placement placement : prefab.parts()) {
            BlockPos target = origin.offset(placement.offset());
            plot.setBlock(target,
                    ModCartParts.PARTS.get(placement.part()).get().defaultBlockState(), 2);
        }
    }

    private static void say(Player player, String message, ChatFormatting colour) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message).withStyle(colour), true);
        }
    }
}
