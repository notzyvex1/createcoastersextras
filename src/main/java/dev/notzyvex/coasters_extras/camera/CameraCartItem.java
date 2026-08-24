package dev.notzyvex.coasters_extras.camera;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Places a Camera Cart on the nearest track.
 *
 * <p>Placed by clicking near track rather than on a specific block, because a coaster's track is a
 * spline between anchorpoints -- most of it is not a block you can click at all. The entity snaps
 * itself to the nearest point on the curve.
 */
public class CameraCartItem extends Item {

    public CameraCartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Vec3 at = Vec3.atCenterOf(context.getClickedPos()).add(0, 0.6, 0);
        CameraCartEntity camera = ModCamera.CAMERA_CART.get().create(level);
        if (camera == null) {
            return InteractionResult.FAIL;
        }
        camera.setPos(at.x, at.y, at.z);

        if (!camera.snapToTrack()) {
            say(player, "No coaster track nearby -- place this next to a track.",
                    ChatFormatting.RED);
            camera.discard();
            return InteractionResult.CONSUME;
        }

        level.addFreshEntity(camera);
        level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_FRAME_ADD_ITEM,
                SoundSource.BLOCKS, 0.8f, 1.3f);
        say(player, "Camera placed. Use the Camera Controller to run it.", ChatFormatting.GREEN);

        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static void say(Player player, String message, ChatFormatting colour) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message).withStyle(colour), true);
        }
    }
}
