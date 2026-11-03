package com.ourcreationstudios.create_better_schematics.testsupport;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.ourcreationstudios.create_better_schematics.compat.SchematicFileFormat;
import com.ourcreationstudios.create_better_schematics.porter.AttributeFilterCompatibility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/** Historical converter retained only to exercise legacy files in compatibility tests. */
public final class SchematicPorter {
    public static final String THRESHOLD_UNIT = SchematicFileFormat.THRESHOLD_UNIT;
    private static final String PORT_DATA = "BetterSchematicsPort";
    private static final long MAX_NBT_BYTES = 128L * 1024 * 1024;
    private static final Set<String> ITEM_COMPONENTS = Set.of(
            "minecraft:custom_data", "minecraft:custom_name", "minecraft:lore", "minecraft:damage",
            "minecraft:repair_cost", "minecraft:unbreakable", "minecraft:enchantments",
            "minecraft:stored_enchantments", "create:filter_items", "create:filter_items_blacklist",
            "create:filter_items_respect_nbt", "create:package_address",
            AttributeFilterCompatibility.MODE, AttributeFilterCompatibility.RULES,
            "!minecraft:enchantments", "!minecraft:stored_enchantments", "!minecraft:attribute_modifiers");

    private SchematicPorter() {}

    public enum Target {
        FORGE_1_20_1("forge_1_20_1", "forge-1.20.1"),
        FABRIC_1_20_1("fabric_1_20_1", "fabric-1.20.1");

        private final String key;
        private final String suffix;

        Target(String key, String suffix) {
            this.key = key;
            this.suffix = suffix;
        }

        public String key() { return key; }
    }

    public record Notice(String key, String detail, int count) {}
    public record Conversion(CompoundTag data, int arms, int switches, int filters, List<Notice> notices) {}
    public record Export(Path file, Conversion conversion) {}

    public static Conversion convert(CompoundTag source, Target target) throws IOException {
        validate(source);
        // The existing compatibility writer supplies legacy item fields and arm positions.
        CompoundTag data = SchematicDataCompatibility.exportSchematic(source);
        Report report = new Report();
        visit(data, target, report, false, 0);
        CompoundTag provenance = source.getCompound(PORT_DATA).copy();
        if (!provenance.contains("SourceDataVersion"))
            provenance.putInt("SourceDataVersion", source.getInt("DataVersion"));
        provenance.putString("Target", target.key);
        provenance.putString("CreateVersion", "6.0");
        data.put(PORT_DATA, provenance);
        data.putInt("DataVersion", 3465);
        return new Conversion(data, report.arms, report.switches, report.filters, report.notices());
    }

