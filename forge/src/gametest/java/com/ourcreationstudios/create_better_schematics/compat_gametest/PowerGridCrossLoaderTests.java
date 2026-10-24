package com.ourcreationstudios.create_better_schematics.compat_gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("better_schematics_tests")
public class PowerGridCrossLoaderTests {
    @GameTestGenerator
    public static List<TestFunction> powerGridTransfers() {
        if (!ModList.get().isLoaded("powergrid"))
            return List.of();
        List<TestFunction> tests = new ArrayList<>();
        for (Rotation rotation : List.of(Rotation.NONE, Rotation.CLOCKWISE_90))
            tests.add(new TestFunction("defaultBatch", "powergrid_neoforge_to_forge_" + rotation.name().toLowerCase(),
                    "better_schematics_tests:empty", Rotation.NONE, 100, 0, true, helper -> {
                        try {
                            transfer(helper, rotation);
                        } catch (IOException e) {
                            throw new IllegalStateException("Wire fixture transfer failed", e);
                        }
                    }));
        tests.add(new TestFunction("defaultBatch", "powergrid_cannon_cannot_duplicate_multistack_materials",
                "better_schematics_tests:empty", Rotation.NONE, 360, 0, true, helper -> {
                    try {
                        cannonCannotDuplicateMaterials(helper);
                    } catch (IOException e) {
                        throw new IllegalStateException("Wire material fixture failed", e);
                    }
                }));
        return tests;
    }

