package dev.riftverse.devtest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModItems;
import dev.riftverse.storyline.StoryFigur;
import dev.riftverse.storyline.StoryLoader;
import dev.riftverse.storyline.StoryManager;
import dev.riftverse.storyline.StoryParser;
import dev.riftverse.storyline.StoryStand;
import dev.riftverse.storyline.Storyline;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für das Story-Gerüst (Meilenstein 9 der neuen Universen): Die Beispiel-Storyline „Der verschwundene
 * Kartograf“ wird von einem Testspieler einmal komplett durchgespielt – Ankunft, Notiz lesen, Lager finden, Dialog
 * mit Antwort, Weggehen bricht ab, Karte abgeben, Turm in der Welt, Abschluss.
 */
final class RauchTestStory {
    private static final UUID ERZAEHLER = UUID.fromString("5a1e0000-0000-4000-8000-0000000057a9");

    private RauchTestStory() {
    }

    static void aufbauen(MinecraftServer server) {
        // --- Laden und Parser -------------------------------------------------------------------------
        Storyline s = StoryLoader.get(Riftverse.id("test_einzigartig/kartograf"));
        check(s != null && StoryLoader.fehler().isEmpty(), "Beispiel-Storyline geladen, keine kaputten Dateien " + StoryLoader.fehler());
        if (s == null) return;
        check(s.schritte().size() == 5 && s.figuren().containsKey("mira") && s.welt() == UniverseType.TEST_EINZIGARTIG,
                "Kartograf: 5 Schritte, Figur Mira, spielt im Prüfstein");
        check(fehlerMeldung("{\"welt\":\"test_einzigartig\",\"titel\":\"x\",\"schritte\":[{\"ausloeser\":{\"typ\":\"ankunft\"},"
                        + "\"aktionen\":[{\"typ\":\"notz\"}]}]}").contains("unbekannter Typ"),
                "Parser meldet unbekannte Aktionen mit Ort in der Datei");
        check(fehlerMeldung("{\"welt\":\"schrottwelt\",\"titel\":\"x\",\"schritte\":[{\"ausloeser\":{\"typ\":\"ankunft\"}}]}")
                .contains("nicht einzigartig"), "Parser lehnt Storylines in normalen Welten ab");
        Storyline eigen = StoryParser.lesen(Riftverse.id("test/eigen"), json("{\"welt\":\"test_einzigartig\",\"titel\":\"x\","
                + "\"geltung\":\"spieler\",\"schritte\":[{\"ausloeser\":{\"typ\":\"ankunft\"}}]}"));
        check(eigen.geltung() == Storyline.Geltung.SPIELER, "Geltung „spieler“ wird gelesen");

        ServerLevel welt = server.getLevel(UniverseType.TEST_EINZIGARTIG.dimension());
        if (welt == null) return;
        UniverseId id = Einzigartig.id(UniverseType.TEST_EINZIGARTIG);
        StoryManager.zuruecksetzen(server, s);
        FakePlayer p = FakePlayerFactory.get(welt, new GameProfile(ERZAEHLER, "Erzaehler"));
        stelle(p, welt, 2072, 2032);
        check(StoryManager.stand(p, eigen) != StoryManager.stand(p, s), "Spieler- und Welt-Stand sind getrennt");

        // --- 1. Ankunft: Brief, Mira, Lager -------------------------------------------------------------
        StoryManager.ankunft(p, id);
        StoryStand st = StoryManager.weltStand(server, s);
        StoryFigur mira = StoryManager.figur(welt, s, "mira");
        check("brief_lesen".equals(st.schritt) && st.notizen.contains("brief"), "Ankunft: Notiz „brief“, weiter zu brief_lesen (" + st.schritt + ")");
        check(mira != null && StoryManager.gueltig(mira), "Ankunft: Mira steht im Lager");
        check(p.getInventory().countItem(ModItems.RIFT_TAGEBUCH.get()) >= 1, "Rift-Tagebuch beim ersten Story-Ereignis");

        // --- 2. Brief im Tagebuch lesen -----------------------------------------------------------------
        check(StoryManager.tagebuchSeiten(p).size() >= 2, "Tagebuch zeigt Storyline und Brief");
        StoryManager.pruefen(p);
        check("lager".equals(st.schritt), "Notiz gelesen: weiter zum Lager (" + st.schritt + ")");

        // --- 3. Lager erreichen -------------------------------------------------------------------------
        stelle(p, welt, 2130, 2033);
        StoryManager.pruefen(p);
        check("gespraech".equals(st.schritt), "Lager erreicht: weiter zum Gespräch (" + st.schritt + ")");
        if (mira == null) return;

        // --- 4. Weggehen bricht das Gespräch ab -----------------------------------------------------------
        StoryManager.angesprochen(p, mira);
        check(StoryManager.imDialog(p), "Rechtsklick auf Mira startet den Dialog");
        stelle(p, welt, 2160, 2033);
        StoryManager.sekunde(p);
        check(!StoryManager.imDialog(p) && "gespraech".equals(st.schritt), "Weggehen beendet das Gespräch, der Schritt bleibt");

        // --- 5. Dialog mit Antwort ------------------------------------------------------------------------
        stelle(p, welt, mira.getBlockX() + 2, mira.getBlockZ());
        StoryManager.angesprochen(p, mira);
        StoryManager.angesprochen(p, mira);
        StoryManager.angesprochen(p, mira);
        StoryManager.angesprochen(p, mira);   // dritte Zeile hat Antworten: bleibt stehen
        check(StoryManager.imDialog(p) && "gespraech".equals(st.schritt), "Zeile mit Antworten wartet auf eine Wahl");
        StoryManager.antwort(p, 0);
        check(!StoryManager.imDialog(p) && "karte".equals(st.schritt) && st.flags.contains("hilft")
                && st.flags.contains(StoryManager.ANTWORT_FLAG + "hilft"), "Antwort 1: Flag „hilft“, weiter zur Karte (" + st.schritt + ")");

        // --- 6. Ohne Karte: nur der Standardsatz ------------------------------------------------------------
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        StoryManager.angesprochen(p, mira);
        check(!StoryManager.imDialog(p) && "karte".equals(st.schritt), "Ohne Karte: Mira sagt nur ihren Standardsatz");

        // --- 7. Karte abgeben, Turm, Abschluss ----------------------------------------------------------------
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MAP));
        StoryManager.angesprochen(p, mira);
        check(StoryManager.imDialog(p) && p.getMainHandItem().isEmpty(), "Karte abgegeben, Mira bedankt sich");
        StoryManager.angesprochen(p, mira);
        StoryManager.angesprochen(p, mira);
        check(!StoryManager.imDialog(p) && st.fertig && st.notizen.contains("lagerkarte"), "Dialog zu Ende: Storyline abgeschlossen");
        check(p.getInventory().countItem(Items.COMPASS) >= 1, "Belohnung: Eldrics Kompass");
        boolean turm = false;
        for (int y = welt.getMinBuildHeight(); y < welt.getMaxBuildHeight() && !turm; y++) {
            turm = welt.getBlockState(new BlockPos(2127, y, 2036)).is(Blocks.SEA_LANTERN);
        }
        check(turm, "Die Welt hat sich verändert: Leuchtfeuer steht");
        check(server.getAdvancements().get(Riftverse.id("story/kartograf")) != null, "Fortschritt „Der verschwundene Kartograf“ geladen");

        // --- 8. Stand übersteht das Speichern -------------------------------------------------------------
        MultiverseData daten = MultiverseData.get(server);
        MultiverseData.Record zurueck = MultiverseData.fromTag(daten.save(new CompoundTag(), null)).existing(id);
        StoryStand geladen = zurueck == null ? null : zurueck.story.welt.get(s.id().toString());
        check(geladen != null && geladen.fertig && geladen.flags.contains("hilft") && geladen.notizen.size() == 2
                && geladen.figuren.containsKey("mira"), "Story-Stand wird gespeichert und geladen");

        // --- Befehle ----------------------------------------------------------------------------------------
        var root = server.getCommands().getDispatcher().getRoot().getChild("riftverse");
        check(root != null && root.getChild("story") != null && root.getChild("testwelt") != null
                && root.getChild("einzigartig") != null, "Befehle /riftverse story, testwelt und einzigartig registriert");
    }

    private static void stelle(FakePlayer p, ServerLevel welt, int x, int z) {
        welt.getChunk(x >> 4, z >> 4);
        int y = welt.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.moveTo(x + 0.5, y, z + 0.5, 0f, 0f);
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String fehlerMeldung(String text) {
        try {
            StoryParser.lesen(Riftverse.id("test/kaputt"), json(text));
            return "";
        } catch (IllegalArgumentException e) {
            return String.valueOf(e.getMessage());
        }
    }
}
