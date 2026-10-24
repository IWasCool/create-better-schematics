package com.ourcreationstudios.create_better_schematics.compat;

import java.util.Objects;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.minecraftforge.items.IItemHandler;

/** Forge 1.20.1 wraps the attached handlers in LazyOptional capabilities. */
public final class CannonWireInventories {
    private CannonWireInventories() {}

    public static Iterable<IItemHandler> attached(SchematicannonBlockEntity cannon) {
        return cannon.attachedInventories.stream().filter(Objects::nonNull)
                .map(capability -> capability.orElse(null)).filter(Objects::nonNull).toList();
    }
}
