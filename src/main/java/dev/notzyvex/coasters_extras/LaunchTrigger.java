package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/**
 * When a Launch Track is allowed to fire.
 *
 * <p>A launch that goes off the instant anything touches it is a hazard, not a ride: you cannot
 * hold a train in the launch section, you cannot stage a dispatch, and you cannot stop the thing
 * re-launching a cart that rolled back. Gating it on redstone turns the launch into something a
 * circuit can time.
 *
 * <p>Same control as the Send dial -- a small icon picker rather than a row on the speed board,
 * because {@code ValueSettingsBoard} shares one {@code maxValue} across every row and a
 * two-position choice on a 0..200 bar is not a choice, it is a guess.
 *
 * <p>{@link #ALWAYS} must stay ordinal 0. A fresh {@code ScrollValueBehaviour} starts at 0 and a
 * missing NBT key reads back as 0, so every launch track already built in every existing world
 * loads as ALWAYS -- which is exactly what it has been doing since the track shipped. Putting
 * ON_REDSTONE first would silently switch off every launch on every server that updates.
 */
public enum LaunchTrigger implements INamedIconOptions {

    /** Fires on contact, every time. The play arrow reads as "runs on its own". */
    ALWAYS(AllIcons.I_PLAY, "always"),

    /** Fires only while the anchorpoint has a signal. Create's own "powered" icon. */
    ON_REDSTONE(AllIcons.I_ACTIVE, "redstone");

    /** Cached so {@link #byIndex(int)} does not clone the array on every call. */
    private static final LaunchTrigger[] VALUES = values();

    public static final int COUNT = VALUES.length;

    private final AllIcons icon;
    private final String langSuffix;
    private final String translationKey;

    LaunchTrigger(AllIcons icon, String name) {
        this.icon = icon;
        // MOD_ID is a compile-time String constant, so this does not pull CoastersExtras into
        // the enum's class initialisation.
        this.langSuffix = "launch_trigger." + name;
        this.translationKey = CoastersExtras.MOD_ID + "." + this.langSuffix;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    /**
     * The FULL translation key.
     *
     * <p>Create's {@code ScrollOptionSettingsFormatter} calls
     * {@code Component.translatable(getTranslationKey())} directly -- it does not prepend
     * {@code create.} the way {@code CreateLang.translate} does -- so the key must be complete
     * or the dial renders the raw key.
     */
    @Override
    public String getTranslationKey() {
        return translationKey;
    }

    /** The same key without the namespace, for {@code LangBuilder.translate}, which prepends it. */
    public String langSuffix() {
        return langSuffix;
    }

    /** True if this setting requires a signal before the launch may fire. */
    public boolean needsSignal() {
        return this == ON_REDSTONE;
    }

    public static LaunchTrigger byIndex(int index) {
        if (index < 0) return VALUES[0];
        if (index >= COUNT) return VALUES[COUNT - 1];
        return VALUES[index];
    }
}
