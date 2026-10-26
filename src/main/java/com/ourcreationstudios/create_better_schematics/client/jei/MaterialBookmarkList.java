package com.ourcreationstudios.create_better_schematics.client.jei;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterial;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Tracks the imported list separately from personal JEI bookmarks. */
public final class MaterialBookmarkList {
    private final Map<CannonMaterial.Key, Entry> entries = new LinkedHashMap<>();

    public void clear() { entries.clear(); }

    public interface Bookmarks {
        boolean contains(ItemStack stack);
        boolean add(ItemStack stack);
        void remove(ItemStack stack);
    }

    public void replace(List<CannonMaterial> materials, Bookmarks bookmarks) {
        Map<CannonMaterial.Key, Entry> replacement = new LinkedHashMap<>();
        for (CannonMaterial material : materials) {
            Entry previous = entries.get(material.key());
            Entry duplicate = replacement.get(material.key());
            int count = duplicate == null ? material.count() : Math.addExact(duplicate.material.count(), material.count());
            replacement.put(material.key(), new Entry(new CannonMaterial(material.stack(), count),
                    previous != null && previous.owned));
        }
        for (var entry : entries.entrySet())
            if (entry.getValue().owned && !replacement.containsKey(entry.getKey()))
                bookmarks.remove(entry.getValue().material.stack());
        for (var entry : replacement.entrySet()) {
            Entry value = entry.getValue();
            if (!bookmarks.contains(value.material.stack())) {
                if (!bookmarks.add(value.material.stack()))
                    throw new IllegalStateException("JEI could not add a material bookmark");
                entry.setValue(new Entry(value.material, true));
            }
        }
        entries.clear();
        entries.putAll(replacement);
    }

    public int count(ItemStack stack) {
        Entry entry = entries.get(new CannonMaterial.Key(stack));
        return entry == null ? 0 : entry.material.count();
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag result = new CompoundTag();
        result.putInt("Version", 1);
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag value = new CompoundTag();
            value.put("Item", entry.material.stack().save(registries));
            value.putInt("Amount", entry.material.count());
            value.putBoolean("Owned", entry.owned);
            list.add(value);
        }
        result.put("Materials", list);
        return result;
    }

    public void read(CompoundTag source, HolderLookup.Provider registries) {
        entries.clear();
        if (source.getInt("Version") != 1)
            return;
        ListTag list = source.getList("Materials", Tag.TAG_COMPOUND);
        if (list.size() > CannonMaterialsPackets.MAX_ENTRIES)
            throw new IllegalArgumentException("Material bookmark file has too many entries");
        for (Tag value : list) {
            CompoundTag tag = (CompoundTag) value;
            int count = tag.getInt("Amount");
            if (count <= 0)
                continue;
            ItemStack.parse(registries, tag.getCompound("Item")).ifPresent(stack -> {
                CannonMaterial material = new CannonMaterial(stack, count);
                entries.put(material.key(), new Entry(material, tag.getBoolean("Owned")));
            });
        }
    }

    private record Entry(CannonMaterial material, boolean owned) {}
}
