package com.ourcreationstudios.create_better_schematics.compat_gametest;

import com.ourcreationstudios.create_better_schematics.compat.AddonConfigurationPlacement;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder("create_better_schematics")
public final class PopularModGameTests {
    @GameTestGenerator
    public static List<TestFunction> popularModChecks() {
        if (!ModList.get().isLoaded("mekanism"))
            return List.of();
        return List.of(new TestFunction("popularMods", "mekanism_native_side_transform_matrix",
                "create_better_schematics:empty", Rotation.NONE, 100, 0, true, helper -> {
            try {
                mekanismSides(helper);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    private static void mekanismSides(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_energy_cube"));
        helper.assertTrue(block instanceof EntityBlock, "Mekanism energy cube must exist");
        EntityBlock factory = (EntityBlock) block;
        var sideType = Class.forName("mekanism.api.RelativeSide");
        Object[] sides = sideType.getEnumConstants();
        var directionOf = sideType.getMethod("getDirection", Direction.class);
        int checked = 0;
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(BlockStateProperties.FACING, facing);
            var original = factory.newBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2)), state);
            original.setLevel(level);
            CompoundTag configured = original.saveWithoutMetadata(level.registryAccess());
            CompoundTag config = configured.getCompound("component_config");
            for (String key : config.getAllKeys())
                if (key.startsWith("config") && config.contains(key, Tag.TAG_INT_ARRAY))
                    config.putIntArray(key, new int[] { 0, 1, 2, 2, 1, 0 });
            original.loadWithComponents(configured, level.registryAccess());
            CompoundTag safe = BlockHelper.prepareBlockEntityData(level, state, original);
            helper.assertTrue(safe != null && !safe.getCompound("component_config").isEmpty(),
                    "Energy cube IO configuration must be copied");
            CompoundTag before = safe.copy();
            for (Rotation rotation : Rotation.values())
                for (Mirror mirror : Mirror.values()) {
                    var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
                    var raw = new StructureTemplate.StructureBlockInfo(original.getBlockPos(), state, safe);
                    var transformed = AddonConfigurationPlacement.process(raw, raw, settings);
                    var targetState = state.mirror(mirror).rotate(rotation);
                    Direction targetFacing = targetState.getValue(BlockStateProperties.FACING);
                    var copy = factory.newBlockEntity(helper.absolutePos(new BlockPos(7, 2, 7)), targetState);
                    copy.setLevel(level);
                    copy.loadWithComponents(transformed.nbt(), level.registryAccess());
                    CompoundTag saved = copy.saveWithoutMetadata(level.registryAccess()).getCompound("component_config");
                    for (String key : before.getCompound("component_config").getAllKeys()) {
                        if (!key.startsWith("config") || !before.getCompound("component_config").contains(key, Tag.TAG_INT_ARRAY))
                            continue;
                        int[] oldModes = before.getCompound("component_config").getIntArray(key);
                        int[] newModes = saved.getIntArray(key);
                        helper.assertTrue(oldModes.length == 6 && newModes.length == 6, "Native side arrays must contain six entries");
                        for (int sourceSide = 0; sourceSide < 6; sourceSide++) {
                            Direction worldSide = (Direction) directionOf.invoke(sides[sourceSide], facing);
                            Direction wanted = rotation.rotate(mirror.mirror(worldSide));
                            int destination = -1;
                            for (int side = 0; side < 6; side++)
                                if (directionOf.invoke(sides[side], targetFacing) == wanted)
                                    destination = side;
                            helper.assertTrue(destination >= 0 && newModes[destination] == oldModes[sourceSide],
                                    "IO must follow Mekanism's native sides: " + facing + "/" + rotation + "/" + mirror);
                        }
                    }
                    helper.assertTrue(safe.equals(before), "Transforming IO must leave source data intact");
                    checked++;
                }
        }
        helper.assertTrue(checked == 72, "All six facings, four rotations and three mirrors must be checked");
        helper.succeed();
    }
}
