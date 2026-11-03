package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.SchematicExport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class BeltCasingGameTests {
    @GameTest(template = "empty")
    public static void mixedCasingWithNbt(GameTestHelper helper) {
        roundTrip(helper, CasingType.ANDESITE, CasingType.BRASS, false, false, Rotation.NONE);
    }

    @GameTest(template = "empty")
    public static void legacyAndesiteWithoutNbt(GameTestHelper helper) {
        roundTrip(helper, CasingType.ANDESITE, CasingType.ANDESITE, true, false, Rotation.NONE);
    }

    @GameTest(template = "empty")
    public static void legacyBrassWithoutNbt(GameTestHelper helper) {
        roundTrip(helper, CasingType.BRASS, CasingType.BRASS, true, false, Rotation.NONE);
    }

    @GameTest(template = "empty")
    public static void legacyMixedCasingWithoutNbtRotated(GameTestHelper helper) {
        roundTrip(helper, CasingType.ANDESITE, CasingType.BRASS, true, false, Rotation.CLOCKWISE_90);
    }

    @GameTest(template = "empty")
    public static void legacyStateRecoversSynthesizedEmptyCasingNbt(GameTestHelper helper) {
        roundTrip(helper, CasingType.BRASS, CasingType.ANDESITE, true, true, Rotation.NONE);
    }

    @GameTest(template = "empty")
    public static void exportedBeltsUseOnlyNativeCreateData(GameTestHelper helper) throws IOException {
        BlockPos source = createBelt(helper, CasingType.ANDESITE, CasingType.BRASS);
        var result = SchematicExport.saveSchematic(Path.of("../build/shareable-fixtures"), "native-belts.nbt",
                true, helper.getLevel(), source, source.east());
        helper.assertTrue(result != null, "The normal Create export writes a shareable belt file");
        var data = net.minecraft.nbt.NbtIo.readCompressed(result.file(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        for (Tag entry : data.getList("palette", Tag.TAG_COMPOUND))
            helper.assertTrue(!((CompoundTag) entry).getCompound("Properties").contains("create_better_schematics_casing"),
                    "Shared files must not depend on an addon block state property");
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag nbt = ((CompoundTag) entry).getCompound("nbt");
            helper.assertTrue(List.of("ANDESITE", "BRASS").contains(nbt.getString("Casing")),
                    "Create's own Casing field retains each segment's material");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ambiguousLegacyPaletteKeepsNativeCasing(GameTestHelper helper) {
        CompoundTag source = new CompoundTag();
        ListTag palettes = new ListTag();
        for (String material : List.of("andesite", "brass")) {
            CompoundTag state = new CompoundTag();
            state.putString("Name", "create:belt");
            CompoundTag properties = new CompoundTag();
            properties.putString("casing", "true");
            properties.putString("create_better_schematics_casing", material);
            state.put("Properties", properties);
            ListTag palette = new ListTag();
            palette.add(state);
            palettes.add(palette);
        }
        source.put("palettes", palettes);
        CompoundTag block = new CompoundTag();
        block.putInt("state", 0);
        CompoundTag nativeData = new CompoundTag();
        nativeData.putString("Casing", "ANDESITE");
        block.put("nbt", nativeData);
        ListTag blocks = new ListTag();
        blocks.add(block);
        source.put("blocks", blocks);
        CompoundTag converted = SchematicDataCompatibility.importSchematic(source);
        helper.assertTrue(converted.getList("blocks", Tag.TAG_COMPOUND).getCompound(0)
                        .getCompound("nbt").getString("Casing").equals("ANDESITE"),
                "Conflicting legacy palette variants must not overwrite valid native NBT");
        helper.assertTrue(palettes.getList(0).getCompound(0).getCompound("Properties")
                        .contains("create_better_schematics_casing"), "Migration leaves the source data intact");
        for (Tag palette : converted.getList("palettes", Tag.TAG_LIST))
            helper.assertTrue(!((ListTag) palette).getCompound(0).getCompound("Properties")
                    .contains("create_better_schematics_casing"), "Migrated palettes use only native state properties");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cannonInFlightSaveReload(GameTestHelper helper) {
        BlockPos source = createBelt(helper, CasingType.ANDESITE, CasingType.BRASS);
        BlockPos target = helper.absolutePos(new BlockPos(3, 2, 4));
        Block shaft = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:shaft"));
        helper.getLevel().setBlock(target.east(), shaft.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), Block.UPDATE_CLIENTS);
        LaunchedItem.ForBelt flight = new LaunchedItem.ForBelt(source, target, ItemStack.EMPTY,
                helper.getLevel().getBlockState(source), new CasingType[]{CasingType.ANDESITE, CasingType.BRASS});
        CompoundTag saved = flight.serializeNBT(helper.getLevel().registryAccess());
        saved.putInt("TicksLeft", 0);
        LaunchedItem reloaded = LaunchedItem.fromNBT(saved, helper.getLevel().registryAccess(),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK));
        helper.assertTrue(reloaded.update(helper.getLevel()), "Cannon flight should place its belt");
        helper.runAfterDelay(3, () -> {
            assertCasing(helper, target, CasingType.ANDESITE);
            assertCasing(helper, target.east(), CasingType.BRASS);
            helper.succeed();
        });
    }

    private static void roundTrip(GameTestHelper helper, CasingType first, CasingType second,
                                   boolean omitNbt, boolean synthesizeNbt, Rotation rotation) {
        BlockPos source = createBelt(helper, first, second);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), source, new Vec3i(2, 1, 1), false, null);
        CompoundTag saved = template.save(new CompoundTag());
        if (omitNbt) {
            for (Tag entry : saved.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag block = (CompoundTag) entry;
                // Recreate the old addon-only encoding, including files whose NBT was stripped.
                int x = block.getList("pos", Tag.TAG_INT).getInt(0);
                CompoundTag properties = saved.getList("palette", Tag.TAG_COMPOUND)
                        .getCompound(block.getInt("state")).getCompound("Properties");
                properties.putString("create_better_schematics_casing", (x == 0 ? first : second).name()
                        .toLowerCase(java.util.Locale.ROOT));
                block.remove("nbt");
                if (synthesizeNbt) {
                    CompoundTag defaultNbt = new CompoundTag();
                    defaultNbt.putString("id", "create:belt");
                    defaultNbt.putString("Casing", "NONE");
                    block.put("nbt", defaultNbt);
                }
            }
        }
        StructureTemplate reloaded = new StructureTemplate();
        reloaded.load(helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK),
                SchematicDataCompatibility.importSchematic(saved));
        BlockPos target = helper.absolutePos(new BlockPos(3, 2, 4));
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setKnownShape(true);
        helper.assertTrue(reloaded.placeInWorld(helper.getLevel(), target, target, settings,
                helper.getLevel().getRandom(), Block.UPDATE_CLIENTS), "Schematic should place");
        BlockPos secondTarget = target.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(1, 0, 0)));
        // Verify actual block entities immediately, including virtual/structure loading before ticks.
        assertCasing(helper, target, first);
        assertCasing(helper, secondTarget, second);
        helper.runAfterDelay(3, () -> {
            assertCasing(helper, target, first);
            assertCasing(helper, secondTarget, second);
            helper.succeed();
        });
    }

    private static BlockPos createBelt(GameTestHelper helper, CasingType first, CasingType second) {
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        Block beltBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:belt"));
        BlockState start = beltBlock.defaultBlockState().setValue(BeltBlock.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BeltBlock.PART, BeltPart.START);
        helper.getLevel().setBlock(source, start, Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(source.east(), start.setValue(BeltBlock.PART, BeltPart.END), Block.UPDATE_CLIENTS);
        BeltBlock.initBelt(helper.getLevel(), source);
        ((BeltBlockEntity) helper.getLevel().getBlockEntity(source)).setCasingType(first);
        ((BeltBlockEntity) helper.getLevel().getBlockEntity(source.east())).setCasingType(second);
        assertCasing(helper, source, first);
        assertCasing(helper, source.east(), second);
        return source;
    }

    private static void assertCasing(GameTestHelper helper, BlockPos pos, CasingType expected) {
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof BeltBlockEntity,
                "Expected belt at " + pos);
        BeltBlockEntity belt = (BeltBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(belt.casing == expected,
                "Belt at " + pos + " expected " + expected + " but got " + belt.casing);
        helper.assertTrue(belt.getBlockState().getBlock().getStateDefinition()
                        .getProperty("create_better_schematics_casing") == null,
                "Installing the mod must not change server/client belt state IDs");
    }
}
