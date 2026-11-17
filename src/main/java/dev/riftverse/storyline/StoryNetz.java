package dev.riftverse.storyline;

import dev.riftverse.Riftverse;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Netzwerk des Story-Gerüsts: Dialog über der Figur, gewählte Antwort, Ziel-Anzeige und das Rift-Tagebuch.
 * Die Client-Seite steht in {@code client/story/ClientStory}; sie wird nur über Lambdas angesprochen, damit der
 * Dedicated Server die Client-Klassen nie lädt.
 */
public final class StoryNetz {
    private StoryNetz() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(Dialog.TYPE, Dialog.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> dev.riftverse.client.story.ClientStory.dialog(p)));
        r.playToClient(Ziel.TYPE, Ziel.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> dev.riftverse.client.story.ClientStory.ziel(p)));
        r.playToClient(Tagebuch.TYPE, Tagebuch.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> dev.riftverse.client.story.ClientStory.tagebuch(p)));
        r.playToServer(Antwort.TYPE, Antwort.STREAM_CODEC, StoryNetz::antwort);
    }

    private static void antwort(Antwort payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) StoryManager.antwort(player, payload.nummer());
        });
    }

    public static void senden(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    // ------------------------------------------------------------------ Pakete

    /**
     * Dialog über einer Figur. {@code entity < 0} schließt alle Dialoge.
     *
     * @param mehr  es folgen weitere Zeilen (Hinweis „Rechtsklick: weiter“)
     * @param dauer Ticks, bis die Blase von selbst verschwindet (0 = bleibt bis zur nächsten Zeile)
     */
    public record Dialog(int entity, String sprecher, String text, List<String> antworten, boolean mehr, int dauer)
            implements CustomPacketPayload {
        public static final Type<Dialog> TYPE = new Type<>(Riftverse.id("story_dialog"));
        public static final StreamCodec<ByteBuf, Dialog> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Dialog::entity,
                ByteBufCodecs.STRING_UTF8, Dialog::sprecher,
                ByteBufCodecs.STRING_UTF8, Dialog::text,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Dialog::antworten,
                ByteBufCodecs.BOOL, Dialog::mehr,
                ByteBufCodecs.VAR_INT, Dialog::dauer,
                Dialog::new);

        public static Dialog schliessen() {
            return new Dialog(-1, "", "", List.of(), false, 0);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Gewählte Antwort (0–3) im laufenden Dialog. */
    public record Antwort(int nummer) implements CustomPacketPayload {
        public static final Type<Antwort> TYPE = new Type<>(Riftverse.id("story_antwort"));
        public static final StreamCodec<ByteBuf, Antwort> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Antwort::new, Antwort::nummer);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Aktives Ziel für die Anzeige oben rechts (leer = keines). */
    public record Ziel(String storyline, String ziel) implements CustomPacketPayload {
        public static final Type<Ziel> TYPE = new Type<>(Riftverse.id("story_ziel"));
        public static final StreamCodec<ByteBuf, Ziel> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Ziel::storyline,
                ByteBufCodecs.STRING_UTF8, Ziel::ziel,
                Ziel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Seiten des Rift-Tagebuchs; der Client öffnet damit die Buchansicht. */
    public record Tagebuch(List<Component> seiten) implements CustomPacketPayload {
        public static final Type<Tagebuch> TYPE = new Type<>(Riftverse.id("story_tagebuch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tagebuch> STREAM_CODEC =
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Tagebuch::new, Tagebuch::seiten);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
