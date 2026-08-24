package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum SendDirection implements INamedIconOptions {

    AUTO(AllIcons.I_REFRESH, "auto"),

    FORWARD(AllIcons.I_CONFIG_NEXT, "forward"),

    REVERSE(AllIcons.I_CONFIG_PREV, "reverse");

    private static final SendDirection[] VALUES = values();

    public static final int COUNT = VALUES.length;

    private final AllIcons icon;
    private final String langSuffix;
    private final String translationKey;

    SendDirection(AllIcons icon, String name) {
        this.icon = icon;
        this.langSuffix = "send_direction." + name;
        this.translationKey = CoastersExtras.MOD_ID + "." + this.langSuffix;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }

    public String langSuffix() {
        return langSuffix;
    }

    public int sign() {
        return switch (this) {
            case FORWARD -> 1;
            case REVERSE -> -1;
            default -> 0;
        };
    }

    public static SendDirection byIndex(int index) {
        if (index < 0) return VALUES[0];
        if (index >= COUNT) return VALUES[COUNT - 1];
        return VALUES[index];
    }

    private static final int LEGACY_MAX = 200;

    public static SendDirection fromLegacyBar(int raw) {
        int clamped = Math.max(0, Math.min(LEGACY_MAX, raw));
        int zone = Math.min(COUNT - 1, clamped * COUNT / (LEGACY_MAX + 1));
        return VALUES[zone];
    }

    public int toLegacyBar() {
        return switch (this) {
            case FORWARD -> 100;
            case REVERSE -> LEGACY_MAX;
            default -> 0;
        };
    }
}
