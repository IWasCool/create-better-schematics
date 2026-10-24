package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeSchematicProcessor;
import com.simibubi.create.content.schematics.SchematicItem;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(method = "loadSchematic", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;load(Lnet/minecraft/core/HolderGetter;Lnet/minecraft/nbt/CompoundTag;)V"), remap = false)
    private static void betterSchematics$upgradeConfiguration(StructureTemplate template,
                                                             HolderGetter<Block> blocks, CompoundTag data) {
        template.load(blocks, SchematicDataCompatibility.importSchematic(data));
    }
}
