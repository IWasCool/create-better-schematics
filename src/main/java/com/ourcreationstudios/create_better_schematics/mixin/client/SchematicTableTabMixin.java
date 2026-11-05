package com.ourcreationstudios.create_better_schematics.mixin.client;

import com.ourcreationstudios.create_better_schematics.client.SchematicTableTab;
import com.ourcreationstudios.create_better_schematics.client.SchematicGuiResources;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IconButton.class, remap = false)
abstract class SchematicTableTabMixin extends AbstractSimiWidget implements SchematicTableTab {
    @Unique private boolean betterSchematics$attachedTab;
    @Shadow protected ScreenElement icon;
    @Shadow public boolean green;

    protected SchematicTableTabMixin(int x, int y) {
        super(x, y);
    }

    @Override
    public void betterSchematics$attachTab() {
        betterSchematics$attachedTab = true;
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$drawAttachedTab(GuiGraphics graphics, int mouseX, int mouseY,
                                                 float partialTicks, CallbackInfo ci) {
        if (!betterSchematics$attachedTab)
            return;
        // Widgets keep their input, focus and tooltips, but the table draws their
        // artwork before its own background so the header covers the tab bases.
        ci.cancel();
    }

    @Override
    public void betterSchematics$renderBehind(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!betterSchematics$attachedTab || !visible)
            return;
        RenderSystem.setShaderColor(1, 1, 1, 1);
        isHovered = isMouseOver(mouseX, mouseY);
        AllGuiTextures texture = !active ? AllGuiTextures.BUTTON_DISABLED
                : isHovered && AllKeys.isMouseButtonDown(0) ? AllGuiTextures.BUTTON_DOWN
                : isHoveredOrFocused() ? AllGuiTextures.BUTTON_HOVER
                : green ? AllGuiTextures.BUTTON_GREEN : AllGuiTextures.BUTTON;
        int x = getX();
        int y = getY();
        // Normal Create art is resolved from the installed dependency. Optional
        // pack styling belongs to Core or an explicit resource override.
        // One-pixel corner rounding keeps the cap subtle; the open bottom joins the header.
        for (int row = 0; row < getHeight(); row++) {
            int inset = row == 0 ? 1 : 0;
            int sourceY = texture.getStartY() + Math.min(row, 14);
            int width = getWidth() - inset * 2;
            graphics.blit(SchematicGuiResources.WIDGETS, x + inset, y + row, texture.getStartX(), sourceY, 1, 1);
            graphics.blit(SchematicGuiResources.WIDGETS, x + inset + 1, y + row, texture.getStartX() + 1, sourceY, width - 2, 1);
            graphics.blit(SchematicGuiResources.WIDGETS, x + getWidth() - inset - 1, y + row,
                    texture.getStartX() + 17, sourceY, 1, 1);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, y + 1, 0);
        float iconScale = Math.min(getWidth() - 2, getHeight() - 4) / 16f;
        graphics.pose().scale(iconScale, iconScale, 1);
        icon.render(graphics, 0, 0);
        graphics.pose().popPose();
    }
}
