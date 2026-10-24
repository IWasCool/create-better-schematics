package com.ourcreationstudios.create_better_schematics;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Keeps material in the block state for editors that omit block entity NBT. */
public final class BeltCasingState {
    public static final EnumProperty<Material> MATERIAL =
            EnumProperty.create("create_better_schematics_casing", Material.class);

    private BeltCasingState() {}

    public enum Material implements StringRepresentable {
        NONE, ANDESITE, BRASS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public CasingType toCreate() {
            return CasingType.valueOf(name());
        }

        public static Material fromCreate(CasingType type) {
            return valueOf(type.name());
        }
    }
}
