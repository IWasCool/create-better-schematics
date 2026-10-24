package com.ourcreationstudios.create_better_schematics.compat_gametest;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.JunctionWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod("better_schematics_compat_tests")
@GameTestHolder("create_better_schematics")
@PrefixGameTestTemplate(false)
public class PowerGridGameTests {
    @GameTest(template = "empty")
    public static void cannonCannotLaunchWireWhenRealExtractionFails(GameTestHelper helper)
            throws IOException, ReflectiveOperationException {
        var level = helper.getLevel();
        String owner = "powergrid_failed_extraction_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("wire.nbt");
        try {
            try (var source = PowerGridGameTests.class.getResourceAsStream("/fixtures/forge-powergrid-wires.nbt")) {
                helper.assertTrue(source != null, "Wire material guard fixture must exist");
                Files.copy(source, file);
            }
            ItemStack blueprint = SchematicItem.create(level, "wire.nbt", owner);
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, helper.absolutePos(new BlockPos(6, 2, 6)));
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 8));
            level.setBlock(cannonPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:schematicannon"))
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
            var cannon = (com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
            cannon.inventory.setStackInSlot(0, blueprint);
            cannon.printer.loadSchematic(blueprint, level, true);
            while (cannon.printer.advanceCurrentPos())
                if (cannon.printer.getPrintStage() == SchematicPrinter.PrintStage.ENTITIES)
                    break;
            var wireItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire"));
            var requirement = cannon.printer.getCurrentRequirement();
            helper.assertTrue(!requirement.isInvalid() && !requirement.isEmpty()
                            && requirement.getRequiredItems().getFirst().stack.is(wireItem),
                    "The cannon must be waiting on a real wire requirement");
            var refusingInventory = new ItemStackHandler(1) {
                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return simulate ? super.extractItem(slot, amount, true) : ItemStack.EMPTY;
                }
            };
            refusingInventory.setStackInSlot(0, new ItemStack(wireItem, 64));
            cannon.attachedInventories.add(refusingInventory);
            cannon.missingItem = requirement.getRequiredItems().getFirst().stack.copy();
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity.State.PAUSED;
            cannon.statusMsg = "missingBlock";
            var tickPrinter = cannon.getClass().getDeclaredMethod("tickPrinter");
            tickPrinter.setAccessible(true);
            tickPrinter.invoke(cannon);
            helper.assertTrue(cannon.remainingFuel == 99,
                    "The simulated inventory must pass the preflight and reach the launch attempt");
            helper.assertTrue(cannon.flyingBlocks.isEmpty() && refusingInventory.getStackInSlot(0).getCount() == 64,
                    "A handler that refuses real extraction must never produce an unpaid wire projectile");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 360)
    public static void cannonCannotDuplicateMultiStackWireMaterials(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.east(4);
        connector(helper, first);
        connector(helper, second);
        var wireItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire"));
        var connectorItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire_connector"));
        int wireCost = 130;
        var original = HangingWireEntity.create(level, new BlockWireEndpoint(first, 0), new BlockWireEndpoint(second, 0),
                new ItemStack(wireItem, wireCost), null);
        level.addFreshEntity(original);
        String owner = "powergrid_no_dupe_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "wire_cost", false, level, first.below(), first.offset(4, 1, 1));
        helper.assertTrue(exported != null, "Multi-stack wire fixture must export");
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            BlockPos anchor = helper.absolutePos(new BlockPos(6, 2, 6));
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 8));
            level.setBlock(cannonPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:schematicannon"))
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
            var cannon = (com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
            level.setBlock(cannonPos.below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(cannonPos.below());
            supply.setItem(0, new ItemStack(Items.STONE, 2));
            supply.setItem(1, new ItemStack(connectorItem, 2));
            supply.setItem(2, new ItemStack(wireItem, 64));
            cannon.inventory.setStackInSlot(0, blueprint);
            cannon.printer.loadSchematic(blueprint, level, true);
            cannon.updateChecklist();
            helper.assertTrue(cannon.checklist.getRequiredAmount(wireItem) == wireCost,
                    "The checklist must include the entire multi-stack wire cost");
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            var destination = new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(anchor.offset(-1, -1, -1)),
                    Vec3.atLowerCornerOf(anchor.offset(5, 4, 2)));
            helper.runAfterDelay(80, () -> {
                var copied = level.getEntitiesOfClass(HangingWireEntity.class, destination);
                helper.assertTrue(copied.isEmpty() && cannon.flyingBlocks.isEmpty(),
                        "A partial stack must not launch or place a wire worth 130 items");
                helper.assertTrue(supply.getItem(2).getCount() == 64 && cannon.missingItem != null
                                && cannon.missingItem.is(wireItem) && cannon.remainingFuel == 96,
                        "Waiting for the full wire cost must not consume partial supplies or wire fuel");
                supply.setItem(3, new ItemStack(wireItem, 64));
                supply.setItem(4, new ItemStack(wireItem, 2));
                helper.succeedWhen(() -> {
                    var placed = level.getEntitiesOfClass(HangingWireEntity.class, destination);
                    helper.assertTrue(placed.size() == 1, "Supplying all 130 items must place one wire");
                    helper.assertTrue(supply.getItem(2).isEmpty() && supply.getItem(3).isEmpty() && supply.getItem(4).isEmpty(),
                            "All 130 wire materials must be consumed before placement");
                    helper.assertTrue(cannon.remainingFuel == 95, "One completed wire must cost one fuel shot");
                    var wire = placed.getFirst();
                    helper.assertTrue(wire.getWireCount() == wireCost, "The placed wire must retain its paid material count");
                    wire.kill();
                    wire.kill();
                    int drops = level.getEntitiesOfClass(ItemEntity.class, destination).stream()
                            .filter(item -> item.getItem().is(wireItem)).mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(drops == wireCost,
                            "Breaking the wire must return exactly the 130 supplied items, without any gain");
                });
            });
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void cannonBuildsWireAndConsumesMaterials(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.east(4);
        connector(helper, first);
        connector(helper, second);
        var wireItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire"));
        var original = HangingWireEntity.create(level, new BlockWireEndpoint(first, 0), new BlockWireEndpoint(second, 0),
                new ItemStack(wireItem, 6), null);
        level.addFreshEntity(original);
        String owner = "powergrid_cannon_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "wires", false, level, first.below(), first.offset(4, 1, 1));
        helper.assertTrue(exported != null, "Cannon fixture must export");
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            BlockPos anchor = helper.absolutePos(new BlockPos(6, 2, 6));
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 8));
            level.setBlock(cannonPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:schematicannon"))
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
            var cannon = (com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
            level.setBlock(cannonPos.below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(cannonPos.below());
            supply.setItem(0, new ItemStack(Items.STONE, 2));
            supply.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire_connector")), 2));
            supply.setItem(2, new ItemStack(wireItem, 6));
            cannon.inventory.setStackInSlot(0, blueprint);
            cannon.printer.loadSchematic(blueprint, level, true);
            cannon.updateChecklist();
            helper.assertTrue(cannon.checklist.getRequiredAmount(wireItem) == 6, "Checklist must include all wire materials");
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            helper.succeedWhen(() -> {
                var copied = level.getEntitiesOfClass(HangingWireEntity.class,
                        new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(anchor.offset(-1, -1, -1)), Vec3.atLowerCornerOf(anchor.offset(5, 4, 2))));
                helper.assertTrue(copied.size() == 1 && copied.getFirst().getWire() != null,
                        "Cannon must build a functional wire after placing its connectors");
                helper.assertTrue(supply.getItem(2).isEmpty(), "Cannon must consume exactly six wire items");
                helper.assertTrue(supply.getItem(0).isEmpty() && supply.getItem(1).isEmpty(), "Cannon must consume its backing blocks and connectors");
                helper.assertTrue(cannon.remainingFuel == 95, "Four blocks and one wire must consume five units of fuel");
            });
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    @GameTest(template = "empty")
    public static void junctionCopiesKeepTheirOwnNetwork(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.east(4);
        connector(helper, first);
        connector(helper, second);
        var material = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire"));
        JunctionWireEndpoint junction = new JunctionWireEndpoint(Vec3.atLowerCornerOf(first.east(2)));
        BlockWireEntity a = BlockWireEntity.create(level, Vec3.atLowerCornerOf(first), new ItemStack(material, 3),
                List.of(new BlockWireEntity.Point(Direction.EAST, 32)));
        a.setEndpoint1(new BlockWireEndpoint(first, 0));
        a.setEndpoint2(junction);
        BlockWireEntity b = BlockWireEntity.create(level, Vec3.atLowerCornerOf(first.east(2)), new ItemStack(material, 3),
                List.of(new BlockWireEntity.Point(Direction.EAST, 32)));
        b.setEndpoint1(junction);
        b.setEndpoint2(new BlockWireEndpoint(second, 0));
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        UUID originalId = junction.serialize().getUUID("Id");
        String owner = "powergrid_junction_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "junction", false, level, first.below(), first.offset(4, 1, 1));
        helper.assertTrue(exported != null, "Junction fixture must export");
        List<Entity> copies = new ArrayList<>();
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, helper.absolutePos(new BlockPos(9, 2, 6)));
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_90);
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, true);
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        BlockHelper.placeSchematicBlock(level, state, pos, new ItemStack(state.getBlock()),
                                BlockHelper.prepareBlockEntityData(level, state, be));
                }, (pos, entity) -> { copies.add(entity); level.addFreshEntity(entity); });
            helper.assertTrue(copies.size() == 2, "Both wires meeting at the junction must copy");
            List<CompoundTag> junctions = new ArrayList<>();
            for (Entity entity : copies) {
                CompoundTag data = entity.saveWithoutId(new CompoundTag());
                for (String endpoint : List.of("Endpoint1", "Endpoint2"))
                    if (data.getCompound(endpoint).getInt("Type") == 1)
                        junctions.add(data.getCompound(endpoint));
            }
            helper.assertTrue(junctions.size() == 2 && junctions.get(0).getUUID("Id").equals(junctions.get(1).getUUID("Id"))
                            && !junctions.get(0).getUUID("Id").equals(originalId),
                    "Copied wires must share a new junction, separate from the original factory");
            helper.runAfterDelay(8, () -> {
                for (Entity entity : copies)
                    helper.assertTrue(((WireEntity) entity).getWire() != null, "Both copied junction wires must conduct");
                helper.succeed();
            });
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    @GameTest(template = "empty")
    public static void partialSelectionExcludesExternalWires(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.east(4);
        connector(helper, first);
        connector(helper, second);
        var material = BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire"));
        var wire = HangingWireEntity.create(level, new BlockWireEndpoint(first, 0), new BlockWireEndpoint(second, 0),
                new ItemStack(material, 6), null);
        level.addFreshEntity(wire);
        var template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        template.fillFromWorld(level, first.below(), new BlockPos(2, 3, 2), true, null);
        ItemStack blueprint = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:schematic")));
        var settings = SchematicItem.getSettings(blueprint).setKnownShape(true);
        BlockPos anchor = helper.absolutePos(new BlockPos(7, 2, 7));
        template.placeInWorld(level, anchor, anchor, settings, level.getRandom(), Block.UPDATE_CLIENTS);
        helper.assertTrue(level.getEntitiesOfClass(HangingWireEntity.class,
                new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(anchor.offset(-2, -1, -2)), Vec3.atLowerCornerOf(anchor.offset(4, 4, 4)))).isEmpty(),
                "A partial selection must not reconnect a copied terminal to the original factory");
        helper.assertTrue(wire.isConnectedTo(first, 0) && wire.isConnectedTo(second, 0), "Partial copying must leave the source wire intact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hangingWireRotatedCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.NONE, false, Input.NATIVE);
    }

    @GameTest(template = "empty")
    public static void hangingWireMirroredCreativePlacement(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.FRONT_BACK, true, Input.NATIVE);
    }

    @GameTest(template = "empty")
    public static void actualForgeWireReturnsToNeoForge(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.NONE, false, Input.FORGE);
    }

    @GameTest(template = "empty")
    public static void legacyForgeWireWithoutSagStillImports(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.NONE, false, Input.LEGACY_FORGE);
    }

    private enum Input { NATIVE, FORGE, LEGACY_FORGE }

    private static void transfer(GameTestHelper helper, Mirror mirror, boolean solidWire, Input input) throws IOException {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.east(4);
        BlockPos origin = first.below();
        connector(helper, first);
        connector(helper, second);
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(solidWire
                ? "powergrid:insulated_copper_wire" : "powergrid:wire"));
        BaseWireEntity original;
        if (solidWire) {
            BlockWireEntity wire = BlockWireEntity.create(level, new BlockWireEndpoint(first, 0), new ItemStack(item, 6),
                    List.of(new BlockWireEntity.Point(Direction.EAST, 64)));
            wire.setEndpoint2(new BlockWireEndpoint(second, 0));
            original = wire;
        } else {
            original = HangingWireEntity.create(level, new BlockWireEndpoint(first, 0), new BlockWireEndpoint(second, 0),
                    new ItemStack(item, 6), null);
        }
        original.setColor(0x42a7db);
        if (!solidWire) {
            CompoundTag configured = original.saveWithoutId(new CompoundTag());
            configured.putFloat("PlacedLength", 5.75f);
            original.load(configured);
        }
        level.addFreshEntity(original);
        CompoundTag originalFirst = original.getEndpoint1().serialize();
        CompoundTag originalSecond = original.getEndpoint2().serialize();
        String owner = "powergrid_test_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "wires", false, level, origin, origin.offset(4, 2, 1));
        helper.assertTrue(exported != null, "Power Grid setup must export");
        try {
            if (input != Input.NATIVE) {
                String fixtureName = input == Input.FORGE ? "forge-powergrid-wires.nbt" : "forge-powergrid-wires-legacy.nbt";
                try (var fixture = PowerGridGameTests.class.getResourceAsStream("/fixtures/" + fixtureName)) {
                    helper.assertTrue(fixture != null, "Actual Forge wire export must exist");
                    Files.copy(fixture, exported.file(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (!solidWire) {
                Path crossLoader = Path.of("..", "build", "cross-loader");
                Files.createDirectories(crossLoader);
                Files.copy(exported.file(), crossLoader.resolve("neoforge-powergrid-wires.nbt"), StandardCopyOption.REPLACE_EXISTING);
            }
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            BlockPos anchor = helper.absolutePos(new BlockPos(9, 2, 6));
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_90);
            blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirror);
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, !solidWire);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Create must load the wire schematic");
            List<Entity> entities = new ArrayList<>();
            while (printer.advanceCurrentPos()) {
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        BlockHelper.placeSchematicBlock(level, state, pos, new ItemStack(state.getBlock()),
                                BlockHelper.prepareBlockEntityData(level, state, be));
                }, (pos, entity) -> entities.add(entity));
            }
            helper.assertTrue(entities.size() == 1 && entities.getFirst() instanceof BaseWireEntity,
                    "Schematic must retain its completed wire entity, got " + entities);
            BaseWireEntity copied = (BaseWireEntity) entities.getFirst();
            var settings = SchematicItem.getSettings(blueprint);
            BlockPos pastedFirst = anchor.offset(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                    .calculateRelativePosition(settings, first.subtract(origin)));
            BlockPos pastedSecond = anchor.offset(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                    .calculateRelativePosition(settings, second.subtract(origin)));
            assertEndpoints(helper, copied, pastedFirst, pastedSecond);
            helper.assertTrue(copied.getWireCount() == 6 && copied.getColor() == original.getColor(),
                    "Wire amount and color must survive schematic transfer");
            if (!solidWire)
                helper.assertTrue(Math.abs(copied.saveWithoutId(new CompoundTag()).getFloat("PlacedLength")
                                - (input == Input.LEGACY_FORGE ? (float) Math.sqrt(16 * 1.01) : 5.75f)) < .0001f,
                        "Hanging wire sag must survive schematic transfer, including the Forge return trip");
            else
                helper.assertTrue(copied.getColor() == 0x42a7db, "Painted insulated wire must retain its actual color");
            helper.assertTrue(!ItemRequirement.of(copied).isInvalid(), "Cannon must accept the copied wire's materials");
            if (solidWire) {
                BlockWireEntity solid = (BlockWireEntity) copied;
                helper.assertTrue(solid.segments.size() == 1 && solid.segments.getFirst().direction == Direction.NORTH
                                && solid.segments.getFirst().gridLength == 64,
                        "Solid wire routing must follow the mirror and rotation");
                helper.assertTrue(copied.level() == level, "Creative placement must bind wires to the real destination world");
                level.addFreshEntity(copied);
            } else {
                LaunchedItem flight = new LaunchedItem.ForEntity(anchor.above(3), copied.blockPosition(), new ItemStack(item, 6), copied);
                CompoundTag saved = flight.serializeNBT(level.registryAccess());
                saved.putInt("TicksLeft", 0);
                LaunchedItem restored = LaunchedItem.fromNBT(saved, level.registryAccess(), level.holderLookup(Registries.BLOCK));
                helper.assertTrue(restored.update(level), "Saved cannon flight must place its wire");
                copied = (BaseWireEntity) level.getEntity(copied.getUUID());
                helper.assertTrue(copied != null, "Cannon must spawn the reloaded wire");
                assertEndpoints(helper, copied, pastedFirst, pastedSecond);
            }
            BaseWireEntity placed = copied;
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(placed instanceof WireEntity wire && wire.getWire() != null,
                        "Copied wire must form a real electrical connection after its terminals initialize");
                helper.succeed();
            });
            helper.assertTrue(originalFirst.equals(original.getEndpoint1().serialize())
                            && originalSecond.equals(original.getEndpoint2().serialize()),
                    "Copying must leave the original wire connected to its original terminals");
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    private static void assertEndpoints(GameTestHelper helper, BaseWireEntity wire, BlockPos first, BlockPos second) {
        helper.assertTrue(wire.getEndpoint1() instanceof BlockWireEndpoint a && a.getPos().equals(first)
                        && wire.getEndpoint2() instanceof BlockWireEndpoint b && b.getPos().equals(second),
                "Pasted wires must connect to the rotated/mirrored copied terminals, got "
                        + wire.getEndpoint1() + " -> " + wire.getEndpoint2());
    }

    private static void connector(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos.south(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("powergrid:wire_connector"))
                .defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    @GameTest(template = "empty")
    public static void cannonRecognizesPowerGridWireMaterials(GameTestHelper helper) {
        BlockPos first = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos second = first.east(4);
        var connector = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("powergrid:wire_connector")).defaultBlockState();
        helper.getLevel().setBlock(first, connector, Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(second, connector, Block.UPDATE_CLIENTS);
        ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("powergrid:wire")), 4);
        HangingWireEntity wire = HangingWireEntity.create(helper.getLevel(), new BlockWireEndpoint(first, 0),
                new BlockWireEndpoint(second, 0), material, null);
        ItemRequirement required = ItemRequirement.of(wire);
        helper.assertTrue(!required.isInvalid() && !required.isEmpty(),
                "Schematicannon must recognize Power Grid wires instead of silently skipping them");
        helper.assertTrue(required.getRequiredItems().size() == 1 && required.getRequiredItems().getFirst().stack.getCount() == 4,
                "Cannon must require the wire's actual material count");
        helper.succeed();
    }
}
