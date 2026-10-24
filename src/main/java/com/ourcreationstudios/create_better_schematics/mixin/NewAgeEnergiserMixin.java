package com.ourcreationstudios.create_better_schematics.mixin;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * New Age 1.2.0 saves capacitorMode but reads capacitorModer. Keep both old and new files readable.
 */
@Pseudo
@Mixin(targets = "org.antarcticgardens.cna.content.energising.EnergiserBehaviour", remap = false)
abstract class NewAgeEnergiserMixin {
    @ModifyVariable(method = "read", at = @At("HEAD"), argsOnly = true, remap = false)
    private CompoundTag betterSchematics$readSavedCapacitorMode(CompoundTag data) {
        if (!data.contains("capacitorMode"))
            return data;
        CompoundTag compatible = data.copy();
        compatible.putBoolean("capacitorModer", data.getBoolean("capacitorMode"));
        return compatible;
    }
}
