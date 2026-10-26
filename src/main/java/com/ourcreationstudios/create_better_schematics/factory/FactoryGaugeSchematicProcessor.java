package com.ourcreationstudios.create_better_schematics.factory;

import com.mojang.serialization.MapCodec;
import com.ourcreationstudios.create_better_schematics.compat.PowerGridWirePlacement;
import com.ourcreationstudios.create_better_schematics.compat.AddonConfigurationPlacement;
import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class FactoryGaugeSchematicProcessor extends StructureProcessor {
    private static final MapCodec<FactoryGaugeSchematicProcessor> CODEC = MapCodec.unit(FactoryGaugeSchematicProcessor::new);
    // Attached directly to Create's placement settings; never serialized or synced.
    private static final StructureProcessorType<FactoryGaugeSchematicProcessor> TYPE = () -> CODEC;
    private final FactoryGaugePlacement placement = new FactoryGaugePlacement();
    private final PowerGridWirePlacement wires = new PowerGridWirePlacement(false);
    private final TransmissionLinks transmission = new TransmissionLinks();

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
        return AddonConfigurationPlacement.process(raw,
                transmission.process(raw, placement.process(raw, placed, settings, template), settings, template), settings);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
