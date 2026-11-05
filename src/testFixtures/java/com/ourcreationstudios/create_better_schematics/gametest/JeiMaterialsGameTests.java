package com.ourcreationstudios.create_better_schematics.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterial;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsService;
import com.ourcreationstudios.create_better_schematics.client.jei.MaterialBookmarkList;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public final class JeiMaterialsGameTests {
    @GameTest(template = "empty")
    public static void realRequirementsPreserveLargeCountsAndItemVariants(GameTestHelper helper) {
        var collector = new CannonMaterialsService.Collector();
        var doubleSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        for (int block = 0; block < 65; block++)
            collector.require(ItemRequirement.of(doubleSlab, null));
        ItemStack first = new ItemStack(Items.IRON_INGOT);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("First ingredient"));
        ItemStack second = new ItemStack(Items.IRON_INGOT);
        second.set(DataComponents.CUSTOM_NAME, Component.literal("Second ingredient"));
        collector.require(new ItemRequirement(new ItemRequirement.StrictNbtStackRequirement(first, ItemRequirement.ItemUseType.CONSUME)));
        collector.require(new ItemRequirement(new ItemRequirement.StrictNbtStackRequirement(second, ItemRequirement.ItemUseType.CONSUME)));
        var materials = collector.materials();
        helper.assertTrue(materials.size() == 3, "Component-specific ingredients must not be merged together");
        CannonMaterial slabs = materials.stream().filter(entry -> entry.stack().is(Items.OAK_SLAB)).findFirst().orElseThrow();
        helper.assertTrue(slabs.count() == 130 && slabs.stack().getCount() == 1,
                "65 double slabs require 130 items, while the JEI display stack remains a normal ingredient");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            CannonMaterial.CODEC.encode(buffer, slabs);
            helper.assertTrue(CannonMaterial.CODEC.decode(buffer).count() == 130,
                    "Amounts above the Minecraft stack limit survive transmission without splitting or clamping");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void refreshingBookmarksRemovesOldImportsAndRetainsPersonalEntries(GameTestHelper helper) {
        var bookmarks = new TestBookmarks();
        bookmarks.add(new ItemStack(Items.DIAMOND));
        bookmarks.add(new ItemStack(Items.STONE));
        var list = new MaterialBookmarkList();
        list.replace(List.of(new CannonMaterial(new ItemStack(Items.STONE), 130),
                new CannonMaterial(new ItemStack(Items.OAK_STAIRS), 2),
                new CannonMaterial(new ItemStack(Items.BARREL), 1)), bookmarks);
        helper.assertTrue(list.count(new ItemStack(Items.STONE)) == 130 && bookmarks.items.size() == 4,
                "The imported list attaches exact quantities without duplicating existing bookmarks");
        var saved = list.write(helper.getLevel().registryAccess());
        var restored = new MaterialBookmarkList();
        restored.read(saved, helper.getLevel().registryAccess());
        restored.replace(List.of(new CannonMaterial(new ItemStack(Items.STONE), 260)), bookmarks);
        helper.assertTrue(!bookmarks.contains(new ItemStack(Items.OAK_STAIRS))
                        && !bookmarks.contains(new ItemStack(Items.BARREL)) && bookmarks.contains(new ItemStack(Items.DIAMOND)),
                "Refreshing after skips removes old imported materials and preserves unrelated personal bookmarks");
        helper.assertTrue(restored.count(new ItemStack(Items.STONE)) == 260,
                "A refresh replaces the required amount rather than accumulating duplicate imports");
        restored.replace(List.of(), bookmarks);
        helper.assertTrue(bookmarks.items.size() == 2 && bookmarks.contains(new ItemStack(Items.STONE))
                        && restored.count(new ItemStack(Items.STONE)) == 0,
                "An empty build clears imported quantities while preserving a preexisting personal bookmark");
        helper.succeed();
    }

    private static final class TestBookmarks implements MaterialBookmarkList.Bookmarks {
        private final Set<CannonMaterial.Key> items = new HashSet<>();
        @Override public boolean contains(ItemStack stack) { return items.contains(new CannonMaterial.Key(stack)); }
        @Override public boolean add(ItemStack stack) { return items.add(new CannonMaterial.Key(stack)); }
        @Override public void remove(ItemStack stack) { items.remove(new CannonMaterial.Key(stack)); }
    }
}
