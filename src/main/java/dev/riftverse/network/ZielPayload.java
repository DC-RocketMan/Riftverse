package dev.riftverse.network;

import dev.riftverse.Riftverse;
import dev.riftverse.item.PortalKanoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ziel-Taste der Kanone ab Mk III (Standard V): nächste Auswahl (Zufall → Kurzstrecke → gespeicherte Ziele);
 * mit Schleichen wird das gewählte Ziel gelöscht. Die Kanone muss in einer Hand liegen.
 */
public record ZielPayload(boolean loeschen) implements CustomPacketPayload {
    public static final Type<ZielPayload> TYPE = new Type<>(Riftverse.id("ziel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZielPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(ZielPayload::new, ZielPayload::loeschen);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ZielPayload::handle);
    }

    private static void handle(ZielPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack gun = player.getMainHandItem().getItem() instanceof PortalKanoneItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof PortalKanoneItem ? player.getOffhandItem() : null;
        if (gun == null) {
            player.displayClientMessage(Component.translatable("message.riftverse.keine_kanone_hand").withStyle(ChatFormatting.RED), true);
            return;
        }
        ((PortalKanoneItem) gun.getItem()).zielWechseln(player, gun, payload.loeschen());
    }
}
