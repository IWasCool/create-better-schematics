package com.ourcreationstudios.create_better_schematics.cannon;

import java.util.List;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CannonMaterialsPackets {
    public static final int MAX_ENTRIES = 4096;
    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;

    private CannonMaterialsPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2").optional();
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player && player.connection.hasChannel(View.TYPE))
                PacketDistributor.sendToPlayer(player, CannonMaterialsService.inspect(player, packet));
        });
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> CannonMaterialsClient.receive(packet));
    }

    public enum Result {
        READY, NO_SCHEMATIC, CHANGED, UNLOADED, TOO_LARGE, FAILED, CLOSED
    }

    public record Request(int menuId, int requestId, int fingerprint) implements CustomPacketPayload {
        public static final Type<Request> TYPE = CannonMaterialsPackets.type("cannon_jei_materials_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::menuId, ByteBufCodecs.VAR_INT, Request::requestId,
                ByteBufCodecs.INT, Request::fingerprint, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }

    public record View(int menuId, int requestId, int fingerprint, List<CannonMaterial> entries, Result result)
            implements CustomPacketPayload {
        public View {
            entries = List.copyOf(entries);
        }
        public static final Type<View> TYPE = CannonMaterialsPackets.type("cannon_jei_materials_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::menuId, ByteBufCodecs.VAR_INT, View::requestId,
                ByteBufCodecs.INT, View::fingerprint,
                CannonMaterial.CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), View::entries,
                ByteBufCodecs.VAR_INT.map(index -> Result.values()[index], Result::ordinal), View::result, View::new);
        @Override public Type<View> type() { return TYPE; }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateBetterSchematics.MODID, path));
    }
}
