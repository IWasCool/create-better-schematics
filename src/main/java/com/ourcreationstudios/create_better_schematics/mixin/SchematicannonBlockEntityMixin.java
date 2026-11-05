package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipService;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipSettings;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.createmod.catnip.levelWrappers.SchematicLevel;
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
    @Unique private SchematicLevel betterSchematics$checkedSchematic;
    @Unique private long betterSchematics$checkedRevision = -1;
    @Unique private boolean betterSchematics$fullySkipped;
    @Shadow private boolean blockSkipped;
    @Shadow private int printerCooldown;
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
        var cannon = (SchematicannonBlockEntity) (Object) this;
        if ("finished".equals(cannon.statusMsg) && cannon.inventory.getStackInSlot(0).isEmpty()) {
            // Older saves retained choices after finishing their previous job.
            betterSchematics$skips.clear();
            cannon.skipMissing = false;
        }
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

    @Inject(method = "tickPrinter", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$releaseSkippedMissingBlock(CallbackInfo ci) {
        var cannon = (SchematicannonBlockEntity) (Object) this;
        // A cancelled tick must still end Create's per-tick skipped-block loop.
        blockSkipped = false;
        if (!cannon.printer.isLoaded()) {
            betterSchematics$checkedSchematic = null;
            betterSchematics$fullySkipped = false;
        }
        // Create's ordinary pause guard requires positive fuel. Creative Crates
        // use zero fuel, so respect the explicit Pause command before that guard.
        // Automatic missing-item, fuel and unloaded-target pauses keep retrying.
        if (cannon.state == SchematicannonBlockEntity.State.PAUSED && "paused".equals(cannon.statusMsg)) {
            ci.cancel();
            return;
        }
        var blueprint = cannon.inventory.getStackInSlot(0);
        if (!blueprint.isEmpty() && blueprint.getItem() instanceof SchematicItem
                && cannon.printer.isLoaded() && !cannon.printer.isErrored()
                && (cannon.state == SchematicannonBlockEntity.State.RUNNING
                || cannon.state == SchematicannonBlockEntity.State.PAUSED
                && ("noGunpowder".equals(cannon.statusMsg) || "missingBlock".equals(cannon.statusMsg)
                || "targetNotLoaded".equals(cannon.statusMsg)))) {
            var schematic = ((SchematicPrinterAccessor) cannon.printer).betterSchematics$getBlockReader();
            if (schematic != betterSchematics$checkedSchematic
                    || betterSchematics$checkedRevision != betterSchematics$skips.revision()) {
                betterSchematics$checkedSchematic = schematic;
                betterSchematics$checkedRevision = betterSchematics$skips.revision();
                betterSchematics$fullySkipped = schematic != null
                        && CannonSkipService.skipsAllBlocks(schematic, betterSchematics$skips);
            }
            // No selected block can launch, and entities still use Create's normal path.
            // Complete through Create so its output schematic and flying blocks remain intact.
            if (betterSchematics$fullySkipped) {
                cannon.finishedPrinting();
                ci.cancel();
                return;
            }
        }
        CannonSkipService.resumeSkippedTarget(cannon);
    }

    @Inject(method = "finishedPrinting", at = @At("RETURN"), remap = false)
    private void betterSchematics$resetCompletedChoices(CallbackInfo ci) {
        var cannon = (SchematicannonBlockEntity) (Object) this;
        betterSchematics$skips.clear();
        betterSchematics$checkedSchematic = null;
        betterSchematics$fullySkipped = false;
        cannon.skipMissing = false;
        cannon.positionNotLoaded = false;
        printerCooldown = 0;
        blockSkipped = false;
        cannon.updateChecklist();
        cannon.setChanged();
        cannon.sendUpdate = true;
    }
}
