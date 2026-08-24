package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.ModCreativeTabs;
import dev.notzyvex.coasters_extras.client.TabSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws our section banners over the creative grid.
 *
 * <p>Extends {@link AbstractContainerScreen} so {@code leftPos} and {@code topPos} are
 * reachable -- they are protected on the superclass, and the banner has to be positioned
 * relative to the panel rather than the window.
 *
 * <p>Injected at TAIL so the banners land above the slots but below tooltips.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeTabBannerMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    private CreativeTabBannerMixin() { super(null, null, null); }   // never called

    @Shadow private static CreativeModeTab selectedTab;

    @Inject(method = "render", at = @At("TAIL"))
    private void coasters_extras$banners(GuiGraphics graphics, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        if (selectedTab == ModCreativeTabs.MAIN.get()) {
            TabSections.render(graphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
    }
}
