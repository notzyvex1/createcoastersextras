package dev.notzyvex.coasters_extras;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

public final class CreateTooltip {

    private static final Style PASSIVE   = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style KEYBIND   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style SUMMARY   = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style CONDITION = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style HIGHLIGHT = Style.EMPTY.withColor(ChatFormatting.GOLD);

    public static boolean collapsed(List<Component> tooltip) {
        if (FMLEnvironment.dist == Dist.CLIENT && shiftDown()) {
            return false;
        }
        tooltip.add(Component.literal("Hold ").setStyle(PASSIVE)
                .append(Component.literal("[Shift]").setStyle(KEYBIND))
                .append(Component.literal(" for Summary").setStyle(PASSIVE)));
        return true;
    }

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

    private static boolean shiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    private CreateTooltip() {}
}
