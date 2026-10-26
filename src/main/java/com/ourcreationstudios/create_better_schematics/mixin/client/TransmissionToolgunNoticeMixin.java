package com.ourcreationstudios.create_better_schematics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Better Schematics supplies schematic support without the optional Toolgun. */
@Pseudo
@Mixin(targets = "com.enxv.aerouniversaljoint.client.ToolgunCompatibilityNotice", remap = false)
abstract class TransmissionToolgunNoticeMixin {
    @Inject(method = "onClientLogin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void betterSchematics$suppressToolgunRecommendation(CallbackInfo ci) {
        ci.cancel();
    }
}
