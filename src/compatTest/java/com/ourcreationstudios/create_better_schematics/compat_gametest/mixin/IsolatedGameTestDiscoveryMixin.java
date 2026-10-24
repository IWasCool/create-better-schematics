package com.ourcreationstudios.create_better_schematics.compat_gametest.mixin;

import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Keep unrelated mods' development tests (and their optional test dependencies) out of this suite.
 */
@Mixin(value = GameTestHooks.class, remap = false)
abstract class IsolatedGameTestDiscoveryMixin {
    @Inject(method = "addGameTestMethods", at = @At("HEAD"), cancellable = true, remap = false)
    private static void betterSchematics$ownTestsOnly(ModFileScanData.AnnotationData annotation, Set<Method> methods,
                                                      CallbackInfo ci) {
        if (!"create_better_schematics".equals(annotation.annotationData().get("value")))
            ci.cancel();
    }
}
