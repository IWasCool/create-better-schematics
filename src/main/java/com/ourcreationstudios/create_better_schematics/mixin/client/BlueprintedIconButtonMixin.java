package com.ourcreationstudios.create_better_schematics.mixin.client;

import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use Create's standard button size and frame for the optional table toolbar. */
@Pseudo
@Mixin(targets = "net.swzo.create_blueprinted.gui.SmallIconButton", remap = false)
abstract class BlueprintedIconButtonMixin extends IconButton {
    protected BlueprintedIconButtonMixin(int x, int y, ScreenElement icon) {
        super(x, y, icon);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void betterSchematics$standardSize(int x, int y, ScreenElement icon, CallbackInfo ci) {
        setWidth(18);
        setHeight(18);
        // Blueprinted's icons are 13px; center them inside Create's 16px icon area.
        this.icon = (graphics, iconX, iconY) -> icon.render(graphics, iconX + 1, iconY + 1);
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$standardFrame(GuiGraphics graphics, int mouseX, int mouseY,
                                                float partialTicks, CallbackInfo ci) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);
        ci.cancel();
    }
}
