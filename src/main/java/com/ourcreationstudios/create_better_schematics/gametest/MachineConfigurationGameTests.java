package com.ourcreationstudios.create_better_schematics.gametest;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.SchematicDataCompatibility;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class MachineConfigurationGameTests {
    @GameTest(template = "empty")
    public static void legacyThresholdPercentages(GameTestHelper helper) {
        ThresholdSwitchBlockEntity threshold = threshold(helper);
        CompoundTag legacy = new CompoundTag();
        legacy.putFloat("OnAbove", .75f);
        legacy.putFloat("OffBelow", .25f);
        legacy.putBoolean("Inverted", true);
        threshold.loadWithComponents(legacy, helper.getLevel().registryAccess());
        threshold.updateCurrentLevel();
        helper.assertTrue(threshold.onWhenAbove == 1296 && threshold.offWhenBelow == 432,
                "Legacy 75%/25% barrel thresholds must become 1296/432, got "
                        + threshold.onWhenAbove + "/" + threshold.offWhenBelow);
        helper.assertTrue(threshold.isInverted(), "Threshold inversion must survive import");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thresholdStackDisplaySurvivesCannon(GameTestHelper helper) {
        ThresholdSwitchBlockEntity threshold = threshold(helper);
        threshold.onWhenAbove = 512;
        threshold.offWhenBelow = 128;
        threshold.inStacks = true;
        CompoundTag safe = new CompoundTag();
        threshold.writeSafe(safe, helper.getLevel().registryAccess());
        helper.assertTrue(safe.getBoolean("InStacks"), "Cannon data must retain stack display mode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyListFilterKeepsConfiguration(GameTestHelper helper) {
        ThresholdSwitchBlockEntity threshold = threshold(helper);
        CompoundTag legacyFilter = new CompoundTag();
        legacyFilter.putString("id", "create:filter");
        legacyFilter.putByte("Count", (byte) 1);
        CompoundTag iron = new CompoundTag();
        iron.putString("id", "minecraft:iron_ingot");
        iron.putByte("Count", (byte) 1);
        iron.putInt("Slot", 3);
        ListTag items = new ListTag();
        items.add(iron);
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", 18);
        inventory.put("Items", items);
        CompoundTag settings = new CompoundTag();
        settings.put("Items", inventory);
        settings.putBoolean("Blacklist", true);
        settings.putBoolean("RespectNBT", true);
        legacyFilter.put("tag", settings);
        CompoundTag data = new CompoundTag();
        data.put("Filter", legacyFilter);
        threshold.loadWithComponents(SchematicDataCompatibility.importSchematic(data),
                helper.getLevel().registryAccess());
        FilteringBehaviour behaviour = threshold.getBehaviour(FilteringBehaviour.TYPE);
        ItemStack filter = behaviour.getFilter();
        helper.assertTrue(Boolean.TRUE.equals(filter.get(AllDataComponents.FILTER_ITEMS_BLACKLIST)),
                "Legacy filter must retain deny list mode");
        helper.assertTrue(Boolean.TRUE.equals(filter.get(AllDataComponents.FILTER_ITEMS_RESPECT_NBT)),
                "Legacy filter must retain respect data mode");
        helper.assertTrue(!behaviour.test(new ItemStack(Items.IRON_INGOT)), "Filter must reject its saved iron entry");
        helper.assertTrue(behaviour.test(new ItemStack(Items.GOLD_INGOT)), "Filter must allow other entries");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armTargetsExportToForge(GameTestHelper helper) {
        BlockPos armPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos depotPos = armPos.east(2);
        helper.getLevel().setBlock(depotPos, block("depot").defaultBlockState(), Block.UPDATE_CLIENTS);
        ArmInteractionPoint point = ArmInteractionPoint.create(helper.getLevel(), depotPos,
                helper.getLevel().getBlockState(depotPos));
        helper.assertTrue(point != null, "Depot should be a valid arm target");
        CompoundTag arm = new CompoundTag();
        ListTag points = new ListTag();
        points.add(point.serialize(armPos));
        arm.put("InteractionPoints", points);
        CompoundTag exported = SchematicDataCompatibility.exportSchematic(arm)
                .getList("InteractionPoints", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(exported.contains("Pos", Tag.TAG_COMPOUND),
                "Forge 1.20.1 requires arm positions as X/Y/Z compounds, got " + exported);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armKeepsTargetsPlacedLater(GameTestHelper helper) {
        BlockPos armPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos depotPos = armPos.east(2);
        helper.getLevel().setBlock(armPos, block("mechanical_arm").defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(depotPos, block("depot").defaultBlockState(), Block.UPDATE_CLIENTS);
        ArmInteractionPoint point = ArmInteractionPoint.create(helper.getLevel(), depotPos,
                helper.getLevel().getBlockState(depotPos));
        ListTag points = new ListTag();
        points.add(point.serialize(armPos));
        CompoundTag data = new CompoundTag();
        data.put("InteractionPoints", points);
        ArmBlockEntity arm = (ArmBlockEntity) helper.getLevel().getBlockEntity(armPos);
        arm.loadWithComponents(data, helper.getLevel().registryAccess());
        helper.getLevel().setBlock(depotPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        // Reproduce a cannon placing an arm before the referenced destination exists.
        arm.tick();
        CompoundTag waiting = new CompoundTag();
        arm.writeSafe(waiting, helper.getLevel().registryAccess());
        helper.assertTrue(waiting.getList("InteractionPoints", Tag.TAG_COMPOUND).size() == 1,
                "Arm must retain destinations that have not been placed yet: " + waiting);
        helper.getLevel().setBlock(depotPos, block("depot").defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.runAfterDelay(25, () -> {
            CompoundTag saved = new CompoundTag();
            arm.writeSafe(saved, helper.getLevel().registryAccess());
            helper.assertTrue(saved.getList("InteractionPoints", Tag.TAG_COMPOUND).size() == 1,
                    "Arm must recover the destination after it is placed");
            SchematicTransferGameTests.assertArmTarget(helper, arm, depotPos);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void partialArmTargetsSurviveReloadWhileMoving(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos armPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos missing = armPos.east();
        BlockPos existing = armPos.east(3);
        var armState = block("mechanical_arm").defaultBlockState();
        var depotState = block("depot").defaultBlockState();
        level.setBlock(armPos, armState, Block.UPDATE_CLIENTS);
        level.setBlock(missing, depotState, Block.UPDATE_CLIENTS);
        level.setBlock(existing, depotState, Block.UPDATE_CLIENTS);
        ListTag points = new ListTag();
        points.add(ArmInteractionPoint.create(level, missing, depotState).serialize(armPos));
        points.add(ArmInteractionPoint.create(level, existing, depotState).serialize(armPos));
        CompoundTag data = new CompoundTag();
        data.put("InteractionPoints", points);
        ArmBlockEntity arm = (ArmBlockEntity) level.getBlockEntity(armPos);
        arm.loadWithComponents(data, level.registryAccess());
        level.setBlock(missing, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        arm.tick();
        SchematicTransferGameTests.assertArmTarget(helper, arm, existing);
        CompoundTag saved = arm.saveWithFullMetadata(level.registryAccess());
        saved.putString("Phase", "MOVE_TO_OUTPUT");
        saved.putInt("TargetPointIndex", 0);
        saved.putFloat("MovementProgress", .25f);
        saved.put("HeldItem", new ItemStack(Items.IRON_INGOT).save(level.registryAccess()));
        level.setBlock(armPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(missing, depotState, Block.UPDATE_CLIENTS);
        level.setBlock(armPos, armState, Block.UPDATE_CLIENTS);
        ArmBlockEntity reloaded = (ArmBlockEntity) level.getBlockEntity(armPos);
        reloaded.loadWithComponents(saved, level.registryAccess());
        reloaded.tick();
        SchematicTransferGameTests.assertArmFirstTarget(helper, reloaded, existing, 2);
        CompoundTag after = reloaded.saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(after.getString("Phase").equals("MOVE_TO_OUTPUT") && after.getInt("TargetPointIndex") == 0,
                "Reloading must retain the current movement and destination index");
        helper.succeed();
    }

    private static ThresholdSwitchBlockEntity threshold(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = block("stockpile_switch").defaultBlockState();
        helper.getLevel().setBlock(pos.relative(ThresholdSwitchBlock.getTargetDirection(state)),
                Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
        return (ThresholdSwitchBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
