package com.ourcreationstudios.create_better_schematics;

import com.ourcreationstudios.create_better_schematics.compat.PowerGridWires;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge companion for the shared Factory Gauge placement implementation. */
@Mod(CreateBetterSchematics.MODID)
public final class CreateBetterSchematics {
    public static final String MODID = "create_better_schematics";

    public CreateBetterSchematics() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(PowerGridWires::registerRequirements));
    }
}
