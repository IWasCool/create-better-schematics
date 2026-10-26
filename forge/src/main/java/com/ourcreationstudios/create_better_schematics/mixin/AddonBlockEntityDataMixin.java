package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.SchematicConfigurationRegistry;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockHelper.class, remap = false)
abstract class AddonBlockEntityDataMixin {
    @Inject(method = "prepareBlockEntityData", at = @At("RETURN"), cancellable = true, remap = false)
    private static void betterSchematics$copyAddonConfiguration(BlockState state, BlockEntity blockEntity,
            CallbackInfoReturnable<CompoundTag> cir) {
        if (blockEntity instanceof BeltBlockEntity belt) {
            CompoundTag data = cir.getReturnValue() == null ? new CompoundTag() : cir.getReturnValue().copy();
            data.putString("id", "create:belt");
            data.putString("Casing", belt.casing.name());
            cir.setReturnValue(data);
        }
        if (blockEntity != null && SchematicConfigurationRegistry.supports(blockEntity))
            cir.setReturnValue(SchematicConfigurationRegistry.merge(blockEntity, cir.getReturnValue(), blockEntity.saveWithoutMetadata()));
    }
}
