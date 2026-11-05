package com.ourcreationstudios.create_better_schematics.gametest;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsService;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.BlockEntry;
import com.ourcreationstudios.create_better_schematics.client.CannonBlockGroup;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public final class CannonMaterialGroupingGameTests {
    @GameTest(template = "empty")
    public static void groupsObtainableItemsWithoutChangingRequirements(GameTestHelper helper) {
        var collector = new CannonMaterialsService.Collector();
        var entries = new java.util.ArrayList<BlockEntry>();
        for (String name : new String[]{"shaft", "andesite_encased_shaft", "brass_encased_shaft", "cogwheel",
                "andesite_encased_cogwheel", "brass_encased_cogwheel", "large_cogwheel",
                "andesite_encased_large_cogwheel", "brass_encased_large_cogwheel", "fluid_pipe",
                "encased_fluid_pipe", "glass_fluid_pipe", "metal_girder_encased_shaft", "belt"}) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", name));
            var requirement = ItemRequirement.of(block.defaultBlockState(), null);
            collector.require(requirement);
            entries.add(new BlockEntry(block, 1, false));
        }
        var materials = collector.materials();
        helper.assertTrue(materials.size() == 6, "Create supplies six obtainable item types, without encased items");
        for (String name : new String[]{"shaft", "cogwheel", "large_cogwheel", "fluid_pipe", "belt_connector", "metal_girder"}) {
            int expected = name.equals("shaft") ? 5 : name.equals("belt_connector") || name.equals("metal_girder") ? 1 : 3;
            helper.assertTrue(materials.stream().anyMatch(m -> BuiltInRegistries.ITEM.getKey(m.stack().getItem()).getPath().equals(name) && m.count() == expected), "Correct requirement tally for " + name);
        }
        var groups = CannonBlockGroup.group(entries);
        helper.assertTrue(groups.size() == 6, "Encased variants merge, while composite girder requirements retain a separate row: " + groups);
        for (String name : new String[]{"shaft", "cogwheel", "large_cogwheel", "fluid_pipe"}) {
            var group = groups.stream().filter(g -> g.id().getPath().equals(name)).findFirst().orElseThrow();
            helper.assertTrue(group.count() == 3 && group.variants().size() == 3, "Group real and encased variants for " + name);
            var skipped = new java.util.HashSet<net.minecraft.world.level.block.Block>();
            skipped.add(group.variants().getFirst().block());
            helper.assertTrue(group.partiallySkipped(skipped), "Old partial choices are not silently discarded");
            group.toggle(skipped);
            helper.assertTrue(group.skipped(skipped) && skipped.size() == 3, "Selecting the item skips every underlying block variant");
            group.toggle(skipped);
            helper.assertTrue(skipped.isEmpty(), "Unskipping restores every variant");
        }
        helper.succeed();
    }
}
