package dev.riftverse.storyline;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModAttachments;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModItems;
import dev.riftverse.storyline.Storyline.Aktion;
import dev.riftverse.storyline.Storyline.Ausloeser;
import dev.riftverse.storyline.Storyline.Figur;
import dev.riftverse.storyline.Storyline.Geltung;
import dev.riftverse.storyline.Storyline.Schritt;
import dev.riftverse.storyline.Storyline.Zeile;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseType;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Führt die Storylines aus: erkennt Auslöser (Ereignisse und einmal pro Sekunde abgefragte Zustände), führt Aktionen
 * aus und hält die Dialoge. Ein Dialog pausiert seine Aktionsliste; was danach kommt, läuft erst, wenn der Dialog zu
 * Ende ist. Geht der Spieler weg, endet das Gespräch, und es beginnt beim nächsten Ansprechen von vorn.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class StoryManager {
    /** Weiter weg als so viele Blöcke endet ein Gespräch. */
    public static final double REICHWEITE = 10.0;
    /** Ticks, die ein Satz ohne Gespräch (Standardsatz der Figur) über dem Kopf steht. */
    public static final int STANDARD_DAUER = 100;
    /** Präfix der Flags, die eine Antwort mit Id setzt. */
    public static final String ANTWORT_FLAG = "antwort:";

    private static final Map<UUID, Sitzung> SITZUNGEN = new HashMap<>();
    private static final Map<UUID, String> LETZTES_ZIEL = new HashMap<>();

    private StoryManager() {
    }

    /** Ein laufender Dialog eines Spielers. */
    static final class Sitzung {
        final Storyline story;
        final String figur;
        final UUID figurUuid;
        final int entityId;
        final ResourceKey<Level> dim;
        final List<Zeile> zeilen;
        final Deque<Aktion> rest;
        int index = -1;

        Sitzung(Storyline story, StoryFigur f, List<Zeile> zeilen, Deque<Aktion> rest) {
            this.story = story;
            this.figur = f.figur();
            this.figurUuid = f.getUUID();
            this.entityId = f.getId();
            this.dim = f.level().dimension();
            this.zeilen = zeilen;
            this.rest = rest;
        }

        @Nullable
        Zeile zeile() {
            return index >= 0 && index < zeilen.size() ? zeilen.get(index) : null;
        }
    }

    @FunctionalInterface
    interface Passt {
        boolean test(Ausloeser a, Storyline s, StoryStand st);
    }

    // ================================================================== Zugriff auf Stände

    @Nullable
    public static UniverseId welt(ServerPlayer p) {
        UniverseId id = Multiverse.at(p.level(), p.blockPosition());
        return Einzigartig.ist(id) ? id : null;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server, Storyline s) {
        return server.getLevel(s.welt().dimension());
    }

    private static MultiverseData daten(MinecraftServer server) {
        return MultiverseData.get(server);
    }

    private static MultiverseData.Record record(MinecraftServer server, Storyline s) {
        return daten(server).record(Einzigartig.id(s.welt()));
    }

    public static StoryStand weltStand(MinecraftServer server, Storyline s) {
        return record(server, s).story.welt(s.id().toString());
    }

    public static StoryStand spielerStand(MinecraftServer server, Storyline s, UUID spieler) {
        return record(server, s).story.spieler(spieler, s.id().toString());
    }

    /** Der Stand, in dem Schritt und Fortschritt liegen (nach der Geltung der Storyline). */
    public static StoryStand stand(ServerPlayer p, Storyline s) {
        return stand(p, s, s.geltung());
    }

    private static StoryStand stand(ServerPlayer p, Storyline s, Geltung g) {
        return g == Geltung.WELT ? weltStand(p.server, s) : spielerStand(p.server, s, p.getUUID());
    }

    /** Flags aus dem Welt- und dem Spieler-Stand zusammen. */
    public static Set<String> flags(ServerPlayer p, Storyline s) {
        Set<String> out = new LinkedHashSet<>(weltStand(p.server, s).flags);
        out.addAll(spielerStand(p.server, s, p.getUUID()).flags);
        return out;
    }

    private static Set<String> gelesen(ServerPlayer p, Storyline s) {
        Set<String> out = new LinkedHashSet<>(weltStand(p.server, s).gelesen);
        out.addAll(spielerStand(p.server, s, p.getUUID()).gelesen);
        return out;
    }

    @Nullable
    public static Schritt aktuell(Storyline s, StoryStand st) {
        if (st.fertig) return null;
        return s.schritt(st.schritt != null ? st.schritt : s.start());
    }

    private static void dirty(MinecraftServer server) {
        daten(server).setDirty();
    }

    // ================================================================== Ereignisse

    /** Ankunft in einem einzigartigen Universum (aus der Begrüßung). */
    public static void ankunft(ServerPlayer p, UniverseId id) {
        ServerLevel level = p.serverLevel();
        figurenSicherstellen(level, id.type());
        ereignis(p, (a, s, st) -> a instanceof Ausloeser.Ankunft);
        zielAktualisieren(p);
    }

    /** Rechtsklick auf eine Story-Figur. */
    public static void angesprochen(ServerPlayer p, StoryFigur f) {
        ResourceLocation sid = ResourceLocation.tryParse(f.storyline());
        Storyline s = sid == null ? null : StoryLoader.get(sid);
        if (s == null) return;
        f.sprichtMit(p);

        Sitzung z = SITZUNGEN.get(p.getUUID());
        if (z != null && z.figurUuid.equals(f.getUUID())) {
            Zeile zeile = z.zeile();
            if (zeile != null && !zeile.antworten().isEmpty()) {
                zeigen(p, z);   // erst eine Antwort wählen
            } else {
                naechsteZeile(p, z);
            }
            return;
        }
        if (z != null) abbrechen(p);

        // Abgeben vor Reden: Hat der Spieler das gesuchte Item in der Hand, wird es übergeben
        ItemStack hand = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (versuche(p, s, (a, story, st) -> a instanceof Ausloeser.Abgeben g && g.figur().equals(f.figur())
                && item(g.item()) != Items.AIR && hand.is(item(g.item())) && hand.getCount() >= g.anzahl(), a -> {
            Ausloeser.Abgeben g = (Ausloeser.Abgeben) a;
            if (!p.getAbilities().instabuild) hand.shrink(g.anzahl());
        })) return;
        if (versuche(p, s, (a, story, st) -> a instanceof Ausloeser.Reden r && r.figur().equals(f.figur()), a -> {})) return;

        Figur def = s.figuren().get(f.figur());
        if (def != null && def.standard() != null) {
            StoryNetz.senden(p, new StoryNetz.Dialog(f.getId(), def.name(), def.standard(), List.of(), false, STANDARD_DAUER));
        }
    }

    /** Gewählte Antwort im laufenden Dialog (0–3). */
    public static void antwort(ServerPlayer p, int nummer) {
        Sitzung z = SITZUNGEN.get(p.getUUID());
        if (z == null) return;
        Zeile zeile = z.zeile();
        if (zeile == null || nummer < 0 || nummer >= zeile.antworten().size()) return;
        Storyline.Antwort a = zeile.antworten().get(nummer);
        Deque<Aktion> q = new ArrayDeque<>(a.aktionen());
        if (a.id() != null) stand(p, z.story).flags.add(ANTWORT_FLAG + a.id());
        boolean endet = a.verzweigt() || a.aktionen().stream().anyMatch(x -> x instanceof Aktion.Dialog);
        if (endet) {
            SITZUNGEN.remove(p.getUUID());
            StoryNetz.senden(p, StoryNetz.Dialog.schliessen());
            q.addAll(z.rest);
            laufe(p, z.story, q);
        } else {
            laufe(p, z.story, q);
            naechsteZeile(p, z);
        }
        dirty(p.server);
        pruefen(p);
    }

    /** Für Rauchtest und Befehle: läuft für diesen Spieler gerade ein Dialog? */
    public static boolean imDialog(ServerPlayer p) {
        return SITZUNGEN.containsKey(p.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        sekunde(p);
    }

    /** Einmal pro Sekunde: Gespräch noch in Reichweite? Dann die Zustands-Auslöser abfragen. */
    public static void sekunde(ServerPlayer p) {
        Sitzung z = SITZUNGEN.get(p.getUUID());
        if (z != null) {
            Entity e = p.level().dimension() == z.dim ? p.serverLevel().getEntity(z.figurUuid) : null;
            if (e == null || e.distanceTo(p) > REICHWEITE) {
                abbrechen(p);
            } else if (e instanceof StoryFigur f) {
                f.sprichtMit(p);
            }
        }
        pruefen(p);
    }

    /** Fragt die Zustands-Auslöser ab (Bereich, Item, Zeit, Flag, Notiz gelesen, Antwort) und aktualisiert das Ziel. */
    public static void pruefen(ServerPlayer p) {
        UniverseId id = welt(p);
        if (id == null) {
            zielSenden(p, "", "");
            return;
        }
        ServerLevel level = p.serverLevel();
        figurenSicherstellen(level, id.type());
        ereignis(p, (a, s, st) -> switch (a) {
            case Ausloeser.Bereich b -> b.pos().abstandXZ(p.getX(), p.getZ()) <= b.radius();
            case Ausloeser.Item i -> item(i.item()) != Items.AIR && p.getInventory().countItem(item(i.item())) >= i.anzahl();
            case Ausloeser.Zeit t -> level.isNight() == t.nacht();
            case Ausloeser.Flag f -> flags(p, s).contains(f.flag());
            case Ausloeser.NotizGelesen n -> gelesen(p, s).contains(n.notiz());
            case Ausloeser.Antwort an -> flags(p, s).contains(ANTWORT_FLAG + an.antwort());
            default -> false;
        });
        zielAktualisieren(p);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || welt(p) == null) return;
        ResourceLocation mob = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        UniverseId id = welt(p);
        for (Storyline s : StoryLoader.fuer(id.type())) {
            StoryStand st = stand(p, s);
            Schritt schritt = aktuell(s, st);
            if (schritt == null) continue;
            for (Ausloeser a : schritt.ausloeser()) {
                if (a instanceof Ausloeser.Besiegt b && b.mob().equals(mob)) {
                    st.zaehler.merge(mob.toString(), 1, Integer::sum);
                    dirty(p.server);
                    break;
                }
            }
        }
        ereignis(p, (a, s, st) -> a instanceof Ausloeser.Besiegt b && st.zaehler.getOrDefault(b.mob().toString(), 0) >= b.anzahl());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer p) || welt(p) == null) return;
        BlockPos pos = event.getPos();
        ereignis(p, (a, s, st) -> a instanceof Ausloeser.Block b && b.pos().equals(pos));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SITZUNGEN.remove(event.getEntity().getUUID());
        LETZTES_ZIEL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && SITZUNGEN.containsKey(p.getUUID())) abbrechen(p);
    }

    // ================================================================== Auslösen

    /** Prüft alle Storylines der Welt des Spielers; bei einem passenden Auslöser laufen die Aktionen des Schritts. */
    static void ereignis(ServerPlayer p, Passt passt) {
        UniverseId id = welt(p);
        if (id == null) return;
        for (Storyline s : StoryLoader.fuer(id.type())) versuche(p, s, passt, a -> {});
    }

    /** Wie {@link #ereignis}, aber für eine Storyline; {@code vorher} läuft mit dem passenden Auslöser vor den Aktionen. */
    private static boolean versuche(ServerPlayer p, Storyline s, Passt passt, java.util.function.Consumer<Ausloeser> vorher) {
        Sitzung z = SITZUNGEN.get(p.getUUID());
        if (z != null && z.story == s) return false;   // während eines Dialogs wartet die Storyline
        StoryStand st = stand(p, s);
        Schritt schritt = aktuell(s, st);
        if (schritt == null || !schritt.bedingung().erfuellt(flags(p, s))) return false;
        for (Ausloeser a : schritt.ausloeser()) {
            if (passt.test(a, s, st)) {
                vorher.accept(a);
                ausloesen(p, s, st, schritt);
                return true;
            }
        }
        return false;
    }

    private static void ausloesen(ServerPlayer p, Storyline s, StoryStand st, Schritt schritt) {
        if (st.schritt == null) st.schritt = schritt.id();
        tagebuchGeben(p);
        Deque<Aktion> q = new ArrayDeque<>(schritt.aktionen());
        q.add(new Aktion.SchrittEnde(schritt.id(), schritt.weiter(), schritt.abschluss()));
        laufe(p, s, q);
        dirty(p.server);
    }

    /** Führt die Aktionen nacheinander aus; ein Dialog hält die Liste an, bis er zu Ende ist. */
    static void laufe(ServerPlayer p, Storyline s, Deque<Aktion> q) {
        while (!q.isEmpty()) {
            Aktion a = q.poll();
            if (a instanceof Aktion.Dialog d) {
                starteDialog(p, s, d, q);
                return;
            }
            try {
                ausfuehren(p, s, a);
            } catch (RuntimeException e) {
                Riftverse.LOGGER.warn("Riftverse-Storyline {}: Aktion {} fehlgeschlagen", s.id(), a, e);
            }
        }
        dirty(p.server);
    }

    private static void ausfuehren(ServerPlayer p, Storyline s, Aktion a) {
        ServerLevel level = level(p.server, s);
        switch (a) {
            case Aktion.Notiz n -> {
                StoryStand st = stand(p, s, n.geltung() != null ? n.geltung() : s.geltung());
                if (st.notizen.add(n.id())) {
                    p.sendSystemMessage(Component.translatable("message.riftverse.story.notiz", n.titel()).withStyle(ChatFormatting.GOLD));
                    p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
                }
            }
            case Aktion.Titel t -> {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(t.untertitel())));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t.titel()).withStyle(ChatFormatting.GOLD)));
            }
            case Aktion.Nachricht m -> p.sendSystemMessage(Component.literal(m.text()));
            case Aktion.ItemGeben i -> {
                Item item = item(i.item());
                if (item == Items.AIR) throw new IllegalArgumentException("unbekanntes Item " + i.item());
                ItemStack stack = new ItemStack(item, i.anzahl());
                if (i.name() != null) stack.set(DataComponents.CUSTOM_NAME, Component.literal(i.name()));
                if (!p.getInventory().add(stack)) p.drop(stack, false);
            }
            case Aktion.Xp x -> p.giveExperiencePoints(x.menge());
            case Aktion.Sound so -> {
                SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(so.sound());
                if (ev == null) ev = SoundEvent.createVariableRangeEvent(so.sound());
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ev, SoundSource.NEUTRAL, so.lautstaerke(), so.tonhoehe());
            }
            case Aktion.FlagSetzen f -> {
                StoryStand st = stand(p, s, f.geltung() != null ? f.geltung() : s.geltung());
                if (f.wert()) st.flags.add(f.flag());
                else st.flags.remove(f.flag());
            }
            case Aktion.FigurAktion f -> {
                if (level == null) return;
                Figur def = s.figuren().get(f.figur());
                if (def == null) return;
                if (f.erscheinen()) figurSetzen(level, s, def, f.ort());
                else figurEntfernen(level, s, f.figur());
            }
            case Aktion.Mob m -> {
                if (level == null) return;
                EntityType<?> typ = BuiltInRegistries.ENTITY_TYPE.get(m.mob());
                BlockPos pos = m.ort().aufloesen(level);
                for (int i = 0; i < m.anzahl(); i++) typ.spawn(level, pos, MobSpawnType.EVENT);
            }
            case Aktion.Bloecke b -> {
                if (level != null) bloeckeSetzen(level, b);
            }
            case Aktion.Schritt sp -> schrittSetzen(p.server, s, stand(p, s), sp.schritt());
            case Aktion.Abschluss ab -> abschliessen(p, s);
            case Aktion.SchrittEnde e -> {
                StoryStand st = stand(p, s);
                if (st.fertig || !e.von().equals(st.schritt)) return;   // inzwischen woanders hingesprungen
                if (e.abschluss()) {
                    abschliessen(p, s);
                } else {
                    String weiter = e.weiter() != null ? e.weiter() : s.naechster(e.von());
                    if (weiter == null) abschliessen(p, s);
                    else schrittSetzen(p.server, s, st, weiter);
                }
            }
            case Aktion.Dialog d -> { }   // läuft über starteDialog
        }
    }

    public static void schrittSetzen(MinecraftServer server, Storyline s, StoryStand st, String schritt) {
        st.schritt = schritt;
        st.zaehler.clear();
        st.fertig = false;
        dirty(server);
    }

    private static void abschliessen(ServerPlayer p, Storyline s) {
        StoryStand st = stand(p, s);
        if (st.fertig) return;
        st.fertig = true;
        dirty(p.server);
        List<ServerPlayer> empfaenger = new ArrayList<>();
        if (s.geltung() == Geltung.WELT) {
            ServerLevel level = level(p.server, s);
            if (level != null) empfaenger.addAll(level.players());
        }
        if (!empfaenger.contains(p)) empfaenger.add(p);
        AdvancementHolder holder = s.fortschritt() != null ? p.server.getAdvancements().get(s.fortschritt()) : null;
        for (ServerPlayer x : empfaenger) {
            x.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            x.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s.titel()).withStyle(ChatFormatting.WHITE)));
            x.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.riftverse.story.abgeschlossen")
                    .withStyle(ChatFormatting.GOLD)));
            x.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1f);
            if (holder != null) x.getAdvancements().award(holder, "abgeschlossen");
        }
    }

    // ================================================================== Dialog

    private static void starteDialog(ServerPlayer p, Storyline s, Aktion.Dialog d, Deque<Aktion> rest) {
        ServerLevel level = level(p.server, s);
        Figur def = s.figuren().get(d.figur());
        StoryFigur f = level != null && def != null ? figur(level, s, d.figur()) : null;
        if (f == null && level != null && def != null) f = figurSetzen(level, s, def, null);
        if (f == null) {
            // Ohne Figur gibt es nichts, worüber der Dialog schweben kann: Zeilen in den Chat, weiter im Text
            for (Zeile z : d.zeilen()) p.sendSystemMessage(Component.literal(z.text()).withStyle(ChatFormatting.GRAY));
            laufe(p, s, rest);
            return;
        }
        Sitzung z = new Sitzung(s, f, d.zeilen(), rest);
        SITZUNGEN.put(p.getUUID(), z);
        f.sprichtMit(p);
        naechsteZeile(p, z);
    }

    private static boolean sichtbar(Zeile z, Set<String> flags) {
        if (z.wenn() == null) return true;
        return z.wenn().startsWith("!") ? !flags.contains(z.wenn().substring(1)) : flags.contains(z.wenn());
    }

    private static void naechsteZeile(ServerPlayer p, Sitzung z) {
        Set<String> flags = flags(p, z.story);
        do {
            z.index++;
        } while (z.index < z.zeilen.size() && !sichtbar(z.zeilen.get(z.index), flags));
        if (z.index >= z.zeilen.size()) {
            beenden(p, z);
            return;
        }
        zeigen(p, z);
    }

    private static void zeigen(ServerPlayer p, Sitzung z) {
        Zeile zeile = z.zeile();
        if (zeile == null) return;
        Set<String> flags = flags(p, z.story);
        boolean mehr = false;
        for (int i = z.index + 1; i < z.zeilen.size() && !mehr; i++) mehr = sichtbar(z.zeilen.get(i), flags);
        Figur def = z.story.figuren().get(z.figur);
        String sprecher = zeile.sprecher() != null ? zeile.sprecher() : def != null ? def.name() : "";
        List<String> antworten = zeile.antworten().stream().map(Storyline.Antwort::text).toList();
        StoryNetz.senden(p, new StoryNetz.Dialog(z.entityId, sprecher, zeile.text(), antworten, mehr || antworten.isEmpty(), 0));
    }

    private static void beenden(ServerPlayer p, Sitzung z) {
        SITZUNGEN.remove(p.getUUID());
        StoryNetz.senden(p, StoryNetz.Dialog.schliessen());
        laufe(p, z.story, z.rest);
        pruefen(p);
    }

    /** Gespräch abbrechen (weggegangen): Die Aktionen nach dem Dialog verfallen, beim nächsten Ansprechen geht es von vorn los. */
    public static void abbrechen(ServerPlayer p) {
        if (SITZUNGEN.remove(p.getUUID()) != null) StoryNetz.senden(p, StoryNetz.Dialog.schliessen());
    }

    // ================================================================== Figuren

    @Nullable
    public static StoryFigur figur(ServerLevel level, Storyline s, String figur) {
        UUID uuid = weltStand(level.getServer(), s).figuren.get(figur);
        return uuid != null && level.getEntity(uuid) instanceof StoryFigur f ? f : null;
    }

    /** Setzt die Figuren mit „sofort“, wenn die Welt die Storyline zum ersten Mal sieht. */
    public static void figurenSicherstellen(ServerLevel level, UniverseType welt) {
        for (Storyline s : StoryLoader.fuer(welt)) {
            StoryStand w = weltStand(level.getServer(), s);
            if (w.figurenGesetzt) continue;
            w.figurenGesetzt = true;
            for (Figur f : s.figuren().values()) {
                if (f.sofort()) figurSetzen(level, s, f, null);
            }
            dirty(level.getServer());
        }
    }

    public static StoryFigur figurSetzen(ServerLevel level, Storyline s, Figur def, @Nullable Storyline.Ort ort) {
        figurEntfernen(level, s, def.id());
        BlockPos pos = (ort != null ? ort : def.ort()).aufloesen(level);
        StoryFigur f = ModEntities.STORY_FIGUR.get().create(level);
        if (f == null) throw new IllegalStateException("Story-Figur konnte nicht erzeugt werden");
        f.einrichten(s, def, pos);
        level.addFreshEntity(f);
        weltStand(level.getServer(), s).figuren.put(def.id(), f.getUUID());
        dirty(level.getServer());
        return f;
    }

    public static void figurEntfernen(ServerLevel level, Storyline s, String figur) {
        UUID uuid = weltStand(level.getServer(), s).figuren.remove(figur);
        if (uuid != null && level.getEntity(uuid) instanceof StoryFigur f) f.discard();
        dirty(level.getServer());
    }

    /** Für die Figur selbst: Ist sie noch die, die der Welt-Stand kennt? Sonst räumt sie sich weg. */
    public static boolean gueltig(StoryFigur f) {
        if (!(f.level() instanceof ServerLevel level)) return true;
        ResourceLocation id = ResourceLocation.tryParse(f.storyline());
        Storyline s = id == null ? null : StoryLoader.get(id);
        if (s == null) return true;   // Storyline (noch) nicht geladen: nichts entscheiden
        return f.getUUID().equals(weltStand(level.getServer(), s).figuren.get(f.figur()));
    }

    // ================================================================== Blöcke

    private static void bloeckeSetzen(ServerLevel level, Aktion.Bloecke b) {
        // Erst alle Ziele bestimmen, dann setzen: sonst verschiebt ein gesetzter Block den „Boden“ für den nächsten
        List<BlockPos> ziele = new ArrayList<>();
        List<BlockState> zustaende = new ArrayList<>();
        for (Storyline.BlockSetzen e : b.bloecke()) {
            Block block = BuiltInRegistries.BLOCK.get(e.block());
            if (block == Blocks.AIR && !e.block().getPath().equals("air")) {
                throw new IllegalArgumentException("unbekannter Block " + e.block());
            }
            BlockState state = block.defaultBlockState();
            if (e.von() != null && e.bis() != null) {
                for (BlockPos pos : BlockPos.betweenClosed(e.von(), e.bis())) {
                    ziele.add(pos.immutable());
                    zustaende.add(state);
                }
            } else if (e.pos() != null) {
                BlockPos pos = e.pos().y() != null ? e.pos().aufloesen(level)
                        : bodenUnterBauten(level, e.pos().x(), e.pos().z()).above(e.ueberBoden());
                ziele.add(pos);
                zustaende.add(state);
            }
        }
        for (int i = 0; i < ziele.size(); i++) level.setBlock(ziele.get(i), zustaende.get(i), Block.UPDATE_ALL);
    }

    /** Erster freier Block über dem Boden (vor dem Setzen bestimmt). */
    private static BlockPos bodenUnterBauten(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    // ================================================================== Ziel-Anzeige

    /** Das aktive Ziel: der Zieltext des aktuellen Schritts der ersten offenen Storyline in dieser Welt. */
    public static void zielAktualisieren(ServerPlayer p) {
        UniverseId id = welt(p);
        if (id == null) {
            zielSenden(p, "", "");
            return;
        }
        for (Storyline s : StoryLoader.fuer(id.type())) {
            Schritt schritt = aktuell(s, stand(p, s));
            if (schritt != null && !schritt.ziel().isEmpty()) {
                zielSenden(p, s.titel(), schritt.ziel());
                return;
            }
        }
        zielSenden(p, "", "");
    }

    private static void zielSenden(ServerPlayer p, String storyline, String ziel) {
        String key = storyline + "\n" + ziel;
        if (key.equals(LETZTES_ZIEL.get(p.getUUID()))) return;
        LETZTES_ZIEL.put(p.getUUID(), key);
        StoryNetz.senden(p, new StoryNetz.Ziel(storyline, ziel));
    }

    // ================================================================== Rift-Tagebuch

    private static void tagebuchGeben(ServerPlayer p) {
        if (Boolean.TRUE.equals(p.getData(ModAttachments.TAGEBUCH_ERHALTEN))) return;
        p.setData(ModAttachments.TAGEBUCH_ERHALTEN, true);
        ItemStack buch = new ItemStack(ModItems.RIFT_TAGEBUCH.get());
        if (!p.getInventory().add(buch)) p.drop(buch, false);
        p.sendSystemMessage(Component.translatable("message.riftverse.story.tagebuch").withStyle(ChatFormatting.GRAY));
    }

    /** Seiten des Rift-Tagebuchs; alle gezeigten Notizen gelten danach als gelesen. */
    public static List<Component> tagebuchSeiten(ServerPlayer p) {
        List<Component> seiten = new ArrayList<>();
        for (Storyline s : StoryLoader.alle()) {
            StoryStand welt = weltStand(p.server, s), eigen = spielerStand(p.server, s, p.getUUID());
            StoryStand st = stand(p, s);
            Set<String> notizen = new LinkedHashSet<>(welt.notizen);
            notizen.addAll(eigen.notizen);
            if (!st.begonnen() && notizen.isEmpty()) continue;

            MutableComponent kopf = Component.literal(s.titel()).withStyle(ChatFormatting.BOLD).append("\n\n");
            Schritt schritt = aktuell(s, st);
            if (st.fertig) {
                kopf.append(Component.translatable("tagebuch.riftverse.abgeschlossen").withStyle(ChatFormatting.DARK_GREEN));
            } else if (schritt != null && !schritt.ziel().isEmpty()) {
                kopf.append(Component.translatable("tagebuch.riftverse.ziel").withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(schritt.ziel()).withStyle(ChatFormatting.RESET));
            }
            if (!s.beschreibung().isEmpty()) kopf.append("\n\n").append(Component.literal(s.beschreibung()).withStyle(ChatFormatting.DARK_GRAY));
            seiten.add(kopf);

            Map<String, Aktion.Notiz> defs = s.notizen();
            for (String nid : notizen) {
                Aktion.Notiz n = defs.get(nid);
                if (n == null) continue;
                List<String> teile = umbrechen(n.text(), 200);
                for (int i = 0; i < teile.size(); i++) {
                    MutableComponent seite = Component.empty();
                    if (i == 0) seite.append(Component.literal(n.titel()).withStyle(ChatFormatting.BOLD)).append("\n\n");
                    seite.append(Component.literal(teile.get(i)));
                    seiten.add(seite);
                }
                eigen.gelesen.add(nid);
            }
        }
        if (seiten.isEmpty()) seiten.add(Component.translatable("tagebuch.riftverse.leer"));
        dirty(p.server);
        return seiten.size() > 100 ? seiten.subList(0, 100) : seiten;
    }

    public static void tagebuchOeffnen(ServerPlayer p) {
        StoryNetz.senden(p, new StoryNetz.Tagebuch(tagebuchSeiten(p)));
        pruefen(p);
    }

    /** Teilt Text in Seiten von höchstens {@code max} Zeichen, an Wortgrenzen. */
    static List<String> umbrechen(String text, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        for (String wort : text.split(" ")) {
            if (b.length() + wort.length() + 1 > max && !b.isEmpty()) {
                out.add(b.toString());
                b.setLength(0);
            }
            if (!b.isEmpty()) b.append(' ');
            b.append(wort);
        }
        if (!b.isEmpty()) out.add(b.toString());
        return out;
    }

    // ================================================================== Befehle

    /** Storyline komplett zurücksetzen: Welt- und Spieler-Stände, Figuren weg, laufende Dialoge beendet. */
    public static void zuruecksetzen(MinecraftServer server, Storyline s) {
        ServerLevel level = level(server, s);
        StoryStand w = weltStand(server, s);
        if (level != null) {
            for (String figur : new ArrayList<>(w.figuren.keySet())) figurEntfernen(level, s, figur);
        }
        MultiverseData.Record rec = record(server, s);
        rec.story.welt.remove(s.id().toString());
        for (Map<String, StoryStand> m : rec.story.spieler.values()) m.remove(s.id().toString());
        for (Map.Entry<UUID, Sitzung> e : new ArrayList<>(SITZUNGEN.entrySet())) {
            if (e.getValue().story == s) {
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null) abbrechen(p);
                else SITZUNGEN.remove(e.getKey());
            }
        }
        LETZTES_ZIEL.clear();
        dirty(server);
    }

    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }
}
