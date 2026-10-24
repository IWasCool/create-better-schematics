package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.BeltCasingState;
import com.ourcreationstudios.create_better_schematics.BeltCasingState.Material;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeltBlockEntity.class, remap = false)
abstract class BeltBlockEntityMixin {
    @Shadow public CasingType casing;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void createisfixed$loadMaterialFromState(BlockEntityType<?> type, BlockPos pos,
                                                    BlockState state, CallbackInfo ci) {
        createisfixed$restoreEncodedMaterial(state);
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void createisfixed$preserveMaterialOnLoad(CompoundTag tag, HolderLookup.Provider registries,
                                                     boolean clientPacket, CallbackInfo ci) {
        // Editors may synthesize a default Casing=NONE tag; saved state material wins.
        // Legacy states with material=none still use Create's original Casing NBT.
        createisfixed$restoreEncodedMaterial(((BeltBlockEntity) (Object) this).getBlockState());
    }

    @Inject(method = "setCasingType", at = @At("RETURN"), remap = false)
    private void createisfixed$saveAppliedMaterial(CasingType type, CallbackInfo ci) {
        createisfixed$writeMaterialToState(type);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void createisfixed$synchronizeMaterial(CallbackInfo ci) {
        BeltBlockEntity belt = (BeltBlockEntity) (Object) this;
        if (belt.getLevel() == null || belt.getLevel().isClientSide)
            return;
        BlockState state = belt.getBlockState();
        if (!state.hasProperty(BeltCasingState.MATERIAL))
            return;
        Material saved = state.getValue(BeltCasingState.MATERIAL);
        if (!state.getValue(BeltBlock.CASING)) {
            if (casing != CasingType.NONE)
                belt.setCasingType(CasingType.NONE);
            else
                createisfixed$writeMaterialToState(CasingType.NONE);
        } else if (saved != Material.NONE) {
            if (casing != saved.toCreate())
                belt.setCasingType(saved.toCreate());
        } else if (casing != CasingType.NONE) {
            // Migrate old worlds from real NBT, never an assumed material.
            createisfixed$writeMaterialToState(casing);
        }
    }

    @Inject(method = "getModelData", at = @At("RETURN"), cancellable = true, remap = false)
    private void createisfixed$renderSavedMaterial(CallbackInfoReturnable<ModelData> cir) {
        BeltBlockEntity belt = (BeltBlockEntity) (Object) this;
        BlockState state = belt.getBlockState();
        if (state.hasProperty(BeltCasingState.MATERIAL) && state.getValue(BeltBlock.CASING)
                && state.getValue(BeltCasingState.MATERIAL) != Material.NONE) {
            cir.setReturnValue(cir.getReturnValue().derive()
                    .with(BeltModel.CASING_PROPERTY, state.getValue(BeltCasingState.MATERIAL).toCreate())
                    .build());
        }
    }

    @Unique
    private void createisfixed$restoreEncodedMaterial(BlockState state) {
        if (state.hasProperty(BeltCasingState.MATERIAL) && state.getValue(BeltBlock.CASING)
                && state.getValue(BeltCasingState.MATERIAL) != Material.NONE)
            casing = state.getValue(BeltCasingState.MATERIAL).toCreate();
    }

    @Unique
    private void createisfixed$writeMaterialToState(CasingType type) {
        BeltBlockEntity belt = (BeltBlockEntity) (Object) this;
        if (belt.getLevel() == null)
            return;
        BlockState state = belt.getBlockState();
        if (!state.hasProperty(BeltCasingState.MATERIAL))
            return;
        Material material = Material.fromCreate(type);
        if (state.getValue(BeltCasingState.MATERIAL) != material)
            belt.getLevel().setBlock(belt.getBlockPos(), state.setValue(BeltCasingState.MATERIAL, material),
                    Block.UPDATE_CLIENTS);
    }
}
