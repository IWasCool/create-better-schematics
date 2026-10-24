package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.AddonInstalledComponents;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemRequirement.class, remap = false)
abstract class AddonMaterialRequirementMixin {
    @Inject(method = "of(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/simibubi/create/content/schematics/requirement/ItemRequirement;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void betterSchematics$includeInstalledParts(BlockState state, BlockEntity entity,
                                                               CallbackInfoReturnable<ItemRequirement> cir) {
        if (AddonInstalledComponents.supports(entity))
            cir.setReturnValue(entity.getLevel() == null ? ItemRequirement.INVALID : AddonInstalledComponents.supplement(
                    cir.getReturnValue(), entity.saveWithoutMetadata(entity.getLevel().registryAccess())));
    }
}
