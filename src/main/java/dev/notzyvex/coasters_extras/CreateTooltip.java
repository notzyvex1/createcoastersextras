package dev.notzyvex.coasters_extras;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Create's tooltip layout, so ours read as part of the same mod family.
 *
 * <p>Collapsed it is one grey line telling you to hold shift. Expanded it is a title, a
 * one-line summary, then condition/behaviour pairs -- the shape Create uses for every machine
 * it ships, which is the shape anyone installing a Create addon is already reading fluently.
 *
 * <p>Lifted out of the balloon item once the tracks needed it too. Text uses Create's
 * {@code _underscore_} convention to mark the words that get highlighted.
 */
public final class CreateTooltip {

    private static final Style PASSIVE   = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style KEYBIND   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style SUMMARY   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style CONDITION = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style HIGHLIGHT = Style.EMPTY.withColor(ChatFormatting.GOLD);

    /**
     * Adds the collapsed line and reports whether the caller should stop there.
     *
     * <p>The environment check matters: {@code appendHoverText} is a common method and
     * anything that builds tooltips server-side would otherwise resolve a client-only class
     * and take a dedicated server down with it.
     */
    public static boolean collapsed(List<Component> tooltip) {
        if (FMLEnvironment.dist == Dist.CLIENT && shiftDown()) {
            return false;
        }
        tooltip.add(Component.literal("Hold ").setStyle(PASSIVE)
                .append(Component.literal("[Shift]").setStyle(KEYBIND))
                .append(Component.literal(" for Summary").setStyle(PASSIVE)));
        return true;
    }

    /** Blank line, coloured title, blank line -- Create's header. */
    public static void title(List<Component> tooltip, String text, ChatFormatting colour) {
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(text).withStyle(colour));
        tooltip.add(Component.empty());
    }

    public static void summary(List<Component> tooltip, String text) {
        tooltip.add(highlight(text, SUMMARY));
        tooltip.add(Component.empty());
    }

    public static void pair(List<Component> tooltip, String condition, String behaviour) {
        tooltip.add(Component.literal(condition).setStyle(CONDITION));
        tooltip.add(Component.literal(" ").append(highlight(behaviour, CONDITION)));
        tooltip.add(Component.empty());
    }

    /** Renders Create's {@code _underscored_} segments in the highlight colour. */
    public static MutableComponent highlight(String text, Style base) {
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

    /** Isolated so {@code Screen} is only ever resolved on a client. */
    private static boolean shiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    private CreateTooltip() {}
}
