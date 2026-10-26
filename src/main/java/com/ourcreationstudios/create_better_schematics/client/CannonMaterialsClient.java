package com.ourcreationstudios.create_better_schematics.client;

import java.util.Locale;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterialsPackets;
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
        if (!JeiMaterials.available() || !serverAvailable() || waiting(screen))
            return;
        ItemStack blueprint = screen.getMenu().contentHolder.inventory.getStackInSlot(0);
        if (!(blueprint.getItem() instanceof SchematicItem))
            return;
        int id = sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        int fingerprint = ItemStack.hashItemAndComponents(blueprint);
        pending = new Pending(screen, id, fingerprint);
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
            JeiMaterials.upload(view.entries());
            message(view.entries().isEmpty() ? "empty" : "success", true, view.entries().size());
        } catch (RuntimeException exception) {
            CreateBetterSchematics.LOGGER.warn("Cannot upload cannon materials to JEI", exception);
            message("error.jei", false);
        }
    }

    private static void message(String key, boolean success, Object... args) {
        var player = Minecraft.getInstance().player;
        if (player != null)
            player.displayClientMessage(text(key, args).copy().withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED), true);
    }

    public static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + ".cannon.jei." + key, args);
    }

    private static final class Pending {
        private final SchematicannonScreen screen;
        private final int id;
        private final int fingerprint;
        private int ticks;
        private Pending(SchematicannonScreen screen, int id, int fingerprint) {
            this.screen = screen;
            this.id = id;
            this.fingerprint = fingerprint;
        }
    }
}
