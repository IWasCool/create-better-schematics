package com.ourcreationstudios.create_better_schematics.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ourcreationstudios.create_better_schematics.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Transforms the electrical endpoints as well as the entity's visual position. */
public final class PowerGridWirePlacement {
    private final Map<UUID, Long> junctionIds = new HashMap<>();
    private final boolean legacyPositions;

    public PowerGridWirePlacement(boolean legacyPositions) {
        this.legacyPositions = legacyPositions;
    }

    public StructureTemplate.StructureEntityInfo process(StructureTemplate.StructureEntityInfo raw,
            StructureTemplate.StructureEntityInfo placed, StructurePlaceSettings settings, BlockPos anchor,
            StructureTemplate template, LevelReader world) {
        if (!PowerGridWires.isWire(raw.nbt.getString("id")))
            return placed;
        ListTag savedPosition = raw.nbt.getList("Pos", Tag.TAG_DOUBLE);
        if (savedPosition.size() != 3)
            return null;
        BlockPos sourceOrigin = new BlockPos((int) Math.round(savedPosition.getDouble(0) - raw.pos.x),
                (int) Math.round(savedPosition.getDouble(1) - raw.pos.y),
                (int) Math.round(savedPosition.getDouble(2) - raw.pos.z));
        CompoundTag data = placed.nbt.copy();
        if (!legacyPositions && !data.contains("PlacedLength", Tag.TAG_ANY_NUMERIC)
                && !data.getString("id").equals("powergrid:block_wire"))
            supplyLegacyLength(data, sourceOrigin, template, world);
        for (String key : java.util.List.of("Endpoint1", "Endpoint2"))
            if (data.contains(key, Tag.TAG_COMPOUND)
                    && !endpoint(data.getCompound(key), sourceOrigin, anchor, settings, template, 0))
                return null;
        for (Tag value : data.getList("Segments", Tag.TAG_COMPOUND)) {
            CompoundTag segment = (CompoundTag) value;
            Direction direction = Direction.from3DDataValue(segment.getInt("Direction"));
            direction = settings.getRotation().rotate(settings.getMirror().mirror(direction));
            segment.putInt("Direction", direction.get3DDataValue());
        }
        // Power Grid applies an extra offset on load if these coordinates don't match Entity.Pos.
        writePosition(data, "LastKnownPos", BlockPos.containing(placed.pos));
        return new StructureTemplate.StructureEntityInfo(placed.pos, placed.blockPos, data);
    }

    private boolean endpoint(CompoundTag endpoint, BlockPos origin, BlockPos anchor,
            StructurePlaceSettings settings, StructureTemplate template, int depth) {
        if (depth > 4)
            return false;
        int type = endpoint.getInt("Type");
        if (type == 7) { // Split power cord: both conductors reference block terminals.
            return endpoint.contains("Endpoint1", Tag.TAG_COMPOUND) && endpoint.contains("Endpoint2", Tag.TAG_COMPOUND)
                    && endpoint(endpoint.getCompound("Endpoint1"), origin, anchor, settings, template, depth + 1)
                    && endpoint(endpoint.getCompound("Endpoint2"), origin, anchor, settings, template, depth + 1);
        }
        if (type == 1) { // Free wire junction. A fresh ID prevents copies joining the original network.
            Vec3 local = new Vec3(endpoint.getFloat("X"), endpoint.getFloat("Y"), endpoint.getFloat("Z"))
                    .subtract(Vec3.atLowerCornerOf(origin));
            if (!inside(local, template) || !endpoint.hasUUID("Id"))
                return false;
            Vec3 position = transform(local, settings, anchor);
            long identity = junctionIds.computeIfAbsent(endpoint.getUUID("Id"), ignored -> UUID.randomUUID().getLeastSignificantBits());
            endpoint.putFloat("X", (float) position.x);
            endpoint.putFloat("Y", (float) position.y);
            endpoint.putFloat("Z", (float) position.z);
            endpoint.putUUID("Id", junctionId(position, identity));
            return true;
        }
        if (type == 0 || type == 5 || type == 6 || type == 8) {
            String key = type == 8 ? "Position" : "Pos";
            BlockPos old = readPosition(endpoint, key);
            if (old == null)
                return false;
            BlockPos local = old.subtract(origin);
            if (!includedBlock(local, template))
                return false;
            BlockPos transformed = StructureTemplate.calculateRelativePosition(settings, local).offset(anchor);
            // Socket and auto-cord endpoints use vanilla positions, unlike normal wire terminals.
            if (type == 6 || type == 8)
                writePosition(endpoint, key, transformed);
            else
                endpoint.putIntArray(key, new int[] { transformed.getX(), transformed.getY(), transformed.getZ() });
            if (type == 8) {
                Vec3 plug = new Vec3(endpoint.getDouble("X"), endpoint.getDouble("Y"), endpoint.getDouble("Z"));
                plug = transform(plug.subtract(Vec3.atLowerCornerOf(origin)), settings, anchor);
                endpoint.putDouble("X", plug.x);
                endpoint.putDouble("Y", plug.y);
                endpoint.putDouble("Z", plug.z);
                if (endpoint.contains("Plug", Tag.TAG_ANY_NUMERIC)) {
                    Direction facing = Direction.from3DDataValue(endpoint.getByte("Plug"));
                    endpoint.putByte("Plug", (byte) settings.getRotation().rotate(settings.getMirror().mirror(facing)).get3DDataValue());
                }
            }
            return true;
        }
        // Temporary placement endpoints refer to a player or an unfinished wire, not a completed build.
        return false;
    }

    private static boolean includedBlock(BlockPos local, StructureTemplate template) {
        var palettes = ((StructureTemplateAccessor) template).betterSchematics$getPalettes();
        if (palettes.isEmpty())
            return false;
        return palettes.get(0).blocks().stream().anyMatch(block -> block.pos().equals(local)
                && !block.state().isAir());
    }

