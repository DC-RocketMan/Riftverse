package dev.riftverse.handbuch;

import dev.riftverse.Config;
import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Das Riftverse-Handbuch: ein GuideME-Guide mit allen Items, Maschinen, Universen und Rezepten.
 * <p>
 * Die Seiten liegen als Markdown unter {@code assets/riftverse/guides/riftverse/handbuch/}. Der Guide selbst wird
 * nur auf dem Client gebaut ({@code client/handbuch/HandbuchClient}); hier steht, was beide Seiten brauchen.
 */
public final class Handbuch {
    /** Id des Guides; GuideME sucht die Seiten unter {@code guides/riftverse/handbuch}. */
    public static final ResourceLocation ID = Riftverse.id("handbuch");
    /** Merker im Spieler: Das Handbuch wurde schon einmal verteilt. */
    public static final String ERHALTEN = "riftverse:handbuch_erhalten";

    private Handbuch() {
    }

    /** Neue Spieler bekommen beim ersten Betreten das Handbuch (abschaltbar in der Config). */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Config.GUIDE_ON_JOIN.get()) return;
        CompoundTag persistent = persistent(player);
        if (persistent.getBoolean(ERHALTEN)) return;
        persistent.putBoolean(ERHALTEN, true);
        ItemStack buch = new ItemStack(ModItems.HANDBUCH.get());
        if (!player.getInventory().add(buch)) player.drop(buch, false);
    }

    /** Unterbereich der Spielerdaten, den NeoForge beim Respawn mitnimmt. */
    private static final String PERSISTED = "PlayerPersisted";

    /** Daten, die den Tod überstehen. */
    private static CompoundTag persistent(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSISTED)) data.put(PERSISTED, new CompoundTag());
        return data.getCompound(PERSISTED);
    }
}
