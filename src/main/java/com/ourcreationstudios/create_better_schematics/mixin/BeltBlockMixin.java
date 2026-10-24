package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.BeltCasingState;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BeltBlock.class, remap = false)
abstract class BeltBlockMixin {
    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"), remap = false)
    private void createisfixed$addSavedCasingMaterial(StateDefinition.Builder<Block, BlockState> builder,
                                                    CallbackInfo ci) {
        builder.add(BeltCasingState.MATERIAL);
    }
}
