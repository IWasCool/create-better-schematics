package com.ourcreationstudios.create_better_schematics.gametest;

import com.ourcreationstudios.create_better_schematics.compat.AddonInstalledComponents;
import com.ourcreationstudios.create_better_schematics.compat.SchematicConfigurationRegistry;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("create_better_schematics")
@PrefixGameTestTemplate(false)
public final class ConfigurationProfileGameTests {
    @GameTest(template = "empty")
    public static void nestedProfilesExcludeRuntimeAndPreserveInputs(GameTestHelper helper) throws Exception {
        SchematicConfigurationRegistry.registerClass(BarrelBlockEntity.class.getName(),
                "CBS_Test.Format.Unit", "CBS_Test.Sequence.*.Value");
        CompoundTag defaults = new CompoundTag();
        defaults.putInt("CBS_TestRuntime", -1);
        SchematicConfigurationRegistry.registerPlacementDefaults(BarrelBlockEntity.class.getName(), defaults);
        defaults.putInt("CBS_TestRuntime", 99);
        // A subclass exercises inherited profiles without registering any addon classes.
        var barrel = new TestBarrel();
        CompoundTag full = TagParser.parseTag("{CBS_Test:{Format:{Unit:'rpm',Energy:500},Sequence:["
                + "{Value:12,Progress:99},{Value:24,Progress:50}]},CBS_TestRuntime:8,Items:[{Slot:0b,id:'minecraft:diamond',count:64}]}");
        CompoundTag safe = TagParser.parseTag("{NativeSetting:7}");
        CompoundTag before = full.copy();
        CompoundTag safeBefore = safe.copy();
        CompoundTag result = SchematicConfigurationRegistry.merge(barrel, safe, full);
        helper.assertTrue(result.getInt("NativeSetting") == 7 && result.getInt("CBS_TestRuntime") == -1,
                "Profiles must preserve native data and defensively copy placement defaults");
        CompoundTag configuration = result.getCompound("CBS_Test");
        helper.assertTrue(configuration.getCompound("Format").getString("Unit").equals("rpm")
                        && !configuration.getCompound("Format").contains("Energy") && !result.contains("Items"),
                "Nested profiles must copy configured fields without inventories or stored resources");
        ListTag sequence = configuration.getList("Sequence", Tag.TAG_COMPOUND);
        helper.assertTrue(sequence.size() == 2 && sequence.getCompound(0).getInt("Value") == 12
                        && sequence.getCompound(1).getInt("Value") == 24
                        && !sequence.getCompound(0).contains("Progress") && !sequence.getCompound(1).contains("Progress"),
                "Structured lists must preserve order and omit runtime counters");
        sequence.getCompound(0).putInt("Value", 100);
        helper.assertTrue(full.equals(before) && safe.equals(safeBefore), "Profile output must not alias either input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidProfilePathsCannotOverrideIdentity(GameTestHelper helper) {
        for (String field : java.util.List.of("id", "x", "y", "z", "Format..Unit", "", "Sequence.*", "*.Value")) {
            boolean rejected = false;
            try {
                SchematicConfigurationRegistry.registerClass("test.InvalidProfile", field);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "Profile must reject invalid path: " + field);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unknownInstalledPartsCannotPrintForFree(GameTestHelper helper) {
        CompoundTag data = new CompoundTag();
        data.putString("Upgrade", "test:unknown_upgrade");
        helper.assertTrue(AddonInstalledComponents.supplement(ItemRequirement.NONE, data).isInvalid(),
                "An unpriced installed upgrade must invalidate the material requirement");
        data.putString("Upgrade", "createdieselgenerators:none");
        helper.assertTrue(AddonInstalledComponents.supplement(ItemRequirement.NONE, data) == ItemRequirement.NONE,
                "An unupgraded engine must not require an upgrade item");
        helper.succeed();
    }

    private static final class TestBarrel extends BarrelBlockEntity {
        private TestBarrel() {
            super(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        }
    }
}
