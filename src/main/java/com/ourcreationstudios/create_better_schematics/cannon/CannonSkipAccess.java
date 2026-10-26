package com.ourcreationstudios.create_better_schematics.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface CannonSkipAccess {
    CannonSkipSettings betterSchematics$getSkipSettings();
    boolean betterSchematics$shouldPlace(BlockPos pos, BlockState state, BlockEntity entity,
            BlockState toReplace, BlockState other, boolean normalCube);
}
