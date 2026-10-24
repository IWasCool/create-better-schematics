package com.ourcreationstudios.create_better_schematics.gametest.mixin;

import java.util.Collection;

import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep this companion's checks independent of Create's bundled upstream test suite. */
@Mixin(value = FabricGameTestHelper.class, remap = false)
abstract class PorterTestDiscoveryMixin {
    @Inject(method = "getTestFunctions", at = @At("RETURN"), cancellable = true, remap = false)
    private static void onlyPorterTests(CallbackInfoReturnable<Collection<TestFunction>> cir) {
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(test -> test.getTestName().startsWith("schematicportercrossloadertests.")).toList());
    }
}
