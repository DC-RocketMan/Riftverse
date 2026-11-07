package dev.riftverse.network;

import dev.riftverse.Riftverse;
import dev.riftverse.item.PortalKanoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Rückkehr-Taste: Der Client meldet den Tastendruck, der Server öffnet ein Rift zum Heimanker. */
public record RueckkehrPayload() implements CustomPacketPayload {
    public static final Type<RueckkehrPayload> TYPE = new Type<>(Riftverse.id("rueckkehr"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RueckkehrPayload> STREAM_CODEC = StreamCodec.unit(new RueckkehrPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, RueckkehrPayload::handle);
    }

    private static void handle(RueckkehrPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack gun = PortalKanoneItem.find(player);
        if (gun == null) {
            player.displayClientMessage(Component.translatable("message.riftverse.keine_kanone").withStyle(ChatFormatting.RED), true);
            return;
        }
        ((PortalKanoneItem) gun.getItem()).returnHome(player, gun);
    }
}
