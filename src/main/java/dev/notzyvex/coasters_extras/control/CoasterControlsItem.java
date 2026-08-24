package dev.notzyvex.coasters_extras.control;

import dev.notzyvex.coasters_extras.CreateTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class CoasterControlsItem extends BlockItem {

    public CoasterControlsItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        if (CreateTooltip.collapsed(tooltip)) {
            return;
        }

        CreateTooltip.title(tooltip, "Coaster Controls", ChatFormatting.GOLD);
        CreateTooltip.summary(tooltip,
                "_Drive_ a coaster yourself, instead of building the motion into the _track_");
        CreateTooltip.pair(tooltip, "When Placed on a Coaster",
                "Becomes a _control stand_ you can take hold of");
        CreateTooltip.pair(tooltip, "When Right-Clicked while Seated",
                "Takes the controls. _W_ accelerates, _S_ brakes and reverses");
        CreateTooltip.pair(tooltip, "While Driving",
                "A _speedometer_ replaces the experience bar, and the lever moves with you");
        CreateTooltip.pair(tooltip, "Standing Up",
                "_Releases_ the controls");
        CreateTooltip.pair(tooltip, "Note",
                "You must be _sitting_ on the coaster. Movement only reaches the server from "
              + "a _passenger_");
    }
}
