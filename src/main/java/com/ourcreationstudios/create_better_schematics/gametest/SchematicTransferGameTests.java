package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class SchematicTransferGameTests {
    @GameTest(template = "empty")
    public static void nativeFileRotatedAndCannonReload(GameTestHelper helper) throws IOException {
        transferFile(helper, false);
    }

    @GameTest(template = "empty")
    public static void forgeFileRotatedAndCannonReload(GameTestHelper helper) throws IOException {
        transferFile(helper, true);
    }

    @GameTest(template = "empty")
    public static void legacyThresholdWaitsForInventory(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = block("stockpile_switch").defaultBlockState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
        ThresholdSwitchBlockEntity threshold = (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(pos);
        CompoundTag legacy = new CompoundTag();
        legacy.putFloat("OnAbove", .75f);
        legacy.putFloat("OffBelow", .25f);
        threshold.loadWithComponents(legacy, helper.getLevel().registryAccess());
        threshold.updateCurrentLevel();
        CompoundTag safe = BlockHelper.prepareBlockEntityData(helper.getLevel(), state, threshold);
        helper.assertTrue(safe.contains("OnAbove") && !safe.contains("OnAboveAmount"),
                "Unresolved percentages must survive virtual levels/cannon serialization: " + safe);
        threshold.loadWithComponents(safe, helper.getLevel().registryAccess());
        helper.getLevel().setBlock(pos.relative(ThresholdSwitchBlock.getTargetDirection(state)),
                Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        threshold.updateCurrentLevel();
        helper.assertTrue(threshold.onWhenAbove == 1296 && threshold.offWhenBelow == 432,
                "Percentages must resolve only after the target inventory arrives");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeAmountsAndUnknownDataSurvive(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = block("stockpile_switch").defaultBlockState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
        ThresholdSwitchBlockEntity threshold = (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(pos);
        CompoundTag nativeData = new CompoundTag();
        nativeData.putInt("OnAboveAmount", 16_777_217);
        nativeData.putInt("OffBelowAmount", 123);
        nativeData.putFloat("OnAbove", .75f);
        nativeData.putFloat("OffBelow", .25f);
        nativeData.putString("ThirdPartySetting", "preserve me");
        CompoundTag original = nativeData.copy();
        CompoundTag imported = SchematicDataCompatibility.importSchematic(nativeData);
        helper.assertTrue(nativeData.equals(original), "Import must leave its input intact");
        helper.assertTrue(imported.equals(SchematicDataCompatibility.importSchematic(imported)),
                "Import must be idempotent");
        helper.assertTrue(imported.getString("ThirdPartySetting").equals("preserve me"),
                "Unknown configuration data must remain intact");
        threshold.loadWithComponents(imported, helper.getLevel().registryAccess());
        CompoundTag safe = new CompoundTag();
        threshold.writeSafe(safe, helper.getLevel().registryAccess());
        helper.assertTrue(safe.getInt("OnAboveAmount") == 16_777_217 && safe.getInt("OffBelowAmount") == 123,
                "Native amounts must win over compatibility percentages without float rounding");
        helper.succeed();
    }

    private static void transferFile(GameTestHelper helper, boolean emulateForge) throws IOException {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos sourceArm = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos sourceDepot = sourceArm.east(2);
        BlockPos sourceThreshold = sourceArm.east(3);
        var thresholdState = block("stockpile_switch").defaultBlockState();
        level.setBlock(sourceDepot, block("depot").defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(sourceArm, block("mechanical_arm").defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(sourceThreshold.relative(ThresholdSwitchBlock.getTargetDirection(thresholdState)),
                Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(sourceThreshold, thresholdState, Block.UPDATE_CLIENTS);
        ArmBlockEntity arm = (ArmBlockEntity) level.getBlockEntity(sourceArm);
        CompoundTag armData = new CompoundTag();
        ListTag points = new ListTag();
        points.add(ArmInteractionPoint.create(level, sourceDepot, level.getBlockState(sourceDepot)).serialize(sourceArm));
        armData.put("InteractionPoints", points);
        armData.putInt("ScrollValue", 2);
        arm.loadWithComponents(armData, level.registryAccess());
        arm.tick();
        ThresholdSwitchBlockEntity threshold = (ThresholdSwitchBlockEntity) level.getBlockEntity(sourceThreshold);
        threshold.onWhenAbove = 1296;
        threshold.offWhenBelow = 432;
        threshold.inStacks = true;
        threshold.setInverted(true);
        threshold.getBehaviour(FilteringBehaviour.TYPE).setFilter(listFilter());
        threshold.updateCurrentLevel();

        String owner = "better_schematics_gametest_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        SchematicExport.SchematicExportResult result = SchematicExport.saveSchematic(directory,
                "transfer", false, level, origin, origin.offset(4, 2, 3));
        helper.assertTrue(result != null, "Create must export the schematic file");
        BlockPos target = helper.absolutePos(new BlockPos(8, 1, 7));
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90)
                .setKnownShape(true);
        BlockPos targetArm = target.offset(StructureTemplate.calculateRelativePosition(settings, sourceArm.subtract(origin)));
        BlockPos targetDepot = target.offset(StructureTemplate.calculateRelativePosition(settings, sourceDepot.subtract(origin)));
        BlockPos targetThreshold = target.offset(StructureTemplate.calculateRelativePosition(settings, sourceThreshold.subtract(origin)));
        try {
            CompoundTag exported = NbtIo.readCompressed(result.file(), NbtAccounter.unlimitedHeap());
            CompoundTag exportedArm = findBlock(exported, "create:mechanical_arm");
            CompoundTag exportedThreshold = findBlock(exported, "create:stockpile_switch");
            helper.assertTrue(exportedArm.getList("InteractionPoints", Tag.TAG_COMPOUND)
                    .getCompound(0).contains("Pos", Tag.TAG_COMPOUND), "Exported file must contain Forge-readable arm positions");
            CompoundTag exportedFilter = exportedThreshold.getCompound("Filter");
            helper.assertTrue(exportedFilter.getByte("Count") == 1 && exportedFilter.getCompound("tag").getBoolean("Blacklist"),
                    "Forge must be able to read filter count and deny list settings");
            helper.assertTrue(exportedThreshold.getFloat("OnAbove") == .75f
                            && exportedThreshold.getInt("OnAboveAmount") == 1296,
                    "Export must contain both threshold formats");
            helper.assertTrue(exported.equals(SchematicDataCompatibility.exportSchematic(exported)),
                    "Export conversion must be idempotent");
            if (emulateForge) {
                stripModernItems(exported);
                exportedThreshold.remove("OnAboveAmount");
                exportedThreshold.remove("OffBelowAmount");
                exported.putInt("DataVersion", 3465);
                NbtIo.writeCompressed(exported, result.file());
            }
            ItemStack blueprint = SchematicItem.create(level, result.fileName(), owner);
            StructureTemplate loaded = SchematicItem.loadSchematic(level, blueprint);
            helper.assertTrue(loaded.placeInWorld(level, target, target, settings, level.getRandom(), Block.UPDATE_CLIENTS),
                    "Create must load and place the schematic from disk");
        } finally {
            Files.deleteIfExists(result.file());
            Files.deleteIfExists(directory);
        }

        ArmBlockEntity placedArm = (ArmBlockEntity) level.getBlockEntity(targetArm);
        new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.NONE).apply(placedArm);
        placedArm.tick();
        assertArmTarget(helper, placedArm, targetDepot);
        CompoundTag safeArm = BlockHelper.prepareBlockEntityData(level, placedArm.getBlockState(), placedArm);
        helper.assertTrue(safeArm.getInt("ScrollValue") == 2, "Arm selection mode must survive file transfer");
        ThresholdSwitchBlockEntity placedThreshold = (ThresholdSwitchBlockEntity) level.getBlockEntity(targetThreshold);
        placedThreshold.updateCurrentLevel();
        assertThreshold(helper, placedThreshold);

        BlockPos cannonThreshold = helper.absolutePos(new BlockPos(10, 2, 2));
        level.setBlock(cannonThreshold.relative(ThresholdSwitchBlock.getTargetDirection(placedThreshold.getBlockState())),
                Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        CompoundTag cannonData = BlockHelper.prepareBlockEntityData(level, placedThreshold.getBlockState(), placedThreshold);
        LaunchedItem flight = new LaunchedItem.ForBlockState(sourceThreshold, cannonThreshold,
                new ItemStack(block("stockpile_switch")), placedThreshold.getBlockState(), cannonData);
        CompoundTag flightSave = flight.serializeNBT(level.registryAccess());
        flightSave.putInt("TicksLeft", 0);
        LaunchedItem restored = LaunchedItem.fromNBT(flightSave, level.registryAccess(),
                level.registryAccess().lookupOrThrow(Registries.BLOCK));
        helper.assertTrue(restored.update(level), "Reloaded cannon flight must place its switch");
        ThresholdSwitchBlockEntity cannonSwitch = (ThresholdSwitchBlockEntity) level.getBlockEntity(cannonThreshold);
        cannonSwitch.updateCurrentLevel();
        assertThreshold(helper, cannonSwitch);
        helper.succeed();
    }

    private static ItemStack listFilter() {
        ItemStack filter = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:filter")));
        filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(filteredIron())));
        filter.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
        filter.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, true);
        return filter;
    }

    private static void assertThreshold(GameTestHelper helper, ThresholdSwitchBlockEntity threshold) {
        helper.assertTrue(threshold.onWhenAbove == 1296 && threshold.offWhenBelow == 432,
                "Transferred thresholds must remain 1296/432, got " + threshold.onWhenAbove + "/" + threshold.offWhenBelow);
        helper.assertTrue(threshold.inStacks && threshold.isInverted(), "Stack mode and inversion must survive");
        FilteringBehaviour filtering = threshold.getBehaviour(FilteringBehaviour.TYPE);
        helper.assertTrue(!filtering.test(filteredIron()) && filtering.test(new ItemStack(Items.IRON_INGOT))
                        && filtering.test(new ItemStack(Items.GOLD_INGOT)),
                "Transferred filter must retain nested item names and deny list behavior");
        helper.assertTrue(Boolean.TRUE.equals(filtering.getFilter().get(AllDataComponents.FILTER_ITEMS_RESPECT_NBT)),
                "Respect data filter option must survive");
    }

    private static ItemStack filteredIron() {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        iron.set(DataComponents.CUSTOM_NAME, Component.literal("Calibration sample"));
        return iron;
    }

    static void assertArmTarget(GameTestHelper helper, ArmBlockEntity arm, BlockPos expected) {
        assertArmFirstTarget(helper, arm, expected, 1);
    }

    static void assertArmFirstTarget(GameTestHelper helper, ArmBlockEntity arm, BlockPos expected, int count) {
        try {
            var outputsField = ArmBlockEntity.class.getDeclaredField("outputs");
            outputsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ArmInteractionPoint> outputs = (List<ArmInteractionPoint>) outputsField.get(arm);
            helper.assertTrue(outputs.size() == count && outputs.getFirst().getPos().equals(expected),
                    "Rotated arm must initialize its actual destination at " + expected);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect initialized arm destinations", e);
        }
    }

    private static CompoundTag findBlock(CompoundTag structure, String id) {
        for (Tag entry : structure.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag nbt = ((CompoundTag) entry).getCompound("nbt");
            if (nbt.getString("id").equals(id))
                return nbt;
        }
        throw new IllegalStateException("Missing block entity " + id);
    }

    private static void stripModernItems(CompoundTag tag) {
        if (tag.contains("id", Tag.TAG_STRING) && tag.contains("Count", Tag.TAG_BYTE)) {
            tag.remove("count");
            tag.remove("components");
        }
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag value = tag.get(key);
            if (value instanceof CompoundTag child)
                stripModernItems(child);
            else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND)
                list.forEach(entry -> stripModernItems((CompoundTag) entry));
        }
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
