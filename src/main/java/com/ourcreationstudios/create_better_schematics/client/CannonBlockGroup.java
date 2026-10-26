package com.ourcreationstudios.create_better_schematics.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.BlockEntry;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Presents obtainable items while retaining every source block type for Apply. */
public record CannonBlockGroup(ItemStack icon, List<BlockEntry> variants) {
    public CannonBlockGroup {
        variants = List.copyOf(variants);
    }

    public Component name() { return icon.isEmpty() ? variants.getFirst().block().getName() : icon.getHoverName(); }

    public ResourceLocation id() {
        return icon.isEmpty() ? BuiltInRegistries.BLOCK.getKey(variants.getFirst().block())
                : BuiltInRegistries.ITEM.getKey(icon.getItem());
    }

    public int count() { return variants.stream().mapToInt(BlockEntry::count).sum(); }

    public boolean skipped(Set<Block> skipped) { return variants.stream().allMatch(entry -> skipped.contains(entry.block())); }

    public boolean partiallySkipped(Set<Block> skipped) {
        return !skipped(skipped) && variants.stream().anyMatch(entry -> skipped.contains(entry.block()));
    }

    public void toggle(Set<Block> skipped) {
        if (skipped(skipped))
            variants.forEach(entry -> skipped.remove(entry.block()));
        else
            variants.forEach(entry -> skipped.add(entry.block()));
    }

    public static List<CannonBlockGroup> group(List<BlockEntry> entries) {
        var groups = new LinkedHashMap<Object, List<BlockEntry>>();
        var icons = new LinkedHashMap<Object, ItemStack>();
        for (var entry : entries) {
            ItemStack icon = itemFor(entry.block());
            Object key = icon.isEmpty() ? entry.block() : icon.getItem();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
            icons.putIfAbsent(key, icon);
        }
        return groups.entrySet().stream().map(entry -> new CannonBlockGroup(icons.get(entry.getKey()), entry.getValue())).toList();
    }

    private static ItemStack itemFor(Block block) {
        ItemStack item = new ItemStack(block.asItem());
        if (block instanceof SpecialBlockItemRequirement || item.isEmpty()) {
            try {
                var requirement = ItemRequirement.of(block.defaultBlockState(), null);
                var required = requirement.getRequiredItems();
                // Composite requirements (such as girder shafts) keep their own block row.
                if (!requirement.isInvalid() && required.size() == 1
                        && required.getFirst().usage == ItemRequirement.ItemUseType.CONSUME
                        && !required.getFirst().stack.isEmpty())
                    return required.getFirst().stack.copyWithCount(1);
            } catch (RuntimeException ignored) {
                // Some optional blocks need a live block entity even for their requirements.
            }
        }
        return item;
    }
}
