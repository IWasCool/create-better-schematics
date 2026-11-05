package com.ourcreationstudios.create_better_schematics.gametest;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public final class ChainDriveGameTests {
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutCreative(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.NONE, Mirror.NONE, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutRotatedCreative(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.CLOCKWISE_90, Mirror.NONE, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutMirroredCreative(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutCannon(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.NONE, Mirror.NONE, true);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutRotatedCannon(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.CLOCKWISE_90, Mirror.NONE, true);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void processorLayoutMirroredCannon(GameTestHelper helper) throws Exception {
        processorLayout(helper, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK, true);
    }

    private static void processorLayout(GameTestHelper helper, Rotation rotation, Mirror mirror, boolean cannon) throws Exception {
        var level = helper.getLevel();
        String owner = "chain_processor_" + UUID.randomUUID();
        var directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        var file = directory.resolve("processor.nbt");
        try {
            try (var input = ChainDriveGameTests.class.getResourceAsStream("/fixtures/twisted-chain-processor.nbt")) {
                helper.assertTrue(input != null, "Processor chain fixture must exist");
                Files.copy(input, file);
            }
            CompoundTag data = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            var palette = data.getList("palette", Tag.TAG_COMPOUND);
            BlockPos anchor = helper.absolutePos(new BlockPos(7, 1, 5));
            ItemStack blueprint = SchematicItem.create(level, file.getFileName().toString(), owner);
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, rotation);
            blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirror);
            var settings = SchematicItem.getSettings(blueprint);
            Map<BlockPos, BlockState> expected = new LinkedHashMap<>();
            for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag block = (CompoundTag) entry;
                BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state")));
                if (!(state.getBlock() instanceof ChainDriveBlock))
                    continue;
                var coords = block.getList("pos", Tag.TAG_INT);
                BlockPos pos = anchor.offset(StructureTemplate.calculateRelativePosition(settings,
                        new BlockPos(coords.getInt(0), coords.getInt(1), coords.getInt(2))));
                Axis shaft = state.getValue(ChainDriveBlock.AXIS);
                Axis connectionAxis = state.getValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE)
                        ? (shaft == Axis.X ? Axis.Y : Axis.X) : (shaft == Axis.Z ? Axis.Y : Axis.Z);
                Direction connection = Direction.fromAxisAndDirection(connectionAxis, AxisDirection.POSITIVE);
                expected.put(pos, transform(state, rotation, mirror, connection, rotation.rotate(mirror.mirror(connection))));
            }
            helper.assertTrue(expected.size() == 6, "Processor fixture must contain all six interleaved chain drives");
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, cannon);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Processor fixture must load");
            if (cannon) {
                BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 10));
                level.setBlock(cannonPos.below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
                var supply = (BarrelBlockEntity) level.getBlockEntity(cannonPos.below());
                Map<net.minecraft.world.item.Item, ItemStack> materials = new LinkedHashMap<>();
                while (printer.advanceCurrentPos()) {
                    var requirement = printer.getCurrentRequirement();
                    if (!requirement.isInvalid())
                        requirement.getRequiredItems().forEach(required -> materials.putIfAbsent(required.stack.getItem(), required.stack.copyWithCount(64)));
                }
                helper.assertTrue(materials.size() <= supply.getContainerSize(), "Processor materials must fit in the test supply");
                int slot = 0;
                for (ItemStack stack : materials.values())
                    supply.setItem(slot++, stack);
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:schematicannon"));
                level.setBlock(cannonPos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
                var machine = (SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
                machine.inventory.setStackInSlot(0, blueprint);
                machine.printer.loadSchematic(blueprint, level, true);
                machine.remainingFuel = 100;
                machine.skipMissing = true;
                machine.state = SchematicannonBlockEntity.State.RUNNING;
                machine.statusMsg = "running";
                helper.startSequence()
                        .thenWaitUntil(() -> helper.assertTrue(machine.state == SchematicannonBlockEntity.State.STOPPED
                                && machine.flyingBlocks.isEmpty(), "Cannon must finish the entire processor"))
                        .thenIdle(5)
                        .thenExecute(() -> expected.forEach((pos, state) -> assertState(helper, pos, state, "Completed cannon build")))
                        .thenSucceed();
            } else {
                while (printer.advanceCurrentPos())
                    printer.handleCurrentTarget((pos, state, entity) -> {
                        if (!state.isAir())
                            BlockHelper.placeSchematicBlock(level, state, pos, null, BlockHelper.prepareBlockEntityData(level, state, entity));
                    }, (pos, entity) -> helper.fail("Processor fixture must not create entities"));
                helper.runAfterDelay(5, () -> {
                    expected.forEach((pos, state) -> assertState(helper, pos, state, "Processor creative placement"));
                    helper.succeed();
                });
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @GameTestGenerator
    public static List<TestFunction> twistedChainDrives() {
        List<TestFunction> tests = new ArrayList<>();
        for (Axis connection : Axis.values())
            for (Axis shaft : Axis.values()) {
                if (connection == shaft)
                    continue;
                for (Rotation rotation : Rotation.values())
                    for (Mirror mirror : Mirror.values())
                        for (boolean cannon : List.of(false, true)) {
                            String name = ("twisted_chain_" + connection + "_" + shaft + "_" + rotation + "_" + mirror
                                    + (cannon ? "_cannon" : "_creative")).toLowerCase(Locale.ROOT);
                            tests.add(new TestFunction("twistedChainDrives", name, "create_better_schematics:empty",
                                    Rotation.NONE, 100, 0, true, helper -> {
                                try {
                                    transfer(helper, connection, shaft, rotation, mirror, cannon);
                                } catch (Exception e) {
                                    throw new IllegalStateException(name + ": " + e.getMessage(), e);
                                }
                            }));
                        }
            }
        return tests;
    }

    private static void transfer(GameTestHelper helper, Axis connectionAxis, Axis firstShaft, Rotation rotation,
            Mirror mirror, boolean cannon) throws Exception {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos anchor = helper.absolutePos(new BlockPos(8, 1, 8));
        Direction connection = Direction.fromAxisAndDirection(connectionAxis, AxisDirection.POSITIVE);
        Axis otherShaft = java.util.Arrays.stream(Axis.values())
                .filter(axis -> axis != connectionAxis && axis != firstShaft).findFirst().orElseThrow();
        Map<BlockPos, BlockState> source = new LinkedHashMap<>();
        for (int index = 0; index < 3; index++) {
            String id = index == 1 ? "adjustable_chain_gearshift" : "encased_chain_drive";
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
            Axis shaft = index == 1 ? otherShaft : firstShaft;
            var part = index == 0 ? ChainDriveBlock.Part.START : index == 2 ? ChainDriveBlock.Part.END : ChainDriveBlock.Part.MIDDLE;
            BlockState state = block.defaultBlockState().setValue(ChainDriveBlock.AXIS, shaft)
                    .setValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE, alongFirst(shaft, connectionAxis))
                    .setValue(ChainDriveBlock.PART, part);
            BlockPos pos = origin.relative(connection, index);
            source.put(pos, state);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        assertConnected(helper, new ArrayList<>(source.keySet()), connection);
        String owner = "twisted_chain_" + UUID.randomUUID();
        var directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "chain", false, level, origin, origin.relative(connection, 2));
        helper.assertTrue(exported != null, "Twisted chain drive must export");
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, rotation);
            blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirror);
            var settings = SchematicItem.getSettings(blueprint);
            Direction targetConnection = rotation.rotate(mirror.mirror(connection));
            Map<BlockPos, BlockState> expected = new LinkedHashMap<>();
            for (var entry : source.entrySet()) {
                BlockPos pos = anchor.offset(StructureTemplate.calculateRelativePosition(settings, entry.getKey().subtract(origin)));
                expected.put(pos, transform(entry.getValue(), rotation, mirror, connection, targetConnection));
            }
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, cannon);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Create must load the saved twisted chain");
            int[] placed = {0};
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, entity) -> {
                    if (state.isAir())
                        return;
                    helper.assertTrue(state.equals(expected.get(pos)),
                            "Printer changed the saved twist: expected " + expected.get(pos) + ", got " + state);
                    CompoundTag safe = BlockHelper.prepareBlockEntityData(level, state, entity);
                    if (cannon) {
                        var flight = new LaunchedItem.ForBlockState(pos.above(3), pos, new ItemStack(state.getBlock()), state, safe);
                        CompoundTag saved = flight.serializeNBT(level.registryAccess());
                        saved.putInt("TicksLeft", 0);
                        var restored = LaunchedItem.fromNBT(saved, level.registryAccess(), level.holderLookup(Registries.BLOCK));
                        helper.assertTrue(restored.update(level), "Reloaded chain-drive projectile must place its block");
                    } else {
                        BlockHelper.placeSchematicBlock(level, state, pos, null, safe);
                    }
                    assertState(helper, pos, expected.get(pos), "Placement");
                    placed[0]++;
                }, (pos, entity) -> helper.fail("Chain drives must not create schematic entities"));
            helper.assertTrue(placed[0] == 3, "Both chain drives and the twisted gearshift must be placed");
            for (var entry : expected.entrySet()) {
                assertState(helper, entry.getKey(), entry.getValue(), "Completed build");
                var entity = level.getBlockEntity(entry.getKey());
                helper.assertTrue(entity != null, "Pasted chain must have a block entity");
                entity.loadWithComponents(entity.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            }
            assertConnected(helper, new ArrayList<>(expected.keySet()), targetConnection);
            helper.runAfterDelay(5, () -> {
                expected.forEach((pos, state) -> assertState(helper, pos, state, "After ticking and reload"));
                source.forEach((pos, state) -> assertState(helper, pos, state, "Original after copying"));
                assertConnected(helper, new ArrayList<>(expected.keySet()), targetConnection);
                helper.succeed();
            });
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    private static BlockState transform(BlockState state, Rotation rotation, Mirror mirror, Direction connection,
            Direction targetConnection) {
        Direction shaft = Direction.fromAxisAndDirection(state.getValue(ChainDriveBlock.AXIS), AxisDirection.POSITIVE);
        Axis targetShaft = rotation.rotate(mirror.mirror(shaft)).getAxis();
        var part = state.getValue(ChainDriveBlock.PART);
        if (connection.getAxisDirection() != targetConnection.getAxisDirection()) {
            if (part == ChainDriveBlock.Part.START)
                part = ChainDriveBlock.Part.END;
            else if (part == ChainDriveBlock.Part.END)
                part = ChainDriveBlock.Part.START;
        }
        return state.setValue(ChainDriveBlock.AXIS, targetShaft)
                .setValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE, alongFirst(targetShaft, targetConnection.getAxis()))
                .setValue(ChainDriveBlock.PART, part);
    }

    private static boolean alongFirst(Axis shaft, Axis connection) {
        return shaft == Axis.X ? connection == Axis.Y : connection == Axis.X;
    }

    private static void assertState(GameTestHelper helper, BlockPos pos, BlockState expected, String stage) {
        BlockState actual = helper.getLevel().getBlockState(pos);
        helper.assertTrue(actual.equals(expected), stage + " changed chain orientation at " + pos
                + ": expected " + expected + ", got " + actual);
    }

    private static void assertConnected(GameTestHelper helper, List<BlockPos> positions, Direction direction) {
        for (int index = 1; index < positions.size(); index++) {
            BlockState first = helper.getLevel().getBlockState(positions.get(index - 1));
            BlockState second = helper.getLevel().getBlockState(positions.get(index));
            helper.assertTrue(ChainDriveBlock.areBlocksConnected(first, second, direction)
                            && ChainDriveBlock.areBlocksConnected(second, first, direction.getOpposite()),
                    "Twisted chain neighbors must stay connected in both directions");
        }
    }
}
