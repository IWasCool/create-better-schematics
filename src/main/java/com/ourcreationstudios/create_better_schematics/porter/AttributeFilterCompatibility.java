package com.ourcreationstudios.create_better_schematics.porter;

import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;

/** Create 6's 1.20 attribute NBT and 1.21 attribute codecs use different field names. */
public final class AttributeFilterCompatibility {
    public static final String RULES = "create:attribute_filter_matched_attributes";
    public static final String MODE = "create:attribute_filter_whitelist_mode";
    private static final String[] MODES = {"whitelist_disj", "whitelist_conj", "blacklist"};
    private static final Map<String, String> FIELDS = Map.of(
            "create:in_tag", "tag", "create:in_item_group", "group", "create:added_by", "modId",
            "create:has_enchant", "enchantId", "create:shulker_fill_level", "level",
            "create:has_fluid", "fluidId", "create:has_name", "name", "create:book_author", "author",
            "create:book_copy", "generation");
    private static final Set<String> SINGLETONS = Set.of("placeable", "consumable", "fluid_container", "enchanted",
            "max_enchanted", "renamed", "damaged", "badly_damaged", "not_stackable", "equipable", "furnace_fuel",
            "washable", "hauntable", "crushable", "smeltable", "smokable", "blastable", "compostable");

    private AttributeFilterCompatibility() {}

    /** Null means that changing the filter could change its logic: do not partially convert it. */
    public static CompoundTag toLegacy(CompoundTag components) {
        CompoundTag settings = new CompoundTag();
        String mode = components.contains(MODE) ? components.getString(MODE) : MODES[0];
        int modeIndex = java.util.List.of(MODES).indexOf(mode);
        if (modeIndex < 0)
            return null;
        settings.putInt("WhitelistMode", modeIndex);
        ListTag rules = new ListTag();
        for (Tag entry : components.getList(RULES, Tag.TAG_COMPOUND)) {
            CompoundTag rule = (CompoundTag) entry;
            CompoundTag attribute = rule.getCompound("attribute");
            String type = attribute.getString("type");
            CompoundTag legacy = new CompoundTag();
            legacy.putString("attributeId", type);
            if (FIELDS.containsKey(type)) {
                Tag value = attribute.get("value");
                if (value == null)
                    return null;
                legacy.put(FIELDS.get(type), value.copy());
            } else if (type.equals("create:has_color")) {
                DyeColor color = DyeColor.byName(attribute.getString("value"), null);
                if (color == null)
                    return null;
                legacy.putInt("color", color.getId());
            } else if (!type.startsWith("create:") || !SINGLETONS.contains(type.substring(7))) {
                return null;
            }
            legacy.putBoolean("Inverted", rule.getBoolean("inverted"));
            rules.add(legacy);
        }
        settings.put("MatchedAttributes", rules);
        return settings;
    }

    public static CompoundTag toComponents(CompoundTag settings) {
        int mode = settings.getInt("WhitelistMode");
        if (mode < 0 || mode >= MODES.length)
            return null;
        ListTag rules = new ListTag();
        for (Tag entry : settings.getList("MatchedAttributes", Tag.TAG_COMPOUND)) {
            CompoundTag legacy = (CompoundTag) entry;
            String type = legacy.getString("attributeId");
            CompoundTag attribute = new CompoundTag();
            attribute.putString("type", type);
            if (FIELDS.containsKey(type)) {
                Tag value = legacy.get(FIELDS.get(type));
                if (value == null)
                    return null;
                if (type.equals("create:in_tag") && legacy.getString("tag").startsWith("forge:"))
                    attribute.putString("value", "c:" + legacy.getString("tag").substring(6));
                else
                    attribute.put("value", value.copy());
            } else if (type.equals("create:has_color")) {
                attribute.putString("value", DyeColor.byId(legacy.getInt("color")).getSerializedName());
            } else if (!type.startsWith("create:") || !SINGLETONS.contains(type.substring(7))) {
                return null;
            } else {
                attribute.put("value", new CompoundTag());
            }
            CompoundTag rule = new CompoundTag();
            rule.put("attribute", attribute);
            rule.putBoolean("inverted", legacy.getBoolean("Inverted"));
            rules.add(rule);
        }
        CompoundTag components = new CompoundTag();
        components.putString(MODE, MODES[mode]);
        components.put(RULES, rules);
        return components;
    }
}
