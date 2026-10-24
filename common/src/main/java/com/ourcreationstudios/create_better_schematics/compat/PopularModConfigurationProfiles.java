package com.ourcreationstudios.create_better_schematics.compat;

/** Configuration profiles audited against the versions pinned in popular-mod-test-lock.json. */
final class PopularModConfigurationProfiles {
    private PopularModConfigurationProfiles() {
    }

    static void register() {
        String mekanism = "mekanism.common.tile.base.TileEntityMekanism";
        block(mekanism, "control_type", "CustomName", "component_ejector.strict_input",
                "component_ejector.color", "component_ejector.input_color");
        // Transmission IDs differ across Mekanism versions. These fields only describe IO modes.
        for (int transmission = 0; transmission <= 6; transmission++)
            block(mekanism, "component_config.config" + transmission, "component_config.eject" + transmission);
        block("mekanism.common.tile.factory.TileEntityFactory", "sorting");
        block("mekanism.common.tile.TileEntityLogisticalSorter", "color", "eject", "round_robin", "single_item", "filters");

        block("appeng.blockentity.misc.InscriberBlockEntity", "customName", "inscriber_separate_sides",
                "auto_export", "inscriber_input_capacity");
        block("appeng.blockentity.storage.IOPortBlockEntity", "customName", "redstone_controlled", "fullness_mode", "operation_mode");
        block("appeng.blockentity.misc.InterfaceBlockEntity", "customName", "config", "priority", "fuzzy_mode");
        block("appeng.blockentity.crafting.PatternProviderBlockEntity", "customName", "priority", "blocking_mode",
                "pattern_access_terminal", "lock_crafting_mode");

        block("com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers",
                "Lock", "Shr", "Qua", "Pri", "Sus", "CustomName");
        // Item is a prototype. Omitting Count creates an empty drawer with the same remembered type.
        // Compacting drawers use a different accounting model and are deliberately not matched here.
        block("com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard", "Drawers.*.Item");

        block("net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity", "displayName", "locked", "showLock",
                "showTier", "showUpgrades", "storageWrapper.sortBy",
                "storageWrapper.settings.memory.slotFilterItems", "storageWrapper.settings.memory.slotFilterStacks",
                "storageWrapper.settings.memory.ignoreNbt", "storageWrapper.settings.no_sort.selectedSlots",
                "storageWrapper.settings.no_sort.color", "storageWrapper.settings.item_display.slots",
                "storageWrapper.settings.item_display.rotations", "storageWrapper.settings.item_display.zOffsets",
                "storageWrapper.settings.item_display.color", "storageWrapper.settings.item_display.displaySide");
    }

    private static void block(String className, String... fields) {
        SchematicConfigurationRegistry.registerClass(className, fields);
    }
}
