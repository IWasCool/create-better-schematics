package com.ourcreationstudios.create_better_schematics.mixin;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArmBlockEntity.class, remap = false)
abstract class ArmBlockEntityMixin {
    @Shadow List<ArmInteractionPoint> inputs;
    @Shadow List<ArmInteractionPoint> outputs;
    @Shadow ListTag interactionPointTag;
    @Shadow boolean updateInteractionPoints;
    @Shadow ArmBlockEntity.Phase phase;
    @Unique private int betterSchematics$retryDelay;
    @Unique private boolean betterSchematics$initialized;

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void betterSchematics$initializeImportedPoints(CompoundTag tag, HolderLookup.Provider registries,
                                                            boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket) {
            updateInteractionPoints = true;
            betterSchematics$retryDelay = 0;
            betterSchematics$initialized = false;
        }
    }

    @Inject(method = "initInteractionPoints", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$waitBeforeRetry(CallbackInfo ci) {
        if (betterSchematics$retryDelay > 0) {
            betterSchematics$retryDelay--;
            ci.cancel();
        } else if (betterSchematics$initialized && updateInteractionPoints && (phase == ArmBlockEntity.Phase.MOVE_TO_INPUT
                || phase == ArmBlockEntity.Phase.MOVE_TO_OUTPUT)) {
            // Rebuilding the lists mid-movement could change the meaning of the target index.
            ci.cancel();
        }
    }

    @Redirect(method = "initInteractionPoints", at = @At(value = "FIELD", target =
            "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;updateInteractionPoints:Z",
            opcode = Opcodes.PUTFIELD), remap = false)
    private void betterSchematics$retainUnresolvedPoints(ArmBlockEntity arm, boolean value) {
        // Preserve these before Create sends its client update, so client-side exports keep them too.
        updateInteractionPoints = value || (interactionPointTag != null
                && inputs.size() + outputs.size() < interactionPointTag.size());
        betterSchematics$initialized = true;
        betterSchematics$retryDelay = updateInteractionPoints ? 20 : 0;
        if (updateInteractionPoints && interactionPointTag != null) {
            // Keep initialized targets first so adding a missing target cannot shift an in-flight index on reload.
            ListTag ordered = new ListTag();
            Set<String> initialized = new HashSet<>();
            for (List<ArmInteractionPoint> points : List.of(inputs, outputs)) {
                for (ArmInteractionPoint point : points) {
                    CompoundTag saved = point.serialize(arm.getBlockPos());
                    ordered.add(saved);
                    initialized.add(betterSchematics$pointKey(saved));
                }
            }
            for (var entry : interactionPointTag) {
                CompoundTag saved = (CompoundTag) entry;
                if (!initialized.contains(betterSchematics$pointKey(saved)))
                    ordered.add(saved.copy());
            }
            interactionPointTag = ordered;
        }
    }

    @Unique
    private static String betterSchematics$pointKey(CompoundTag point) {
        return point.getString("Type") + "/" + NBTHelper.readEnum(point, "Mode", ArmInteractionPoint.Mode.class)
                + "/" + NBTHelper.readBlockPos(point, "Pos").asLong();
    }
}
