package com.ourcreationstudios.create_better_schematics.compat_gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ourcreationstudios.create_better_schematics.compat.SchematicConfigurationRegistry;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Real addon instances, exported .nbt files, printer transforms and saved cannon projectiles.
 */
@GameTestHolder("create_better_schematics")
public final class AddonTransferGameTests {
    @GameTestGenerator
    public static List<TestFunction> addonTransfers() throws Exception {
        List<TestFunction> tests = new ArrayList<>();
        for (var entry : transferCases()) {
            JsonObject specification = entry.getAsJsonObject();
            if (!ModList.get().isLoaded(specification.get("mod").getAsString()))
                continue;
            for (boolean mirrored : List.of(false, true))
                for (boolean cannon : specification.has("creative") && specification.get("creative").getAsBoolean()
                        ? List.of(true, false) : List.of(true)) {
                    String name = "addon_" + specification.get("name").getAsString() + (mirrored ? "_mirrored" : "_rotated")
                            + (cannon ? "" : "_creative");
                    tests.add(new TestFunction("addonTransfers", name, "create_better_schematics:empty",
                            Rotation.NONE, 100, 0, true, helper -> {
                        try {
                            transfer(helper, specification, mirrored, cannon);
                        } catch (Exception e) {
                            throw new IllegalStateException(name + ": " + e.getMessage(), e);
                        }
                    }));
                }
            if (specification.has("materials"))
                tests.add(new TestFunction("addonTransfers", "addon_" + specification.get("name").getAsString()
                        + "_consumes_components", "create_better_schematics:empty", Rotation.NONE, 240, 0, true, helper -> {
                    try {
                        buildWithCannon(helper, specification);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }));
        }
        return tests;
    }

    private static JsonArray transferCases() throws Exception {
        JsonArray cases = new JsonArray();
        for (String resource : List.of("/addon-transfer-cases.json", "/popular-mod-transfer-cases.json",
                "/transmission-transfer-cases.json"))
            try (var input = AddonTransferGameTests.class.getResourceAsStream(resource)) {
                if (input == null)
                    throw new IllegalStateException("Missing transfer cases: " + resource);
                cases.addAll(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray());
            }
        return cases;
    }

    private static void transfer(GameTestHelper helper, JsonObject specification, boolean mirrored, boolean cannon) throws Exception {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos anchor = helper.absolutePos(new BlockPos(7, 2, 7));
        BlockState state = findBlock(helper, specification, source);
        boolean backing = specification.has("backing") && specification.get("backing").getAsBoolean();
        if (backing)
            for (Direction direction : Direction.values())
                level.setBlock(source.relative(direction), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source, state, Block.UPDATE_CLIENTS);
        BlockEntity original = level.getBlockEntity(source);
        helper.assertTrue(original != null, "Source addon block must have a block entity");
        CompoundTag wanted = TagParser.parseTag(specification.get("settings").getAsString());
        CompoundTag reset = specification.has("reset") ? TagParser.parseTag(specification.get("reset").getAsString()) : new CompoundTag();
        CompoundTag configured = original.saveWithoutMetadata(level.registryAccess());
        configured.merge(wanted.copy());
        if (specification.has("seed"))
            configured.merge(TagParser.parseTag(specification.get("seed").getAsString()));
        original.loadWithComponents(configured, level.registryAccess());
        if (specification.has("seed_item"))
            seedInventory(helper, original, specification.get("seed_item").getAsString());
        CompoundTag sourceData = original.saveWithoutMetadata(level.registryAccess());
        assertSettings(helper, wanted, sourceData, "Source fixture");
        if (specification.has("source_check"))
            assertSettings(helper, TagParser.parseTag(specification.get("source_check").getAsString()), sourceData, "Seeded source");
        long sourceItems = specification.has("empty_storage") ? inventoryItems(helper, original) : 0;
        assertMaterials(helper, specification, original);
        assertSafe(helper, specification, wanted, BlockHelper.prepareBlockEntityData(level, state, original), original);

        var directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve("addon_transfer_" + UUID.randomUUID());
        var exported = SchematicExport.saveSchematic(directory, "configuration", false, level,
                backing ? source.offset(-1, -1, -1) : source, backing ? source.offset(1, 1, 1) : source);
        helper.assertTrue(exported != null, "Addon configuration must export to disk");
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), directory.getFileName().toString());
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_90);
            blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirrored ? Mirror.FRONT_BACK : Mirror.NONE);
            CompoundTag expected = wanted.copy();
            if (mirrored && specification.has("mirrored"))
                expected.merge(TagParser.parseTag(specification.get("mirrored").getAsString()));
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, cannon);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Create must load the exported addon schematic");
            List<PlannedBlock> planned = new ArrayList<>();
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, targetState, entity) -> {
                    if (targetState.isAir())
                        return;
                    CompoundTag safe = BlockHelper.prepareBlockEntityData(level, targetState, entity);
                    planned.add(new PlannedBlock(pos, targetState, safe, entity));
                }, (pos, entity) -> {
                });
            for (PlannedBlock block : planned)
                if (!block.state().hasBlockEntity())
                    BlockHelper.placeSchematicBlock(level, block.state(), block.pos(), new ItemStack(block.state().getBlock()), block.data());
            int placed = 0;
            for (PlannedBlock block : planned) {
                if (!block.state().hasBlockEntity())
                    continue;
                BlockPos pos = block.pos();
                BlockState targetState = block.state();
                BlockEntity entity = block.entity();
                CompoundTag safe = block.data();
                assertMaterials(helper, specification, entity);
                assertSafe(helper, specification, expected, safe, entity);
                assertSettings(helper, reset, safe, "Placement defaults");
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (cannon) {
                    LaunchedItem flight = new LaunchedItem.ForBlockState(pos.above(3), pos,
                            new ItemStack(targetState.getBlock()), targetState, safe);
                    CompoundTag saved = flight.serializeNBT(level.registryAccess());
                    saved.putInt("TicksLeft", 0);
                    LaunchedItem restored = LaunchedItem.fromNBT(saved, level.registryAccess(),
                            level.registryAccess().lookupOrThrow(Registries.BLOCK));
                    helper.assertTrue(restored.update(level), "Saved cannon projectile must complete");
                } else {
                    BlockHelper.placeSchematicBlock(level, targetState, pos, null, safe);
                }
                BlockEntity copy = level.getBlockEntity(pos);
                helper.assertTrue(copy != null, "Cannon must place the addon block");
                helper.assertTrue(level.getBlockState(pos).equals(targetState), "Cannon must preserve transformed block state");
                assertSettings(helper, expected, copy.saveWithoutMetadata(level.registryAccess()), "Cannon placement");
                assertSettings(helper, reset, copy.saveWithoutMetadata(level.registryAccess()), "Cannon runtime reset");
                assertEmptyStorage(helper, specification, copy);
                copy.loadWithComponents(copy.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                assertSettings(helper, expected, copy.saveWithoutMetadata(level.registryAccess()), "Reload");
                assertEmptyStorage(helper, specification, copy);
                placed++;
            }
            helper.assertTrue(placed == 1, "Printer must produce exactly one addon block; got " + placed);
            assertSettings(helper, wanted, original.saveWithoutMetadata(level.registryAccess()), "Original after copying");
            if (specification.has("strict_excluded"))
                helper.assertTrue(sourceData.equals(original.saveWithoutMetadata(level.registryAccess())),
                        "Copying must leave all source settings and physical link references intact");
            if (specification.has("empty_storage"))
                helper.assertTrue(inventoryItems(helper, original) == sourceItems, "Copying must leave the source inventory unchanged");
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    private record PlannedBlock(BlockPos pos, BlockState state, CompoundTag data, BlockEntity entity) {
    }

    private static void seedInventory(GameTestHelper helper, BlockEntity entity, String itemId) {
        var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(itemId));
        helper.assertTrue(item != Items.AIR, "Seed item must exist: " + itemId);
        for (Direction side : inventorySides()) {
            var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), side);
            if (inventory == null)
                continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                int offeredCount = item.getDefaultMaxStackSize();
                ItemStack offered = new ItemStack(item, offeredCount);
                ItemStack remaining = inventory.insertItem(slot, offered, false);
                if (remaining.getCount() < offeredCount)
                    return;
            }
        }
        helper.fail("Source fixture must contain real stored items: " + itemId);
    }

    private static long inventoryItems(GameTestHelper helper, BlockEntity entity) {
        long total = 0;
        // Each face is checked consistently, including handlers exposing the same underlying inventory.
        for (Direction side : inventorySides()) {
            var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), side);
            if (inventory != null)
                for (int slot = 0; slot < inventory.getSlots(); slot++)
                    total += inventory.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static List<Direction> inventorySides() {
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        return sides;
    }

    private static void assertEmptyStorage(GameTestHelper helper, JsonObject specification, BlockEntity entity) throws Exception {
        if (!specification.has("empty_storage") || !specification.get("empty_storage").getAsBoolean())
            return;
        for (Direction side : inventorySides()) {
            var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), side);
            if (inventory != null)
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    helper.assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Pasting must not copy stored items");
                    helper.assertTrue(inventory.extractItem(slot, 64, true).isEmpty(), "Ghost filters must not become extractable items");
                }
            var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), side);
            if (energy != null)
                helper.assertTrue(energy.getEnergyStored() == 0, "Pasting must not copy stored energy");
            var fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), side);
            if (fluids != null)
                for (int tank = 0; tank < fluids.getTanks(); tank++)
                    helper.assertTrue(fluids.getFluidInTank(tank).isEmpty(), "Pasting must not copy stored fluids");
        }
        if (specification.has("empty_check"))
            assertSettings(helper, TagParser.parseTag(specification.get("empty_check").getAsString()),
                    entity.saveWithoutMetadata(helper.getLevel().registryAccess()), "Empty copied storage");
    }

    private static void buildWithCannon(GameTestHelper helper, JsonObject specification) throws Exception {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos target = helper.absolutePos(new BlockPos(7, 2, 7));
        BlockState state = findBlock(helper, specification, source);
        level.setBlock(source, state, Block.UPDATE_CLIENTS);
        BlockEntity original = level.getBlockEntity(source);
        CompoundTag wanted = TagParser.parseTag(specification.get("settings").getAsString());
        CompoundTag configured = original.saveWithoutMetadata(level.registryAccess());
        configured.merge(wanted.copy());
        original.loadWithComponents(configured, level.registryAccess());
        assertMaterials(helper, specification, original);
        String owner = "addon_cannon_" + UUID.randomUUID();
        var directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "installed_parts", false, level, source, source);
        helper.assertTrue(exported != null, "Upgraded engine must export");
        try {
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, target);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            BlockPos cannonPosition = helper.absolutePos(new BlockPos(2, 2, 8));
            level.setBlock(cannonPosition.below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            BarrelBlockEntity supply = (BarrelBlockEntity) level.getBlockEntity(cannonPosition.below());
            int slot = 0;
            for (var required : ItemRequirement.of(state, original).getRequiredItems())
                supply.setItem(slot++, required.stack.copy());
            var cannonBlock = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:schematicannon"));
            level.setBlock(cannonPosition, cannonBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
            SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) level.getBlockEntity(cannonPosition);
            cannon.inventory.setStackInSlot(0, blueprint);
            cannon.printer.loadSchematic(blueprint, level, true);
            cannon.updateChecklist();
            for (var entry : specification.getAsJsonObject("materials").entrySet()) {
                var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(entry.getKey()));
                helper.assertTrue(cannon.checklist.getRequiredAmount(item) == entry.getValue().getAsInt(),
                        "Cannon checklist must charge for " + entry.getKey());
            }
            cannon.remainingFuel = 10;
            cannon.skipMissing = false;
            cannon.state = SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            helper.succeedWhen(() -> {
                BlockEntity copied = level.getBlockEntity(target);
                helper.assertTrue(copied != null, "Cannon must finish the upgraded engine");
                assertSettings(helper, wanted, copied.saveWithoutMetadata(level.registryAccess()), "Working cannon");
                helper.assertTrue(supply.isEmpty(), "Cannon must consume the engine and its installed upgrade exactly once");
                helper.assertTrue(cannon.remainingFuel == 9, "One engine must consume one unit of cannon fuel");
            });
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
    }

    private static void assertMaterials(GameTestHelper helper, JsonObject specification, BlockEntity entity) {
        if (!specification.has("materials"))
            return;
        ItemRequirement requirement = ItemRequirement.of(entity.getBlockState(), entity);
        helper.assertTrue(!requirement.isInvalid(), "Addon block must have valid material requirements");
        for (var entry : specification.getAsJsonObject("materials").entrySet()) {
            String id = entry.getKey();
            int actual = requirement.getRequiredItems().stream().filter(required ->
                            required.usage == ItemRequirement.ItemUseType.CONSUME
                                    && BuiltInRegistries.ITEM.getKey(required.stack.getItem()).toString().equals(id))
                    .mapToInt(required -> required.stack.getCount()).sum();
            helper.assertTrue(actual == entry.getValue().getAsInt(), "Material cost for " + id + " must be "
                    + entry.getValue().getAsInt() + ", got " + actual);
        }
    }

    private static BlockState findBlock(GameTestHelper helper, JsonObject specification, BlockPos position) {
        String namespace = specification.has("namespace") ? specification.get("namespace").getAsString()
                : specification.get("mod").getAsString();
        String target = specification.get("target").getAsString();
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(namespace) || block.asItem() == Items.AIR || !(block instanceof EntityBlock factory))
                continue;
            if (specification.has("block") && !id.getPath().equals(specification.get("block").getAsString()))
                continue;
            BlockState state = block.defaultBlockState();
            BlockEntity entity = factory.newBlockEntity(position, state);
            if (entity != null && entity.getType().isValid(state) && (matches(entity, target) || entity instanceof SmartBlockEntity smart
                    && smart.getAllBehaviours().stream().anyMatch(behaviour -> matches(behaviour, target))))
                return state;
        }
        helper.fail("Loaded addon has no block matching " + namespace + ": " + target);
        throw new IllegalStateException("No addon block found");
    }

    private static boolean matches(Object instance, String name) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass())
            if (type.getSimpleName().equals(name))
                return true;
        return false;
    }

    private static void assertSafe(GameTestHelper helper, JsonObject specification, CompoundTag wanted, CompoundTag safe, BlockEntity entity) {
        helper.assertTrue(safe != null, "Cannon must accept the addon configuration");
        assertSettings(helper, wanted, safe, "Cannon safe data");
        // Check what our profiles add; an addon's own safe writer remains authoritative for its native fields.
        CompoundTag supplement = SchematicConfigurationRegistry.merge(entity, null,
                entity.saveWithoutMetadata(helper.getLevel().registryAccess()));
        if (specification.has("excluded"))
            for (var key : specification.getAsJsonArray("excluded")) {
                String[] path = key.getAsString().split("\\.");
                helper.assertTrue(supplement == null || !containsPath(supplement, path, 0),
                        "Configuration profile must exclude " + key.getAsString());
                if (specification.has("empty_storage") || specification.has("strict_excluded"))
                    helper.assertTrue(!containsPath(safe, path, 0), "Cannon data must exclude " + key.getAsString());
            }
    }

    private static boolean containsPath(net.minecraft.nbt.Tag tag, String[] path, int index) {
        if (tag == null)
            return false;
        if (index == path.length)
            return true;
        if (path[index].equals("*") && tag instanceof ListTag list)
            return list.stream().anyMatch(entry -> containsPath(entry, path, index + 1));
        return tag instanceof CompoundTag compound && containsPath(compound.get(path[index]), path, index + 1);
    }

    private static void assertSettings(GameTestHelper helper, CompoundTag wanted, CompoundTag actual, String stage) {
        for (String key : wanted.getAllKeys()) {
            helper.assertTrue(actual.contains(key), stage + " lost " + key + "; actual: " + actual);
            if (wanted.get(key) instanceof CompoundTag nested)
                assertSettings(helper, nested, actual.getCompound(key), stage + "." + key);
            else if (wanted.get(key) instanceof ListTag list && list.getElementType() == net.minecraft.nbt.Tag.TAG_COMPOUND) {
                ListTag saved = actual.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
                helper.assertTrue(saved.size() == list.size(), stage + " changed " + key + " entry count");
                for (int i = 0; i < list.size(); i++)
                    assertSettings(helper, list.getCompound(i), saved.getCompound(i), stage + "." + key + "[" + i + "]");
            } else
                helper.assertTrue(wanted.get(key).equals(actual.get(key)), stage + " changed " + key + ": expected "
                        + wanted.get(key) + ", got " + actual.get(key));
        }
    }
}
