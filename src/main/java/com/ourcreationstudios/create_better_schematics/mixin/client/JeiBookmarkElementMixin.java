package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.text.NumberFormat;
import java.util.Locale;
import com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient;
import com.ourcreationstudios.create_better_schematics.client.jei.JeiMaterials;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.elements.IngredientBookmarkElement", remap = false)
abstract class JeiBookmarkElementMixin {
    @Shadow public abstract ITypedIngredient<?> getTypedIngredient();

    @Inject(method = "createRenderOverlay", at = @At("RETURN"), cancellable = true, remap = false)
    private void betterSchematics$drawRequiredAmount(CallbackInfoReturnable<IDrawable> cir) {
        ItemStack stack = getTypedIngredient().getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        if (stack.isEmpty())
            return;
        IDrawable original = cir.getReturnValue();
        // Resolve quantities when drawing, including bookmarks that already existed
        // before the first import or were cached by JEI before an amount changed.
        cir.setReturnValue(new IDrawable() {
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override public void draw(GuiGraphics graphics, int x, int y) {
                if (original != null)
                    original.draw(graphics, x, y);
                int count = JeiMaterials.count(stack);
                if (count <= 0)
                    return;
                String label = count < 1000 ? Integer.toString(count) : count < 1_000_000
                        ? String.format(Locale.ROOT, "%.1fk", count / 1000.0)
                        : String.format(Locale.ROOT, "%.1fm", count / 1_000_000.0);
                var font = Minecraft.getInstance().font;
                float scale = Math.min(1f, 17f / font.width(label));
                graphics.pose().pushPose();
                graphics.pose().translate(x + 17, y + 17, 200);
                graphics.pose().scale(scale, scale, 1);
                graphics.drawString(font, label, -font.width(label), -font.lineHeight, 0xffffff, true);
                graphics.pose().popPose();
            }
        });
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), remap = false)
    private void betterSchematics$explainRequiredAmount(@Coerce ITooltipBuilder tooltip, @Coerce Object tooltipHelper,
            IIngredientRenderer<?> renderer, IIngredientHelper<?> helper, CallbackInfo ci) {
        ItemStack stack = getTypedIngredient().getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        int count = stack.isEmpty() ? 0 : JeiMaterials.count(stack);
        if (count > 0)
            tooltip.add(CannonMaterialsClient.text("quantity", NumberFormat.getIntegerInstance().format(count))
                    .copy().withStyle(ChatFormatting.GREEN));
    }
}
