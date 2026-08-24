package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.ModCreativeTabs;
import dev.notzyvex.coasters_extras.client.TabSections;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Builds our creative tab's contents with a blank row reserved above each section.
 *
 * <p>The section banners are drawn over empty grid rows, which means the contents list has to
 * contain {@link ItemStack#EMPTY} entries. Vanilla's {@code Output.accept} rejects empty
 * stacks, so the list cannot be produced through the normal builder and is written directly
 * here instead.
 *
 * <p>Only our own tab is touched; every other tab falls through to vanilla untouched.
 */
@Mixin(CreativeModeTab.class)
public class CreativeTabContentsMixin {

    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("RETURN"))
    private void coasters_extras$sectionedContents(CreativeModeTab.ItemDisplayParameters params,
                                                   CallbackInfo ci) {
        Object self = this;
        if (TabSections.isEmpty() || self != ModCreativeTabs.MAIN.get()) {
            return;
        }
        List<ItemStack> display = new LinkedList<>();
        Set<ItemStack> search = new LinkedHashSet<>();
        TabSections.buildContents(display::add, search::add);
        this.displayItems = display;
        this.displayItemsSearchTab = search;
    }
}
