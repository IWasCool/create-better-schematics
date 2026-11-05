package com.ourcreationstudios.create_better_schematics.mixin;

import com.simibubi.create.content.schematics.SchematicPrinter;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicPrinter.class, remap = false)
public interface SchematicPrinterAccessor {
    @Accessor("blockReader")
    SchematicLevel betterSchematics$getBlockReader();
}
