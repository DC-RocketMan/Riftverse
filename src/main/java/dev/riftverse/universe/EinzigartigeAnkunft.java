package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Begrüßung in einzigartigen Universen. Beim ersten Besuch eines Spielers: großer Titel in Gold, Untertitel
 * „Ein einzigartiges Universum“, der Text aus der Sprachdatei im Chat, ein tiefer Klang, eine Erinnerungskarte und
 * der Fortschritt „Etwas zieht dich“. Bei jeder Rückkehr: der Titel und einer von mehreren wechselnden Sätzen.
 * <p>
 * Ob jemand schon da war, zählt pro Spieler ({@link MultiverseData.Record#angekommen}); der Story-Stand der Welt
 * gilt dagegen für alle.
 */
public final class EinzigartigeAnkunft {
    /** Fortschritt beim ersten einzigartigen Universum. */
    public static final ResourceLocation FORTSCHRITT = Riftverse.id("etwas_zieht_dich");
    public static final String KRITERIUM = "einzigartig";

    private EinzigartigeAnkunft() {
    }

    public static void ankommen(ServerPlayer player, UniverseId id) {
        MultiverseData data = MultiverseData.get(player.server);
        MultiverseData.Record rec = data.record(id);
        rec.visits++;
        boolean erstes = rec.angekommen.add(player.getUUID());
        data.setDirty();

        UniverseType type = id.type();
        Component untertitel = erstes
                ? Component.translatable("message.riftverse.einzigartig.untertitel").withStyle(ChatFormatting.WHITE)
                : Einzigartig.rueckkehr(type, 1 + player.getRandom().nextInt(Einzigartig.RUECKKEHR_SAETZE)).copy()
                        .withStyle(ChatFormatting.WHITE);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, erstes ? 100 : 60, 30));
        player.connection.send(new ClientboundSetSubtitleTextPacket(untertitel));
        player.connection.send(new ClientboundSetTitleTextPacket(Einzigartig.anzeigeName(type).copy().withStyle(ChatFormatting.GOLD)));

        if (erstes) {
            player.sendSystemMessage(Component.literal("⌁ ").withStyle(ChatFormatting.GOLD)
                    .append(Einzigartig.ersterBesuch(type).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)));
            player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0f, 0.5f);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.0f, 0.6f);
            erinnerungskarte(player, id);
            fortschritt(player);
        } else {
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.8f, 0.7f);
        }
    }

    /** Einmalig beim ersten Besuch: eine Koordinatenkarte zurück in diese Welt (ab Kanone Mk II nutzbar). */
    public static ItemStack erinnerungskarte(ServerPlayer player, UniverseId id) {
        ItemStack karte = new ItemStack(ModItems.KOORDINATENKARTE.get());
        karte.set(ModDataComponents.ZIEL.get(), id);
        ItemStack geschenk = karte.copy();
        if (!player.getInventory().add(karte)) player.drop(karte, false);
        player.sendSystemMessage(Component.translatable("message.riftverse.einzigartig.karte", id.label())
                .withStyle(ChatFormatting.GRAY));
        return geschenk;
    }

    private static void fortschritt(ServerPlayer player) {
        AdvancementHolder holder = player.server.getAdvancements().get(FORTSCHRITT);
        if (holder != null) player.getAdvancements().award(holder, KRITERIUM);
    }
}
