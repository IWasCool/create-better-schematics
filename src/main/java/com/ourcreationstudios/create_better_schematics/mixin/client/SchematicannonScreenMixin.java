package com.ourcreationstudios.create_better_schematics.mixin.client;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.client.CannonSkipScreen;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicannonScreen.class, remap = false)
abstract class SchematicannonScreenMixin {
    @Shadow protected IconButton skipMissingButton;
    @Shadow protected abstract boolean placementSettingsHidden();

    @Inject(method = "initPlacementSettings", at = @At("RETURN"), remap = false)
    private void betterSchematics$openSkipPicker(CallbackInfo ci) {
        if (!placementSettingsHidden() && skipMissingButton != null)
            skipMissingButton.withCallback(() -> CannonSkipScreen.open((SchematicannonScreen) (Object) this));
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$describeSkipPicker(CallbackInfo ci) {
        if (placementSettingsHidden() || skipMissingButton == null)
            return;
        var cannon = ((SchematicannonScreen) (Object) this).getMenu().contentHolder;
        int selected = ((CannonSkipAccess) cannon).betterSchematics$getSkipSettings().skipped().size();
        skipMissingButton.green = selected > 0 || cannon.skipMissing;
        skipMissingButton.setToolTip(Component.translatable(CreateBetterSchematics.MODID + ".cannon.skip.title"));
        skipMissingButton.getToolTip().add(Component.translatable(CreateBetterSchematics.MODID + ".cannon.skip.button_help", selected));
    }
}
