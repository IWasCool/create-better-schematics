package com.ourcreationstudios.create_better_schematics.compat;

import java.util.ArrayList;

import com.ourcreationstudios.create_better_schematics.porter.AttributeFilterCompatibility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Supplements 1.20 stacks with native 1.21 components, retaining the original fields. */
final class PortableItemComponents {
    private PortableItemComponents() {}

    static void supplement(CompoundTag schematic) {
        visit(schematic, 0, false);
    }

    private static void visit(CompoundTag data, int depth, boolean configurationRoot) {
        if (depth > 64)
            return;
        for (String key : new ArrayList<>(data.getAllKeys())) {
            Tag value = data.get(key);
            if (value instanceof CompoundTag compound) {
                boolean blockOrEntity = key.equals("nbt")
                        && (data.contains("state", Tag.TAG_ANY_NUMERIC) || data.contains("blockPos", Tag.TAG_LIST));
                visit(compound, depth + 1, blockOrEntity);
            } else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                for (Tag entry : list)
                    visit((CompoundTag) entry, depth + 1, false);
            }
        }
        if (configurationRoot || !data.contains("id", Tag.TAG_STRING)
                || !data.contains("Count", Tag.TAG_BYTE) || data.contains("count", Tag.TAG_ANY_NUMERIC))
            return;

        CompoundTag legacy = data.getCompound("tag").copy();
        CompoundTag components = new CompoundTag();
        move(legacy, "Damage", components, "minecraft:damage");
        move(legacy, "RepairCost", components, "minecraft:repair_cost");
        if (legacy.getBoolean("Unbreakable"))
            components.put("minecraft:unbreakable", new CompoundTag());
        legacy.remove("Unbreakable");
        CompoundTag display = legacy.getCompound("display");
        move(display, "Name", components, "minecraft:custom_name");
        move(display, "Lore", components, "minecraft:lore");
        if (display.isEmpty())
            legacy.remove("display");
        enchantments(legacy, "Enchantments", components, "minecraft:enchantments");
        enchantments(legacy, "StoredEnchantments", components, "minecraft:stored_enchantments");

        if (data.getString("id").equals("create:filter")) {
            ListTag contents = new ListTag();
            for (Tag entry : legacy.getCompound("Items").getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag item = ((CompoundTag) entry).copy();
                int slot = item.getInt("Slot");
                if (slot < 0 || slot >= 18)
                    continue;
                item.remove("Slot");
                CompoundTag content = new CompoundTag();
                content.putInt("slot", slot);
                content.put("item", item);
                contents.add(content);
            }
            if (legacy.contains("Items", Tag.TAG_COMPOUND)) {
                components.put("create:filter_items", contents);
                legacy.remove("Items");
            }
            move(legacy, "Blacklist", components, "create:filter_items_blacklist");
            move(legacy, "RespectNBT", components, "create:filter_items_respect_nbt");
        }
        if (data.getString("id").equals("create:attribute_filter")) {
            CompoundTag attributes = AttributeFilterCompatibility.toComponents(legacy);
            // Unsupported addon predicates remain in their original encoding.
            if (attributes == null)
                return;
            components.merge(attributes);
            legacy.remove("WhitelistMode");
            legacy.remove("MatchedAttributes");
        }
        move(legacy, "Address", components, "create:package_address");
        if (!legacy.isEmpty())
            components.put("minecraft:custom_data", legacy);
        data.putInt("count", data.getByte("Count"));
        if (!components.isEmpty())
            data.put("components", components);
    }

    private static void move(CompoundTag source, String key, CompoundTag target, String destination) {
        Tag value = source.get(key);
        if (value != null) {
            target.put(destination, value.copy());
            source.remove(key);
        }
    }

    private static void enchantments(CompoundTag legacy, String key, CompoundTag components, String destination) {
        if (!legacy.contains(key, Tag.TAG_LIST))
            return;
        CompoundTag levels = new CompoundTag();
        for (Tag entry : legacy.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag enchantment = (CompoundTag) entry;
            levels.putInt(enchantment.getString("id"), enchantment.getInt("lvl"));
        }
        CompoundTag value = new CompoundTag();
        value.put("levels", levels);
        components.put(destination, value);
        legacy.remove(key);
    }
}
