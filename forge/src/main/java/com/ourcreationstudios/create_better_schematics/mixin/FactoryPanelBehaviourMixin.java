package com.ourcreationstudios.create_better_schematics.mixin;

import java.util.List;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeConnections;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactoryPanelBehaviour.class, remap = false)
abstract class FactoryPanelBehaviourMixin {
    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void betterSchematics$rememberExportPosition(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        var pos = ((FactoryPanelBehaviour) (Object) this).getPos();
        if (!clientPacket)
            tag.putIntArray(FactoryGaugeConnections.ORIGIN, new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    @Inject(method = "writeSafe", at = @At("RETURN"), remap = false)
    private void betterSchematics$saveSetup(CompoundTag tag, CallbackInfo ci) {
        FactoryPanelBehaviour panel = (FactoryPanelBehaviour) (Object) this;
        if (!panel.active)
            return;
        CompoundTag full = new CompoundTag();
        panel.write(full, false);
        String slot = panel.slot.name().toLowerCase(java.util.Locale.ROOT);
        CompoundTag settings = tag.getCompound(slot);
        CompoundTag saved = full.getCompound(slot);
        for (String key : List.of("Targeting", "TargetedBy", "TargetedByLinks", "Craft", "RecipeOutput",
                "PromiseClearingInterval", "UpTo"))
            if (saved.contains(key))
                settings.put(key, saved.get(key).copy());
        settings.remove("Promises");
        tag.putBoolean("Restocker", ((FactoryPanelBlockEntity) panel.blockEntity).restocker);
    }

    @Inject(method = { "initialize", "lazyTick" }, at = @At("RETURN"), remap = false)
    private void betterSchematics$restoreLinksPlacedLater(CallbackInfo ci) {
        FactoryPanelBehaviour panel = (FactoryPanelBehaviour) (Object) this;
        if (!panel.active || panel.getWorld().isClientSide())
            return;
        for (FactoryPanelConnection connection : panel.targetedByLinks.values()) {
            FactoryPanelSupportBehaviour support = FactoryPanelBehaviour.linkAt(panel.getWorld(), connection);
            if (support != null)
                support.connect(panel);
        }
    }
}