    /** Saves a new, uniquely named sibling. Neither the source nor an earlier export is overwritten. */
    public static Export export(Path directory, String filename, Target target) throws IOException {
        Path root = directory.toRealPath();
        Path source = root.resolve(filename).normalize();
        if (!filename.endsWith(".nbt") || !root.equals(source.getParent())
                || !source.getFileName().toString().equals(filename)
                || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Select an existing .nbt file in the schematics folder.");
        Conversion conversion = convert(NbtIo.readCompressed(source, NbtAccounter.create(MAX_NBT_BYTES)), target);
        String basename = filename.substring(0, filename.length() - 4);
        // Leave room for the target suffix on filesystems with 255-character name limits.
        basename = basename.substring(0, Math.min(180, basename.length())) + "-" + target.suffix;
        Path temporary = Files.createTempFile(root, ".better-schematics-", ".tmp");
        try {
            NbtIo.writeCompressed(conversion.data, temporary);
            for (int number = 1; number <= 10_000; number++) {
                Path output = root.resolve(basename + (number == 1 ? "" : "-" + number) + ".nbt");
                try {
                    Files.move(temporary, output);
                    return new Export(output, conversion);
                } catch (FileAlreadyExistsException ignored) {
                    // Retrying must never replace a user's earlier file.
                }
            }
            throw new IOException("Too many existing exports with this name.");
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void validate(CompoundTag data) throws IOException {
        ListTag size = data.getList("size", Tag.TAG_INT);
        boolean palette = data.contains("palette", Tag.TAG_LIST) || data.contains("palettes", Tag.TAG_LIST);
        if (size.size() != 3 || size.getInt(0) <= 0 || size.getInt(1) <= 0 || size.getInt(2) <= 0
                || !data.contains("blocks", Tag.TAG_LIST) || !palette)
            throw new IOException("This file is not a Create/Minecraft structure schematic.");
    }

    private static void visit(CompoundTag tag, Target target, Report report, boolean configurationRoot, int depth)
            throws IOException {
        if (depth > 64)
            throw new IOException("Schematic configuration is nested too deeply to convert.");
        if (configurationRoot) {
            switch (tag.getString("id")) {
                case "create:mechanical_arm" -> report.arms++;
                case "create:stockpile_switch" -> {
                    report.switches++;
                    convertThresholdUnits(tag, target, report);
                }
                default -> { }
            }
        } else if (tag.contains("id", Tag.TAG_STRING) && tag.contains("Count", Tag.TAG_BYTE)) {
            if (tag.getString("id").equals("create:filter") || tag.getString("id").equals("create:attribute_filter"))
                report.filters++;
            CompoundTag components = tag.getCompound("components");
            if (tag.getString("id").equals("create:attribute_filter")) {
                if (!components.isEmpty() && AttributeFilterCompatibility.toLegacy(components) == null)
                    throw new IOException("This attribute filter contains an unsupported rule. The source file is unchanged.");
                for (Tag rule : tag.getCompound("tag").getList("MatchedAttributes", Tag.TAG_COMPOUND)) {
                    CompoundTag attribute = (CompoundTag) rule;
                    if (attribute.getString("attributeId").equals("create:in_tag")) {
                        String name = attribute.getString("tag");
                        if (target == Target.FORGE_1_20_1 && name.startsWith("c:"))
                            attribute.putString("tag", "forge:" + name.substring(2));
                        else if (target == Target.FABRIC_1_20_1 && name.startsWith("forge:"))
                            attribute.putString("tag", "c:" + name.substring(6));
                    }
                }
            }
            for (String component : components.getAllKeys())
                if (!ITEM_COMPONENTS.contains(component))
                    report.add("component", component);
            if (tag.getInt("count") > 127)
                report.add("stack_count", tag.getString("id"));
            tag.remove("count");
            tag.remove("components");
        }
        // Visit after removing the modern fields: list-filter entries otherwise occur twice.
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag value = tag.get(key);
            if (value instanceof CompoundTag child) {
                boolean blockOrEntity = key.equals("nbt") && (tag.contains("state", Tag.TAG_ANY_NUMERIC)
                        || tag.contains("blockPos", Tag.TAG_LIST));
                visit(child, target, report, blockOrEntity, depth + 1);
            } else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                for (Tag entry : list)
                    visit((CompoundTag) entry, target, report, false, depth + 1);
            }
        }
    }

    private static void convertThresholdUnits(CompoundTag tag, Target target, Report report) throws IOException {
        String unit = tag.getString(THRESHOLD_UNIT);
        if (!tag.contains("OnAboveAmount", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("OffBelowAmount", Tag.TAG_ANY_NUMERIC)) {
            // Create 6 cannot read percentage-only settings without the companion's migration.
            report.add("legacy_threshold", tag.getString("id"));
            return;
        }
        if (target == Target.FABRIC_1_20_1 && unit.isEmpty()) {
            report.add("threshold_unit", tag.getString("id"));
            return;
        }
        if (unit.equals("millibuckets") && target == Target.FABRIC_1_20_1) {
            scaleThreshold(tag, 81, 1);
            tag.putString(THRESHOLD_UNIT, "droplets");
        } else if (unit.equals("droplets") && target == Target.FORGE_1_20_1) {
            scaleThreshold(tag, 1, 81);
            tag.putString(THRESHOLD_UNIT, "millibuckets");
        }
    }

    private static void scaleThreshold(CompoundTag tag, int numerator, int denominator) throws IOException {
        for (String key : List.of("OnAboveAmount", "OffBelowAmount", "CurrentAmount", "CurrentMinAmount", "CurrentMaxAmount")) {
            if (!tag.contains(key, Tag.TAG_ANY_NUMERIC) || tag.getInt(key) < 0)
                continue;
            long scaled = Math.round((double) tag.getInt(key) * numerator / denominator);
            if (scaled > Integer.MAX_VALUE)
                throw new IOException("A fluid threshold exceeds the target Create version's integer limit.");
            tag.putInt(key, (int) scaled);
        }
    }

    private static final class Report {
        int arms;
        int switches;
        int filters;
        final Map<String, Integer> warnings = new TreeMap<>();

        void add(String key, String detail) { warnings.merge(key + "\n" + detail, 1, Integer::sum); }

        List<Notice> notices() {
            return warnings.entrySet().stream().map(entry -> {
                String[] parts = entry.getKey().split("\n", 2);
                return new Notice(parts[0], parts[1], entry.getValue());
            }).toList();
        }
    }
}
