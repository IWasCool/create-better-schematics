package com.ourcreationstudios.create_better_schematics.mixin;

import com.ourcreationstudios.create_better_schematics.compat.PowerGridWires;
import com.ourcreationstudios.create_better_schematics.compat.CannonWireInventories;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicannonBlockEntity.class, remap = false)
abstract class SchematicannonWireMaterialsMixin {
    @Shadow protected abstract boolean grabItemsFromAttachedInventories(StackRequirement required, boolean simulate);

    @Unique private PowerGridWires.WireRequirement betterSchematics$wireBudget;
    @Unique private Boolean betterSchematics$wireAvailable;
    @Unique private boolean betterSchematics$wireExtractionAttempted;
    @Unique private boolean betterSchematics$wirePaid;

    @Redirect(method = "tickPrinter", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/schematics/SchematicPrinter;getCurrentRequirement()Lcom/simibubi/create/content/schematics/requirement/ItemRequirement;"), remap = false)
    private ItemRequirement betterSchematics$startWireBudget(SchematicPrinter printer) {
        ItemRequirement requirement = printer.getCurrentRequirement();
        betterSchematics$wireBudget = requirement instanceof PowerGridWires.WireRequirement wire ? wire : null;
        betterSchematics$wireAvailable = null;
        betterSchematics$wireExtractionAttempted = betterSchematics$wirePaid = false;
        return requirement;
    }

    @Redirect(method = "tickPrinter", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/schematics/cannon/SchematicannonBlockEntity;grabItemsFromAttachedInventories(Lcom/simibubi/create/content/schematics/requirement/ItemRequirement$StackRequirement;Z)Z"), require = 2, remap = false)
    private boolean betterSchematics$payWholeWire(SchematicannonBlockEntity cannon, StackRequirement required, boolean simulate) {
        if (betterSchematics$wireBudget == null)
            return grabItemsFromAttachedInventories(required, simulate);
        if (cannon.hasCreativeCrate)
            return true;
        // Independent simulations of [64, 64, 2] can all see the same stack of 64.
        // Reserve the full cost instead, and charge it once across the real inventory slots.
        if (simulate) {
            if (betterSchematics$wireAvailable == null)
                betterSchematics$wireAvailable = betterSchematics$moveWireMaterials(cannon, required,
                        betterSchematics$wireBudget.itemCount(), true);
            return betterSchematics$wireAvailable;
        }
        if (!betterSchematics$wireExtractionAttempted) {
            betterSchematics$wireExtractionAttempted = true;
            betterSchematics$wirePaid = betterSchematics$moveWireMaterials(cannon, required,
                    betterSchematics$wireBudget.itemCount(), false);
        }
        return betterSchematics$wirePaid;
    }

    @Unique
    private static boolean betterSchematics$moveWireMaterials(SchematicannonBlockEntity cannon,
            StackRequirement required, int remaining, boolean simulate) {
        for (var inventory : CannonWireInventories.attached(cannon)) {
            if (inventory == null)
                continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                int requested = Math.min(remaining, required.stack.getMaxStackSize());
                ItemStack available = inventory.extractItem(slot, requested, true);
                if (available.isEmpty() || !required.matches(available))
                    continue;
                int amount = Math.min(remaining, available.getCount());
                if (!simulate) {
                    available = inventory.extractItem(slot, amount, false);
                    if (available.isEmpty() || !required.matches(available))
                        return false;
                    amount = Math.min(remaining, available.getCount());
                }
                remaining -= amount;
                if (remaining == 0)
                    return true;
            }
        }
        return false;
    }

    @Inject(method = "launchEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterSchematics$refuseUnpaidWire(BlockPos target, ItemStack icon, Entity entity, CallbackInfo ci) {
        var cannon = (SchematicannonBlockEntity) (Object) this;
        // Create ignores the actual extraction return value. Fail closed if a handler's
        // real extraction disagrees with its simulation, including aliased inventories.
        if (betterSchematics$wireBudget != null && !cannon.hasCreativeCrate && !betterSchematics$wirePaid
                && PowerGridWires.isWire(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()))
            ci.cancel();
    }
}
