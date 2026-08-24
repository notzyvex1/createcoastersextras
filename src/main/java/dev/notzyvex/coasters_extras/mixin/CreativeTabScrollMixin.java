package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.client.TabSections;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativeTabScrollMixin {

    @Shadow protected abstract int getRowIndexForScroll(float scroll);

    @Inject(method = "scrollTo", at = @At("HEAD"))
    private void coasters_extras$trackRow(float scroll, CallbackInfo ci) {
        TabSections.currentRow = this.getRowIndexForScroll(scroll);
    }
}
