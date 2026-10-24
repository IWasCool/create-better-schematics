package com.ourcreationstudios.create_better_schematics.compat;

import java.util.List;

import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Optional integration using Power Grid's serialized wire format, without loading its classes. */
public final class PowerGridWires {
    private static final List<String> TYPES = List.of("hanging_wire", "block_wire", "cord", "string_light_cord");

    private PowerGridWires() {}

    public static boolean isWire(String id) {
        return TYPES.stream().anyMatch(type -> id.equals("powergrid:" + type));
    }

    public static void registerRequirements() {
        for (String name : TYPES) {
            var id = ResourceLocation.parse("powergrid:" + name);
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> {
                if (SchematicRequirementRegistries.ENTITIES.get(type) == null)
                    SchematicRequirementRegistries.ENTITIES.register(type, PowerGridWires::requirement);
            });
        }
    }

    private static ItemRequirement requirement(Entity entity) {
        CompoundTag item = entity.saveWithoutId(new CompoundTag()).getCompound("Item");
        ResourceLocation id = ResourceLocation.tryParse(item.getString("Id"));
        int count = item.getInt("Count");
        if (id == null || count < 0 || count > 65_536 || !BuiltInRegistries.ITEM.containsKey(id))
            return ItemRequirement.INVALID;
        // Split solid wires can legitimately own zero items. The cannon still places the segment.
        if (count == 0)
            return ItemRequirement.NONE;
        int totalCount = count;
        var material = BuiltInRegistries.ITEM.get(id);
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        while (count > 0) {
            int amount = Math.min(count, new ItemStack(material).getMaxStackSize());
            stacks.add(new ItemStack(material, amount));
            count -= amount;
        }
        return new WireRequirement(stacks, totalCount);
    }

    /** Keeps normal stack-sized icons while the cannon checks and pays one complete wire budget. */
    public static final class WireRequirement extends ItemRequirement {
        private final int itemCount;

        private WireRequirement(List<ItemStack> stacks, int itemCount) {
            super(ItemUseType.CONSUME, stacks);
            this.itemCount = itemCount;
        }

        public int itemCount() {
            return itemCount;
        }
    }
}
