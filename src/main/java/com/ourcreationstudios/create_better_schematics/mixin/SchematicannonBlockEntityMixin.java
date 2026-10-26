package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipService;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipSettings;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicannonBlockEntity.class, remap = false)
abstract class SchematicannonBlockEntityMixin implements CannonSkipAccess {
    @Unique private final CannonSkipSettings betterSchematics$skips = new CannonSkipSettings();
    @Shadow protected abstract boolean shouldPlace(BlockPos pos, BlockState state, BlockEntity entity,
            BlockState toReplace, BlockState other, boolean normalCube);

    @Override
    public boolean betterSchematics$shouldPlace(BlockPos pos, BlockState state, BlockEntity entity,
            BlockState toReplace, BlockState other, boolean normalCube) {
        return shouldPlace(pos, state, entity, toReplace, other, normalCube);
    }

    @Override
    public CannonSkipSettings betterSchematics$getSkipSettings() {
        return betterSchematics$skips;
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void betterSchematics$readSkips(CompoundTag tag, HolderLookup.Provider registries,
                                           boolean clientPacket, CallbackInfo ci) {
        betterSchematics$skips.read(tag);
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void betterSchematics$writeSkips(CompoundTag tag, HolderLookup.Provider registries,
                                            boolean clientPacket, CallbackInfo ci) {
        betterSchematics$skips.write(tag);
    }

    @Inject(method = "shouldPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$skipSelectedBlocks(BlockPos pos, BlockState state, BlockEntity blockEntity,
            BlockState toReplace, BlockState other, boolean normalCube, CallbackInfoReturnable<Boolean> cir) {
        if (betterSchematics$skips.skips(state))
            cir.setReturnValue(false);
    }

    @Inject(method = "tickPrinter", at = @At("HEAD"), remap = false)
    private void betterSchematics$releaseSkippedMissingBlock(CallbackInfo ci) {
        CannonSkipService.resumeSkippedTarget((SchematicannonBlockEntity) (Object) this);
    }
}
