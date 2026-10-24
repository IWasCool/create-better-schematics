package com.ourcreationstudios.create_better_schematics.compat;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Set;

/**
 * Installed parts are configuration only when the schematicannon pays for those parts.
 */
public final class AddonInstalledComponents {
    private static final Set<String> ENGINES = Set.of(
            "com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity",
            "com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlockEntity",
            "com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlockEntity");
    private static final Map<String, String> UPGRADES = Map.of(
            "createdieselgenerators:silencer", "createdieselgenerators:engine_silencer",
            "createdieselgenerators:turbocharger", "createdieselgenerators:engine_turbocharger");

    private AddonInstalledComponents() {
    }

    static void registerProfiles() {
        ENGINES.forEach(type -> SchematicConfigurationRegistry.registerClass(type, "Upgrade"));
    }

    public static boolean supports(BlockEntity entity) {
        if (entity == null)
            return false;
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass())
            if (ENGINES.contains(type.getName()))
                return true;
        return false;
    }

    public static ItemRequirement supplement(ItemRequirement base, CompoundTag data) {
        if (base.isInvalid())
            return base;
        String upgrade = data.getString("Upgrade");
        if (upgrade.equals("createdieselgenerators:none"))
            return base;
        String itemId = UPGRADES.get(upgrade);
        // Unknown installed parts must not be printed for free.
        if (itemId == null)
            return ItemRequirement.INVALID;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == Items.AIR)
            return ItemRequirement.INVALID;
        // Preserve a native requirement if an addon version already charges for this part.
        if (base.getRequiredItems().stream().anyMatch(required -> required.usage == ItemRequirement.ItemUseType.CONSUME
                && required.stack.is(item) && required.stack.getCount() >= 1))
            return base;
        return base.union(new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, item));
    }
}
