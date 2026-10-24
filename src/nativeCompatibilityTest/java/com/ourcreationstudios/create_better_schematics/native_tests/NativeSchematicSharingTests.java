package com.ourcreationstudios.create_better_schematics.native_tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("better_schematics_native_tests")
@PrefixGameTestTemplate(false)
public final class NativeSchematicSharingTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeCreateLoadsSharedFile(GameTestHelper helper) throws Exception {
        helper.assertTrue(!ModList.get().isLoaded("create_better_schematics"),
                "Better Schematics and its mixins must be absent from this server");
        CompoundTag data = fixture();
        helper.assertTrue(data.getInt("DataVersion") == SharedConstants.getCurrentVersion().getDataVersion().getVersion(),
                "The file uses the recipient's native Minecraft version");
        helper.assertTrue(data.getCompound("CreateBetterSchematics").getInt("FormatVersion") == 1,
                "This is a real file exported with Better Schematics");
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        place(helper, data, anchor);
        BlockPos armPos = position(data, "create:mechanical_arm").offset(anchor);
        BlockPos depotPos = position(data, "create:depot").offset(anchor);
        BlockPos switchPos = position(data, "create:stockpile_switch").offset(anchor);
        helper.runAfterDelay(2, () -> {
            ArmBlockEntity arm = (ArmBlockEntity) helper.getLevel().getBlockEntity(armPos);
            helper.assertTrue(arm != null, "Stock Create places the mechanical arm");
            try {
                var field = ArmBlockEntity.class.getDeclaredField("outputs");
                field.setAccessible(true);
                @SuppressWarnings("unchecked") List<ArmInteractionPoint> outputs = (List<ArmInteractionPoint>) field.get(arm);
                helper.assertTrue(outputs.size() == 1 && outputs.getFirst().getPos().equals(depotPos),
                        "Stock Create restores arm destinations without the addon");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
            assertSwitch(helper, (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(switchPos));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void nativeCannonReadsEmbeddedSettings(GameTestHelper helper) throws Exception {
        helper.assertTrue(!ModList.get().isLoaded("create_better_schematics"), "No addon placement code is loaded");
        CompoundTag data = fixture();
        CompoundTag switchData = blockData(data, "create:stockpile_switch");
        BlockPos target = helper.absolutePos(new BlockPos(2, 2, 2));
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        var state = registry.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.parse("create:stockpile_switch"))).value().defaultBlockState();
        helper.getLevel().setBlock(target.relative(ThresholdSwitchBlock.getTargetDirection(state)),
                Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        LaunchedItem flight = new LaunchedItem.ForBlockState(target.above(3), target,
                new ItemStack(state.getBlock()), state, switchData.copy());
        CompoundTag savedFlight = flight.serializeNBT(helper.getLevel().registryAccess());
        savedFlight.putInt("TicksLeft", 0);
        LaunchedItem restored = LaunchedItem.fromNBT(savedFlight, helper.getLevel().registryAccess(), registry);
        helper.assertTrue(restored.update(helper.getLevel()), "The unmodified cannon places the shared configuration");
        assertSwitch(helper, (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(target));
        helper.succeed();
    }

    private static void assertSwitch(GameTestHelper helper, ThresholdSwitchBlockEntity threshold) {
        helper.assertTrue(threshold != null, "Stock Create places the threshold switch");
        threshold.updateCurrentLevel();
        helper.assertTrue(threshold.onWhenAbove == 1296 && threshold.offWhenBelow == 432,
                "Native threshold amounts survive without the addon");
        helper.assertTrue(threshold.inStacks && threshold.isInverted(), "Native display mode and inversion survive");
        FilteringBehaviour filtering = threshold.getBehaviour(FilteringBehaviour.TYPE);
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Calibration sample"));
        helper.assertTrue(!filtering.test(namedIron) && filtering.test(new ItemStack(Items.IRON_INGOT))
                        && filtering.test(new ItemStack(Items.GOLD_INGOT)),
                "Native list filters preserve entries, names and deny-list behavior");
        helper.assertTrue(Boolean.TRUE.equals(filtering.getFilter().get(AllDataComponents.FILTER_ITEMS_RESPECT_NBT)),
                "Respect Data remains enabled without Better Schematics");
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void missingWireAddonDoesNotBlockTheBuild(GameTestHelper helper) throws IOException {
        helper.assertTrue(!ModList.get().isLoaded("create_better_schematics") && !ModList.get().isLoaded("powergrid"),
                "Neither Better Schematics nor the wire addon is installed on the recipient");
        CompoundTag data = fixture();
        CompoundTag wires;
        try (var stream = NativeSchematicSharingTests.class.getResourceAsStream("/fixtures/powergrid-wires.nbt")) {
            if (stream == null) throw new IOException("Missing captured Power Grid wire fixture");
            wires = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        }
        ListTag entities = wires.getList("entities", Tag.TAG_COMPOUND);
        helper.assertTrue(!entities.isEmpty(), "The test includes actual exported addon wire entities");
        data.put("entities", entities.copy());
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        place(helper, data, anchor);
        BlockPos switchPos = position(data, "create:stockpile_switch").offset(anchor);
        assertSwitch(helper, (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(switchPos));
        helper.succeed();
    }

    private static CompoundTag fixture() throws IOException {
        try (var stream = NativeSchematicSharingTests.class.getResourceAsStream("/fixtures/neoforge-native.nbt")) {
            if (stream == null) throw new IOException("Missing real shared schematic fixture");
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        }
    }

    private static void place(GameTestHelper helper, CompoundTag data, BlockPos anchor) throws IOException {
        String owner = "native-sharing-" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("shared.nbt");
        NbtIo.writeCompressed(data, file);
        byte[] before = Files.readAllBytes(file);
        try {
            ItemStack blueprint = SchematicItem.create(helper.getLevel(), "shared.nbt", owner);
            var template = SchematicItem.loadSchematic(helper.getLevel(), blueprint);
            helper.assertTrue(template.placeInWorld(helper.getLevel(), anchor, anchor,
                    new StructurePlaceSettings().setKnownShape(true), helper.getLevel().getRandom(), Block.UPDATE_CLIENTS),
                    "Unmodified Create loads and places the enhanced schematic");
            helper.assertTrue(Arrays.equals(before, Files.readAllBytes(file)), "Stock Create does not rewrite the shared file");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static BlockPos position(CompoundTag data, String id) {
        for (Tag value : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) value;
            // Schematics omit block-entity IDs for plain blocks such as the depot.
            int state = block.getInt("state");
            String blockId = data.getList("palette", Tag.TAG_COMPOUND).getCompound(state).getString("Name");
            if (blockId.equals(id)) {
                ListTag pos = block.getList("pos", Tag.TAG_INT);
                return new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            }
        }
        throw new IllegalStateException("Missing fixture block: " + id);
    }

    private static CompoundTag blockData(CompoundTag data, String id) {
        for (Tag value : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) value;
            if (block.getCompound("nbt").getString("id").equals(id)) return block.getCompound("nbt");
        }
        throw new IllegalStateException("Missing fixture block data: " + id);
    }
}
