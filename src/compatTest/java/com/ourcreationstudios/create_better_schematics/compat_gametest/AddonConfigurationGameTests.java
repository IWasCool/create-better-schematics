package com.ourcreationstudios.create_better_schematics.compat_gametest;

import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.ourcreationstudios.create_better_schematics.compat.SchematicConfigurationRegistry;

@GameTestHolder("create_better_schematics")
@PrefixGameTestTemplate(false)
public class AddonConfigurationGameTests {
    @GameTest(template = "empty")
    public static void tomsStorageEmitterKeepsFilterAndThreshold(GameTestHelper helper) throws IOException {
        roundTrip(helper, "toms_storage:level_emitter", data -> {
            data.put("Filter", new ItemStack(Items.IRON_INGOT).save(helper.getLevel().registryAccess()));
            data.putInt("Count", 384);
            data.putBoolean("lessThan", true);
        }, saved -> {
            helper.assertTrue(saved.getInt("Count") == 384 && saved.getBoolean("lessThan"),
                    "Tom's Storage emitter threshold and comparison must survive copying");
            helper.assertTrue(ItemStack.parseOptional(helper.getLevel().registryAccess(), saved.getCompound("Filter"))
                    .is(Items.IRON_INGOT), "Tom's Storage emitter filter must survive copying");
        });
    }

    @GameTest(template = "empty")
    public static void tomsStorageHopperKeepsGhostFilter(GameTestHelper helper) throws IOException {
        roundTrip(helper, "toms_storage:basic_inventory_hopper", data ->
                data.put("Filter", new ItemStack(Items.GOLD_INGOT).save(helper.getLevel().registryAccess())), saved ->
                helper.assertTrue(ItemStack.parseOptional(helper.getLevel().registryAccess(), saved.getCompound("Filter"))
                        .is(Items.GOLD_INGOT), "Tom's Storage hopper filter must survive copying"));
    }

    @GameTest(template = "empty")
    public static void powerGridMeterKeepsDisplayUnits(GameTestHelper helper) throws IOException {
        roundTrip(helper, "powergrid:energy_meter", data -> data.putBoolean("Wh", true), saved ->
                helper.assertTrue(saved.getBoolean("Wh"), "Power Grid energy meter display units must survive copying"));
    }

    @GameTest(template = "empty")
    public static void craftsAndAdditionsMotorKeepsSpeed(GameTestHelper helper) throws IOException {
        roundTrip(helper, "createaddition:electric_motor", data -> data.putInt("ScrollValue", -96), saved ->
                helper.assertTrue(saved.getInt("ScrollValue") == -96, "Crafts & Additions motor speed/direction must survive copying"));
    }

    @GameTest(template = "empty")
    public static void configurationProfilesCopyDeclaredFields(GameTestHelper helper) {
        SchematicConfigurationRegistry.register(ResourceLocation.parse("minecraft:barrel"), "CustomSetting");
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        var barrel = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(pos);
        barrel.setItem(0, new ItemStack(Items.DIAMOND, 32));
        CompoundTag full = barrel.saveWithoutMetadata(level.registryAccess());
        full.putString("CustomSetting", "Configured storage");
        CompoundTag before = full.copy();
        CompoundTag safe = SchematicConfigurationRegistry.merge(barrel, null, full);
        helper.assertTrue(safe.getString("CustomSetting").equals("Configured storage"),
                "Other mods must be able to register their configuration keys");
        helper.assertTrue(!safe.contains("Items"), "A configuration profile must not copy undeclared inventory contents");
        helper.assertTrue(full.equals(before), "Addon profile copying must leave its input intact");
        helper.succeed();
    }

    private static void roundTrip(GameTestHelper helper, String blockId,
            java.util.function.Consumer<CompoundTag> configure, java.util.function.Consumer<CompoundTag> verify) throws IOException {
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos copy = helper.absolutePos(new BlockPos(7, 2, 7));
        level.setBlock(source, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        var original = level.getBlockEntity(source);
        helper.assertTrue(original != null, "Addon block must exist: " + blockId);
        CompoundTag data = original.saveWithoutMetadata(level.registryAccess());
        configure.accept(data);
        original.loadWithComponents(data, level.registryAccess());
        CompoundTag safe = BlockHelper.prepareBlockEntityData(level, original.getBlockState(), original);
        helper.assertTrue(safe != null, "Addon configuration must be accepted by the cannon: " + blockId);
        verify.accept(safe);
        BlockHelper.placeSchematicBlock(level, original.getBlockState(), copy, new ItemStack(block), safe);
        var copied = level.getBlockEntity(copy);
        helper.assertTrue(copied != null, "Cannon must place addon block: " + blockId);
        verify.accept(copied.saveWithoutMetadata(level.registryAccess()));
        String owner = "addon_configuration_" + UUID.randomUUID();
        var directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "configuration", false, level, source, source);
        helper.assertTrue(exported != null, "Addon configuration must export to a real schematic file");
        try {
            var blueprint = SchematicItem.create(level, exported.fileName(), owner);
            var template = SchematicItem.loadSchematic(level, blueprint);
            BlockPos fromFile = copy.above(2);
            template.placeInWorld(level, fromFile, fromFile, SchematicItem.getSettings(blueprint).setKnownShape(true),
                    level.getRandom(), Block.UPDATE_CLIENTS);
            var imported = level.getBlockEntity(fromFile);
            helper.assertTrue(imported != null, "Addon block must import from disk");
            verify.accept(imported.saveWithoutMetadata(level.registryAccess()));
        } finally {
            Files.deleteIfExists(exported.file());
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }
}
