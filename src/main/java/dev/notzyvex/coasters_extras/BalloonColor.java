package dev.notzyvex.coasters_extras;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;

/**
 * Every balloon colour this mod adds.
 *
 * <p>Red is deliberately absent: Create: Coasters Simulated already registers
 * {@code simulatedcoasters:red_balloon}. Registering our own would produce a duplicate
 * item <em>and</em> a conflicting recipe (both would be wool + string + iron nugget).
 * Instead we pull their red into our creative tab so the set still looks complete.
 */
public enum BalloonColor {
    WHITE      ("white",       Items.WHITE_WOOL),
    ORANGE     ("orange",      Items.ORANGE_WOOL),
    MAGENTA    ("magenta",     Items.MAGENTA_WOOL),
    LIGHT_BLUE ("light_blue",  Items.LIGHT_BLUE_WOOL),
    YELLOW     ("yellow",      Items.YELLOW_WOOL),
    LIME       ("lime",        Items.LIME_WOOL),
    PINK       ("pink",        Items.PINK_WOOL),
    GRAY       ("gray",        Items.GRAY_WOOL),
    LIGHT_GRAY ("light_gray",  Items.LIGHT_GRAY_WOOL),
    CYAN       ("cyan",        Items.CYAN_WOOL),
    PURPLE     ("purple",      Items.PURPLE_WOOL),
    BLUE       ("blue",        Items.BLUE_WOOL),
    BROWN      ("brown",       Items.BROWN_WOOL),
    GREEN      ("green",       Items.GREEN_WOOL),
    BLACK      ("black",       Items.BLACK_WOOL),
    /** Not a vanilla dye colour -- our signature item, and the creative tab icon. */
    RAINBOW    ("rainbow",     Items.WHITE_WOOL);


    private final String name;
    private final Item wool;

    BalloonColor(String name, Item wool) {
        this.name = name;
        this.wool = wool;
    }

    /** Registry path, e.g. {@code light_blue_balloon}. */
    public String blockName() {
        return name + "_balloon";
    }

    public String colorName() {
        return name;
    }

    /** Crafting ingredient for this colour. Rainbow has no matching wool. */
    public Item wool() {
        return wool;
    }

    public boolean isRainbow() {
        return this == RAINBOW;
    }

    /** English display name, e.g. "Light Blue Balloon". */
    public String displayName() {
        StringBuilder sb = new StringBuilder();
        for (String part : name.split("_")) {
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1))
              .append(' ');
        }
        return sb.append("Balloon").toString();
    }

    /**
     * Tooltip tint for this colour. Rainbow is handled separately as a gradient.
     *
     * <p>The default is not laziness: a copycat balloon's colour is whatever its source block
     * happens to be, and there is no honest single number for "oak planks". Plain white leaves
     * the tooltip legible without claiming a tint the balloon does not have.
     */
    public int rgb() {
        return switch (this) {
            case WHITE      -> 0xF9FFFE; case ORANGE     -> 0xF9801D;
            case MAGENTA    -> 0xC74EBD; case LIGHT_BLUE -> 0x3AB3DA;
            case YELLOW     -> 0xFED83D; case LIME       -> 0x80C71F;
            case PINK       -> 0xF38BAA; case GRAY       -> 0x8B9296;
            case LIGHT_GRAY -> 0x9D9D97; case CYAN       -> 0x169C9C;
            case PURPLE     -> 0x8932B8; case BLUE       -> 0x5A63D8;
            case BROWN      -> 0x835432; case GREEN      -> 0x5E7C16;
            case BLACK      -> 0x555555; case RAINBOW    -> 0xFFFFFF;
            default         -> 0xFFFFFF;
        };
    }

    /** The matching vanilla dye colour, or null for rainbow and for copycat balloons. */
    public DyeColor dyeColor() {
        if (isRainbow()) return null;
        return DyeColor.byName(name, null);
    }
}
