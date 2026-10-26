package com.ourcreationstudios.create_better_schematics.compat;

import net.minecraft.nbt.CompoundTag;

/** A pending, paid schematic connection, retained until both copied endpoints exist. */
public interface TransmissionLinkAccess {
    CompoundTag betterSchematics$getPendingLink();
    void betterSchematics$setPendingLink(CompoundTag link);
}
