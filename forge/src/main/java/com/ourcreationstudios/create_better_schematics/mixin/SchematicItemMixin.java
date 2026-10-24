package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeSchematicProcessor;
import com.simibubi.create.content.schematics.SchematicItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicItem.class, remap = false)
abstract class SchematicItemMixin {
    @Inject(method = "getSettings(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;",
            at = @At("RETURN"), remap = false)
    private static void betterSchematics$relocateGaugeConnections(ItemStack blueprint, boolean processNBT,
            CallbackInfoReturnable<StructurePlaceSettings> cir) {
        cir.getReturnValue().addProcessor(new FactoryGaugeSchematicProcessor());
    }
}
