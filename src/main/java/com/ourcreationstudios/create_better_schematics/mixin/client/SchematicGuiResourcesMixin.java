package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.util.List;
import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ourcreationstudios.create_better_schematics.client.SchematicGuiResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiPackResourceManager.class, remap = false)
abstract class SchematicGuiResourcesMixin {
    @Unique private boolean betterSchematics$clientResources;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void betterSchematics$resourceType(PackType type, List<PackResources> packs, CallbackInfo ci) {
        betterSchematics$clientResources = type == PackType.CLIENT_RESOURCES;
    }

    @ModifyReturnValue(method = "getResource", at = @At("RETURN"), remap = false)
    private Optional<Resource> betterSchematics$nativeWidgets(Optional<Resource> current, ResourceLocation id) {
        if (betterSchematics$clientResources && SchematicGuiResources.WIDGETS.equals(id) && current.isEmpty())
            return SchematicGuiResources.nativeWidgets((ResourceManager) this);
        return current;
    }
}
