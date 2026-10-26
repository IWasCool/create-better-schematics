package com.ourcreationstudios.create_better_schematics.compat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Optional metadata inside an ordinary Create/Minecraft structure, never a required custom format. */
public final class SchematicFileFormat {
    public static final String METADATA = "CreateBetterSchematics";
    public static final int VERSION = 1;
    public static final String THRESHOLD_UNIT = "BetterSchematicsThresholdUnit";

    private SchematicFileFormat() {}

    /** Adds optional provenance and native item equivalents without changing DataVersion. */
    public static CompoundTag markExport(CompoundTag source, String loader) {
        CompoundTag result = nativeCopy(source);
        if (!result.contains("blocks", Tag.TAG_LIST) || result.getList("size", Tag.TAG_INT).size() != 3)
            return result;
        PortableItemComponents.supplement(result);
        CompoundTag metadata = result.getCompound(METADATA).copy();
        metadata.putInt("FormatVersion", VERSION);
        if (!metadata.contains("SourceDataVersion", Tag.TAG_ANY_NUMERIC))
            metadata.putInt("SourceDataVersion", result.getInt("DataVersion"));
        if (!metadata.contains("SourceLoader", Tag.TAG_STRING))
            metadata.putString("SourceLoader", loader);
        result.put(METADATA, metadata);
        return result;
    }

    /** The 1.20.1 companions adapt an in-memory copy; the shared source file stays native. */
    public static CompoundTag forLegacyReader(CompoundTag source, boolean fabric) throws IOException {
        CompoundTag result = nativeCopy(source);
        convert(result, fabric, 0);
        if (result.getInt("DataVersion") > 3465)
            result.putInt("DataVersion", 3465);
        return result;
    }

    /** Migrate the old addon-only belt property into Create's own block entity NBT. */
    public static CompoundTag nativeCopy(CompoundTag source) {
        CompoundTag result = source.copy();
        List<ListTag> palettes = new ArrayList<>();
        for (Tag entry : result.getList("palettes", Tag.TAG_LIST))
            palettes.add((ListTag) entry);
        if (palettes.isEmpty())
            palettes.add(result.getList("palette", Tag.TAG_COMPOUND));
        Map<Integer, String> materials = new HashMap<>();
        boolean first = true;
        for (ListTag palette : palettes) {
            Map<Integer, String> variant = new HashMap<>();
            for (int index = 0; index < palette.size(); index++) {
                CompoundTag state = palette.getCompound(index);
                if (!state.getString("Name").equals("create:belt"))
                    continue;
                CompoundTag properties = state.getCompound("Properties");
                String material = properties.getString("create_better_schematics_casing");
                properties.remove("create_better_schematics_casing");
                if (properties.getString("casing").equals("true")
                        && (material.equals("andesite") || material.equals("brass")))
                    variant.put(index, material.toUpperCase(java.util.Locale.ROOT));
            }
            if (first)
                materials.putAll(variant);
            else
                // Alternate palettes share block NBT: do not invent a material
                // when two variants describe different casings for the same index.
                materials.entrySet().removeIf(entry -> !entry.getValue().equals(variant.get(entry.getKey())));
            first = false;
        }
        for (Tag entry : result.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            String material = materials.get(block.getInt("state"));
            if (material == null)
                continue;
            CompoundTag nbt = block.getCompound("nbt");
            nbt.putString("id", "create:belt");
            nbt.putString("Casing", material);
            block.put("nbt", nbt);
        }
        return result;
    }

    private static void convert(CompoundTag data, boolean fabric, int depth) throws IOException {
        if (depth > 64)
            throw new IOException("Schematic configuration is nested too deeply to read safely.");
        if (data.getString("id").equals("create:stockpile_switch"))
            convertThresholdUnits(data, fabric);
        for (Tag value : data.getList("InteractionPoints", Tag.TAG_COMPOUND)) {
            CompoundTag point = (CompoundTag) value;
            int[] pos = point.getIntArray("Pos");
            if (pos.length == 3) {
                CompoundTag legacy = new CompoundTag();
                legacy.putInt("X", pos[0]);
                legacy.putInt("Y", pos[1]);
                legacy.putInt("Z", pos[2]);
                point.put("Pos", legacy);
            }
        }
        if (data.getString("id").equals("create:attribute_filter")) {
            for (Tag value : data.getCompound("tag").getList("MatchedAttributes", Tag.TAG_COMPOUND)) {
                CompoundTag rule = (CompoundTag) value;
                if (!rule.getString("attributeId").equals("create:in_tag"))
                    continue;
                String tag = rule.getString("tag");
                if (fabric && tag.startsWith("forge:"))
                    rule.putString("tag", "c:" + tag.substring(6));
                else if (!fabric && tag.startsWith("c:"))
                    rule.putString("tag", "forge:" + tag.substring(2));
            }
        }
        for (String key : new ArrayList<>(data.getAllKeys())) {
            Tag value = data.get(key);
            if (value instanceof CompoundTag compound)
                convert(compound, fabric, depth + 1);
            else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND)
                for (Tag entry : list)
                    convert((CompoundTag) entry, fabric, depth + 1);
        }
    }

    private static void convertThresholdUnits(CompoundTag data, boolean fabric) throws IOException {
        String unit = data.getString(THRESHOLD_UNIT);
        int numerator = fabric && unit.equals("millibuckets") ? 81 : 1;
        int denominator = !fabric && unit.equals("droplets") ? 81 : 1;
        if (numerator == denominator)
            return; // Unknown units are preserved rather than guessed from an unrelated inventory.
        for (String key : List.of("OnAboveAmount", "OffBelowAmount", "CurrentAmount", "CurrentMinAmount", "CurrentMaxAmount")) {
            if (!data.contains(key, Tag.TAG_ANY_NUMERIC) || data.getInt(key) < 0)
                continue;
            long scaled = Math.round((double) data.getInt(key) * numerator / denominator);
            if (scaled > Integer.MAX_VALUE)
                throw new IOException("A fluid threshold exceeds this Create version's integer limit.");
            data.putInt(key, (int) scaled);
        }
        data.putString(THRESHOLD_UNIT, fabric ? "droplets" : "millibuckets");
    }
}
