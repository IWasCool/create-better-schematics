package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.simibubi.create.content.schematics.SchematicExport;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = SchematicExport.class, remap = false)
abstract class SchematicExportMixin {
    @ModifyArg(method = "saveSchematic", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V"), index = 0, remap = false)
    private static CompoundTag betterSchematics$writePortableConfiguration(CompoundTag data) {
        return SchematicDataCompatibility.exportSchematic(data);
    }
}
