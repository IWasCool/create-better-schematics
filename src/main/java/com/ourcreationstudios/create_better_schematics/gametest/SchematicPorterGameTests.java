package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.ourcreationstudios.create_better_schematics.porter.SchematicPorter;
import com.ourcreationstudios.create_better_schematics.porter.SchematicPorter.Target;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.logistics.filter.AttributeFilterWhitelistMode;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute.ItemAttributeEntry;
import com.simibubi.create.content.logistics.item.filter.attribute.AllItemAttributeTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.AddedByAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class SchematicPorterGameTests {
    @GameTest(template = "empty")
    public static void thresholdSerializationDoesNotObserveTarget(GameTestHelper helper) {
        var state = block("stockpile_switch").defaultBlockState();
        var registered = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.parse("create:stockpile_switch"));
        ThresholdSwitchBlockEntity isolated = new ThresholdSwitchBlockEntity(registered, BlockPos.ZERO, state) {
            @Override
            public ThresholdType getTypeOfCurrentTarget() {
                throw new AssertionError("Serialization must not query or load the observed inventory's chunk");
            }
        };
        isolated.setLevel(helper.getLevel());
        isolated.onWhenAbove = 2000;
        isolated.offWhenBelow = 500;
        isolated.writeSafe(new CompoundTag(), helper.getLevel().registryAccess());
        isolated.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void porterWritesRealMachineFiles(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos armPos = origin.offset(0, 1, 1);
        BlockPos depotPos = armPos.east(2);
        level.setBlock(depotPos, block("depot").defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(armPos, block("mechanical_arm").defaultBlockState(), Block.UPDATE_CLIENTS);
        ArmBlockEntity arm = (ArmBlockEntity) level.getBlockEntity(armPos);
        CompoundTag armData = new CompoundTag();
        ListTag points = new ListTag();
        points.add(ArmInteractionPoint.create(level, depotPos, level.getBlockState(depotPos)).serialize(armPos));
        armData.put("InteractionPoints", points);
        armData.putInt("ScrollValue", 2);
        arm.loadWithComponents(armData, level.registryAccess());
        arm.tick();
        configureSwitch(helper, origin.offset(4, 1, 1), listFilter(), false);
        configureSwitch(helper, origin.offset(4, 1, 3), attributeFilter(), false);
        Path directory = Files.createTempDirectory(Path.of("schematics"), "porter-test-");
        try {
            var source = SchematicExport.saveSchematic(directory, "machines", false, level, origin, origin.offset(5, 2, 4));
            helper.assertTrue(source != null, "Create must save a real source schematic");
            byte[] original = Files.readAllBytes(source.file());
            CompoundTag originalData = NbtIo.readCompressed(source.file(), NbtAccounter.unlimitedHeap());
            Path sharedFixtures = Path.of("../build/shareable-fixtures");
            Files.createDirectories(sharedFixtures);
            Files.copy(source.file(), sharedFixtures.resolve("neoforge-shared-machines.nbt"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            for (Target target : Target.values()) {
                var exported = SchematicPorter.export(directory, source.fileName(), target);
                var report = exported.conversion();
                helper.assertTrue(report.arms() == 1 && report.switches() == 2 && report.filters() == 2,
                        "Report must count machines and each filter once: " + report);
                helper.assertTrue(report.notices().isEmpty(), "Core settings must be supported: " + report.notices());
                CompoundTag data = NbtIo.readCompressed(exported.file(), NbtAccounter.unlimitedHeap());
                helper.assertTrue(data.getInt("DataVersion") == 3465, "Output must target Minecraft 1.20.1");
                helper.assertTrue(data.equals(SchematicPorter.convert(data, target).data()),
                        "Converting the output again must be idempotent");
                CompoundTag exportedArm = findBlock(data, "create:mechanical_arm", 0);
                helper.assertTrue(exportedArm.getInt("ScrollValue") == 2 && exportedArm
                        .getList("InteractionPoints", Tag.TAG_COMPOUND).getCompound(0).contains("Pos", Tag.TAG_COMPOUND),
                        "Arm selection and legacy target coordinates must survive");
                assertListFilter(helper, findBlock(data, "create:stockpile_switch", 0));
                CompoundTag imported = SchematicDataCompatibility.importSchematic(data);
                ItemStack attributes = ItemStack.parseOptional(level.registryAccess(),
                        findBlock(imported, "create:stockpile_switch", 1).getCompound("Filter"));
                helper.assertTrue(attributes.get(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE)
                        == AttributeFilterWhitelistMode.WHITELIST_CONJ, "Attribute conjunction mode must round-trip");
                var filter = FilterItemStack.of(attributes);
                helper.assertTrue(filter.test(level, new ItemStack(Items.IRON_INGOT))
                        && !filter.test(level, new ItemStack(Items.OAK_PLANKS)),
                        "Attribute predicates and inversion must retain their actual behavior");
                Path fixtures = Path.of("..", "build", "cross-loader");
                Files.createDirectories(fixtures);
                Files.copy(exported.file(), fixtures.resolve("porter-" + target.key() + ".nbt"),
                        StandardCopyOption.REPLACE_EXISTING);
                byte[] previous = Files.readAllBytes(exported.file());
                var second = SchematicPorter.export(directory, source.fileName(), target);
                helper.assertTrue(!second.file().equals(exported.file())
                        && Arrays.equals(previous, Files.readAllBytes(exported.file())),
                        "Repeated exports must create new files without overwriting an earlier result");
            }
            helper.assertTrue(Arrays.equals(original, Files.readAllBytes(source.file()))
                    && originalData.equals(NbtIo.readCompressed(source.file(), NbtAccounter.unlimitedHeap())),
                    "The original schematic must remain byte-for-byte unchanged");
        } finally {
            cleanup(directory);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fabricFluidThresholdUsesDroplets(GameTestHelper helper) throws IOException {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        ThresholdSwitchBlockEntity threshold = configureSwitch(helper, pos, ItemStack.EMPTY, true);
        threshold.onWhenAbove = 2000;
        threshold.offWhenBelow = 500;
        CompoundTag safe = threshold.saveWithFullMetadata(helper.getLevel().registryAccess());
        safe.putString("id", "create:stockpile_switch");
        helper.assertTrue(safe.getString(SchematicPorter.THRESHOLD_UNIT).equals("millibuckets"),
                "A real tank observer must export its fluid-unit metadata");
        CompoundTag source = structure(safe);
        Path sharedFixtures = Path.of("../build/shareable-fixtures");
        Files.createDirectories(sharedFixtures);
        NbtIo.writeCompressed(SchematicDataCompatibility.exportSchematic(source),
                sharedFixtures.resolve("neoforge-shared-fluid.nbt"));
        CompoundTag fabric = SchematicPorter.convert(source, Target.FABRIC_1_20_1).data();
        Path fixtures = Path.of("..", "build", "cross-loader");
        Files.createDirectories(fixtures);
        NbtIo.writeCompressed(fabric, fixtures.resolve("porter-fluid-fabric.nbt"));
        CompoundTag copied = findBlock(fabric, "create:stockpile_switch", 0);
        helper.assertTrue(copied.getInt("OnAboveAmount") == 162000 && copied.getInt("OffBelowAmount") == 40500,
                "Fabric needs 81 droplets per millibucket");
        helper.assertTrue(fabric.equals(SchematicPorter.convert(fabric, Target.FABRIC_1_20_1).data()),
                "A second Fabric export must not multiply fluid thresholds again");
        CompoundTag forge = SchematicPorter.convert(fabric, Target.FORGE_1_20_1).data();
        helper.assertTrue(findBlock(forge, "create:stockpile_switch", 0).getInt("OnAboveAmount") == 2000,
                "Re-targeting a Fabric export to Forge must restore millibuckets");
        helper.assertTrue(findBlock(SchematicDataCompatibility.importSchematic(fabric), "create:stockpile_switch", 0)
                .getInt("OffBelowAmount") == 500, "NeoForge imports must normalize the exported Fabric fluid units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void porterReportsUnsupportedDataWithoutTouchingSource(GameTestHelper helper) throws IOException {
        CompoundTag machine = new CompoundTag();
        machine.putString("id", "toms_storage:ts.level_emitter");
        machine.putByte("Count", (byte) 17);
        machine.putInt("count", 23);
        CompoundTag filter = (CompoundTag) listFilter().save(helper.getLevel().registryAccess());
        filter.getCompound("components").putInt("minecraft:custom_model_data", 42);
        machine.put("Filter", filter);
        machine.putString("UnknownAddonSetting", "keep me");
        CompoundTag source = structure(machine);
        CompoundTag before = source.copy();
        var converted = SchematicPorter.convert(source, Target.FORGE_1_20_1);
        CompoundTag copied = findBlock(converted.data(), "toms_storage:ts.level_emitter", 0);
        helper.assertTrue(converted.notices().size() == 1
                && converted.notices().getFirst().detail().equals("minecraft:custom_model_data"),
                "An unsupported item component must appear in the export report");
        helper.assertTrue(copied.getByte("Count") == 17 && copied.getInt("count") == 23
                && copied.getString("UnknownAddonSetting").equals("keep me"),
                "Block entity Count fields and unknown settings must not be treated as item stacks");
        helper.assertTrue(source.equals(before), "Conversion must not mutate any source data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void porterRejectsInvalidFilesAndUnsupportedRules(GameTestHelper helper) throws IOException {
        Path directory = Files.createTempDirectory(Path.of("schematics"), "porter-invalid-");
        try {
            NbtIo.writeCompressed(new CompoundTag(), directory.resolve("invalid.nbt"));
            expectFailure(helper, () -> SchematicPorter.export(directory, "invalid.nbt", Target.FORGE_1_20_1));
            expectFailure(helper, () -> SchematicPorter.export(directory, "../invalid.nbt", Target.FORGE_1_20_1));
            expectFailure(helper, () -> SchematicPorter.export(directory, "missing.nbt", Target.FABRIC_1_20_1));
            CompoundTag machine = new CompoundTag();
            machine.putString("id", "create:stockpile_switch");
            CompoundTag filter = (CompoundTag) attributeFilter().save(helper.getLevel().registryAccess());
            filter.getCompound("components").getList("create:attribute_filter_matched_attributes", Tag.TAG_COMPOUND)
                    .getCompound(0).getCompound("attribute").putString("type", "unknown:custom_rule");
            machine.put("Filter", filter);
            expectFailure(helper, () -> SchematicPorter.convert(structure(machine), Target.FORGE_1_20_1));
            try (var files = Files.list(directory)) {
                helper.assertTrue(files.count() == 1, "Rejected conversions must not leave partial output files");
            }
        } finally {
            cleanup(directory);
        }
        helper.succeed();
    }

    private static ThresholdSwitchBlockEntity configureSwitch(GameTestHelper helper, BlockPos pos,
                                                               ItemStack filter, boolean fluid) {
        var level = helper.getLevel();
        var state = block("stockpile_switch").defaultBlockState();
        level.setBlock(pos.relative(ThresholdSwitchBlock.getTargetDirection(state)),
                fluid ? block("fluid_tank").defaultBlockState() : Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        var threshold = (ThresholdSwitchBlockEntity) level.getBlockEntity(pos);
        threshold.onWhenAbove = 1296;
        threshold.offWhenBelow = 432;
        threshold.inStacks = true;
        threshold.setInverted(true);
        threshold.getBehaviour(FilteringBehaviour.TYPE).setFilter(filter);
        threshold.updateCurrentLevel();
        return threshold;
    }

    private static ItemStack listFilter() {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        iron.set(DataComponents.CUSTOM_NAME, Component.literal("Porter sample"));
        ItemStack styled = new ItemStack(Items.IRON_INGOT);
        styled.set(DataComponents.CUSTOM_NAME, Component.literal("Styled sample").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        styled.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Line one"),
                Component.literal("Line two").withStyle(ChatFormatting.ITALIC))));
        ItemStack filter = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:filter")));
        filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, iron, ItemStack.EMPTY, ItemStack.EMPTY, styled)));
        filter.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
        filter.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, true);
        return filter;
    }

    private static ItemStack attributeFilter() {
        ItemStack filter = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:attribute_filter")));
        filter.set(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE, AttributeFilterWhitelistMode.WHITELIST_CONJ);
        filter.set(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES, List.of(
                new ItemAttributeEntry(new AddedByAttribute("minecraft"), false),
                new ItemAttributeEntry(new InTagAttribute(TagKey.create(Registries.ITEM,
                        ResourceLocation.parse("minecraft:planks"))), true),
                new ItemAttributeEntry(AllItemAttributeTypes.ENCHANTED.createAttribute(), true)));
        return filter;
    }

    private static void assertListFilter(GameTestHelper helper, CompoundTag threshold) {
        helper.assertTrue(threshold.getInt("OnAboveAmount") == 1296 && threshold.getInt("OffBelowAmount") == 432
                && threshold.getBoolean("InStacks") && threshold.getBoolean("Inverted"),
                "Threshold limits, display and inversion must survive in the actual file");
        CompoundTag filter = threshold.getCompound("Filter");
        CompoundTag settings = filter.getCompound("tag");
        CompoundTag entry = settings.getCompound("Items").getList("Items", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(filter.getByte("Count") == 1 && !filter.contains("components") && !filter.contains("count")
                && settings.getBoolean("Blacklist") && settings.getBoolean("RespectNBT")
                && entry.getInt("Slot") == 2 && entry.getByte("Count") == 1
                && entry.getCompound("tag").getCompound("display").getString("Name").contains("Porter sample"),
                "Legacy filter NBT must retain its sparse slot, nested item data and matching options");
    }

    private static CompoundTag structure(CompoundTag blockEntity) {
        CompoundTag structure = new CompoundTag();
        structure.put("size", ints(1, 1, 1));
        CompoundTag state = new CompoundTag();
        state.putString("Name", "create:stockpile_switch");
        ListTag palette = new ListTag();
        palette.add(state);
        structure.put("palette", palette);
        CompoundTag block = new CompoundTag();
        block.put("pos", ints(0, 0, 0));
        block.putInt("state", 0);
        block.put("nbt", blockEntity);
        ListTag blocks = new ListTag();
        blocks.add(block);
        structure.put("blocks", blocks);
        structure.putInt("DataVersion", 3955);
        return structure;
    }

    private static ListTag ints(int... values) {
        ListTag result = new ListTag();
        for (int value : values)
            result.add(net.minecraft.nbt.IntTag.valueOf(value));
        return result;
    }

    private static CompoundTag findBlock(CompoundTag structure, String id, int index) {
        for (Tag entry : structure.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag data = ((CompoundTag) entry).getCompound("nbt");
            if (data.getString("id").equals(id) && index-- == 0)
                return data;
        }
        throw new IllegalStateException("Missing block: " + id);
    }

    private static void cleanup(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : files.toList())
                Files.delete(file);
        }
        Files.delete(directory);
    }

    private static void expectFailure(GameTestHelper helper, IoAction action) {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        helper.fail("Conversion should have failed without creating an export");
    }

    @FunctionalInterface
    private interface IoAction { void run() throws IOException; }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
