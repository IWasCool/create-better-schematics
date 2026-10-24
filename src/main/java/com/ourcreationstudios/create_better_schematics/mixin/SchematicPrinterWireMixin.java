package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.PowerGridWires;
import com.simibubi.create.content.schematics.SchematicPrinter;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SchematicPrinter.class, remap = false)
abstract class SchematicPrinterWireMixin {
    @Shadow private int printingEntityIndex;
    @Shadow private SchematicLevel blockReader;

    @Redirect(method = "advanceCurrentPos", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;isInside(Lnet/minecraft/core/Vec3i;)Z"), remap = false)
    private boolean betterSchematics$includeSurfaceWires(BoundingBox bounds, Vec3i pos) {
        // A wire lying on a block face can rotate onto the next cell's boundary.
        // Its endpoints were already checked against the selected blocks by our processor.
        if (printingEntityIndex >= 0 && printingEntityIndex < blockReader.getEntityList().size()
                && PowerGridWires.isWire(BuiltInRegistries.ENTITY_TYPE
                        .getKey(blockReader.getEntityList().get(printingEntityIndex).getType()).toString()))
            return true;
        return bounds.isInside(pos);
    }
}
