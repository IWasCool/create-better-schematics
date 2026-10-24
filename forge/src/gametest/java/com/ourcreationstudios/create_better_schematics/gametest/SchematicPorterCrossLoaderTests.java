package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Loads the actual output of the NeoForge table's porter with Create 6.0.8 on Forge. */
@GameTestHolder("better_schematics_tests")
@PrefixGameTestTemplate(false)
public class SchematicPorterCrossLoaderTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void porterFileRotatedCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.NONE);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void porterFileMirroredCannonReload(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.FRONT_BACK);
    }

    private static void transfer(GameTestHelper helper, Mirror mirror) throws IOException {
        transfer(helper, mirror, "/fixtures/neoforge-ported-machines.nbt");
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void sharedNativeFileNeedsNoConversionButton(GameTestHelper helper) throws IOException {
        transfer(helper, Mirror.FRONT_BACK, "/fixtures/neoforge-shared-machines.nbt");
    }

    private static void transfer(GameTestHelper helper, Mirror mirror, String resource) throws IOException {
        var level = helper.getLevel();
        String owner = "porter_test_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        Files.createDirectories(directory);
        Path file = directory.resolve("ported-machines.nbt");
        try (var fixture = SchematicPorterCrossLoaderTests.class.getResourceAsStream(resource)) {
            helper.assertTrue(fixture != null, "Real porter fixture must exist");
            Files.copy(fixture, file);
        }
        Map<BlockPos, Placement> placements = new LinkedHashMap<>();
        try {
            ItemStack blueprint = SchematicItem.create(level, file.getFileName().toString(), owner);
            blueprint.getOrCreateTag().put("Anchor", NbtUtils.writeBlockPos(helper.absolutePos(new BlockPos(8, 2, 7))));
            blueprint.getOrCreateTag().putBoolean("Deployed", true);
            blueprint.getOrCreateTag().putString("Rotation", Rotation.CLOCKWISE_90.name());
            blueprint.getOrCreateTag().putString("Mirror", mirror.name());
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, true);
            helper.assertTrue(printer.isLoaded() && !printer.isErrored(), "Forge must load the converted file");
            while (printer.advanceCurrentPos())
                printer.handleCurrentTarget((pos, state, be) -> {
                    if (!state.isAir())
                        placements.put(pos, new Placement(state, BlockHelper.prepareBlockEntityData(state, be)));
                }, (pos, entity) -> {});
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
        BlockPos armPos = find(placements, "create:mechanical_arm");
        BlockPos depotPos = find(placements, "create:depot");
        // A cannon can place the arm before its output. Save/reload while that target is missing.
        launch(helper, armPos, placements.get(armPos));
        ArmBlockEntity arm = (ArmBlockEntity) level.getBlockEntity(armPos);
        arm.tick();
        arm.load(arm.saveWithFullMetadata());
        for (var entry : placements.entrySet())
            if (!entry.getKey().equals(armPos))
                launch(helper, entry.getKey(), entry.getValue());
        helper.runAfterDelay(25, () -> {
            arm.tick();
            assertArm(helper, arm, depotPos);
            arm.load(arm.saveWithFullMetadata());
            arm.tick();
            assertArm(helper, arm, depotPos);
            int filters = 0;
            for (var entry : placements.entrySet()) {
                if (!(level.getBlockEntity(entry.getKey()) instanceof ThresholdSwitchBlockEntity threshold))
                    continue;
                threshold.updateCurrentLevel();
                helper.assertTrue(threshold.onWhenAbove == 1296 && threshold.offWhenBelow == 432
                        && threshold.inStacks && threshold.isInverted(),
                        "Forge cannon/reload must retain amounts, stack display and inversion");
                FilteringBehaviour filtering = threshold.getBehaviour(FilteringBehaviour.TYPE);
                ItemStack filter = filtering.getFilter();
                if (BuiltInRegistries.ITEM.getKey(filter.getItem()).getPath().equals("filter")) {
                    ItemStack sample = new ItemStack(Items.IRON_INGOT);
                    sample.setHoverName(Component.literal("Porter sample"));
                    ItemStack styled = new ItemStack(Items.IRON_INGOT);
                    styled.setHoverName(Component.literal("Styled sample").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    ListTag lore = new ListTag();
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Line one"))));
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Line two").withStyle(ChatFormatting.ITALIC))));
                    styled.getOrCreateTag().getCompound("display").put("Lore", lore);
                    helper.assertTrue(!filtering.test(sample) && filtering.test(new ItemStack(Items.IRON_INGOT))
                            && !filtering.test(styled)
                            && filter.getOrCreateTag().getBoolean("Blacklist")
                            && filter.getOrCreateTag().getBoolean("RespectNBT"),
                            "Forge must retain sparse filter items, custom names and deny/respect-data options: "
                                    + filter.getTag() + "; sample=" + sample.getTag());
                } else {
                    helper.assertTrue(filter.getOrCreateTag().getInt("WhitelistMode") == 1
                            && filtering.test(new ItemStack(Items.IRON_INGOT))
                            && !filtering.test(new ItemStack(Items.OAK_PLANKS)),
                            "Forge attribute conjunction and inverted tag rule must behave correctly");
                }
                CompoundTag saved = threshold.saveWithFullMetadata();
                threshold.load(saved);
                helper.assertTrue(threshold.inStacks && threshold.onWhenAbove == 1296,
                        "Forge world reload must preserve the imported switch settings");
                filters++;
            }
            helper.assertTrue(filters == 2, "Both kinds of filter must be exercised");
            helper.succeed();
        });
    }

    private static void assertArm(GameTestHelper helper, ArmBlockEntity arm, BlockPos depot) {
        try {
            var field = ArmBlockEntity.class.getDeclaredField("outputs");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ArmInteractionPoint> outputs = (List<ArmInteractionPoint>) field.get(arm);
            helper.assertTrue(outputs.size() == 1 && outputs.get(0).getPos().equals(depot),
                    "Forge arm must retain and resolve its transformed target, even when placed first");
            CompoundTag safe = BlockHelper.prepareBlockEntityData(arm.getBlockState(), arm);
            helper.assertTrue(safe.getInt("ScrollValue") == 2, "Forge arm must retain its selection mode");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BlockPos find(Map<BlockPos, Placement> placements, String id) {
        return placements.entrySet().stream().filter(entry -> BuiltInRegistries.BLOCK.getKey(entry.getValue().state.getBlock())
                .equals(new ResourceLocation(id))).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private static void launch(GameTestHelper helper, BlockPos pos, Placement placement) {
        if (!placement.state.hasBlockEntity()) {
            helper.getLevel().setBlock(pos, placement.state, 3);
            return;
        }
        LaunchedItem flight = new LaunchedItem.ForBlockState(pos.above(3), pos,
                new ItemStack(placement.state.getBlock()), placement.state, placement.data);
        CompoundTag saved = flight.serializeNBT();
        saved.putInt("TicksLeft", 0);
        LaunchedItem restored = LaunchedItem.fromNBT(saved, helper.getLevel().holderLookup(Registries.BLOCK));
        helper.assertTrue(restored.update(helper.getLevel()), "Reloaded cannon flight must place its block");
    }

    private record Placement(BlockState state, CompoundTag data) {}
}
