package com.ourcreationstudios.create_better_schematics.mixin;

import java.io.IOException;
import com.ourcreationstudios.create_better_schematics.compat.SchematicFileFormat;
import com.simibubi.create.content.schematics.SchematicItem;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = SchematicItem.class, remap = false)
abstract class SchematicItemMixin {
    @ModifyArg(method = "loadSchematic", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;load(Lnet/minecraft/core/HolderGetter;Lnet/minecraft/nbt/CompoundTag;)V", remap = true),
            index = 1, remap = false)
    private static CompoundTag betterSchematics$readSharedFile(CompoundTag data) throws IOException {
        return SchematicFileFormat.forLegacyReader(data, true);
    }
}
