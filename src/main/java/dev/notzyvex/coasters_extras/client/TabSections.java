package dev.notzyvex.coasters_extras.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Section headers inside our creative tab.
 *
 * <p>Adapted from Create Simulated's creative tab sections (MIT). Theirs is data-driven --
 * sections come from JSON, colours from Veil, banners animate on hover, and items are bound
 * to sections through Registrate. None of that is needed for one tab with three groups, so
 * this keeps only the mechanism: reserve a grid row per section, then draw a banner on it.
 *
 * <p>How the row reservation works, which is the non-obvious part: the creative grid is a
 * flat list of stacks, nine per row. Emitting nine {@link ItemStack#EMPTY} leaves a blank
 * row, and the banner is drawn over it. After each section the list is padded to the end of
 * its row and then given nine more blanks for the next header.
 *
 * <p>Vanilla's {@code Output.accept} rejects empty stacks, so the contents have to be built
 * by mixin rather than through the normal builder.
 */
public final class TabSections {

    /** Matches the banner sprite: one grid row, full width of the item area. */
    public static final int BANNER_W = 162;
    public static final int BANNER_H = 18;

    /** Grid rows are 18px; the item area starts at this offset inside the screen. */
    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int VISIBLE_ROWS = 5;

    /** Top row currently scrolled to. Updated from the scroll mixin. */
    public static int currentRow = 0;

    private static final List<Section> SECTIONS = new ArrayList<>();

    /**
     * A banner: a vertical strip of {@link #frames} rows, each {@link #BANNER_H} tall.
     *
     * <p>Two ways to animate one, and the choice is per banner because they want opposite things.
     *
     * <p><b>Manual (default).</b> The frame count and speed live here and the row is picked in
     * {@code draw}. An mcmeta animation is driven by the texture manager and runs whether anyone
     * is looking or not, so hover-only animation -- the casing banners dissolving only while the
     * cursor is on them -- has to be done this way.
     *
     * <p><b>Sprite ({@code sprite = true}).</b> The strip lives under
     * {@code textures/gui/sprites/} beside a {@code .png.mcmeta} and vanilla animates it. The
     * frame count then comes from the IMAGE, not from a number repeated here -- which is the
     * point: the two used to be able to disagree, and every time the sheet was re-authored with
     * a different frame count this number had to be remembered separately or the animation
     * broke. The cost is that it always runs, so this is for banners that should never sit
     * still, like the balloons' day-night sky.
     *
     * <p>For a sprite banner {@code texture} is a SPRITE ID, not a file path -- vanilla resolves
     * {@code coasters_extras:section/banner_balloons} to
     * {@code textures/gui/sprites/section/banner_balloons.png}. {@code frames} and
     * {@code frametime} are ignored on that path.
     */
    public record Banner(ResourceLocation texture, int frames, int frametime, boolean sprite,
                         float scrollPxPerSecond) {
        public Banner(ResourceLocation texture) { this(texture, 1, 1, false, 0f); }
        public Banner(ResourceLocation texture, int frames, int frametime) {
            this(texture, frames, frametime, false, 0f);
        }

        /** A vanilla-animated banner. {@code id} is a sprite id, not a texture path. */
        public static Banner sprite(ResourceLocation id) {
            return new Banner(id, 1, 1, true, 0f);
        }

        /**
         * A banner whose texture is one long horizontal strip, scrolled sideways forever.
         *
         * <p>The third animation style, and the only one that is not a stack of frames. Frames
         * cost a texture row each, so a strip long enough to hold all ~287 track materials
         * would need one frame per pixel of travel -- a sheet over a hundred thousand pixels
         * tall, far past what any GPU will upload. Scrolling the u coordinate instead makes the
         * length free: the texture stays 18px tall and the animation is two blits, one for the
         * part before the wrap and one for the part after.
         *
         * <p>{@code pxPerSecond} is texture pixels, not screen pixels, so the speed does not
         * change with GUI scale.
         */
        public static Banner scroll(ResourceLocation texture, float pxPerSecond) {
            return new Banner(texture, 1, 1, false, pxPerSecond);
        }

        /**
         * A hover-animated banner whose frame count is read from the image.
         *
         * <p>Same behaviour as the manual path, minus the one number that could go stale. The
         * count passed to the constructor has to match {@code imageHeight / 18} exactly -- a
         * wrong frametime only changes speed, but a wrong frame count corrupts the UV window
         * and the banner renders as garbage. Since the image already knows, ask it.
         */
        public static Banner autoFrames(ResourceLocation texture, int frametime) {
            return new Banner(texture, 0, frametime, false, 0f);
        }
    }

    /**
     * Frame counts read out of the banner PNGs, so no Java constant has to track the art.
     *
     * <p>Cleared on resource reload, because a resource pack may swap a banner for one with a
     * different number of frames.
     */
    private static final java.util.Map<ResourceLocation, int[]> SIZE_CACHE =
            new java.util.HashMap<>();

    public static void invalidateFrameCache() {
        SIZE_CACHE.clear();
    }

    /**
     * A banner PNG's width and height, read straight out of its IHDR.
     *
     * <p>The header is fixed-layout -- width at bytes 16..19, height at 20..23 -- so this is a
     * 24-byte read with no image decode and no native allocation. Returns {@code null} if the
     * texture cannot be read, and every caller has a static fallback for that, so a missing or
     * corrupt banner renders plainly instead of throwing inside the creative screen.
     */
    private static int[] sizeOf(ResourceLocation tex) {
        return SIZE_CACHE.computeIfAbsent(tex, id -> {
            try (java.io.InputStream in = Minecraft.getInstance().getResourceManager()
                    .getResourceOrThrow(id).open()) {
                byte[] hdr = in.readNBytes(24);
                if (hdr.length < 24) {
                    return null;
                }
                int w = ((hdr[16] & 0xFF) << 24) | ((hdr[17] & 0xFF) << 16)
                        | ((hdr[18] & 0xFF) << 8) | (hdr[19] & 0xFF);
                int h = ((hdr[20] & 0xFF) << 24) | ((hdr[21] & 0xFF) << 16)
                        | ((hdr[22] & 0xFF) << 8) | (hdr[23] & 0xFF);
                return new int[]{ w, h };
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * How many {@link #BANNER_H}-tall frames a banner strip holds.
     *
     * <p>Reads the PNG's IHDR rather than decoding the image: the header is fixed-layout and
     * the height is bytes 20..23, so this is a 24-byte read and no native image allocation.
     * Falls back to a single frame if the texture cannot be read, which renders the top row
     * statically rather than throwing inside the creative screen.
     */
    private static int framesOf(Banner banner) {
        if (banner.frames() > 0) {
            return banner.frames();
        }
        int[] size = sizeOf(banner.texture());
        return size == null ? 1 : Math.max(1, size[1] / BANNER_H);
    }

    /** How long a scrolling banner's strip is. Falls back to the visible width. */
    private static int stripWidthOf(Banner banner) {
        int[] size = sizeOf(banner.texture());
        return size == null ? BANNER_W : Math.max(BANNER_W, size[0]);
    }

    /** The default label colour -- Create's warm gold, which is what every section used. */
    public static final int TITLE_GOLD = 0xFFFFD34D;
    /** DEADLINE purple, for the section that should read as ours rather than as Create's. */
    public static final int TITLE_PURPLE = 0xFFB96BFF;

    public record Section(Component title, Banner banner, List<ItemStack> items,
                          boolean rainbowTitle, int titleColor) {}

    /** A section and the grid row its banner sits on. */
    private record Placed(Component title, Banner banner, int row, boolean rainbow,
                          int titleColor) {}

    private static final List<Placed> PLACED = new ArrayList<>();

    public static void define(Component title, Banner banner, List<ItemStack> items,
                              boolean rainbowTitle) {
        define(title, banner, items, rainbowTitle, TITLE_GOLD);
    }

    /** Same, with the label drawn in a specific colour instead of the default gold. */
    public static void define(Component title, Banner banner, List<ItemStack> items,
                              boolean rainbowTitle, int titleColor) {
        SECTIONS.add(new Section(title, banner, items, rainbowTitle, titleColor));
    }

    public static boolean isEmpty() {
        return SECTIONS.isEmpty();
    }

    /**
     * Emits the tab's contents with a blank row reserved above each section, recording which
     * row each banner landed on.
     */
    public static void buildContents(Consumer<ItemStack> display, Consumer<ItemStack> search) {
        PLACED.clear();
        int row = 0;
        for (int s = 0; s < SECTIONS.size(); s++) {
            Section section = SECTIONS.get(s);

            for (int i = 0; i < 9; i++) display.accept(ItemStack.EMPTY);   // the banner's row
            PLACED.add(new Placed(section.title(), section.banner(), row,
                    section.rainbowTitle(), section.titleColor()));
            row++;

            int count = 0;
            for (ItemStack stack : section.items()) {
                display.accept(stack);
                search.accept(stack);
                count++;
            }
            row += (int) Math.ceil(count / 9.0);

            // finish the part-filled row so the next banner starts on a clean one
            int pad = count % 9;
            if (pad != 0) {
                for (int i = 0; i < 9 - pad; i++) display.accept(ItemStack.EMPTY);
            }
        }
    }

    /**
     * When each banner's hover animation started, keyed by grid row. Absent means "not hovered".
     *
     * <p>Kept so the animation always begins at frame 0 under the cursor rather than joining a
     * loop already in progress, which is what makes it read as a response to the hover instead
     * of something that happened to be running anyway.
     */
    private static final java.util.Map<Integer, Long> HOVER_SINCE = new java.util.HashMap<>();

    /** Draws every banner currently scrolled into view. Called from the screen mixin. */
    public static void render(GuiGraphics graphics, int leftPos, int topPos,
                              int mouseX, int mouseY) {
        if (PLACED.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        long now = net.minecraft.Util.getMillis();

        for (Placed p : PLACED) {
            int visibleRow = p.row() - currentRow;
            if (visibleRow < 0 || visibleRow >= VISIBLE_ROWS) continue;

            int x = leftPos + GRID_X;
            int y = topPos + GRID_Y + visibleRow * BANNER_H;

            Banner banner = p.banner();
            boolean hovered = mouseX >= x && mouseX < x + BANNER_W
                    && mouseY >= y && mouseY < y + BANNER_H;

            if (banner.scrollPxPerSecond() > 0f) {
                // A window walking along one long strip. The content should travel to the
                // right, so the window walks LEFT along the texture -- hence texW minus the
                // distance rather than plus it.
                HOVER_SINCE.remove(p.row());
                int texW = stripWidthOf(banner);
                long travelled = (long) ((now / 1000.0) * banner.scrollPxPerSecond());
                int u = (int) Math.floorMod(texW - travelled, (long) texW);

                // Two blits, because the window straddles the wrap for most of the loop.
                // Drawing one and letting the sampler wrap is not an option: the GUI texture
                // is clamped, so the tail would smear the last column across the seam.
                int head = Math.min(BANNER_W, texW - u);
                graphics.blit(banner.texture(), x, y, u, 0, head, BANNER_H, texW, BANNER_H);
                if (head < BANNER_W) {
                    graphics.blit(banner.texture(), x + head, y, 0, 0,
                            BANNER_W - head, BANNER_H, texW, BANNER_H);
                }
            } else if (banner.sprite()) {
                // Vanilla drives this one from its .png.mcmeta, so there is no frame to pick
                // and no frame COUNT to keep in step with the image -- that is the whole reason
                // this path exists. It animates continuously rather than on hover.
                HOVER_SINCE.remove(p.row());
                graphics.blitSprite(banner.texture(), x, y, BANNER_W, BANNER_H);
            } else {
                int frames = framesOf(banner);
                int frame = 0;
                if (hovered && frames > 1) {
                    long since = HOVER_SINCE.computeIfAbsent(p.row(), k -> now);
                    // Ticks, not frames: at 60fps a frametime of 1 would blur, and the sheets
                    // are authored against Minecraft's own 50ms tick.
                    long ticks = (now - since) / 50L;
                    frame = (int) ((ticks / banner.frametime()) % frames);
                } else {
                    HOVER_SINCE.remove(p.row());
                }

                // Plain texture rather than a GUI sprite: the frame row is chosen here, so what
                // is wanted is a v-offset into the strip, which the sprite path does not expose.
                graphics.blit(banner.texture(), x, y, 0, frame * BANNER_H,
                        BANNER_W, BANNER_H, BANNER_W, frames * BANNER_H);
            }

            Component title = p.title();
            int w = font.width(title);
            graphics.fill(x + 2, y + 2, x + w + 8, y + BANNER_H - 2, 0xA0000000);

            if (p.rainbow()) {
                drawRainbow(graphics, font, title.getString(), x + 5, y + 5);
            } else {
                // drawn twice, offset, so the label stays readable over a busy banner
                // Shadow derived from the label rather than hardcoded, so a section
                // can pick any colour and still get a matching drop shadow. The
                // eighth-brightness step reproduces the old gold pair exactly.
                int colour = p.titleColor();
                int shadow = 0xFF000000 | ((colour >>> 3) & 0x1F1F1F);
                graphics.drawString(font, title, x + 6, y + 6, shadow, false);
                graphics.drawString(font, title, x + 5, y + 5, colour, false);
            }
        }
    }

    /**
     * Per-character hue sweep, so the word itself is a rainbow.
     *
     * <p>Drawn a character at a time because a Component carries one colour per style run --
     * a gradient needs a separate colour per glyph. Advance comes from the font rather than a
     * fixed width so it stays correct with a resource pack.
     */
    private static void drawRainbow(GuiGraphics graphics, Font font, String text, int x, int y) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float hue = text.length() > 1 ? (float) i / (text.length() - 1) : 0f;
            int rgb = java.awt.Color.HSBtoRGB(hue * 0.85f, 0.85f, 1.0f) & 0xFFFFFF;
            graphics.drawString(font, ch, cx + 1, y + 1, 0xFF1A1005, false);   // shadow
            graphics.drawString(font, ch, cx, y, 0xFF000000 | rgb, false);
            cx += font.width(ch);
        }
    }

    private TabSections() {}
}
