package dev.notzyvex.coasters_extras;

import net.minecraft.ChatFormatting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Balloon item tooltip, styled to match Create (and therefore the base mod's red balloon),
 * so ours sit alongside theirs without looking bolted on.
 *
 * <p>Collapsed: {@code Hold [Shift] for Summary}.
 * Expanded: a coloured flavour line, a summary, then condition/behaviour pairs.
 *
 * <p>Text uses Create's {@code _underscore_} convention to mark highlighted words; see
 * {@link #highlight}.
 */
public class BalloonBlockItem extends BlockItem {

    /** Create's palette, so our tooltips match theirs exactly. */
    private static final Style PASSIVE   = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style KEYBIND   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style SUMMARY   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style CONDITION = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style BEHAVIOUR = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style HIGHLIGHT = Style.EMPTY.withColor(ChatFormatting.GOLD);

    private final BalloonColor color;

    public BalloonBlockItem(Block block, Item.Properties props, BalloonColor color) {
        super(block, props);
        this.color = color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);

        // Guarded, and the check reads the environment rather than the level: appendHoverText
        // is a common method, and anything that calls it server-side -- some mods build
        // tooltips there -- would otherwise try to resolve a client-only class and take the
        // dedicated server down with it.
        if (FMLEnvironment.dist != Dist.CLIENT || !coasters_extras$shiftDown()) {
            tooltip.add(Component.literal("Hold ").setStyle(PASSIVE)
                    .append(Component.literal("[Shift]").setStyle(KEYBIND))
                    .append(Component.literal(" for Summary").setStyle(PASSIVE)));
            return;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("The ").withStyle(ChatFormatting.WHITE)
                .append(colouredName())
                .append(Component.literal(" Balloon!").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.empty());

        tooltip.add(highlight("Pop! Useful utility item for _moving physics contraptions_", SUMMARY));
        tooltip.add(Component.empty());

        pair(tooltip, "When Placed on a Sublevel",
                "_Lifts_ sublevel slightly off the ground. Orients them _upright_");
        pair(tooltip, "When Shot with Projectile",
                "_Pops_ all nearby balloons, dropping the sublevel");
        pair(tooltip, "When Wrenched while Sneaking",
                "_Retrieves_ the balloon without popping nearby balloons");
    }

    private static void pair(List<Component> tooltip, String condition, String behaviour) {
        tooltip.add(Component.literal(condition).setStyle(CONDITION));
        tooltip.add(Component.literal(" ").append(highlight(behaviour, BEHAVIOUR)));
        tooltip.add(Component.empty());
    }

    /** Renders Create's {@code _underscored_} segments in the highlight colour. */
    private static MutableComponent highlight(String text, Style base) {
        MutableComponent out = Component.empty();
        boolean hot = false;
        for (String part : text.split("_", -1)) {
            if (!part.isEmpty()) {
                out.append(Component.literal(part).setStyle(hot ? HIGHLIGHT : base));
            }
            hot = !hot;
        }
        return out;
    }

    /** Flat dye colour, or a per-character sweep for rainbow. */
    private MutableComponent colouredName() {
        String name = prettyColourName();

        if (!color.isRainbow()) {
            return Component.literal(name)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.rgb())));
        }

        MutableComponent out = Component.empty();
        int len = Math.max(1, name.length() - 1);
        for (int i = 0; i < name.length(); i++) {
            float hue = (float) i / len * 0.78F;   // red -> violet, same range as the texture
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F) & 0xFFFFFF;
            out.append(Component.literal(String.valueOf(name.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return out;
    }

    private String prettyColourName() {
        StringBuilder sb = new StringBuilder();
        String[] parts = color.colorName().split("_");
        for (int i = 0; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }
    /** Isolated so {@code Screen} is only ever resolved on a client. */
    private static boolean coasters_extras$shiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}
