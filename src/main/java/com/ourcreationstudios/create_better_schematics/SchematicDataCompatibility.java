package com.ourcreationstudios.create_better_schematics;

import java.util.ArrayList;

import com.mojang.serialization.Dynamic;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeConnections;
import com.ourcreationstudios.create_better_schematics.compat.SchematicFileFormat;
import com.ourcreationstudios.create_better_schematics.porter.SchematicPorter;
import com.ourcreationstudios.create_better_schematics.porter.AttributeFilterCompatibility;
import com.ourcreationstudios.create_better_schematics.porter.LegacyTextComponents;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/** Translates configuration data at the schematic file boundary, leaving the original intact. */
public final class SchematicDataCompatibility {
    private static final int MINECRAFT_1_20_1 = 3465;

    private SchematicDataCompatibility() {}

    public static CompoundTag importSchematic(CompoundTag schematic) {
        return convert(SchematicFileFormat.nativeCopy(schematic), false, 0);
    }

    public static CompoundTag exportSchematic(CompoundTag schematic) {
        return SchematicFileFormat.markExport(convert(schematic.copy(), true, 0), "neoforge");
    }

    private static CompoundTag convert(CompoundTag tag, boolean exporting, int depth) {
        return convert(tag, exporting, depth, false);
    }

    private static CompoundTag convert(CompoundTag tag, boolean exporting, int depth, boolean configurationRoot) {
        if (depth > 64)
            return tag;

        if (!configurationRoot && !exporting && isItem(tag) && tag.contains("Count", Tag.TAG_BYTE)
                && !tag.contains("count", Tag.TAG_ANY_NUMERIC))
            tag = upgradeItem(tag, depth);

        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound) {
                // Addon block entities can also have an id and Count (for example, a level emitter).
                // Only their nested item stacks belong in Minecraft's item data fixer.
                boolean blockOrEntity = key.equals("nbt") && (tag.contains("state", Tag.TAG_ANY_NUMERIC)
                        || tag.contains("blockPos", Tag.TAG_LIST));
                tag.put(key, convert(compound, exporting, depth + 1, blockOrEntity));
            } else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                for (int i = 0; i < list.size(); i++)
                    list.set(i, convert(list.getCompound(i), exporting, depth + 1));
            }
        }

        FactoryGaugeConnections.convert(tag, exporting);
        if (!exporting)
            SchematicPorter.normalizeImportedThreshold(tag);
        if (exporting) {
            if (!configurationRoot && isItem(tag) && tag.contains("count", Tag.TAG_ANY_NUMERIC))
                addLegacyItemData(tag);
            // Catnip accepts both formats; Forge 1.20.1 only accepts the compound form.
            for (Tag entry : tag.getList("InteractionPoints", Tag.TAG_COMPOUND)) {
                CompoundTag point = (CompoundTag) entry;
                int[] pos = point.getIntArray("Pos");
                if (pos.length == 3)
                    point.put("Pos", legacyPosition(new BlockPos(pos[0], pos[1], pos[2])));
            }
        }
        return tag;
    }

    private static boolean isItem(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING)
                && (tag.contains("Count", Tag.TAG_BYTE) || tag.contains("count", Tag.TAG_ANY_NUMERIC));
    }

    private static CompoundTag upgradeItem(CompoundTag legacy, int depth) {
        // Replace Create's stock attribute fixer, which emits an incompatible component type.
        CompoundTag attributeComponents = null;
        if (legacy.getString("id").equals("create:attribute_filter")) {
            attributeComponents = AttributeFilterCompatibility.toComponents(legacy.getCompound("tag"));
            if (attributeComponents == null)
                return legacy;
        }
        // Includes Create's component migration, which schematic loading normally bypasses.
        CompoundTag fixerInput = legacy.copy();
        if (attributeComponents != null)
            fixerInput.putString("id", "minecraft:paper");
        CompoundTag upgraded = (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK,
                new Dynamic<Tag>(NbtOps.INSTANCE, fixerInput), MINECRAFT_1_20_1,
                SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (attributeComponents != null) {
            upgraded.putString("id", legacy.getString("id"));
            CompoundTag components = upgraded.getCompound("components");
            components.merge(attributeComponents);
            CompoundTag custom = components.getCompound("minecraft:custom_data");
            custom.remove("WhitelistMode");
            custom.remove("MatchedAttributes");
            if (custom.isEmpty())
                components.remove("minecraft:custom_data");
            upgraded.put("components", components);
        }
        if (legacy.getString("id").equals("create:filter")) {
            CompoundTag settings = legacy.getCompound("tag");
            if (settings.contains("Items", Tag.TAG_COMPOUND)) {
                ListTag contents = new ListTag();
                for (Tag entry : settings.getCompound("Items").getList("Items", Tag.TAG_COMPOUND)) {
                    CompoundTag item = ((CompoundTag) entry).copy();
                    int slot = item.getInt("Slot");
                    if (slot < 0 || slot >= 18)
                        continue;
                    item.remove("Slot");
                    CompoundTag content = new CompoundTag();
                    content.putInt("slot", slot);
                    // Create's stock fixer copies only id/count here, losing nested item data.
                    content.put("item", convert(item, false, depth + 1));
                    contents.add(content);
                }
                CompoundTag components = upgraded.getCompound("components");
                components.put("create:filter_items", contents);
                upgraded.put("components", components);
            }
        }
        return upgraded;
    }

    private static void addLegacyItemData(CompoundTag item) {
        item.putByte("Count", (byte) Math.clamp(item.getInt("count"), 0, 127));
        CompoundTag components = item.getCompound("components");
        CompoundTag legacy = components.getCompound("minecraft:custom_data").copy();
        copy(components, "minecraft:damage", legacy, "Damage");
        copy(components, "minecraft:repair_cost", legacy, "RepairCost");
        if (components.contains("minecraft:unbreakable"))
            legacy.putBoolean("Unbreakable", true);
        if (components.contains("minecraft:custom_name") || components.contains("minecraft:lore")) {
            CompoundTag display = legacy.getCompound("display");
            if (components.contains("minecraft:custom_name", Tag.TAG_STRING))
                display.putString("Name", LegacyTextComponents.convert(components.getString("minecraft:custom_name")));
            if (components.contains("minecraft:lore", Tag.TAG_LIST)) {
                ListTag lore = new ListTag();
                for (Tag line : components.getList("minecraft:lore", Tag.TAG_STRING))
                    lore.add(StringTag.valueOf(LegacyTextComponents.convert(line.getAsString())));
                display.put("Lore", lore);
            }
            legacy.put("display", display);
        }
        copyEnchantments(components, "minecraft:enchantments", legacy, "Enchantments");
        copyEnchantments(components, "minecraft:stored_enchantments", legacy, "StoredEnchantments");
        // Component removals are explicit patches, used on Create's filter items too.
        if (components.contains("!minecraft:enchantments"))
            legacy.put("Enchantments", new ListTag());
        if (components.contains("!minecraft:stored_enchantments"))
            legacy.put("StoredEnchantments", new ListTag());
        if (components.contains("!minecraft:attribute_modifiers"))
            legacy.put("AttributeModifiers", new ListTag());
        if (item.getString("id").equals("create:filter")) {
            if (components.contains("create:filter_items", Tag.TAG_LIST)) {
                CompoundTag inventory = new CompoundTag();
                inventory.putInt("Size", 18);
                ListTag items = new ListTag();
                for (Tag entry : components.getList("create:filter_items", Tag.TAG_COMPOUND)) {
                    CompoundTag content = (CompoundTag) entry;
                    CompoundTag stack = content.getCompound("item").copy();
                    stack.putInt("Slot", content.getInt("slot"));
                    items.add(stack);
                }
                inventory.put("Items", items);
                legacy.put("Items", inventory);
            }
            copy(components, "create:filter_items_blacklist", legacy, "Blacklist");
            copy(components, "create:filter_items_respect_nbt", legacy, "RespectNBT");
        }
        copy(components, "create:package_address", legacy, "Address");
        if (item.getString("id").equals("create:attribute_filter")) {
            CompoundTag attributes = AttributeFilterCompatibility.toLegacy(components);
            if (attributes != null)
                legacy.merge(attributes);
        }
        if (!legacy.isEmpty())
            item.put("tag", legacy);
    }

    private static void copy(CompoundTag from, String source, CompoundTag to, String target) {
        Tag value = from.get(source);
        if (value != null)
            to.put(target, value.copy());
    }

    private static void copyEnchantments(CompoundTag components, String source, CompoundTag legacy, String target) {
        if (!components.contains(source, Tag.TAG_COMPOUND))
            return;
        ListTag entries = new ListTag();
        CompoundTag levels = components.getCompound(source).getCompound("levels");
        for (String id : levels.getAllKeys()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putShort("lvl", (short) levels.getInt(id));
            entries.add(entry);
        }
        legacy.put(target, entries);
    }

    private static CompoundTag legacyPosition(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }
}
