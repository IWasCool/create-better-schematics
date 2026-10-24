package com.ourcreationstudios.create_better_schematics.compat;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Relocates IO settings without loading classes from an optional mod. */
public final class AddonConfigurationPlacement {
    private AddonConfigurationPlacement() {
    }

    public static StructureTemplate.StructureBlockInfo process(StructureTemplate.StructureBlockInfo raw,
            StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings settings) {
        if (placed == null || placed.nbt() == null
                || !BuiltInRegistries.BLOCK.getKey(raw.state().getBlock()).getNamespace().equals("mekanism"))
            return placed;
        Direction before = facing(raw.state());
        Direction after = facing(raw.state().mirror(settings.getMirror()).rotate(settings.getRotation()));
        if (before == null || after == null)
            return placed;
        CompoundTag data = placed.nbt().copy();
        CompoundTag config = data.getCompound("component_config");
        for (String key : config.getAllKeys())
            if (key.startsWith("config") && config.contains(key, Tag.TAG_INT_ARRAY))
                config.putIntArray(key, transform(config.getIntArray(key), before, after, settings));
        CompoundTag ejector = data.getCompound("component_ejector");
        if (ejector.contains("input_color", Tag.TAG_INT_ARRAY))
            ejector.putIntArray("input_color", transform(ejector.getIntArray("input_color"), before, after, settings));
        return new StructureTemplate.StructureBlockInfo(placed.pos(), placed.state(), data);
    }

    private static int[] transform(int[] source, Direction before, Direction after, StructurePlaceSettings settings) {
        if (source.length != 6)
            return source;
        int[] transformed = new int[6];
        for (int side = 0; side < 6; side++) {
            Direction worldSide = relativeSide(before, side);
            Direction target = settings.getRotation().rotate(settings.getMirror().mirror(worldSide));
            for (int destination = 0; destination < 6; destination++)
                if (relativeSide(after, destination) == target) {
                    transformed[destination] = source[side];
                    break;
                }
        }
        return transformed;
    }

    private static Direction facing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return null;
    }

    // Mekanism RelativeSide order: FRONT, LEFT, RIGHT, BACK, TOP, BOTTOM.
    private static Direction relativeSide(Direction facing, int side) {
        boolean vertical = facing.getAxis() == Direction.Axis.Y;
        return switch (side) {
            case 0 -> facing;
            case 1 -> vertical ? Direction.EAST : facing.getClockWise();
            case 2 -> vertical ? Direction.WEST : facing.getCounterClockWise();
            case 3 -> facing.getOpposite();
            case 4 -> vertical ? (facing == Direction.DOWN ? Direction.NORTH : Direction.SOUTH) : Direction.UP;
            case 5 -> vertical ? (facing == Direction.DOWN ? Direction.SOUTH : Direction.NORTH) : Direction.DOWN;
            default -> throw new IllegalArgumentException("Unknown relative side: " + side);
        };
    }
}
