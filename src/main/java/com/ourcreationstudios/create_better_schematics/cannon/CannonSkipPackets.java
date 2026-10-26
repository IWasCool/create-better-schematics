package com.ourcreationstudios.create_better_schematics.cannon;

import java.util.List;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.client.CannonSkipScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CannonSkipPackets {
    // Registry IDs keep even the maximum serverbound selection below the 32 KiB payload limit.
    public static final int MAX_BLOCK_TYPES = 4096;
    private static final StreamCodec<RegistryFriendlyByteBuf, Block> BLOCK_CODEC = ByteBufCodecs.registry(Registries.BLOCK);

    private CannonSkipPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Saving schematics is local; only the optional cannon picker needs a peer.
        var registrar = event.registrar("2").optional();
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                var cannon = CannonSkipService.openCannon(player, packet.menuId());
                if (cannon != null)
                    PacketDistributor.sendToPlayer(player, CannonSkipService.inspect(cannon, packet.menuId()));
            }
        });
        registrar.playToServer(Save.TYPE, Save.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                Result result = CannonSkipService.apply(player, packet);
                PacketDistributor.sendToPlayer(player,
                        new View(packet.menuId(), packet.fingerprint(), List.of(), packet.skipMissing(), result));
            }
        });
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> CannonSkipScreen.receive(packet));
    }

    public enum Result {
        READY, APPLIED, NO_SCHEMATIC, CHANGED, TOO_LARGE, FAILED, CLOSED
    }

    public record BlockEntry(Block block, int count, boolean skipped) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockEntry> CODEC = StreamCodec.composite(
                BLOCK_CODEC, BlockEntry::block, ByteBufCodecs.VAR_INT, BlockEntry::count,
                ByteBufCodecs.BOOL, BlockEntry::skipped, BlockEntry::new);
    }

    public record Request(int menuId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = CannonSkipPackets.type("cannon_skip_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::menuId, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Save(int menuId, int fingerprint, List<Block> skipped, boolean skipMissing) implements CustomPacketPayload {
        public Save {
            skipped = List.copyOf(skipped);
        }
        public static final Type<Save> TYPE = CannonSkipPackets.type("cannon_skip_save");
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Save::menuId, ByteBufCodecs.INT, Save::fingerprint,
                BLOCK_CODEC.apply(ByteBufCodecs.list(MAX_BLOCK_TYPES)), Save::skipped,
                ByteBufCodecs.BOOL, Save::skipMissing, Save::new);
        @Override public Type<Save> type() { return TYPE; }
    }

    public record View(int menuId, int fingerprint, List<BlockEntry> entries, boolean skipMissing, Result result)
            implements CustomPacketPayload {
        public View {
            entries = List.copyOf(entries);
        }
        public static final Type<View> TYPE = CannonSkipPackets.type("cannon_skip_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::menuId, ByteBufCodecs.INT, View::fingerprint,
                BlockEntry.CODEC.apply(ByteBufCodecs.list(MAX_BLOCK_TYPES)), View::entries,
                ByteBufCodecs.BOOL, View::skipMissing,
                ByteBufCodecs.VAR_INT.map(value -> Result.values()[value], Result::ordinal), View::result, View::new);
        @Override public Type<View> type() { return TYPE; }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateBetterSchematics.MODID, path));
    }
}
