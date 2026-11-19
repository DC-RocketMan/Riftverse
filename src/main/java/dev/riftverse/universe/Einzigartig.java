package dev.riftverse.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Einzigartige Universen (Meilenstein 8): Von diesen Typen gibt es genau ein Universum, die Zelle 0,0. Es wird nie
 * aufgeräumt, würfelt keine Modifikatoren und behält seinen Stand. Landet ein Spieler zum ersten Mal dort, kommt er
 * am szenischen Ankunftspunkt an; danach an einer zufälligen sicheren Stelle.
 * <p>
 * Alle Texte stehen in der Sprachdatei unter {@code einzigartig.riftverse.<typ>.*}, damit sie sich ohne Code
 * umschreiben lassen:
 * <ul>
 *     <li>{@code .erster_besuch} – Chat-Text beim ersten Besuch</li>
 *     <li>{@code .rueckkehr.1} bis {@code .rueckkehr.4} – Untertitel bei der Rückkehr, sie wechseln sich ab</li>
 * </ul>
 *
 * @param type      der Universumstyp
 * @param vorlage   welcher bestehende Typ Gelände, Deko und Erze liefert, bis die Welt ein eigenes Aussehen hat
 * @param terrain   festes Gelände statt gewürfelt
 * @param palette   Index der festen Gesteins-Palette der Vorlage
 * @param name      Name im Code (Chroniken, Logs); angezeigt wird {@code universe.riftverse.<typ>} aus der Sprachdatei
 * @param ankunftDx Versatz des Ankunftspunkts zur Zellmitte (x)
 * @param ankunftDz Versatz des Ankunftspunkts zur Zellmitte (z)
 */
public record Einzigartig(UniverseType type, UniverseType vorlage, UniverseProfile.Terrain terrain, int palette,
                          String name, int ankunftDx, int ankunftDz) {

    /** So viele Rückkehr-Sätze gibt es pro Welt in der Sprachdatei. */
    public static final int RUECKKEHR_SAETZE = 4;
    /** Spätere Ankünfte landen höchstens so weit von der Zellmitte entfernt (mit Abstand zum Rand). */
    public static final int STREUUNG = Multiverse.CELL / 2 - Multiverse.BORDER - 400;

    private static final Map<UniverseType, Einzigartig> ALLE = Map.of(
            UniverseType.TEST_EINZIGARTIG, new Einzigartig(UniverseType.TEST_EINZIGARTIG, UniverseType.ZEITDILATATION,
                    UniverseProfile.Terrain.TERRACED, 1, "Prüfstein", 24, -16),
            // Ankunft auf dem Aussichtshügel nordwestlich des Hauptdorfs (siehe DorfPlan.HUEGEL_DX/DZ)
            UniverseType.MAGISCHES_DORF, new Einzigartig(UniverseType.MAGISCHES_DORF, UniverseType.MAGISCHES_DORF,
                    UniverseProfile.Terrain.DORFLAND, 0, "Bridge's Inn", -150, -110),
            // Meilenstein 11: Dichte-Testwelt, Ankunft zwischen dem schwebenden Testberg (Osten) und dem Bogen (Westen)
            UniverseType.TEST_DICHTE, new Einzigartig(UniverseType.TEST_DICHTE, UniverseType.TEST_DICHTE,
                    UniverseProfile.Terrain.DICHTETEST, 0, "Dichteprobe", 0, -30));

    @Nullable
    public static Einzigartig of(UniverseType type) {
        return type.einzigartig() ? ALLE.get(type) : null;
    }

    /** Das eine Universum dieses Typs. */
    public static UniverseId id(UniverseType type) {
        return new UniverseId(type, 0, 0);
    }

    /** Ist das ein einzigartiges Universum? (Andere Zellen eines einzigartigen Typs werden nie angesteuert.) */
    public static boolean ist(@Nullable UniverseId id) {
        return id != null && id.type().einzigartig();
    }

    /** Der szenische Ankunftspunkt für den ersten Besuch (Höhe wird beim Landen gesucht). */
    public BlockPos ankunft() {
        int c = Multiverse.cellCenter(0);
        return new BlockPos(c + ankunftDx, 100, c + ankunftDz);
    }

    /** Ein zufälliger Landepunkt für spätere Besuche, irgendwo in der Welt. */
    public static BlockPos zufaelligeAnkunft(UniverseId id, RandomSource random) {
        int x = Multiverse.cellCenter(id.cellX()) + random.nextInt(STREUUNG * 2 + 1) - STREUUNG;
        int z = Multiverse.cellCenter(id.cellZ()) + random.nextInt(STREUUNG * 2 + 1) - STREUUNG;
        return new BlockPos(x, 100, z);
    }

    // ------------------------------------------------------------------ Texte

    public static Component anzeigeName(UniverseType type) {
        return type.displayName();
    }

    public static Component ersterBesuch(UniverseType type) {
        return Component.translatable("einzigartig.riftverse." + type.id() + ".erster_besuch");
    }

    /** Rückkehr-Satz Nummer {@code n} (1 bis {@link #RUECKKEHR_SAETZE}). */
    public static Component rueckkehr(UniverseType type, int n) {
        return Component.translatable("einzigartig.riftverse." + type.id() + ".rueckkehr." + n);
    }
}
