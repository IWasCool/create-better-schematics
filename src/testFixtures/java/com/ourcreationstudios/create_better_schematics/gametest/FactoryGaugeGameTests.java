package com.ourcreationstudios.create_better_schematics.gametest;

import java.util.List;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeSchematicProcessor;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class FactoryGaugeGameTests {
    @GameTest(template = "empty")
    public static void nativeGaugeFileRotatedCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, false, false, false);
    }

    @GameTest(template = "empty")
    public static void forgeGaugeFileMirroredCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, true, true, false);
    }

    @GameTest(template = "empty")
    public static void actualForgeExportReturnsToNeoForge(GameTestHelper helper) throws IOException {
        transfer(helper, true, false, true);
    }

    @GameTest(template = "empty")
    public static void cannonRetainsGaugeConfiguration(GameTestHelper helper) {
        FactoryPanelBlockEntity gauge = gauge(helper, new BlockPos(2, 2, 2));
        FactoryPanelBehaviour source = configure(gauge, PanelSlot.TOP_LEFT, Items.IRON_INGOT);
        FactoryPanelBehaviour target = configure(gauge, PanelSlot.BOTTOM_RIGHT, Items.IRON_BLOCK);
        target.addConnection(source.getPanelPosition());
        target.targetedBy.get(source.getPanelPosition()).amount = 9;
        target.targetedBy.get(source.getPanelPosition()).arrowBendMode = 3;
        target.recipeOutput = 4;
        target.promiseClearingInterval = 7;
        target.activeCraftingArrangement = List.of(new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT));
        CompoundTag safe = BlockHelper.prepareBlockEntityData(helper.getLevel(), gauge.getBlockState(), gauge);
        CompoundTag panel = safe.getCompound("bottom_right");
        helper.assertTrue(panel.getInt("RecipeOutput") == 4 && panel.getInt("PromiseClearingInterval") == 7,
                "Cannon must preserve recipe output and promise expiry settings: " + panel);
        helper.assertTrue(panel.getList("TargetedBy", Tag.TAG_COMPOUND).size() == 1
                        && panel.getList("Craft", Tag.TAG_COMPOUND).size() == 3,
                "Cannon must preserve recipe connections and crafting arrangement: " + panel);
        helper.assertTrue(safe.getCompound("top_left").getList("Targeting", Tag.TAG_COMPOUND).size() == 1,
                "Source gauges must retain their outgoing connections");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restockerCopiesSettingsWithoutPendingDeliveries(GameTestHelper helper) {
        FactoryPanelBlockEntity gauge = gauge(helper, new BlockPos(2, 2, 2));
        configure(gauge, PanelSlot.BOTTOM_RIGHT, Items.IRON_BLOCK);
        gauge.restocker = true;
        CompoundTag safe = BlockHelper.prepareBlockEntityData(helper.getLevel(), gauge.getBlockState(), gauge);
        helper.assertTrue(safe.getBoolean("Restocker"), "Cannon must preserve restocker mode");
        helper.assertTrue(!safe.getCompound("bottom_right").contains("Promises"),
                "Copies must not duplicate the original factory's pending deliveries");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialCopyLeavesExternalGaugesDisconnected(GameTestHelper helper) {
        FactoryPanelBlockEntity source = gauge(helper, new BlockPos(2, 2, 2));
        FactoryPanelBlockEntity external = gauge(helper, new BlockPos(4, 2, 2));
        FactoryPanelBehaviour ingredient = configure(source, PanelSlot.TOP_LEFT, Items.IRON_INGOT);
        FactoryPanelBehaviour target = configure(external, PanelSlot.BOTTOM_RIGHT, Items.IRON_BLOCK);
        target.addConnection(ingredient.getPanelPosition());
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), source.getBlockPos(), new BlockPos(1, 1, 1), false, null);
        BlockPos copy = helper.absolutePos(new BlockPos(8, 2, 8));
        var settings = SchematicItem.getSettings(ItemStack.EMPTY).setKnownShape(true);
        template.placeInWorld(helper.getLevel(), copy, copy, settings, helper.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        FactoryPanelBehaviour pasted = ((FactoryPanelBlockEntity) helper.getLevel().getBlockEntity(copy)).panels.get(PanelSlot.TOP_LEFT);
        helper.assertTrue(pasted.targeting.isEmpty(), "Copying only one endpoint must not link back to the original factory");
        helper.assertTrue(ingredient.targeting.contains(target.getPanelPosition()), "Partial copies must leave originals connected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gaugeSlotsFollowEveryMountRotationAndMirror(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos original = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos copy = helper.absolutePos(new BlockPos(8, 2, 8));
        for (Direction direction : Direction.values()) {
            level.setBlock(original.relative(direction), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(copy.relative(direction), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        List<net.minecraft.world.item.Item> items = List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.REDSTONE, Items.COAL);
        for (AttachFace face : AttachFace.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = block("factory_gauge").defaultBlockState()
                        .setValue(FactoryPanelBlock.FACE, face).setValue(FactoryPanelBlock.FACING, facing);
                level.setBlock(original, state, Block.UPDATE_CLIENTS);
                FactoryPanelBlockEntity source = (FactoryPanelBlockEntity) level.getBlockEntity(original);
                for (PanelSlot slot : PanelSlot.values())
                    configure(source, slot, items.get(slot.ordinal()));
                StructureTemplate template = new StructureTemplate();
                template.fillFromWorld(level, original, new BlockPos(1, 1, 1), false, null);
                for (Rotation rotation : Rotation.values()) {
                    for (Mirror mirror : Mirror.values()) {
                        var settings = new StructurePlaceSettings().setKnownShape(true).setRotation(rotation).setMirror(mirror)
                                .addProcessor(new FactoryGaugeSchematicProcessor());
                        template.placeInWorld(level, copy, copy, settings, level.getRandom(), Block.UPDATE_CLIENTS);
                        FactoryPanelBlockEntity pasted = (FactoryPanelBlockEntity) level.getBlockEntity(copy);
                        for (PanelSlot slot : PanelSlot.values()) {
                            var sourceOffset = source.panels.get(slot).getSlotPositioning().getLocalOffset(level, original, state);
                            var expected = StructureTemplate.transform(sourceOffset, mirror, rotation, BlockPos.ZERO);
                            FactoryPanelBehaviour actual = pasted.panels.values().stream()
                                    .filter(panel -> panel.active && panel.getFilter().is(items.get(slot.ordinal()))).findFirst().orElseThrow();
                            var offset = actual.getSlotPositioning().getLocalOffset(level, copy, pasted.getBlockState());
                            helper.assertTrue(offset.distanceToSqr(expected) < 0.000001,
                                    "Gauge slot geometry must follow " + face + "/" + facing + "/" + rotation + "/" + mirror + "/" + slot);
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    private static void transfer(GameTestHelper helper, boolean legacy, boolean mirror, boolean actualForge) throws IOException {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        FactoryPanelBlockEntity source = gauge(helper, new BlockPos(1, 2, 1));
        FactoryPanelBlockEntity destination = gauge(helper, new BlockPos(3, 2, 1));
        source.setBlockState(source.getBlockState().setValue(FactoryPanelBlock.FACE, AttachFace.WALL)
                .setValue(FactoryPanelBlock.FACING, Direction.NORTH));
        destination.setBlockState(source.getBlockState());
        level.setBlock(source.getBlockPos(), source.getBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(destination.getBlockPos(), destination.getBlockState(), Block.UPDATE_CLIENTS);
        // Place supports behind the wall-mounted gauges.
        level.setBlock(source.getBlockPos().south(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(destination.getBlockPos().south(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        FactoryPanelBehaviour iron = configure(source, PanelSlot.TOP_LEFT, Items.IRON_INGOT);
        FactoryPanelBehaviour gold = configure(source, PanelSlot.TOP_RIGHT, Items.GOLD_INGOT);
        configure(source, PanelSlot.BOTTOM_LEFT, Items.REDSTONE);
        configure(source, PanelSlot.BOTTOM_RIGHT, Items.COAL);
        FactoryPanelBehaviour target = configure(destination, PanelSlot.BOTTOM_RIGHT, Items.IRON_BLOCK);
        target.addConnection(iron.getPanelPosition());
        target.addConnection(gold.getPanelPosition());
        target.targetedBy.get(iron.getPanelPosition()).amount = 9;
        target.targetedBy.get(iron.getPanelPosition()).arrowBendMode = 3;
        target.targetedBy.get(gold.getPanelPosition()).amount = 2;
        target.recipeOutput = 4;
        target.promiseClearingInterval = 7;
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Gauge ingredient"));
        target.activeCraftingArrangement = List.of(namedIron, ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT));
        BlockPos linkPos = origin.offset(1, 1, 2);
        level.setBlock(linkPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(linkPos, block("redstone_link").defaultBlockState(), Block.UPDATE_CLIENTS);
        RedstoneLinkBlockEntity link = (RedstoneLinkBlockEntity) level.getBlockEntity(linkPos);
        target.addConnection(new FactoryPanelPosition(linkPos, PanelSlot.TOP_LEFT));
        helper.assertTrue(link.panelSupport.getLinkedPanels().size() == 1, "Fixture must have a redstone connection");

        String owner = "factory_gauge_test_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "gauges", false, level, origin, origin.offset(2, 2, 2));
        helper.assertTrue(exported != null, "Gauge schematic must export");
        try {
            if (actualForge) {
                try (var fixture = FactoryGaugeGameTests.class.getResourceAsStream("/fixtures/forge-factory-gauges.nbt")) {
                    helper.assertTrue(fixture != null, "Real Forge-exported fixture must exist");
                    Files.copy(fixture, exported.file(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (!legacy) {
                Path crossLoader = Path.of("..", "build", "cross-loader");
                Files.createDirectories(crossLoader);
                Files.copy(exported.file(), crossLoader.resolve("neoforge-factory-gauges.nbt"), StandardCopyOption.REPLACE_EXISTING);
            } else {
                CompoundTag forge = NbtIo.readCompressed(exported.file(), NbtAccounter.unlimitedHeap());
                stripModernData(forge);
                forge.putInt("DataVersion", 3465);
                NbtIo.writeCompressed(forge, exported.file());
            }
            ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
            BlockPos anchor = helper.absolutePos(new BlockPos(8, 2, 7));
            blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
            blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_90);
            blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirror ? Mirror.FRONT_BACK : Mirror.NONE);
            SchematicPrinter printer = new SchematicPrinter();
            // The custom processor must also run for the creative/OP placement path.
            printer.loadSchematic(blueprint, level, legacy);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Create must load the gauge schematic");
            Map<BlockPos, Placement> placements = new HashMap<>();
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        placements.put(pos, new Placement(state, BlockHelper.prepareBlockEntityData(level, state, be)));
                }, (pos, entity) -> {});
            var settings = SchematicItem.getSettings(blueprint);
            BlockPos pastedSource = anchor.offset(StructureTemplate.calculateRelativePosition(settings, source.getBlockPos().subtract(origin)));
            BlockPos pastedTarget = anchor.offset(StructureTemplate.calculateRelativePosition(settings, destination.getBlockPos().subtract(origin)));
            BlockPos pastedLink = anchor.offset(StructureTemplate.calculateRelativePosition(settings, linkPos.subtract(origin)));
            for (var entry : placements.entrySet())
                if (!entry.getValue().state().hasBlockEntity())
                    BlockHelper.placeSchematicBlock(level, entry.getValue().state(), entry.getKey(),
                            new ItemStack(entry.getValue().state().getBlock()), null);
            Placement support = placements.get(pastedLink);
            helper.assertTrue(support.data().getList("LinkedGauges", Tag.TAG_COMPOUND).size() == 1,
                    "Cannon must preserve the support's linked gauges");
            launch(helper, pastedLink, support);
            var copiedLink = (RedstoneLinkBlockEntity) level.getBlockEntity(pastedLink);
            copiedLink.panelSupport.shouldBePoweredTristate(); // The gauge has not arrived yet.
            launch(helper, pastedTarget, placements.get(pastedTarget));
            launch(helper, pastedSource, placements.get(pastedSource));
            helper.assertTrue(level.getBlockState(pastedTarget).getBlock() instanceof FactoryPanelBlock
                            && level.getBlockState(pastedSource).getBlock() instanceof FactoryPanelBlock,
                    "Copied gauges must remain physically placed on their backing blocks");
            FactoryPanelBlockEntity pasted = (FactoryPanelBlockEntity) level.getBlockEntity(pastedTarget);
            FactoryPanelBehaviour copied = pasted.panels.get(mirror ? PanelSlot.BOTTOM_LEFT : PanelSlot.BOTTOM_RIGHT);
            copied.lazyTick();
            assertCopied(helper, copied, pastedSource, pastedLink, mirror);
            helper.assertTrue(copiedLink.panelSupport.getLinkedPanels().contains(copied.getPanelPosition()),
                    "Support placed before its gauge must reconnect after the gauge arrives");
            CompoundTag saved = pasted.saveWithFullMetadata(level.registryAccess());
            pasted.loadWithComponents(saved, level.registryAccess());
            assertCopied(helper, copied, pastedSource, pastedLink, mirror);
            helper.assertTrue(iron.targeting.contains(target.getPanelPosition())
                            && link.panelSupport.getLinkedPanels().contains(target.getPanelPosition()),
                    "Pasting must leave the original factory connected to its original gauges");
            helper.assertTrue(((FactoryPanelBlockEntity) level.getBlockEntity(pastedSource)).activePanels() == 4,
                    "All four gauge slots must survive copying");
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    private static void assertCopied(GameTestHelper helper, FactoryPanelBehaviour panel, BlockPos source,
                                     BlockPos link, boolean mirror) {
        FactoryPanelPosition iron = new FactoryPanelPosition(source, mirror ? PanelSlot.TOP_RIGHT : PanelSlot.TOP_LEFT);
        FactoryPanelConnection connection = panel.targetedBy.get(iron);
        helper.assertTrue(panel.active && panel.recipeOutput == 4 && panel.promiseClearingInterval == 7,
                "Gauge recipe output, expiry and active slot must survive placement/reload");
        helper.assertTrue(panel.count == 32 && panel.upTo && panel.recipeAddress.equals("Iron works")
                        && panel.network.equals(UUID.fromString("208c1549-b187-4481-a0ca-44f707ec8c88")),
                "Gauge amount, units, address, and logistics network must survive");
        helper.assertTrue(panel.targetedBy.size() == 2 && connection != null && connection.amount == 9
                        && connection.arrowBendMode == 3 && FactoryPanelBehaviour.at(helper.getLevel(), iron) != null,
                "Rotated/mirrored recipe connections must point at the pasted gauges: " + panel.targetedBy.keySet());
        helper.assertTrue(panel.targetedByLinks.size() == 1 && panel.targetedByLinks.containsKey(link),
                "Redstone connections must move with the pasted factory");
        helper.assertTrue(panel.activeCraftingArrangement.size() == 3 && panel.activeCraftingArrangement.get(1).isEmpty()
                        && panel.activeCraftingArrangement.get(0).getHoverName().getString().equals("Gauge ingredient"),
                "Crafting layouts must retain empty slots and ingredient item data");
    }

    private static void launch(GameTestHelper helper, BlockPos pos, Placement placement) {
        LaunchedItem flight = new LaunchedItem.ForBlockState(pos.above(3), pos, new ItemStack(placement.state().getBlock()),
                placement.state(), placement.data());
        CompoundTag saved = flight.serializeNBT(helper.getLevel().registryAccess());
        saved.putInt("TicksLeft", 0);
        LaunchedItem restored = LaunchedItem.fromNBT(saved, helper.getLevel().registryAccess(),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK));
        helper.assertTrue(restored.update(helper.getLevel()), "Saved cannon projectile must place its gauge/link");
    }

    private static void stripModernData(CompoundTag tag) {
        if (tag.contains("Count", Tag.TAG_BYTE)) {
            tag.remove("count");
            tag.remove("components");
        }
        if (tag.contains("Slot", Tag.TAG_INT) && tag.contains("X", Tag.TAG_INT)) {
            tag.remove("position");
            tag.remove("pos");
            tag.remove("slot");
            tag.remove("amount");
            tag.remove("arrow_bending");
        }
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound)
                stripModernData(compound);
            else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND)
                list.forEach(entry -> stripModernData((CompoundTag) entry));
        }
    }

    private record Placement(BlockState state, CompoundTag data) {}

    @GameTest(template = "empty")
    public static void forgeGaugeConnectionsImport(GameTestHelper helper) {
        FactoryPanelBlockEntity gauge = gauge(helper, new BlockPos(2, 2, 2));
        FactoryPanelBehaviour target = configure(gauge, PanelSlot.BOTTOM_RIGHT, Items.IRON_BLOCK);
        CompoundTag data = gauge.saveWithFullMetadata(helper.getLevel().registryAccess());
        CompoundTag panel = data.getCompound("bottom_right");
        CompoundTag connection = new CompoundTag();
        connection.putInt("X", gauge.getBlockPos().getX() + 2);
        connection.putInt("Y", gauge.getBlockPos().getY());
        connection.putInt("Z", gauge.getBlockPos().getZ());
        connection.putInt("Slot", PanelSlot.TOP_RIGHT.ordinal());
        connection.putInt("Amount", 9);
        connection.putInt("ArrowBending", 3);
        ListTag connections = new ListTag();
        connections.add(connection);
        panel.put("TargetedBy", connections);
        gauge.loadWithComponents(SchematicDataCompatibility.importSchematic(data), helper.getLevel().registryAccess());
        helper.assertTrue(target.targetedBy.size() == 1,
                "Forge X/Y/Z/Slot connections must load on NeoForge");
        FactoryPanelConnection imported = target.targetedBy.values().iterator().next();
        helper.assertTrue(imported.from.equals(new FactoryPanelPosition(gauge.getBlockPos().east(2), PanelSlot.TOP_RIGHT))
                        && imported.amount == 9 && imported.arrowBendMode == 3,
                "Forge connections must retain position, panel slot, amount, and arrow routing");
        helper.succeed();
    }

    private static FactoryPanelBehaviour configure(FactoryPanelBlockEntity gauge, PanelSlot slot,
                                                   net.minecraft.world.item.Item item) {
        FactoryPanelBehaviour panel = gauge.panels.get(slot);
        panel.enable();
        panel.setFilter(new ItemStack(item));
        panel.count = 32;
        panel.upTo = true;
        panel.network = UUID.fromString("208c1549-b187-4481-a0ca-44f707ec8c88");
        panel.recipeAddress = "Iron works";
        return panel;
    }

    private static FactoryPanelBlockEntity gauge(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos, block("factory_gauge").defaultBlockState(), Block.UPDATE_CLIENTS);
        return (FactoryPanelBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:" + path));
    }
}
