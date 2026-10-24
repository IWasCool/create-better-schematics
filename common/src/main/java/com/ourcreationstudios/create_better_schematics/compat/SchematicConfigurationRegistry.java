package com.ourcreationstudios.create_better_schematics.compat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Opt-in configuration fields for addons that do not implement Create's safe-NBT API.
 */
public final class SchematicConfigurationRegistry {
    private static final Map<ResourceLocation, Set<String>> FIELDS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> BEHAVIOURS = new ConcurrentHashMap<>();
    private static final Map<String, CompoundTag> DEFAULTS = new ConcurrentHashMap<>();

    static {
        register(ResourceLocation.parse("toms_storage:level_emitter"), "Filter", "Count", "lessThan");
        register(ResourceLocation.parse("toms_storage:basic_inventory_hopper"), "Filter");
        register(ResourceLocation.parse("toms_storage:inv_cable_connector"), "channel", "block");
        register(ResourceLocation.parse("toms_storage:painted"), "block");
        register(ResourceLocation.parse("toms_storage:inv_proxy"), "block");
        register(ResourceLocation.parse("powergrid:energy_meter"), "Wh");
        AddonConfigurationProfiles.register();
    }

    private SchematicConfigurationRegistry() {
    }

    /**
     * Call during common setup with the block entity type ID and its configuration keys.
     */
    public static void register(ResourceLocation blockEntityType, String... configurationKeys) {
        add(FIELDS, blockEntityType, configurationKeys);
    }

    /**
     * Matches a class and its subclasses without loading an optional addon's classes.
     */
    public static void registerClass(String className, String... configurationKeys) {
        add(CLASSES, className, configurationKeys);
    }

    /**
     * Supplements the safe data of every SmartBlockEntity hosting this behaviour.
     */
    public static void registerBehaviour(String className, String... configurationKeys) {
        add(BEHAVIOURS, className, configurationKeys);
    }

    /**
     * Placement-only values, for addons whose readers interpret a missing runtime field as active.
     */
    public static void registerPlacementDefaults(String className, CompoundTag defaults) {
        if (defaults.getAllKeys().stream().anyMatch(Set.of("id", "x", "y", "z")::contains))
            throw new IllegalArgumentException("Placement defaults must not override block identity");
        DEFAULTS.merge(className, defaults.copy(), (existing, added) -> existing.copy().merge(added));
    }

    private static <K> void add(Map<K, Set<String>> profiles, K key, String... configurationKeys) {
        Set<String> fields = Set.of(configurationKeys);
        for (String field : fields) {
            String[] path = field.split("\\.", -1);
            if (java.util.Arrays.stream(path).anyMatch(String::isBlank)
                    || Set.of("id", "x", "y", "z", "*").contains(path[0]) || path[path.length - 1].equals("*"))
                throw new IllegalArgumentException("Schematic profiles must name configuration fields, not block identity");
        }
        profiles.merge(key, fields, (existing, added) -> {
            Set<String> merged = new HashSet<>(existing);
            merged.addAll(added);
            return Set.copyOf(merged);
        });
    }

    public static boolean supports(BlockEntity blockEntity) {
        return !fieldsFor(blockEntity).isEmpty() || !defaultsFor(blockEntity.getClass()).isEmpty();
    }

    public static CompoundTag merge(BlockEntity blockEntity, CompoundTag safe, CompoundTag full) {
        Set<String> fields = fieldsFor(blockEntity);
        CompoundTag defaults = defaultsFor(blockEntity.getClass());
        if (fields.isEmpty() && defaults.isEmpty())
            return safe;
        CompoundTag result = safe == null ? new CompoundTag() : safe.copy();
        for (String field : fields)
            copyField(full, result, field.split("\\."), 0);
        result.merge(defaults);
        return result;
    }

    private static CompoundTag defaultsFor(Class<?> type) {
        CompoundTag result = new CompoundTag();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            CompoundTag defaults = DEFAULTS.get(current.getName());
            if (defaults != null)
                for (String key : defaults.getAllKeys())
                    if (!result.contains(key))
                        result.put(key, defaults.get(key).copy());
        }
        return result;
    }

    private static Set<String> fieldsFor(BlockEntity blockEntity) {
        Set<String> fields = new HashSet<>();
        Set<String> registered = FIELDS.get(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
        if (registered != null)
            fields.addAll(registered);
        addClassFields(blockEntity.getClass(), CLASSES, fields);
        if (blockEntity instanceof SmartBlockEntity smart)
            smart.getAllBehaviours().forEach(behaviour -> addClassFields(behaviour.getClass(), BEHAVIOURS, fields));
        return fields;
    }

    private static void addClassFields(Class<?> type, Map<String, Set<String>> profiles, Set<String> fields) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Set<String> registered = profiles.get(current.getName());
            if (registered != null)
                fields.addAll(registered);
        }
    }

    private static void copyField(CompoundTag from, CompoundTag to, String[] path, int index) {
        String key = path[index];
        if (index == path.length - 1) {
            if (from.contains(key))
                to.put(key, from.get(key).copy());
        } else if (index + 2 < path.length && path[index + 1].equals("*") && from.contains(key, Tag.TAG_LIST)) {
            ListTag source = from.getList(key, Tag.TAG_COMPOUND);
            ListTag target = to.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < source.size(); i++) {
                CompoundTag entry = i < target.size() ? target.getCompound(i) : new CompoundTag();
                copyField(source.getCompound(i), entry, path, index + 2);
                if (i >= target.size())
                    target.add(entry);
            }
            to.put(key, target);
        } else if (from.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag target = to.getCompound(key);
            copyField(from.getCompound(key), target, path, index + 1);
            if (!target.isEmpty())
                to.put(key, target);
        }
    }
}
