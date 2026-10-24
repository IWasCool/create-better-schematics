package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Uses an actual NeoForge export, not a reconstructed legacy NBT fixture. */
@Mod("better_schematics_tests")
@GameTestHolder("better_schematics_tests")
@PrefixGameTestTemplate(false)
public class FactoryGaugeCrossLoaderTests {
    @GameTest(template = "empty")
    public static void neoforgeFileRotatedCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, Rotation.CLOCKWISE_90, Mirror.NONE, false);
    }

    @GameTest(template = "empty")
    public static void neoforgeFileMirroredCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK, false);
    }

    @GameTest(template = "empty")
    public static void forgeReexportsNativeGaugeConfiguration(GameTestHelper helper) throws IOException {
        transfer(helper, Rotation.NONE, Mirror.NONE, true);
    }

    private static void transfer(GameTestHelper helper, Rotation rotation, Mirror mirror, boolean reexport) throws IOException {
        var level = helper.getLevel();
        String owner = "cross_loader_test_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("gauges.nbt");
        try (var fixture = FactoryGaugeCrossLoaderTests.class.getResourceAsStream("/fixtures/neoforge-factory-gauges.nbt")) {
            helper.assertTrue(fixture != null, "Real NeoForge schematic fixture must exist");
            Files.copy(fixture, file);
        }
        try {
            ItemStack blueprint = SchematicItem.create(level, "gauges.nbt", owner);
            BlockPos anchor = helper.absolutePos(new BlockPos(8, 2, 7));
            blueprint.getOrCreateTag().put("Anchor", NbtUtils.writeBlockPos(anchor));
            blueprint.getOrCreateTag().putBoolean("Deployed", true);
            blueprint.getOrCreateTag().putString("Rotation", rotation.name());
            blueprint.getOrCreateTag().putString("Mirror", mirror.name());
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, !reexport);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Forge must load a NeoForge gauge file");
            Map<BlockPos, Placement> placements = new HashMap<>();
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        placements.put(pos, new Placement(state, BlockHelper.prepareBlockEntityData(state, be)));
                }, (pos, entity) -> {});
            var settings = SchematicItem.getSettings(blueprint);
            BlockPos source = anchor.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, 1, 0)));
            BlockPos target = anchor.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(2, 1, 0)));
            BlockPos link = anchor.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(1, 1, 2)));
            for (var entry : placements.entrySet())
                if (!entry.getValue().state().hasBlockEntity())
                    BlockHelper.placeSchematicBlock(level, entry.getValue().state(), entry.getKey(),
                            new ItemStack(entry.getValue().state().getBlock()), null);
            launch(helper, link, placements.get(link));
            var support = ((RedstoneLinkBlockEntity) level.getBlockEntity(link)).panelSupport;
            support.shouldBePoweredTristate();
            launch(helper, target, placements.get(target));
            launch(helper, source, placements.get(source));
            helper.assertTrue(level.getBlockState(target).getBlock() instanceof FactoryPanelBlock
                            && level.getBlockState(source).getBlock() instanceof FactoryPanelBlock,
                    "Forge must retain the physical gauge blocks and their backing blocks");
            var targetGauge = (FactoryPanelBlockEntity) level.getBlockEntity(target);
            boolean mirrored = mirror != Mirror.NONE;
            FactoryPanelBehaviour copied = targetGauge.panels.get(mirrored ? PanelSlot.BOTTOM_LEFT : PanelSlot.BOTTOM_RIGHT);
            copied.lazyTick();
            assertCopied(helper, copied, source, link, mirrored);
            helper.assertTrue(((FactoryPanelBlockEntity) level.getBlockEntity(source)).activePanels() == 4,
                    "Forge must preserve all four gauge slots");
            helper.assertTrue(support.getLinkedPanels().contains(copied.getPanelPosition()),
                    "Forge must restore a support placed before its gauge");
            targetGauge.load(targetGauge.saveWithFullMetadata());
            assertCopied(helper, copied, source, link, mirrored);
            if (reexport) {
                Path output = Path.of("..", "..", "build", "cross-loader");
                var exported = SchematicExport.saveSchematic(output, "forge-factory-gauges", true,
                        level, anchor, anchor.offset(2, 2, 2));
                helper.assertTrue(exported != null, "Forge must export a native gauge schematic for NeoForge");
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    private static void assertCopied(GameTestHelper helper, FactoryPanelBehaviour panel, BlockPos source,
                                     BlockPos link, boolean mirrored) {
        FactoryPanelPosition iron = new FactoryPanelPosition(source, mirrored ? PanelSlot.TOP_RIGHT : PanelSlot.TOP_LEFT);
        var connection = panel.targetedBy.get(iron);
        helper.assertTrue(panel.active && panel.recipeOutput == 4 && panel.promiseClearingInterval == 7,
                "Forge must retain recipe output and promise expiry");
        helper.assertTrue(panel.count == 32 && panel.upTo && panel.recipeAddress.equals("Iron works")
                        && panel.network.equals(UUID.fromString("208c1549-b187-4481-a0ca-44f707ec8c88")),
                "Forge must retain gauge amount, units, address and network");
        helper.assertTrue(panel.targetedBy.size() == 2 && connection != null && connection.amount == 9
                        && connection.arrowBendMode == 3 && FactoryPanelBehaviour.at(helper.getLevel(), iron) != null,
                "Forge must connect the pasted gauges to each other");
        helper.assertTrue(panel.targetedByLinks.size() == 1 && panel.targetedByLinks.containsKey(link),
                "Forge must relocate redstone connections");
        helper.assertTrue(panel.activeCraftingArrangement.size() == 3 && panel.activeCraftingArrangement.get(1).isEmpty()
                        && panel.activeCraftingArrangement.get(0).getHoverName().getString().equals("Gauge ingredient"),
                "Forge must retain crafting layout and ingredient item data");
    }

    private static void launch(GameTestHelper helper, BlockPos pos, Placement placement) {
        helper.assertTrue(placement != null, "Schematic must contain the expected gauge/link at " + pos);
        LaunchedItem flight = new LaunchedItem.ForBlockState(pos.above(3), pos, new ItemStack(placement.state().getBlock()),
                placement.state(), placement.data());
        CompoundTag saved = flight.serializeNBT();
        saved.putInt("TicksLeft", 0);
        LaunchedItem restored = LaunchedItem.fromNBT(saved, helper.getLevel().holderLookup(Registries.BLOCK));
        helper.assertTrue(restored.update(helper.getLevel()), "Reloaded Forge cannon projectile must place its block");
    }

    private record Placement(BlockState state, CompoundTag data) {}
}
