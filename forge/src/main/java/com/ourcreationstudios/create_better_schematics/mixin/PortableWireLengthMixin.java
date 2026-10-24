package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.PowerGridWires;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge Power Grid ignores the NeoForge sag field; retain it for the return trip. */
@Mixin(Entity.class)
abstract class PortableWireLengthMixin {
    @Unique private float betterSchematics$portableWireLength;

    @Inject(method = "load", at = @At("HEAD"))
    private void betterSchematics$readWireLength(CompoundTag data, CallbackInfo ci) {
        betterSchematics$portableWireLength = 0;
        if (PowerGridWires.isWire(BuiltInRegistries.ENTITY_TYPE.getKey(((Entity) (Object) this).getType()).toString())
                && data.contains("PlacedLength", Tag.TAG_ANY_NUMERIC)) {
            float length = data.getFloat("PlacedLength");
            if (Float.isFinite(length) && length > 0)
                betterSchematics$portableWireLength = length;
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void betterSchematics$writeWireLength(CompoundTag data, CallbackInfoReturnable<CompoundTag> cir) {
        if (betterSchematics$portableWireLength > 0 && !data.contains("PlacedLength", Tag.TAG_ANY_NUMERIC))
            data.putFloat("PlacedLength", betterSchematics$portableWireLength);
    }
}
