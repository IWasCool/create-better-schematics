package com.ourcreationstudios.create_better_schematics.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeConnections;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactoryPanelSupportBehaviour.class, remap = false)
abstract class FactoryPanelSupportBehaviourMixin extends BlockEntityBehaviour {
    protected FactoryPanelSupportBehaviourMixin(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void betterSchematics$rememberExportPosition(CompoundTag tag, HolderLookup.Provider registries,
                                                        boolean clientPacket, CallbackInfo ci) {
        var pos = getPos();
        if (!clientPacket)
            tag.putIntArray(FactoryGaugeConnections.ORIGIN, new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }
}
