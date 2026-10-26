package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.SchematicConfigurationRegistry;
import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinks;
import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinkAccess;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockHelper.class, remap = false)
abstract class AddonBlockEntityDataMixin {
    @Inject(method = "prepareBlockEntityData", at = @At("RETURN"), cancellable = true, remap = false)
    private static void betterSchematics$copyAddonConfiguration(Level level, BlockState state, BlockEntity blockEntity,
            CallbackInfoReturnable<CompoundTag> cir) {
        if (blockEntity instanceof BeltBlockEntity belt) {
            CompoundTag data = cir.getReturnValue() == null ? new CompoundTag() : cir.getReturnValue().copy();
            data.putString("id", "create:belt");
            data.putString("Casing", belt.casing.name());
            cir.setReturnValue(data);
        }
        if (blockEntity != null && SchematicConfigurationRegistry.supports(blockEntity))
            cir.setReturnValue(SchematicConfigurationRegistry.merge(blockEntity, cir.getReturnValue(),
                    blockEntity.saveWithoutMetadata(level.registryAccess())));
        cir.setReturnValue(TransmissionLinks.prepare(blockEntity, cir.getReturnValue()));
    }

    @Inject(method = "placeSchematicBlock", at = @At("RETURN"), remap = false)
    private static void betterSchematics$connectPlacedJoint(Level world, BlockState state, BlockPos target,
            ItemStack stack, CompoundTag data, CallbackInfo ci) {
        if (data != null && data.contains(TransmissionLinks.PENDING)
                && world.getBlockEntity(target) instanceof TransmissionLinkAccess access)
            TransmissionLinks.tick(world.getBlockEntity(target), access);
    }
}
