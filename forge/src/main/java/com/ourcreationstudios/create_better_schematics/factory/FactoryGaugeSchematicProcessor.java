package com.ourcreationstudios.create_better_schematics.factory;

import com.mojang.serialization.Codec;
import com.ourcreationstudios.create_better_schematics.compat.PowerGridWirePlacement;
import com.ourcreationstudios.create_better_schematics.compat.AddonConfigurationPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class FactoryGaugeSchematicProcessor extends StructureProcessor {
    private static final Codec<FactoryGaugeSchematicProcessor> CODEC = Codec.unit(FactoryGaugeSchematicProcessor::new);
    // This runtime-only processor does not add a registry entry required by peers.
    private static final StructureProcessorType<FactoryGaugeSchematicProcessor> TYPE = () -> CODEC;
    private final FactoryGaugePlacement placement = new FactoryGaugePlacement();
    private final PowerGridWirePlacement wires = new PowerGridWirePlacement(true);

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader world, BlockPos origin,
            StructureTemplate.StructureEntityInfo raw, StructureTemplate.StructureEntityInfo placed,
            StructurePlaceSettings settings, StructureTemplate template) {
        return wires.process(raw, placed, settings, origin, template, world);
    }

    @Override
    public StructureTemplate.StructureBlockInfo process(LevelReader world, BlockPos origin, BlockPos pivot,
            StructureTemplate.StructureBlockInfo raw, StructureTemplate.StructureBlockInfo placed,
            StructurePlaceSettings settings, StructureTemplate template) {
        return AddonConfigurationPlacement.process(raw, placement.process(raw, placed, settings, template), settings);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
