package com.ourcreationstudios.create_better_schematics.client.jei;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class BetterSchematicsJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateBetterSchematics.MODID, "cannon_materials");
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!JeiMaterials.supportedVersion())
            return;
        // JEI's API-only runtime (used by other recipe viewers) has no bookmark UI.
        if (!runtime.getBookmarkManager().getClass().getName().startsWith("mezz.jei.gui.bookmarks."))
            return;
        JeiMaterials.start(materials -> {
            var ingredients = runtime.getIngredientManager();
            var bookmarks = runtime.getBookmarkManager();
            // Validate every ingredient before replacing any previous imported entries.
            for (var material : materials)
                ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, material.stack(), false).orElseThrow();
            JeiMaterials.quantities().replace(materials, new MaterialBookmarkList.Bookmarks() {
                private ITypedIngredient<ItemStack> typed(ItemStack stack) {
                    return ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, stack.copyWithCount(1), false).orElseThrow();
                }
                @Override public boolean contains(ItemStack stack) { return bookmarks.contains(typed(stack)); }
                @Override public boolean add(ItemStack stack) { return bookmarks.add(typed(stack)); }
                @Override public void remove(ItemStack stack) { bookmarks.remove(typed(stack)); }
            });
            JeiMaterials.save();
            if (!materials.isEmpty() && runtime.getBookmarkOverlay() instanceof JeiBookmarkVisibility visibility)
                visibility.betterSchematics$showMaterialBookmarks();
        });
    }

    @Override public void onRuntimeUnavailable() {
        JeiMaterials.stop();
    }
}
