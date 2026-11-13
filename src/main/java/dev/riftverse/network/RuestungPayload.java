package dev.riftverse.network;

import dev.riftverse.Riftverse;
import dev.riftverse.ruestung.Ruestung;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tasten der Multiversal-Rüstung (siehe {@link Ruestung#aktion}). */
public record RuestungPayload(int aktion) implements CustomPacketPayload {
    public static final Type<RuestungPayload> TYPE = new Type<>(Riftverse.id("ruestung"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuestungPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(RuestungPayload::new, RuestungPayload::aktion);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, RuestungPayload::handle);
    }

    private static void handle(RuestungPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) Ruestung.aktion(player, payload.aktion());
    }
}
