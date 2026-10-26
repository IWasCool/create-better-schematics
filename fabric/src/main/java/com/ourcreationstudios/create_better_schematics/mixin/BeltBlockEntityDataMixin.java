package com.ourcreationstudios.create_better_schematics.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockHelper.class, remap = false)
abstract class BeltBlockEntityDataMixin {
    @Inject(method = "prepareBlockEntityData", at = @At("RETURN"), cancellable = true, remap = false)
    private static void betterSchematics$saveNativeCasing(BlockState state, BlockEntity blockEntity,
            CallbackInfoReturnable<CompoundTag> cir) {
        if (blockEntity instanceof BeltBlockEntity belt) {
            CompoundTag data = cir.getReturnValue() == null ? new CompoundTag() : cir.getReturnValue().copy();
            data.putString("id", "create:belt");
            data.putString("Casing", belt.casing.name());
            cir.setReturnValue(data);
        }
    }
}
