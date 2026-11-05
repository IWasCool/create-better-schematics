package com.ourcreationstudios.create_better_schematics.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LaunchedItem.ForBelt.class, remap = false)
abstract class SchematicannonBeltPlacementMixin {
    @Redirect(method = "place", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;createBelts(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"), remap = false)
    private void betterSchematics$preserveSavedAxis(Level world, BlockPos first, BlockPos last) {
        var flight = (LaunchedItem.ForBelt) (Object) this;
        BlockState saved = flight.state;
        Direction.Axis axis = saved.getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS ? Direction.Axis.Y
                : saved.getValue(BeltBlock.HORIZONTAL_FACING).getClockWise().getAxis();
        // The connector infers sideways belts from destination shafts. In a schematic,
        // the saved belt determines the axis instead of unrelated blocks already there.
        int dx = Integer.signum(last.getX() - first.getX());
        int dy = Integer.signum(last.getY() - first.getY());
        int dz = Integer.signum(last.getZ() - first.getZ());
        for (int segment = 0; segment < flight.length; segment++) {
            BlockPos pos = first.offset(dx * segment, dy * segment, dz * segment);
            if (world.getBlockState(pos).getDestroySpeed(world, pos) == -1) {
                BeltConnectorItem.createBelts(world, first, last);
                return;
            }
        }
        for (int segment = 0; segment < flight.length; segment++) {
            BlockPos pos = first.offset(dx * segment, dy * segment, dz * segment);
            BlockState existing = world.getBlockState(pos);
            if (ShaftBlock.isShaft(existing) && existing.getValue(BlockStateProperties.AXIS) != axis)
                world.setBlock(pos, existing.setValue(BlockStateProperties.AXIS, axis), Block.UPDATE_CLIENTS);
        }
        BeltConnectorItem.createBelts(world, first, last);
    }
}
