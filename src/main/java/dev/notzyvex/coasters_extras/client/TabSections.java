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

public final class TabSections {

    public static final int BANNER_W = 162;
    public static final int BANNER_H = 18;

    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int VISIBLE_ROWS = 5;

    public static int currentRow = 0;

    private static final List<Section> SECTIONS = new ArrayList<>();

    public record Banner(ResourceLocation texture, int frames, int frametime, boolean sprite,
                         float scrollPxPerSecond) {
        public Banner(ResourceLocation texture) { this(texture, 1, 1, false, 0f); }
        public Banner(ResourceLocation texture, int frames, int frametime) {
            this(texture, frames, frametime, false, 0f);
        }

        public static Banner sprite(ResourceLocation id) {
            return new Banner(id, 1, 1, true, 0f);
        }

        public static Banner scroll(ResourceLocation texture, float pxPerSecond) {
            return new Banner(texture, 1, 1, false, pxPerSecond);
        }

        public static Banner autoFrames(ResourceLocation texture, int frametime) {
            return new Banner(texture, 0, frametime, false, 0f);
        }
    }

    private static final java.util.Map<ResourceLocation, int[]> SIZE_CACHE =
            new java.util.HashMap<>();

    public static void invalidateFrameCache() {
        SIZE_CACHE.clear();
    }

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

    private static int framesOf(Banner banner) {
        if (banner.frames() > 0) {
            return banner.frames();
        }
        int[] size = sizeOf(banner.texture());
        return size == null ? 1 : Math.max(1, size[1] / BANNER_H);
    }

    private static int stripWidthOf(Banner banner) {
        int[] size = sizeOf(banner.texture());
        return size == null ? BANNER_W : Math.max(BANNER_W, size[0]);
    }

    public static final int TITLE_GOLD = 0xFFFFD34D;
    public static final int TITLE_PURPLE = 0xFFB96BFF;

    public record Section(Component title, Banner banner, List<ItemStack> items,
                          boolean rainbowTitle, int titleColor) {}

    private record Placed(Component title, Banner banner, int row, boolean rainbow,
                          int titleColor) {}

    private static final List<Placed> PLACED = new ArrayList<>();

    public static void define(Component title, Banner banner, List<ItemStack> items,
                              boolean rainbowTitle) {
        define(title, banner, items, rainbowTitle, TITLE_GOLD);
    }

    public static void define(Component title, Banner banner, List<ItemStack> items,
                              boolean rainbowTitle, int titleColor) {
        SECTIONS.add(new Section(title, banner, items, rainbowTitle, titleColor));
    }

    public static boolean isEmpty() {
        return SECTIONS.isEmpty();
    }

    public static void buildContents(Consumer<ItemStack> display, Consumer<ItemStack> search) {
        PLACED.clear();
        int row = 0;
        for (int s = 0; s < SECTIONS.size(); s++) {
            Section section = SECTIONS.get(s);

            for (int i = 0; i < 9; i++) display.accept(ItemStack.EMPTY);
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

            int pad = count % 9;
            if (pad != 0) {
                for (int i = 0; i < 9 - pad; i++) display.accept(ItemStack.EMPTY);
            }
        }
    }

    private static final java.util.Map<Integer, Long> HOVER_SINCE = new java.util.HashMap<>();

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
                HOVER_SINCE.remove(p.row());
                int texW = stripWidthOf(banner);
                long travelled = (long) ((now / 1000.0) * banner.scrollPxPerSecond());
                int u = (int) Math.floorMod(texW - travelled, (long) texW);

                int head = Math.min(BANNER_W, texW - u);
                graphics.blit(banner.texture(), x, y, u, 0, head, BANNER_H, texW, BANNER_H);
                if (head < BANNER_W) {
                    graphics.blit(banner.texture(), x + head, y, 0, 0,
                            BANNER_W - head, BANNER_H, texW, BANNER_H);
                }
            } else if (banner.sprite()) {
                HOVER_SINCE.remove(p.row());
                graphics.blitSprite(banner.texture(), x, y, BANNER_W, BANNER_H);
            } else {
                int frames = framesOf(banner);
                int frame = 0;
                if (hovered && frames > 1) {
                    long since = HOVER_SINCE.computeIfAbsent(p.row(), k -> now);
                    long ticks = (now - since) / 50L;
                    frame = (int) ((ticks / banner.frametime()) % frames);
                } else {
                    HOVER_SINCE.remove(p.row());
                }

                graphics.blit(banner.texture(), x, y, 0, frame * BANNER_H,
                        BANNER_W, BANNER_H, BANNER_W, frames * BANNER_H);
            }

            Component title = p.title();
            int w = font.width(title);
            graphics.fill(x + 2, y + 2, x + w + 8, y + BANNER_H - 2, 0xA0000000);

            if (p.rainbow()) {
                drawRainbow(graphics, font, title.getString(), x + 5, y + 5);
            } else {
                int colour = p.titleColor();
                int shadow = 0xFF000000 | ((colour >>> 3) & 0x1F1F1F);
                graphics.drawString(font, title, x + 6, y + 6, shadow, false);
                graphics.drawString(font, title, x + 5, y + 5, colour, false);
            }
        }
    }

    private static void drawRainbow(GuiGraphics graphics, Font font, String text, int x, int y) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float hue = text.length() > 1 ? (float) i / (text.length() - 1) : 0f;
            int rgb = java.awt.Color.HSBtoRGB(hue * 0.85f, 0.85f, 1.0f) & 0xFFFFFF;
            graphics.drawString(font, ch, cx + 1, y + 1, 0xFF1A1005, false);
            graphics.drawString(font, ch, cx, y, 0xFF000000 | rgb, false);
            cx += font.width(ch);
        }
    }

    private TabSections() {}
}
