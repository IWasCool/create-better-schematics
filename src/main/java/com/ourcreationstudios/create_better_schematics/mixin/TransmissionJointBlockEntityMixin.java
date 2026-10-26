package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinkAccess;
import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.enxv.aerouniversaljoint.content.UniversalJointBlockEntity", remap = false)
abstract class TransmissionJointBlockEntityMixin implements TransmissionLinkAccess {
    @Unique private CompoundTag betterSchematics$pendingLink = new CompoundTag();

    @Override public CompoundTag betterSchematics$getPendingLink() { return betterSchematics$pendingLink; }
    @Override public void betterSchematics$setPendingLink(CompoundTag link) { betterSchematics$pendingLink = link.copy(); }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void betterSchematics$readPendingLink(CompoundTag tag, HolderLookup.Provider registries,
                                                 boolean clientPacket, CallbackInfo ci) {
        betterSchematics$pendingLink = tag.getCompound(TransmissionLinks.PENDING).copy();
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void betterSchematics$writePendingLink(CompoundTag tag, HolderLookup.Provider registries,
                                                  boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("LinkedPos"))
            tag.put(TransmissionLinks.ORIGIN, NbtUtils.writeBlockPos(((BlockEntity) (Object) this).getBlockPos()));
        if (!betterSchematics$pendingLink.isEmpty())
            tag.put(TransmissionLinks.PENDING, betterSchematics$pendingLink.copy());
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void betterSchematics$restoreRod(CallbackInfo ci) {
        TransmissionLinks.tick((BlockEntity) (Object) this, this);
    }
}
