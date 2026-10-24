package com.ourcreationstudios.create_better_schematics.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.Result;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.Save;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipService;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.foundation.utility.CreatePaths;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public class CannonSkipGameTests {
    @GameTest(template = "empty")
    public static void repeatedFilterPacketsCannotTransferItems(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            var level = helper.getLevel();
            level.setBlock(cannon.getBlockPos().below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (BarrelBlockEntity) level.getBlockEntity(cannon.getBlockPos().below());
            supply.setItem(0, new ItemStack(Items.STONE, 64));
            supply.setItem(1, new ItemStack(Items.DIAMOND, 17));
            cannon.inventory.setStackInSlot(2, new ItemStack(Items.BOOK, 5));
            cannon.inventory.setStackInSlot(4, new ItemStack(Items.GUNPOWDER, 16));
            FakePlayer player = openMenu(helper, cannon);
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 23));
            player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 3));
            var cannonBefore = cannon.inventory.serializeNBT(level.registryAccess()).copy();
            var playerBefore = player.getInventory().save(new ListTag()).copy();
            var supplyBefore = supply.saveWithFullMetadata(level.registryAccess());
            ItemStack carriedBefore = player.containerMenu.getCarried().copy();
            int fingerprint = CannonSkipService.inspect(cannon, 7).fingerprint();
            Save select = new Save(7, fingerprint, List.of(Blocks.STONE, Blocks.STONE, Blocks.OAK_STAIRS), true);
            Save clear = new Save(7, fingerprint, List.of(), false);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                CannonSkipPackets.Save.CODEC.encode(buffer, select);
                Save decoded = CannonSkipPackets.Save.CODEC.decode(buffer);
                for (int repeat = 0; repeat < 16; repeat++) {
                    helper.assertTrue(CannonSkipService.apply(player, decoded) == Result.APPLIED,
                            "Replayed valid filter saves must remain configuration-only");
                    helper.assertTrue(settings(cannon).skipped().size() == 2,
                            "Repeated block IDs must be deduplicated");
                    helper.assertTrue(CannonSkipService.apply(player, clear) == Result.APPLIED,
                            "Clearing a filter must not refund any items");
                }
                helper.assertTrue(CannonSkipService.apply(player,
                        new Save(7, fingerprint, List.of(Blocks.DIAMOND_BLOCK), false)) == Result.CHANGED,
                        "Forged block choices must be rejected");
                helper.assertTrue(CannonSkipService.apply(player,
                        new Save(7, fingerprint + 1, List.of(), false)) == Result.CHANGED,
                        "Stale saves must be rejected");
                helper.assertTrue(CannonSkipService.apply(player,
                        new Save(8, fingerprint, List.of(), false)) == Result.CLOSED,
                        "A forged menu ID must be rejected");
                helper.assertTrue(ItemStack.matches(carriedBefore, player.containerMenu.getCarried()),
                        "Filter saves must never change the cursor stack");
                player.containerMenu = player.inventoryMenu;
                helper.assertTrue(CannonSkipService.apply(player, decoded) == Result.CLOSED,
                        "Packets sent after closing the cannon must be rejected");
            } finally {
                buffer.release();
            }
            helper.assertTrue(cannonBefore.equals(cannon.inventory.serializeNBT(level.registryAccess()))
                            && playerBefore.equals(player.getInventory().save(new ListTag()))
                            && supplyBefore.equals(supply.saveWithFullMetadata(level.registryAccess())),
                    "Valid, replayed, forged, and stale saves must leave every real inventory unchanged");
            helper.assertTrue(cannon.flyingBlocks.isEmpty() && cannon.state == SchematicannonBlockEntity.State.STOPPED,
                    "Filter packets must not launch or build any items");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void changingFilterDuringFlightCannotRefundOrCloneBlocks(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            var level = helper.getLevel();
            level.setBlock(cannon.getBlockPos().below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (BarrelBlockEntity) level.getBlockEntity(cannon.getBlockPos().below());
            supply.setItem(0, new ItemStack(Items.STONE, 64));
            supply.setItem(1, new ItemStack(Items.OAK_STAIRS, 64));
            supply.setItem(2, new ItemStack(Items.BARREL, 64));
            cannon.printer.loadSchematic(fixture.blueprint(), level, true);
            cannon.updateChecklist();
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            cannon.tick();
            helper.assertTrue(cannon.flyingBlocks.size() == 1 && supply.getItem(0).getCount() == 63,
                    "The first projectile must be paid for before it is launched");
            var flightBefore = cannon.flyingBlocks.getFirst().serializeNBT(level.registryAccess());
            BlockPos targetBefore = cannon.printer.getCurrentTarget();
            cannon.state = SchematicannonBlockEntity.State.PAUSED;
            FakePlayer player = openMenu(helper, cannon);
            int fingerprint = CannonSkipService.inspect(cannon, 7).fingerprint();
            for (int repeat = 0; repeat < 16; repeat++) {
                helper.assertTrue(CannonSkipService.apply(player,
                        new Save(7, fingerprint, List.of(Blocks.STONE, Blocks.OAK_STAIRS, Blocks.BARREL), false)) == Result.APPLIED,
                        "A filter may be changed while a paid projectile is in flight");
                helper.assertTrue(CannonSkipService.apply(player,
                        new Save(7, fingerprint, List.of(), false)) == Result.APPLIED,
                        "A filter may be cleared while a paid projectile is in flight");
            }
            helper.assertTrue(cannon.flyingBlocks.size() == 1
                            && flightBefore.equals(cannon.flyingBlocks.getFirst().serializeNBT(level.registryAccess()))
                            && targetBefore.equals(cannon.printer.getCurrentTarget()),
                    "Replayed filter changes must not replace a projectile or rewind the printer");
            for (int tick = 0; tick < 30; tick++)
                cannon.tick();
            helper.assertTrue(level.getBlockState(fixture.target()).is(Blocks.STONE) && cannon.flyingBlocks.isEmpty(),
                    "The already paid projectile must place exactly once");
            helper.assertTrue(supply.getItem(0).getCount() == 63 && supply.getItem(1).getCount() == 64
                            && supply.getItem(2).getCount() == 64 && cannon.remainingFuel == 99,
                    "Toggling filters must neither refund the launched block nor charge extra materials or fuel");
            helper.assertTrue(level.getBlockState(fixture.target().east()).isAir()
                            && level.getBlockState(fixture.target().south()).isAir(),
                    "Changing filters on a paused cannon must not launch additional blocks");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catalogIncludesVariantsAndCounts(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            settings(fixture.cannon()).replace(Set.of(ResourceLocation.parse("minecraft:stone")));
            var view = CannonSkipService.inspect(fixture.cannon(), 7);
            helper.assertTrue(view.result() == Result.READY && view.entries().size() == 3,
                    "Picker must list all three block types, excluding air");
            helper.assertTrue(view.entries().stream().anyMatch(entry -> entry.block() == Blocks.STONE
                    && entry.count() == 2 && entry.skipped()), "Stone count and selected state must be correct");
            helper.assertTrue(view.entries().stream().anyMatch(entry -> entry.block() == Blocks.OAK_STAIRS
                    && entry.count() == 2 && !entry.skipped()), "Stair variants must be grouped by block type");
            helper.assertTrue(fixture.cannon().state == SchematicannonBlockEntity.State.STOPPED,
                    "Opening the picker must not start or advance the cannon");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                CannonSkipPackets.View.CODEC.encode(buffer, view);
                helper.assertTrue(CannonSkipPackets.View.CODEC.decode(buffer).equals(view),
                        "The client must receive the complete block catalog");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skippedBlocksPersistAndSync(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            settings(cannon).replace(Set.of(ResourceLocation.parse("minecraft:stone"), ResourceLocation.parse("minecraft:oak_stairs")));
            CompoundTag saved = cannon.saveWithFullMetadata(helper.getLevel().registryAccess());
            var restored = new SchematicannonBlockEntity(cannon.getType(), cannon.getBlockPos(), cannon.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(settings(restored).skips(Blocks.STONE.defaultBlockState())
                    && settings(restored).skips(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST))
                    && !settings(restored).skips(Blocks.BARREL.defaultBlockState()), "Selections must survive a world reload for all variants");
            CompoundTag packet = cannon.writeClient(new CompoundTag(), helper.getLevel().registryAccess());
            restored.readClient(packet, helper.getLevel().registryAccess());
            helper.assertTrue(settings(restored).skipped().equals(settings(cannon).skipped()),
                    "Block entity updates must synchronize selections to clients");
            settings(cannon).replace(Set.of());
            restored.readClient(cannon.writeClient(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(settings(restored).skipped().isEmpty(), "Clearing selections must synchronize too");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void selectionRequiresCurrentAccessibleMenu(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "skip_test"));
            player.setPos(cannon.getBlockPos().getX() + .5, cannon.getBlockPos().getY(), cannon.getBlockPos().getZ() + .5);
            player.containerMenu = SchematicannonMenu.create(7, player.getInventory(), cannon);
            var view = CannonSkipService.inspect(cannon, 7);
            Save save = new Save(7, view.fingerprint(), List.of(Blocks.STONE), false);
            helper.assertTrue(CannonSkipService.apply(player, save) == Result.APPLIED, "An open cannon must accept its player's selections");
            helper.assertTrue(cannon.checklist.getRequiredAmount(Items.STONE) == 0
                            && cannon.checklist.getRequiredAmount(Items.OAK_STAIRS) == 0,
                    "An unloaded printer should remain unloaded after changing settings");
            helper.assertTrue(CannonSkipService.apply(player, new Save(8, view.fingerprint(), List.of(), false)) == Result.CLOSED,
                    "Packets must not edit a different menu");
            helper.assertTrue(CannonSkipService.apply(player, new Save(7, view.fingerprint(), List.of(Blocks.DIAMOND_BLOCK), false)) == Result.CHANGED,
                    "Selections must belong to the loaded schematic");
            helper.assertTrue(CannonSkipService.apply(player, new Save(7, view.fingerprint() + 1, List.of(), false)) == Result.CHANGED,
                    "An outdated picker must not overwrite another schematic's settings");
            player.setPos(cannon.getBlockPos().getX() + 32, cannon.getBlockPos().getY(), cannon.getBlockPos().getZ());
            helper.assertTrue(CannonSkipService.apply(player, save) == Result.CLOSED, "Walking away must invalidate menu edits");
            helper.assertTrue(settings(cannon).skips(Blocks.STONE.defaultBlockState()), "Rejected edits must leave selections intact");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void cannonSkipsSelectedWithMaterialsAvailable(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            helper.getLevel().setBlock(cannon.getBlockPos().below(), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            var supply = (BarrelBlockEntity) helper.getLevel().getBlockEntity(cannon.getBlockPos().below());
            supply.setItem(0, new ItemStack(Items.STONE, 64));
            supply.setItem(1, new ItemStack(Items.OAK_STAIRS, 64));
            supply.setItem(2, new ItemStack(Items.BARREL, 64));
            settings(cannon).replace(Set.of(ResourceLocation.parse("minecraft:stone")));
            cannon.printer.loadSchematic(fixture.blueprint(), helper.getLevel(), true);
            cannon.updateChecklist();
            helper.assertTrue(cannon.checklist.getRequiredAmount(Items.STONE) == 0
                            && cannon.checklist.getRequiredAmount(Items.OAK_STAIRS) == 2
                            && cannon.checklist.getRequiredAmount(Items.BARREL) == 1,
                    "Material checklist must exclude selected types and retain other requirements");
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            BlockPos target = fixture.target();
            helper.succeedWhen(() -> {
                helper.assertTrue(helper.getLevel().getBlockState(target.east(2)).is(Blocks.BARREL)
                        && helper.getLevel().getBlockState(target.south()).is(Blocks.OAK_STAIRS)
                        && helper.getLevel().getBlockState(target.east().south()).is(Blocks.OAK_STAIRS),
                        "Cannon must finish building the unselected block types");
                helper.assertTrue(helper.getLevel().getBlockState(target).isAir()
                        && helper.getLevel().getBlockState(target.east()).isAir(), "Selected stone must never be placed");
                helper.assertTrue(supply.getItem(0).getCount() == 64, "Skipping must not consume selected materials");
                helper.assertTrue(cannon.remainingFuel == 97, "Only the three unselected blocks may consume fuel");
            });
        }
    }

    @GameTest(template = "empty")
    public static void skippingCurrentMissingBlockReleasesCannon(GameTestHelper helper) throws IOException {
        try (Fixture fixture = fixture(helper)) {
            var cannon = fixture.cannon();
            cannon.printer.loadSchematic(fixture.blueprint(), helper.getLevel(), true);
            cannon.remainingFuel = 100;
            cannon.skipMissing = false;
            cannon.state = SchematicannonBlockEntity.State.RUNNING;
            cannon.statusMsg = "running";
            cannon.tick();
            helper.assertTrue(cannon.missingItem != null && cannon.missingItem.is(Items.STONE), "Cannon must first pause on missing stone");
            settings(cannon).replace(Set.of(ResourceLocation.parse("minecraft:stone")));
            cannon.tick();
            helper.assertTrue(cannon.missingItem == null || !cannon.missingItem.is(Items.STONE),
                    "New skip choices must release the current missing block instead of looping forever");
            helper.assertTrue(cannon.remainingFuel == 100, "Skipping a missing block must not spend fuel");
        }
        helper.succeed();
    }

    private static FakePlayer openMenu(GameTestHelper helper, SchematicannonBlockEntity cannon) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "skip_dupe_test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(cannon.getBlockPos().getX() + .5, cannon.getBlockPos().getY(), cannon.getBlockPos().getZ() + .5);
        player.containerMenu = SchematicannonMenu.create(7, player.getInventory(), cannon);
        return player;
    }

    private static com.ourcreationstudios.create_better_schematics.cannon.CannonSkipSettings settings(SchematicannonBlockEntity cannon) {
        return ((CannonSkipAccess) cannon).betterSchematics$getSkipSettings();
    }

    private static Fixture fixture(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(source, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source.east(2), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source.south(), Blocks.OAK_STAIRS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(source.east().south(), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), Block.UPDATE_CLIENTS);
        BlockPos cannonPos = helper.absolutePos(new BlockPos(1, 2, 8));
        Block cannonBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:schematicannon"));
        level.setBlock(cannonPos, cannonBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
        var cannon = (SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
        String owner = "cannon_skip_gametest_" + UUID.randomUUID();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        var exported = SchematicExport.saveSchematic(directory, "blocks", false, level, source, source.offset(2, 0, 1));
        helper.assertTrue(exported != null, "Test schematic must export");
        ItemStack blueprint = SchematicItem.create(level, exported.fileName(), owner);
        BlockPos target = helper.absolutePos(new BlockPos(6, 2, 6));
        blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, target);
        blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        cannon.inventory.setStackInSlot(0, blueprint);
        return new Fixture(cannon, blueprint, target, exported.file(), directory);
    }

    private record Fixture(SchematicannonBlockEntity cannon, ItemStack blueprint, BlockPos target, Path file, Path directory)
            implements AutoCloseable {
        @Override public void close() throws IOException {
            if (!cannon.printer.isLoaded())
                cannon.inventory.setStackInSlot(0, ItemStack.EMPTY);
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
