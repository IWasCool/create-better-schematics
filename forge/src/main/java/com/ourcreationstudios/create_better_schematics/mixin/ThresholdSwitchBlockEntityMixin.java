package com.ourcreationstudios.create_better_schematics.mixin;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ThresholdSwitchBlockEntity.class, remap = false)
abstract class ThresholdSwitchBlockEntityMixin {
    @Shadow public int onWhenAbove;
    @Shadow public int offWhenBelow;
    @Shadow public int currentMinLevel;
    @Shadow public int currentMaxLevel;
    @Shadow public boolean inStacks;

    @Unique private boolean betterSchematics$pendingPercentages;
    @Unique private float betterSchematics$legacyOn;
    @Unique private float betterSchematics$legacyOff;
    @Unique private String betterSchematics$thresholdUnit = "";

    @Inject(method = "read", at = @At("HEAD"), remap = false)
    private void betterSchematics$readLegacyThresholds(CompoundTag tag,
                                                       boolean clientPacket, CallbackInfo ci) {
        betterSchematics$thresholdUnit = tag.getString("BetterSchematicsThresholdUnit");
        // Native amounts take precedence in files carrying both formats.
        betterSchematics$pendingPercentages = !tag.contains("OnAboveAmount", Tag.TAG_ANY_NUMERIC)
                && !tag.contains("OffBelowAmount", Tag.TAG_ANY_NUMERIC)
                && tag.contains("OnAbove", Tag.TAG_ANY_NUMERIC) && tag.contains("OffBelow", Tag.TAG_ANY_NUMERIC)
                && Float.isFinite(tag.getFloat("OnAbove")) && Float.isFinite(tag.getFloat("OffBelow"));
        if (betterSchematics$pendingPercentages) {
            betterSchematics$legacyOn = Mth.clamp(tag.getFloat("OnAbove"), 0, 1);
            betterSchematics$legacyOff = Mth.clamp(tag.getFloat("OffBelow"), 0, 1);
        }
    }

    @Inject(method = "updateCurrentLevel", at = @At("RETURN"), remap = false)
    private void betterSchematics$resolveLegacyThresholds(CallbackInfo ci) {
        ThresholdSwitchBlockEntity threshold = (ThresholdSwitchBlockEntity) (Object) this;
        // Observe the target while Create is already updating it. Saving must never load a chunk.
        String previousUnit = betterSchematics$thresholdUnit;
        betterSchematics$thresholdUnit = switch (threshold.getTypeOfCurrentTarget()) {
            case FLUID -> "millibuckets";
            case ITEM -> "items";
            case CUSTOM -> "custom";
            case UNSUPPORTED -> betterSchematics$thresholdUnit;
        };
        if (!betterSchematics$thresholdUnit.equals(previousUnit) && !threshold.getLevel().isClientSide) {
            threshold.setChanged();
            threshold.sendData();
        }
        if (!betterSchematics$pendingPercentages || currentMaxLevel <= currentMinLevel)
            return;
        long range = (long) currentMaxLevel - currentMinLevel;
        onWhenAbove = (int) (currentMinLevel + Math.round((double) betterSchematics$legacyOn * range));
        offWhenBelow = (int) (currentMinLevel + Math.round((double) betterSchematics$legacyOff * range));
        betterSchematics$pendingPercentages = false;
        // Evaluate the output again with the migrated limits, once the real capacity is known.
        threshold.updateCurrentLevel();
        threshold.setChanged();
        threshold.sendData();
    }

    @Inject(method = "writeCommon", at = @At("RETURN"), remap = false)
    private void betterSchematics$writePortableThresholds(CompoundTag tag, CallbackInfo ci) {
        if (betterSchematics$pendingPercentages) {
            // Virtual schematic levels and unfinished builds may not have an inventory yet.
            tag.remove("OnAboveAmount");
            tag.remove("OffBelowAmount");
            tag.putFloat("OnAbove", betterSchematics$legacyOn);
            tag.putFloat("OffBelow", betterSchematics$legacyOff);
        } else {
            // Avoid rounding large integer settings through Create's float serializer.
            tag.putInt("OnAboveAmount", onWhenAbove);
            tag.putInt("OffBelowAmount", offWhenBelow);
            if (currentMaxLevel > currentMinLevel) {
                double range = (double) currentMaxLevel - currentMinLevel;
                tag.putFloat("OnAbove", (float) Mth.clamp((onWhenAbove - (double) currentMinLevel) / range, 0, 1));
                tag.putFloat("OffBelow", (float) Mth.clamp((offWhenBelow - (double) currentMinLevel) / range, 0, 1));
            }
        }
        tag.putBoolean("InStacks", inStacks);
        if (!betterSchematics$thresholdUnit.isEmpty())
            tag.putString("BetterSchematicsThresholdUnit", betterSchematics$thresholdUnit);
    }
}
