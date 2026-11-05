package com.ourcreationstudios.create_better_schematics.client;

import java.util.Locale;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets;
import com.ourcreationstudios.create_better_schematics.client.emi.EmiCreateSchematicsMaterials;
import com.ourcreationstudios.create_better_schematics.client.jei.JeiMaterials;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CannonMaterialsClient {
    private static int sequence;
    private static Pending pending;
    private CannonMaterialsClient() {}

    public static void clearPending() { pending = null; }

    public static boolean usesEmi() { return EmiCreateSchematicsMaterials.installed(); }

    public static boolean supportedViewer() { return usesEmi() || JeiMaterials.supportedVersion(); }

    public static boolean exportAvailable() {
        return usesEmi() ? EmiCreateSchematicsMaterials.available() : JeiMaterials.available();
    }

    public static boolean needsEmiAddon() { return usesEmi() && !EmiCreateSchematicsMaterials.addonInstalled(); }

    public static boolean serverAvailable() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(CannonMaterialsPackets.Request.TYPE)
                && connection.hasChannel(CannonMaterialsPackets.View.TYPE);
    }

    public static boolean waiting(SchematicannonScreen screen) {
        return pending != null && pending.screen == screen;
    }

    public static void tick(SchematicannonScreen screen) {
        if (waiting(screen) && ++pending.ticks > 100) {
            pending = null;
            message("error.timeout", false);
        }
    }

    public static void request(SchematicannonScreen screen) {
        request(screen, false);
    }

    public static void refreshAfterSkips(SchematicannonScreen screen) {
        var cannon = screen.getMenu().contentHolder;
        var blueprint = cannon.inventory.getStackInSlot(0);
        boolean source = usesEmi() ? EmiCreateSchematicsMaterials.isSource(cannon.getBlockPos(), blueprint)
                : JeiMaterials.isSource(cannon.getBlockPos(), blueprint);
        if (!source && !waiting(screen))
            return;
        // An earlier reply must not restore materials calculated before Apply succeeded.
        pending = null;
        request(screen, true);
    }

    private static void request(SchematicannonScreen screen, boolean automatic) {
        if (!exportAvailable() || !serverAvailable() || waiting(screen))
            return;
        ItemStack blueprint = screen.getMenu().contentHolder.inventory.getStackInSlot(0);
        if (!(blueprint.getItem() instanceof SchematicItem))
            return;
        int id = sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        int fingerprint = ItemStack.hashItemAndComponents(blueprint);
        pending = new Pending(screen, id, fingerprint, automatic);
        PacketDistributor.sendToServer(new CannonMaterialsPackets.Request(screen.getMenu().containerId, id, fingerprint));
    }

    public static void receive(CannonMaterialsPackets.View view) {
        var request = pending;
        if (request == null || request.id != view.requestId() || request.screen.getMenu().containerId != view.menuId())
            return;
        pending = null;
        if (Minecraft.getInstance().screen != request.screen || request.fingerprint != view.fingerprint()
                || ItemStack.hashItemAndComponents(request.screen.getMenu().contentHolder.inventory.getStackInSlot(0))
                        != view.fingerprint())
            return;
        if (view.result() != CannonMaterialsPackets.Result.READY) {
            message("error." + view.result().name().toLowerCase(Locale.ROOT), false);
            return;
        }
        try {
            var cannon = request.screen.getMenu().contentHolder;
            if (usesEmi())
                EmiCreateSchematicsMaterials.upload(view.entries(), cannon.getBlockPos(), cannon.inventory.getStackInSlot(0));
            else
                JeiMaterials.upload(view.entries(), cannon.getBlockPos(), cannon.inventory.getStackInSlot(0));
            if (!request.automatic && usesEmi() && !view.entries().isEmpty())
                message("success", true, view.entries().size());
        } catch (RuntimeException exception) {
            CreateBetterSchematics.LOGGER.warn("Cannot export cannon materials to {}", usesEmi() ? "EMI: Create Schematics" : "JEI", exception);
            message(usesEmi() ? "error.emi" : "error.jei", false);
        }
    }

    private static void message(String key, boolean success, Object... args) {
        var player = Minecraft.getInstance().player;
        if (player != null)
            player.displayClientMessage(text(key, args).copy().withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED), true);
    }

    public static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + (usesEmi() ? ".cannon.emi." : ".cannon.jei.") + key, args);
    }

    private static final class Pending {
        private final SchematicannonScreen screen;
        private final int id;
        private final int fingerprint;
        private final boolean automatic;
        private int ticks;
        private Pending(SchematicannonScreen screen, int id, int fingerprint, boolean automatic) {
            this.screen = screen;
            this.id = id;
            this.fingerprint = fingerprint;
            this.automatic = automatic;
        }
    }
}