    private static void cannonCannotDuplicateMaterials(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        String owner = "pg_no_dupe_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("wire_cost.nbt");
        try {
            CompoundTag schematic;
            try (var fixture = PowerGridCrossLoaderTests.class.getResourceAsStream("/fixtures/neoforge-powergrid-wires.nbt")) {
                helper.assertTrue(fixture != null, "Actual NeoForge wire export must exist");
                schematic = NbtIo.readCompressed(fixture);
            }
            for (Tag entityTag : schematic.getList("entities", Tag.TAG_COMPOUND)) {
                CompoundTag entity = ((CompoundTag) entityTag).getCompound("nbt");
                if (entity.getString("id").equals("powergrid:hanging_wire"))
                    entity.getCompound("Item").putInt("Count", 130);
            }
            try (var output = Files.newOutputStream(file)) {
                NbtIo.writeCompressed(schematic, output);
            }
            BlockPos anchor = helper.absolutePos(new BlockPos(7, 2, 6));
            ItemStack blueprint = SchematicItem.create(level, "wire_cost.nbt", owner);
            blueprint.getOrCreateTag().put("Anchor", NbtUtils.writeBlockPos(anchor));
            blueprint.getOrCreateTag().putBoolean("Deployed", true);
            BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 8));
            level.setBlock(cannonPos, BuiltInRegistries.BLOCK.get(new ResourceLocation("create:schematicannon"))
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
            var cannon = (SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
            level.setBlock(cannonPos.below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (BarrelBlockEntity) level.getBlockEntity(cannonPos.below());
            var wireItem = BuiltInRegistries.ITEM.get(new ResourceLocation("powergrid:wire"));
            var connectorItem = BuiltInRegistries.ITEM.get(new ResourceLocation("powergrid:wire_connector"));
            supply.setItem(0, new ItemStack(Items.STONE, 2));
            supply.setItem(1, new ItemStack(connectorItem, 2));
            supply.setItem(2, new ItemStack(wireItem, 64));
            cannon.inventory.setStackInSlot(0, blueprint);
            cannon.printer.loadSchematic(blueprint, level, true);
            cannon.updateChecklist();
            helper.assertTrue(cannon.checklist.getRequiredAmount(wireItem) == 130,
                    "Forge must account for the complete 130-item wire cost");
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            var destination = new AABB(Vec3.atLowerCornerOf(anchor.offset(-1, -1, -1)),
                    Vec3.atLowerCornerOf(anchor.offset(5, 4, 2)));
            var wireType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("powergrid:hanging_wire"));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(level.getEntities((Entity) null, destination, entity -> entity.getType() == wireType).isEmpty()
                                && cannon.flyingBlocks.isEmpty(),
                        "Forge must not launch a 130-item wire using only 64 supplied items");
                helper.assertTrue(supply.getItem(2).getCount() == 64 && cannon.remainingFuel == 96,
                        "Forge must retain partial supplies while waiting for the full wire cost");
                supply.setItem(3, new ItemStack(wireItem, 64));
                supply.setItem(4, new ItemStack(wireItem, 2));
                helper.succeedWhen(() -> {
                    var wires = level.getEntities((Entity) null, destination, entity -> entity.getType() == wireType);
                    helper.assertTrue(wires.size() == 1, "Full payment must place exactly one Forge wire");
                    helper.assertTrue(supply.getItem(2).isEmpty() && supply.getItem(3).isEmpty() && supply.getItem(4).isEmpty()
                                    && cannon.remainingFuel == 95,
                            "Forge must consume all 130 wire items and one wire fuel shot");
                    Entity wire = wires.get(0);
                    wire.kill();
                    wire.kill();
                    int drops = level.getEntitiesOfClass(ItemEntity.class, destination).stream()
                            .filter(item -> item.getItem().is(wireItem)).mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(drops == 130, "Repeated wire removal must never return more than the 130 paid items");
                });
            });
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void transfer(GameTestHelper helper, Rotation rotation) throws IOException {
        var level = helper.getLevel();
        String owner = "pg_cross_loader_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("wires.nbt");
        try (var fixture = PowerGridCrossLoaderTests.class.getResourceAsStream("/fixtures/neoforge-powergrid-wires.nbt")) {
            helper.assertTrue(fixture != null, "Actual NeoForge wire export must exist");
            Files.copy(fixture, file);
        }
        Entity wire;
        BlockPos anchor = helper.absolutePos(new BlockPos(7, 2, 6));
        ItemStack blueprint;
        try {
            blueprint = SchematicItem.create(level, "wires.nbt", owner);
            blueprint.getOrCreateTag().put("Anchor", NbtUtils.writeBlockPos(anchor));
            blueprint.getOrCreateTag().putBoolean("Deployed", true);
            blueprint.getOrCreateTag().putString("Rotation", rotation.name());
            blueprint.getOrCreateTag().putString("Mirror", Mirror.NONE.name());
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, true);
            List<Entity> entities = new ArrayList<>();
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        BlockHelper.placeSchematicBlock(level, state, pos, new ItemStack(state.getBlock()),
                                BlockHelper.prepareBlockEntityData(state, be));
                }, (pos, entity) -> entities.add(entity));
            helper.assertTrue(entities.size() == 1, "Forge must retain the Power Grid wire entity");
            wire = entities.get(0);
            var required = ItemRequirement.of(wire);
            helper.assertTrue(!required.isInvalid() && required.getRequiredItems().get(0).stack.getCount() == 6,
                    "Forge cannon must require all six wire items");
            LaunchedItem flight = new LaunchedItem.ForEntity(anchor.above(3), wire.blockPosition(), required.getRequiredItems().get(0).stack, wire);
            CompoundTag saved = flight.serializeNBT();
            saved.putInt("TicksLeft", 0);
            LaunchedItem restored = LaunchedItem.fromNBT(saved, level.holderLookup(Registries.BLOCK));
            helper.assertTrue(restored.update(level), "Forge cannon must place its saved wire flight");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        Entity placed = level.getEntity(wire.getUUID());
        helper.assertTrue(placed != null, "Forge must spawn the copied wire");
        var settings = SchematicItem.getSettings(blueprint);
        BlockPos first = anchor.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, 1, 0)));
        BlockPos second = anchor.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(4, 1, 0)));
        CompoundTag data = placed.saveWithoutId(new CompoundTag());
        helper.assertTrue(position(data.getCompound("Endpoint1")).equals(first)
                        && position(data.getCompound("Endpoint2")).equals(second),
                "Forge must relocate the endpoints to the pasted connectors");
        helper.assertTrue(Math.abs(data.getFloat("PlacedLength") - 5.75f) < .0001f,
                "Forge must retain NeoForge's configured wire sag through cannon serialization");
        helper.runAfterDelay(8, () -> {
            try {
                helper.assertTrue(placed.getClass().getMethod("getWire").invoke(placed) != null,
                        "Forge wire must form an actual electrical connection");
                if (rotation == Rotation.NONE) {
                    var output = SchematicExport.saveSchematic(Path.of("..", "..", "build", "cross-loader"),
                            "forge-powergrid-wires", true, level, anchor, anchor.offset(4, 2, 1));
                    helper.assertTrue(output != null, "Forge must reexport its native wire format");
                }
                helper.succeed();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot inspect Power Grid connection", e);
            }
        });
    }

    private static BlockPos position(CompoundTag endpoint) {
        int[] pos = endpoint.getIntArray("Pos");
        return new BlockPos(pos[0], pos[1], pos[2]);
    }
}
