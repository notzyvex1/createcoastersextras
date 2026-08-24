package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum LaunchTrigger implements INamedIconOptions {

    ALWAYS(AllIcons.I_PLAY, "always"),

    ON_REDSTONE(AllIcons.I_ACTIVE, "redstone");

    private static final LaunchTrigger[] VALUES = values();

    public static final int COUNT = VALUES.length;

    private final AllIcons icon;
    private final String langSuffix;
    private final String translationKey;

    LaunchTrigger(AllIcons icon, String name) {
        this.icon = icon;
        this.langSuffix = "launch_trigger." + name;
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

    public boolean needsSignal() {
        return this == ON_REDSTONE;
    }

    public static LaunchTrigger byIndex(int index) {
        if (index < 0) return VALUES[0];
        if (index >= COUNT) return VALUES[COUNT - 1];
        return VALUES[index];
    }
}
