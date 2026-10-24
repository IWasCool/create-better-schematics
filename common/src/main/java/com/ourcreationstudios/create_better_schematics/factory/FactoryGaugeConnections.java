package com.ourcreationstudios.create_better_schematics.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** The two Create 6 loaders use different NBT for the same gauge connections. */
public final class FactoryGaugeConnections {
    public static final String ORIGIN = "CreateBetterSchematicsGaugeOrigin";
    public static final List<String> SLOTS = List.of("top_left", "top_right", "bottom_left", "bottom_right");

    private FactoryGaugeConnections() {}

    public static void convert(CompoundTag tag, boolean exporting) {
        convertList(tag.getList("Targeting", Tag.TAG_COMPOUND), false, exporting);
        convertList(tag.getList("LinkedGauges", Tag.TAG_COMPOUND), false, exporting);
        convertList(tag.getList("TargetedBy", Tag.TAG_COMPOUND), true, exporting);
        convertList(tag.getList("TargetedByLinks", Tag.TAG_COMPOUND), true, exporting);
    }

    private static void convertList(ListTag list, boolean connection, boolean exporting) {
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            CompoundTag position = connection ? entry.getCompound("position") : entry;
            if (!validPosition(position) && legacyPosition(entry)) {
                if (connection)
                    position = new CompoundTag();
                position.putIntArray("pos", new int[] { entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z") });
                position.putString("slot", SLOTS.get(Math.floorMod(entry.getInt("Slot"), SLOTS.size())));
                if (connection)
                    entry.put("position", position);
            }
            if (connection) {
                if (!entry.contains("amount", Tag.TAG_ANY_NUMERIC) && entry.contains("Amount", Tag.TAG_ANY_NUMERIC))
                    entry.putInt("amount", entry.getInt("Amount"));
                if (!entry.contains("arrow_bending", Tag.TAG_ANY_NUMERIC))
                    entry.putInt("arrow_bending", entry.contains("ArrowBending", Tag.TAG_ANY_NUMERIC)
                            ? entry.getInt("ArrowBending") : -1);
            }
            if (!exporting || !validPosition(position))
                continue;
            BlockPos pos = position(position);
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            entry.putInt("Slot", SLOTS.indexOf(position.getString("slot")));
            if (connection) {
                entry.putInt("Amount", entry.getInt("amount"));
                entry.putInt("ArrowBending", entry.getInt("arrow_bending"));
            }
        }
    }

    public static boolean validPosition(CompoundTag position) {
        return position.getIntArray("pos").length == 3 && SLOTS.contains(position.getString("slot"));
    }

    public static BlockPos position(CompoundTag position) {
        int[] coordinates = position.getIntArray("pos");
        return new BlockPos(coordinates[0], coordinates[1], coordinates[2]);
    }

    private static boolean legacyPosition(CompoundTag tag) {
        return tag.contains("X", Tag.TAG_ANY_NUMERIC) && tag.contains("Y", Tag.TAG_ANY_NUMERIC)
                && tag.contains("Z", Tag.TAG_ANY_NUMERIC) && tag.contains("Slot", Tag.TAG_ANY_NUMERIC);
    }
}
