package dev.riftverse.devtest;

import com.mojang.authlib.GameProfile;
import dev.riftverse.Config;
import dev.riftverse.portal.RiftTravel;
import dev.riftverse.registry.ModConditions;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.Anfaengerschutz;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.EinzigartigeAnkunft;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseCleanup;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für Meilenstein 8 der neuen Universen: Gewichte (100.000 simulierte Sprünge), Gewichte aus der Config,
 * einzigartige Universen (feste Zelle, festes Profil, nie aufgeräumt, erste Ankunft am szenischen Punkt, danach
 * zufällig, Begrüßung, Erinnerungskarte, Zurücksetzen auf Anforderung) und den Anfängerschutz.
 */
final class RauchTestEinzigartig {
    private static final int SPRUENGE = 100_000;
    private static final UUID PRUEFER = UUID.fromString("5a1e0000-0000-4000-8000-00000000e1a8");

    private RauchTestEinzigartig() {
    }

    static void aufbauen(MinecraftServer server, ServerLevel overworld) {
        gewichte();
        einzigartig(server);
        anfaengerschutz();
    }

    // ------------------------------------------------------------------ Gewichte

    private static void gewichte() {
        check(UniverseType.SCHROTT.weight() == 8 && UniverseType.KRISTALL.weight() == 8 && UniverseType.TOXISCH.weight() == 8
                        && UniverseType.NIEDRIGGRAVITATION.weight() == 6 && UniverseType.ZEITDILATATION.weight() == 6
                        && UniverseType.ORGANISCH.weight() == 6 && UniverseType.HOCHGRAVITATION.weight() == 5
                        && UniverseType.SPIEGEL.weight() == 5 && UniverseType.LEERE.weight() == 3,
                "Gewichte: T1 je 8, T2 je 6, Hochgravitation und Spiegel je 5, Leere Welt 3");

        for (int stufe = 1; stufe <= 4; stufe++) {
            List<UniverseType> typen = UniverseType.randomWith(stufe);
            Map<UniverseType, Integer> zaehler = new EnumMap<>(UniverseType.class);
            RandomSource r = RandomSource.create(4242L + stufe);
            for (int i = 0; i < SPRUENGE; i++) zaehler.merge(Multiverse.waehle(r, typen), 1, Integer::sum);
            double summe = 0, abweichung = 0;
            for (UniverseType t : typen) {
                double soll = Multiverse.chance(t, typen);
                summe += soll;
                abweichung = Math.max(abweichung, Math.abs(zaehler.getOrDefault(t, 0) / (double) SPRUENGE - soll));
            }
            check(Math.abs(summe - 1) < 1e-9 && abweichung < 0.01, "Mk " + stufe + ": " + SPRUENGE
                    + " Sprünge folgen den Gewichten (größte Abweichung " + String.format("%.4f", abweichung) + ")");
        }
        check(Math.abs(Multiverse.chance(UniverseType.LEERE, UniverseType.randomWith(4)) - 3.0 / 55) < 1e-9,
                "Mk IV: Leere Welt 3 von 55 (5,5 %)");

        // Config überschreibt Gewichte, 0 = nie Zufallsziel
        List<? extends String> vorher = Config.GEWICHTE.get();
        Config.GEWICHTE.set(List.of("schrottwelt=0", "kristallwelt = 2"));
        try {
            check(UniverseType.SCHROTT.weight() == 0 && UniverseType.KRISTALL.weight() == 2 && UniverseType.TOXISCH.weight() == 8,
                    "Config: Gewichte überschrieben (Schrott 0, Kristall 2)");
            RandomSource r = RandomSource.create(7);
            boolean nieSchrott = true;
            for (int i = 0; i < 10_000; i++) nieSchrott &= Multiverse.waehle(r, UniverseType.randomWith(1)) != UniverseType.SCHROTT;
            check(nieSchrott, "Config: Gewicht 0 ist nie ein Zufallsziel");
        } finally {
            Config.GEWICHTE.set(vorher);
        }
        check(UniverseType.SCHROTT.weight() == 8, "Config: zurück auf Standard");
    }

    // ------------------------------------------------------------------ Einzigartige Universen

