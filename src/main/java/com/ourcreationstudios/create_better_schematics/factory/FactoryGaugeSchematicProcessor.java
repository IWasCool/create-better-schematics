package com.ourcreationstudios.create_better_schematics.factory;

import com.mojang.serialization.MapCodec;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.compat.PowerGridWirePlacement;
import com.ourcreationstudios.create_better_schematics.compat.AddonConfigurationPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FactoryGaugeSchematicProcessor extends StructureProcessor {
    public static final DeferredRegister<StructureProcessorType<?>> TYPES = DeferredRegister.create(
            Registries.STRUCTURE_PROCESSOR, CreateBetterSchematics.MODID);
    private static final MapCodec<FactoryGaugeSchematicProcessor> CODEC = MapCodec.unit(FactoryGaugeSchematicProcessor::new);
    private static final java.util.function.Supplier<StructureProcessorType<FactoryGaugeSchematicProcessor>> TYPE =
            TYPES.register("factory_gauge_connections", () -> () -> CODEC);
    private final FactoryGaugePlacement placement = new FactoryGaugePlacement();
    private final PowerGridWirePlacement wires = new PowerGridWirePlacement(false);

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
        return TYPE.get();
    }
}
