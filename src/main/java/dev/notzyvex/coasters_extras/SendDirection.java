package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/**
 * Which way a track sends a ride out.
 *
 * <p>This used to be a row on the shared value board, which was the wrong instrument for it.
 * {@code ValueSettingsBoard} is a record with ONE {@code maxValue} for every row, so a board
 * that also carries a 0..200 speed forces the direction row to be a 0..200 drag bar as well --
 * a three-way choice presented as two hundred positions, with the meaning hidden behind a
 * formatter that reprinted thirds of the bar as words.
 *
 * <p>Create already has the right control: {@code ScrollOptionBehaviour} overrides
 * {@code createBoard} to return a board whose {@code maxValue} is {@code options.length - 1}
 * and whose formatter is a {@code ScrollOptionSettingsFormatter}. That is the whole reason the
 * Mechanical Bearing's dial is a small icon picker rather than a slider, and implementing
 * {@link INamedIconOptions} is all it takes to get one.
 *
 * <p>AUTO must stay ordinal 0. A fresh {@code ScrollValueBehaviour} starts at value 0 and a
 * missing NBT key reads back as 0, so anything else here would mean every untouched anchorpoint
 * in every existing world loaded with a forced direction.
 */
public enum SendDirection implements INamedIconOptions {

    /** Keep whatever the cart arrived with. The loop icon reads as "carry on round". */
    AUTO(AllIcons.I_REFRESH, "auto"),

    /** Always leave along the curve's own tangent. */
    FORWARD(AllIcons.I_CONFIG_NEXT, "forward"),

    /** Always leave against it. */
    REVERSE(AllIcons.I_CONFIG_PREV, "reverse");

    /** Cached so {@link #byIndex(int)} does not clone the array on every call. */
    private static final SendDirection[] VALUES = values();

    public static final int COUNT = VALUES.length;

    private final AllIcons icon;
    private final String langSuffix;
    private final String translationKey;

    SendDirection(AllIcons icon, String name) {
        this.icon = icon;
        // MOD_ID is a compile-time String constant, so this does not pull CoastersExtras into
        // the enum's class initialisation.
        this.langSuffix = "send_direction." + name;
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
     * {@code create.} the way {@code CreateLang.translate} does -- so our own namespace is
     * safe to use here and the key must be complete.
     */
    @Override
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * The same key without the namespace, for {@code LangBuilder.translate}, which prepends it.
     *
     * <p>Exists so the goggle tooltip and the dial cannot drift apart into two spellings of the
     * same word -- they now read the same lang entry.
     */
    public String langSuffix() {
        return langSuffix;
    }

    /** 0 auto, +1 forward, -1 reverse: the contract {@code CoasterCartDriveMixin} expects. */
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

    // ---------------------------------------------------------------------------------------
    // Migration off the old shared bar
    // ---------------------------------------------------------------------------------------

    /** Ceiling of the old shared board -- {@code StationBoostBehaviour.MAX_LAUNCH}. */
    private static final int LEGACY_MAX = 200;

    /**
     * Decodes a pre-2.2 {@code StationDirection} value.
     *
     * <p>The old dial stored the RAW handle position on a 0..200 bar and read it through three
     * equal zones, so an existing world holds numbers like 0, 97 or 200 rather than 0, 1 or 2.
     * Reading those as ordinals would turn every station set to "forward" into an out-of-range
     * index, and every one set to "reverse" too.
     *
     * <p>This is deliberately the same arithmetic the old {@code zoneOf} used, including the
     * {@code +1} that keeps the very top of the bar inside the last zone rather than one past
     * it -- copied rather than referenced so the old method can be deleted outright.
     */
    public static SendDirection fromLegacyBar(int raw) {
        int clamped = Math.max(0, Math.min(LEGACY_MAX, raw));
        int zone = Math.min(COUNT - 1, clamped * COUNT / (LEGACY_MAX + 1));
        return VALUES[zone];
    }

    /**
     * A bar position that {@link #fromLegacyBar} maps back to this constant.
     *
     * <p>Written alongside the new key so that downgrading the mod does not silently flip every
     * station: an older build reads {@code StationDirection}, and these three numbers land in
     * the middle of the three zones it expects.
     */
    public int toLegacyBar() {
        return switch (this) {
            case FORWARD -> 100;
            case REVERSE -> LEGACY_MAX;
            default -> 0;
        };
    }
}