    private static void supplyLegacyLength(CompoundTag data, BlockPos origin, StructureTemplate template, LevelReader world) {
        Vec3 first = endpointPosition(data.getCompound("Endpoint1"), origin, template, 0);
        Vec3 second = endpointPosition(data.getCompound("Endpoint2"), origin, template, 0);
        if (first == null || second == null)
            return;
        double horizontal = 1.01;
        double vertical = 1.2;
        // Query the installed wire definition, including datapack overrides, when available.
        if (world instanceof Level level) {
            try {
                var registry = Class.forName("org.patryk3211.powergrid.electricity.wire.registry.WireRegistry");
                var id = ResourceLocation.tryParse(data.getCompound("Item").getString("Id"));
                if (id != null) {
                    Object definition = registry.getMethod("forItem", Level.class, net.minecraft.world.item.Item.class)
                            .invoke(null, level, BuiltInRegistries.ITEM.get(id));
                    if (definition != null) {
                        horizontal = ((Number) definition.getClass().getMethod("horizontalCoefficient").invoke(definition)).doubleValue();
                        vertical = ((Number) definition.getClass().getMethod("verticalCoefficient").invoke(definition)).doubleValue();
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Older Power Grid releases use the default curve coefficients.
            }
        }
        Vec3 delta = second.subtract(first);
        double length = Math.sqrt(horizontal * (delta.x * delta.x + delta.z * delta.z) + vertical * delta.y * delta.y);
        if (Double.isFinite(length))
            data.putFloat("PlacedLength", (float) Math.max(length, .01));
    }

    private static Vec3 endpointPosition(CompoundTag endpoint, BlockPos origin, StructureTemplate template, int depth) {
        if (depth > 4)
            return null;
        int type = endpoint.getInt("Type");
        if (type == 7) {
            Vec3 a = endpointPosition(endpoint.getCompound("Endpoint1"), origin, template, depth + 1);
            Vec3 b = endpointPosition(endpoint.getCompound("Endpoint2"), origin, template, depth + 1);
            return a == null || b == null ? null : a.add(b).scale(.5);
        }
        if (type == 1)
            return new Vec3(endpoint.getFloat("X"), endpoint.getFloat("Y"), endpoint.getFloat("Z"));
        if (type == 8)
            return new Vec3(endpoint.getDouble("X"), endpoint.getDouble("Y"), endpoint.getDouble("Z"));
        BlockPos position = readPosition(endpoint, "Pos");
        if (position == null)
            return null;
        var palettes = ((StructureTemplateAccessor) template).betterSchematics$getPalettes();
        if (!palettes.isEmpty()) {
            BlockPos local = position.subtract(origin);
            for (var block : palettes.get(0).blocks()) {
                if (!block.pos().equals(local))
                    continue;
                try {
                    Object terminal = type == 6
                            ? block.state().getBlock().getClass().getMethod("socket", net.minecraft.world.level.block.state.BlockState.class)
                                    .invoke(block.state().getBlock(), block.state())
                            : block.state().getBlock().getClass().getMethod("terminal", net.minecraft.world.level.block.state.BlockState.class, int.class)
                                    .invoke(block.state().getBlock(), block.state(), endpoint.getInt("Terminal"));
                    if (terminal != null)
                        return ((Vec3) terminal.getClass().getMethod("getOrigin").invoke(terminal)).add(Vec3.atLowerCornerOf(position));
                } catch (ReflectiveOperationException ignored) {
                    // Retain an approximate legacy curve for addon terminal implementations.
                }
                break;
            }
        }
        return Vec3.atCenterOf(position);
    }

    private static boolean inside(Vec3 local, StructureTemplate template) {
        var size = template.getSize();
        return local.x >= 0 && local.y >= 0 && local.z >= 0
                && local.x <= size.getX() && local.y <= size.getY() && local.z <= size.getZ();
    }

    private static Vec3 transform(Vec3 local, StructurePlaceSettings settings, BlockPos anchor) {
        return StructureTemplate.transform(local, settings.getMirror(), settings.getRotation(), settings.getRotationPivot())
                .add(Vec3.atLowerCornerOf(anchor));
    }

    public static BlockPos readPosition(CompoundTag data, String key) {
        int[] coordinates = data.getIntArray(key);
        if (coordinates.length == 3)
            return new BlockPos(coordinates[0], coordinates[1], coordinates[2]);
        CompoundTag legacy = data.getCompound(key);
        if (legacy.contains("X", Tag.TAG_ANY_NUMERIC) && legacy.contains("Y", Tag.TAG_ANY_NUMERIC)
                && legacy.contains("Z", Tag.TAG_ANY_NUMERIC))
            return new BlockPos(legacy.getInt("X"), legacy.getInt("Y"), legacy.getInt("Z"));
        return null;
    }

    private static CompoundTag legacyPosition(BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("X", pos.getX());
        data.putInt("Y", pos.getY());
        data.putInt("Z", pos.getZ());
        return data;
    }

    private void writePosition(CompoundTag tag, String key, BlockPos pos) {
        if (legacyPositions)
            tag.put(key, legacyPosition(pos));
        else
            tag.putIntArray(key, new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    private static UUID junctionId(Vec3 position, long identity) {
        BlockPos pos = BlockPos.containing(position);
        long high = ((long) pos.getX() << 32) | ((pos.getY() & 0xffffL) << 16) | 0x8000L | ((identity >>> 28) & 0xfffL);
        long low = (pos.getZ() & 0xffffffffL) | Long.MIN_VALUE | ((identity & 0xfffffffL) << 32);
        return new UUID(high, low);
    }
}
