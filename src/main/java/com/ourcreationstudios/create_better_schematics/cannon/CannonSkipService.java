package com.ourcreationstudios.create_better_schematics.cannon;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.BlockEntry;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.Result;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.Save;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.View;
import com.ourcreationstudios.create_better_schematics.mixin.StructureTemplateAccessor;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class CannonSkipService {
    private CannonSkipService() {}

    public static boolean skipsAllBlocks(SchematicLevel schematic, CannonSkipSettings settings) {
        if (settings.isEmpty() || !schematic.getEntityList().isEmpty())
            return false;
        boolean foundBlock = false;
        // Inspect captured entries, never the potentially much larger bounding volume.
        for (var state : schematic.getBlockMap().values()) {
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID))
                continue;
            foundBlock = true;
            if (!settings.skips(state))
                return false;
        }
        return foundBlock;
    }

    public static SchematicannonBlockEntity openCannon(ServerPlayer player, int menuId) {
        if (!(player.containerMenu instanceof SchematicannonMenu menu) || menu.containerId != menuId)
            return null;
        SchematicannonBlockEntity cannon = menu.contentHolder;
        if (cannon == null || cannon.isRemoved() || cannon.getLevel() != player.level()
                || !cannon.canPlayerUse(player) || !menu.stillValid(player))
            return null;
        return cannon;
    }

    public static View inspect(SchematicannonBlockEntity cannon, int menuId) {
        ItemStack blueprint = cannon.inventory.getStackInSlot(0);
        int fingerprint = ItemStack.hashItemAndComponents(blueprint);
        if (!(blueprint.getItem() instanceof SchematicItem))
            return new View(menuId, fingerprint, List.of(), cannon.skipMissing, Result.NO_SCHEMATIC);
        try {
            StructureTemplate template = SchematicItem.loadSchematic(cannon.getLevel(), blueprint);
            List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).betterSchematics$getPalettes();
            Map<Block, Integer> counts = new HashMap<>();
            if (!palettes.isEmpty()) {
                for (StructureTemplate.StructureBlockInfo info : palettes.getFirst().blocks()) {
                    if (!info.state().isAir() && !info.state().is(Blocks.STRUCTURE_VOID))
                        counts.merge(info.state().getBlock(), 1, Integer::sum);
                }
            }
            if (counts.size() > CannonSkipPackets.MAX_BLOCK_TYPES)
                return new View(menuId, fingerprint, List.of(), cannon.skipMissing, Result.TOO_LARGE);
            Set<net.minecraft.resources.ResourceLocation> skipped = ((CannonSkipAccess) cannon)
                    .betterSchematics$getSkipSettings().skipped();
            List<BlockEntry> entries = counts.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> BuiltInRegistries.BLOCK.getKey(entry.getKey())))
                    .map(entry -> new BlockEntry(entry.getKey(), entry.getValue(),
                            skipped.contains(BuiltInRegistries.BLOCK.getKey(entry.getKey()))))
                    .toList();
            return new View(menuId, fingerprint, entries, cannon.skipMissing, Result.READY);
        } catch (RuntimeException exception) {
            CreateBetterSchematics.LOGGER.warn("Cannot read Schematicannon block list at {}", cannon.getBlockPos(), exception);
            return new View(menuId, fingerprint, List.of(), cannon.skipMissing, Result.FAILED);
        }
    }

    public static Result apply(ServerPlayer player, Save save) {
        SchematicannonBlockEntity cannon = openCannon(player, save.menuId());
        if (cannon == null)
            return Result.CLOSED;
        View current = inspect(cannon, save.menuId());
        if (current.result() != Result.READY || current.fingerprint() != save.fingerprint())
            return Result.CHANGED;
        Set<Block> allowed = current.entries().stream().map(BlockEntry::block).collect(Collectors.toSet());
        if (!allowed.containsAll(save.skipped()))
            return Result.CHANGED;
        ((CannonSkipAccess) cannon).betterSchematics$getSkipSettings().replace(save.skipped().stream()
                .map(BuiltInRegistries.BLOCK::getKey).collect(Collectors.toSet()));
        cannon.skipMissing = save.skipMissing();
        resumeSkippedTarget(cannon);
        cannon.updateChecklist();
        cannon.setChanged();
        cannon.sendUpdate = true;
        cannon.sendData();
        return Result.APPLIED;
    }

    public static void resumeSkippedTarget(SchematicannonBlockEntity cannon) {
        if (cannon.missingItem == null || !cannon.printer.isLoaded() || cannon.printer.isErrored()
                || cannon.printer.getPrintStage() == SchematicPrinter.PrintStage.ENTITIES)
            return;
        cannon.printer.handleCurrentTarget((pos, state, blockEntity) -> {
            if (((CannonSkipAccess) cannon).betterSchematics$getSkipSettings().skips(state)) {
                cannon.missingItem = null;
                if (cannon.state == SchematicannonBlockEntity.State.PAUSED && "missingBlock".equals(cannon.statusMsg)) {
                    cannon.state = SchematicannonBlockEntity.State.RUNNING;
                    cannon.statusMsg = "searching";
                }
                cannon.sendUpdate = true;
            }
        }, (pos, entity) -> {});
    }
}
