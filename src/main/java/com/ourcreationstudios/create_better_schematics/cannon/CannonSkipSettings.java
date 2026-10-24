package com.ourcreationstudios.create_better_schematics.cannon;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class CannonSkipSettings {
    private static final String NBT_KEY = "CreateBetterSchematicsSkippedBlocks";
    private final Set<ResourceLocation> skipped = new HashSet<>();

    public Set<ResourceLocation> skipped() {
        return Set.copyOf(skipped);
    }

    public void replace(Set<ResourceLocation> blocks) {
        skipped.clear();
        skipped.addAll(blocks);
    }

    public boolean skips(BlockState state) {
        return skipped.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public void read(CompoundTag tag) {
        skipped.clear();
        for (Tag entry : tag.getList(NBT_KEY, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null)
                skipped.add(id);
        }
    }

    public void write(CompoundTag tag) {
        ListTag list = new ListTag();
        skipped.stream().sorted().forEach(id -> list.add(StringTag.valueOf(id.toString())));
        tag.put(NBT_KEY, list);
    }
}
