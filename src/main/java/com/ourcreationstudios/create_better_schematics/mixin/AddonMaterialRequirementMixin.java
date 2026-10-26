package com.ourcreationstudios.create_better_schematics.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.ourcreationstudios.create_better_schematics.compat.AddonInstalledComponents;
import com.ourcreationstudios.create_better_schematics.compat.TransmissionLinks;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ItemRequirement.class, remap = false)
abstract class AddonMaterialRequirementMixin {
    @WrapMethod(method = "of(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/simibubi/create/content/schematics/requirement/ItemRequirement;",
            remap = false)
    private static ItemRequirement betterSchematics$includeInstalledParts(BlockState state, BlockEntity entity,
                                                                         Operation<ItemRequirement> original) {
        // Preserve other addons' requirements, including Azimuth's cancellable return hook.
        ItemRequirement requirements = original.call(state, entity);
        if (AddonInstalledComponents.supports(entity))
            requirements = entity.getLevel() == null ? ItemRequirement.INVALID : AddonInstalledComponents.supplement(
                    requirements, entity.saveWithoutMetadata(entity.getLevel().registryAccess()));
        return TransmissionLinks.requirements(requirements, entity);
    }
}