    private static void einzigartig(MinecraftServer server) {
        UniverseType test = UniverseType.TEST_EINZIGARTIG;
        UniverseId id = Einzigartig.id(test);
        Einzigartig def = Einzigartig.of(test);

        check(ModConditions.entwicklung(), "Rauchtest läuft im Entwicklungsmodus");
        check(test.einzigartig() && test.entwicklung() && def != null, "Testwelt ist einzigartig und nur für Entwickler");
        check(!UniverseType.randomWith(4).contains(test) && !UniverseType.reachableWith(4).contains(test),
                "Testwelt ist kein Zufallsziel und mit keiner Kanone erreichbar");
        boolean immerNull = true;
        RandomSource r = RandomSource.create(99);
        for (int i = 0; i < 100; i++) immerNull &= Multiverse.random(r, List.of(test)).equals(id);
        check(immerNull, "Einzigartig: jeder Sprung führt in die Zelle 0,0");

        UniverseProfile p = UniverseProfile.get(id);
        check(p.modifiers.isEmpty() && def != null && p.terrain == def.terrain() && p.look == def.vorlage()
                        && "Prüfstein".equals(p.title()),
                "Einzigartig: festes Profil (" + p.terrain.label + ", Vorlage " + p.look.id() + ", keine Modifikatoren)");
        check(id.label().getString().length() > 0 && !id.label().getString().startsWith("Universum "),
                "Einzigartig: Name statt Code");

        // Aufräumen: nie, außer auf Anforderung
        MultiverseData.Record alt = new MultiverseData.Record();
        alt.lastVisit = 0;
        long lange = UniverseCleanup.maxAgeTicks() * 5;
        check(!UniverseCleanup.shouldReset(id, alt, lange, UniverseCleanup.maxAgeTicks(), 64)
                        && UniverseCleanup.shouldReset(new UniverseId(UniverseType.KRISTALL, 4, 4), alt, lange, UniverseCleanup.maxAgeTicks(), 64),
                "Einzigartig: wird nie aufgeräumt, ein normales Universum schon");

        // Speichern: Ankünfte und Reset-Anforderung überstehen den Neustart
        MultiverseData daten = new MultiverseData();
        MultiverseData.Record rec = daten.record(id);
        rec.angekommen.add(PRUEFER);
        rec.resetAngefordert = true;
        rec.visits = 3;
        MultiverseData geladen = MultiverseData.fromTag(daten.save(new CompoundTag(), null));
        MultiverseData.Record zurueck = geladen.existing(id);
        check(zurueck != null && zurueck.angekommen.contains(PRUEFER) && zurueck.resetAngefordert && zurueck.visits == 3,
                "Einzigartig: Ankünfte und Reset-Anforderung werden gespeichert");
        if (zurueck != null) {
            zurueck.komplettReset();
            check(zurueck.angekommen.isEmpty() && !zurueck.resetAngefordert && zurueck.visits == 0
                    && zurueck.state == MultiverseData.STATE_NONE, "Einzigartig: Zurücksetzen beginnt alles von vorn");
        }

        // Landen und Begrüßung in der Testwelt
        ServerLevel welt = server.getLevel(test.dimension());
        check(welt != null, "Testwelt test_einzigartig ist im Entwicklungsmodus geladen");
        if (welt == null || def == null) return;
        FakePlayer spieler = FakePlayerFactory.get(welt, new GameProfile(PRUEFER, "Pruefer"));
        MultiverseData echt = MultiverseData.get(server);
        echt.record(id).angekommen.remove(PRUEFER);

        BlockPos erste = RiftTravel.landeplatzEinzigartig(welt, id, spieler);
        BlockPos ziel = def.ankunft();
        int abstand = Math.max(Math.abs(erste.getX() - ziel.getX()), Math.abs(erste.getZ() - ziel.getZ()));
        check(abstand <= 34 && RiftTravel.isSafe(welt, erste), "Erste Ankunft am szenischen Punkt ("
                + erste.toShortString() + ", " + abstand + " Blöcke daneben)");

        int karten = karten(spieler);
        try {
            EinzigartigeAnkunft.ankommen(spieler, id);
        } catch (RuntimeException e) {
            check(false, "Begrüßung beim ersten Besuch läuft ohne Fehler: " + e);
        }
        check(echt.record(id).angekommen.contains(PRUEFER), "Erster Besuch wird pro Spieler gemerkt");
        check(karten(spieler) == karten + 1, "Erinnerungskarte beim ersten Besuch");
        try {
            EinzigartigeAnkunft.ankommen(spieler, id);
        } catch (RuntimeException e) {
            check(false, "Begrüßung bei der Rückkehr läuft ohne Fehler: " + e);
        }
        check(karten(spieler) == karten + 1, "Bei der Rückkehr keine zweite Karte");

        BlockPos spaeter = RiftTravel.landeplatzEinzigartig(welt, id, spieler);
        check(RiftTravel.isSafe(welt, spaeter) && id.equals(Multiverse.at(welt, spaeter)),
                "Spätere Ankunft an einer zufälligen sicheren Stelle der Welt (" + spaeter.toShortString() + ")");
        check(server.getAdvancements().get(EinzigartigeAnkunft.FORTSCHRITT) != null, "Fortschritt „Etwas zieht dich“ geladen");
    }

    private static int karten(FakePlayer spieler) {
        int n = 0;
        for (int i = 0; i < spieler.getInventory().getContainerSize(); i++) {
            ItemStack s = spieler.getInventory().getItem(i);
            if (s.is(ModItems.KOORDINATENKARTE.get()) && Einzigartig.id(UniverseType.TEST_EINZIGARTIG).equals(s.get(ModDataComponents.ZIEL.get()))) {
                n += s.getCount();
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ Anfängerschutz

    private static void anfaengerschutz() {
        check(!Config.ANFAENGERSCHUTZ.get(), "Anfängerschutz ist standardmäßig aus");
        RandomSource r = RandomSource.create(3);
        int maske = 0;
        boolean alleVerschieden = true;
        for (int i = 0; i < 3; i++) {
            UniverseType t = Anfaengerschutz.waehle(maske, r);
            alleVerschieden &= t != null && (maske & Anfaengerschutz.bit(t)) == 0;
            if (t != null) maske |= Anfaengerschutz.bit(t);
        }
        check(alleVerschieden && Anfaengerschutz.waehle(maske, r) == null,
                "Anfängerschutz: Schrott-, Kristall- und Toxische Welt je einmal, danach Zufall");
    }

    static void ergebnis(MinecraftServer server, ServerLevel overworld) {
        MultiverseData.Record rec = MultiverseData.get(server).existing(Einzigartig.id(UniverseType.TEST_EINZIGARTIG));
        check(rec != null && rec.angekommen.contains(PRUEFER), "Testwelt behält ihren Stand bis zum Testende");
    }
}
