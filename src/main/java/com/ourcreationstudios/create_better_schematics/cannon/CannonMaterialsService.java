package com.ourcreationstudios.create_better_schematics.cannon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets.Request;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets.Result;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets.View;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CannonMaterialsService {
    private CannonMaterialsService() {}

    public static View inspect(ServerPlayer player, Request request) {
        var cannon = CannonSkipService.openCannon(player, request.menuId());
        if (cannon == null)
            return reply(request, List.of(), Result.CLOSED);
        ItemStack blueprint = cannon.inventory.getStackInSlot(0);
        if (ItemStack.hashItemAndComponents(blueprint) != request.fingerprint())
            return reply(request, List.of(), Result.CHANGED);
        if (!(blueprint.getItem() instanceof SchematicItem))
            return reply(request, List.of(), Result.NO_SCHEMATIC);
        try {
            View view = collect(cannon, request);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(256, CannonMaterialsPackets.MAX_PAYLOAD_BYTES),
                    player.registryAccess());
            try {
                CannonMaterialsPackets.View.CODEC.encode(buffer, view);
                return view;
            } catch (RuntimeException tooLarge) {
                return reply(request, List.of(), Result.TOO_LARGE);
            } finally {
                buffer.release();
            }
        } catch (ArithmeticException overflow) {
            return reply(request, List.of(), Result.TOO_LARGE);
        } catch (RuntimeException exception) {
            CreateBetterSchematics.LOGGER.warn("Cannot read materials for cannon at {}", cannon.getBlockPos(), exception);
            return reply(request, List.of(), Result.FAILED);
        }
    }

    private static View collect(SchematicannonBlockEntity cannon, Request request) {
        // A separate printer keeps this action read-only: no cannon progress,
        // inventories, fuel, projectiles, or skip choices are changed.
        var preview = new SchematicPrinter();
        preview.loadSchematic(cannon.inventory.getStackInSlot(0).copy(), cannon.getLevel(), true);
        if (!preview.isLoaded() || preview.isErrored())
            return reply(request, List.of(), Result.FAILED);
        var checklist = new Collector();
        preview.markAllBlockRequirements(checklist, cannon.getLevel(),
                ((CannonSkipAccess) cannon)::betterSchematics$shouldPlace);
        preview.markAllEntityRequirements(checklist);
        if (checklist.blocksNotLoaded)
            return reply(request, List.of(), Result.UNLOADED);
        List<CannonMaterial> entries = checklist.materials();
        return entries.size() > CannonMaterialsPackets.MAX_ENTRIES
                ? reply(request, List.of(), Result.TOO_LARGE) : reply(request, entries, Result.READY);
    }

    private static View reply(Request request, List<CannonMaterial> entries, Result result) {
        return new View(request.menuId(), request.requestId(), request.fingerprint(), entries, result);
    }

    public static final class Collector extends MaterialChecklist {
        private final Map<CannonMaterial.Key, Amount> amounts = new LinkedHashMap<>();

        @Override public void require(ItemRequirement requirement) {
            if (requirement.isEmpty() || requirement.isInvalid())
                return;
            for (var required : requirement.getRequiredItems()) {
                if (required.stack.isEmpty())
                    continue;
                var amount = amounts.computeIfAbsent(new CannonMaterial.Key(required.stack),
                        key -> new Amount(required.stack.copyWithCount(1)));
                if (required.usage == ItemRequirement.ItemUseType.DAMAGE)
                    amount.damage = Math.addExact(amount.damage, required.stack.getCount());
                else
                    amount.consumed = Math.addExact(amount.consumed, required.stack.getCount());
            }
        }

        public List<CannonMaterial> materials() {
            List<CannonMaterial> result = new ArrayList<>();
            for (Amount amount : amounts.values()) {
                int durability = Math.max(1, amount.stack.getMaxDamage() - amount.stack.getDamageValue());
                int tools = (int) Math.ceil((double) amount.damage / durability);
                result.add(new CannonMaterial(amount.stack, Math.addExact(amount.consumed, tools)));
            }
            result.sort(Comparator.comparing((CannonMaterial material) -> BuiltInRegistries.ITEM.getKey(material.stack().getItem()))
                    .thenComparingInt(material -> ItemStack.hashItemAndComponents(material.stack())));
            return List.copyOf(result);
        }

        private static final class Amount {
            private final ItemStack stack;
            private int consumed;
            private int damage;
            private Amount(ItemStack stack) { this.stack = stack; }
        }
    }
}
