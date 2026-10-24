package com.ourcreationstudios.create_better_schematics.compat;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

/** Loader-specific access to the cannon's attached item handlers. */
public final class CannonWireInventories {
    private CannonWireInventories() {}

    public static Iterable<IItemHandler> attached(SchematicannonBlockEntity cannon) {
        return cannon.attachedInventories;
    }
}
