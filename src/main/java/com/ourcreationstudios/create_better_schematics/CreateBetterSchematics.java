package com.ourcreationstudios.create_better_schematics;

import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets;
import com.ourcreationstudios.create_better_schematics.factory.FactoryGaugeSchematicProcessor;
import com.ourcreationstudios.create_better_schematics.compat.PowerGridWires;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(CreateBetterSchematics.MODID)
public final class CreateBetterSchematics {
    public static final String MODID = "create_better_schematics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateBetterSchematics(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CannonSkipPackets::register);
        FactoryGaugeSchematicProcessor.TYPES.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PowerGridWires::registerRequirements);
    }
}
