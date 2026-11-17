package dev.riftverse;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Riftverse-Einstellungen (config/riftverse-common.toml).
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    /** Was bei Überspannung passiert. */
    public static final ModConfigSpec.EnumValue<OvervoltageMode> OVERVOLTAGE;
    /** Blanke Kabel unter Strom verpassen einen Schlag. */
    public static final ModConfigSpec.BooleanValue CABLE_SHOCK;
    /** Faktor auf den Kabelverlust aller Leitungen (0 = verlustfrei). */
    public static final ModConfigSpec.DoubleValue CABLE_LOSS;
    /** Faktor auf die Menge an Zinnerz-Adern. */
    public static final ModConfigSpec.DoubleValue TIN_DENSITY;
    /** Faktor auf die Menge an Xenit-Adern in der Krater-Zone. */
    public static final ModConfigSpec.DoubleValue XENIT_DENSITY;
    /** Faktor auf die Menge an Fluxkristall-Adern. */
    public static final ModConfigSpec.DoubleValue FLUX_DENSITY;
    /** Zinnerz in der Oberwelt erzeugen (im Pack ggf. aus, wenn ein anderer Mod Zinn liefert). */
    public static final ModConfigSpec.BooleanValue TIN_ORE;
    /** Unbebaute, lange nicht besuchte Universen beim Serverstart zurücksetzen. */
    public static final ModConfigSpec.BooleanValue CLEANUP;
    /** So viele Ingame-Tage ohne Besuch, dann wird zurückgesetzt. */
    public static final ModConfigSpec.IntValue CLEANUP_DAYS;
    /** Ab so vielen dauerhaft platzierten Blöcken gilt ein Universum als bebaut. */
    public static final ModConfigSpec.IntValue BUILD_THRESHOLD;
    /** Gewichte der Universumstypen bei Zufallssprüngen überschreiben ("schrottwelt=8"). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GEWICHTE;
    /** Die ersten drei Mk-I-Sprünge führen in Schrott-, Kristall- und Toxische Welt. */
    public static final ModConfigSpec.BooleanValue ANFAENGERSCHUTZ;
    /** Testwelten (Prüfstein) auch im normalen Spiel, mit /riftverse testwelt erreichbar. */
    public static final ModConfigSpec.BooleanValue TESTWELTEN;
    /** Prototyp: Boss kurz nach der Ankunft starten (bis zum Story-Modus aus). */
    public static final ModConfigSpec.BooleanValue BOSS_ON_ARRIVAL;
    /** Neue Spieler bekommen beim ersten Betreten das Riftverse-Handbuch. */
    public static final ModConfigSpec.BooleanValue GUIDE_ON_JOIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("energie");
        OVERVOLTAGE = b
                .comment("Überspannung: Ein Paket ist größer, als Maschine oder Leitung vertragen.",
                        "explode = Maschine explodiert sofort wie in IndustrialCraft, Leitung schmilzt",
                        "burnout = Maschine brennt ohne Explosion durch, Leitung schmilzt",
                        "off     = keine Folgen")
                .defineEnum("overvoltage", OvervoltageMode.EXPLODE);
        CABLE_SHOCK = b
                .comment("Blanke Kabel unter Strom geben bei Berührung einen Schlag.")
                .define("cable_shock", true);
        CABLE_LOSS = b
                .comment("Faktor auf den Kabelverlust (Kupfer 0,5 · Flux 1 · Gravitit 2 · Null 0,05 FE pro Block).",
                        "0 = verlustfrei, 2 = doppelter Verlust. Universums-Modifikatoren (supraleitend, hochohmig) kommen dazu.")
                .defineInRange("kabelverlust", 1.0, 0.0, 10.0);
        b.pop();

        b.push("welt");
        TIN_ORE = b
                .comment("Zinnerz in der Oberwelt erzeugen. Wirkt beim nächsten Start der Welt.")
                .define("tin_ore", true);
        TIN_DENSITY = b
                .comment("Faktor auf die Zahl der Zinnerz-Adern (1 = 14 Adern à bis zu 9 Erz pro Chunk wie bei Mekanism).",
                        "0 schaltet Zinnerz ebenfalls ab. Wirkt nur auf neu erzeugte Chunks.")
                .defineInRange("zinn_dichte", 1.0, 0.0, 8.0);
        XENIT_DENSITY = b
                .comment("Faktor auf die Xenit-Adern rund um die Meteoritenkrater (der Krater selbst bleibt gleich).",
                        "Wirkt nur auf neu erzeugte Chunks.")
                .defineInRange("xenit_dichte", 1.0, 0.0, 8.0);
        FLUX_DENSITY = b
                .comment("Faktor auf die Fluxkristall-Adern unter Y 0. Wirkt nur auf neu erzeugte Chunks.")
                .defineInRange("flux_dichte", 1.0, 0.0, 8.0);
        b.pop();

        b.push("universen");
        CLEANUP = b
                .comment("Universen ohne Bauten und ohne Rift-Anker zurücksetzen, die lange nicht besucht wurden.",
                        "Passiert nur beim Serverstart; Typ, Seed, Modifikatoren und Story-Daten bleiben erhalten.")
                .define("aufraeumen", true);
        CLEANUP_DAYS = b
                .comment("Ingame-Tage ohne Besuch bis zum Zurücksetzen.")
                .defineInRange("tage", 10, 1, 1000);
        BUILD_THRESHOLD = b
                .comment("Mehr als so viele dauerhaft platzierte Blöcke (ohne Fackeln und Gerüste) gelten als Bau.",
                        "Eine Block-Entity (Truhe, Maschine, Bett) zählt immer als Bau.")
                .defineInRange("bloecke", 64, 0, 100000);
        GEWICHTE = b
                .comment("Gewichte der Universumstypen bei Zufallssprüngen, z. B. [\"schrottwelt=8\", \"leere_welt=1\"].",
                        "Die Chance auf einen Typ ist sein Gewicht geteilt durch die Summe aller Typen, die die Kanone erreicht.",
                        "0 = nie ein Zufallsziel. Leer = Standardwerte: schrottwelt, kristallwelt, toxische_welt je 8;",
                        "niedriggravitation, zeitdilatation, organische_welt je 6; hochgravitation, spiegelwelt je 5; leere_welt 3.")
                .defineListAllowEmpty("gewichte", List.of(), () -> "schrottwelt=8",
                        o -> o instanceof String s && s.matches("[a-z0-9_]+\\s*=\\s*\\d+"));
        ANFAENGERSCHUTZ = b
                .comment("Anfängerschutz: Die ersten drei Sprünge mit der Kanone Mk I führen in die Schrott-, Kristall- und",
                        "Toxische Welt (jede genau einmal, in zufälliger Reihenfolge). Standardmäßig aus: Zufall ist King.")
                .define("anfaengerschutz", false);
        b.pop();

        b.push("story");
        BOSS_ON_ARRIVAL = b
                .comment("Prototyp aus dem ersten Entwurf: Boss 6 Sekunden nach der Ankunft starten.",
                        "Bis zum Story-Modus aus, damit Universen beim Betreten friedlich sind.")
                .define("boss_bei_ankunft", false);
        b.pop();

        b.push("entwicklung");
        TESTWELTEN = b
                .comment("Testwelten wie den Prüfstein (mit der Beispiel-Storyline) auch im normalen Spiel laden.",
                        "Erreichbar mit /riftverse testwelt. Wirkt beim nächsten Laden der Welt.")
                .define("testwelten", false);
        b.pop();

        b.push("handbuch");
        GUIDE_ON_JOIN = b
                .comment("Neue Spieler bekommen beim ersten Betreten das Riftverse-Handbuch ins Inventar.")
                .define("beim_start", true);
        b.pop();

        SPEC = b.build();
    }

    /** Für datengetriebene Schalter (z. B. Biome-Modifier mit "config": "tin_ore"). */
    public static boolean isEnabled(String option) {
        return switch (option) {
            case "tin_ore" -> TIN_ORE.get() && TIN_DENSITY.get() > 0;
            default -> true;
        };
    }

    private static List<? extends String> gewichteQuelle;
    private static Map<String, Integer> gewichteCache = Map.of();

    /** Gewicht aus der Config für einen Universumstyp, oder null für den Standardwert. */
    @Nullable
    public static Integer gewichtFuer(String typ) {
        List<? extends String> liste;
        try {
            liste = GEWICHTE.get();
        } catch (IllegalStateException e) {
            return null;   // Config noch nicht geladen
        }
        if (liste != gewichteQuelle) {
            Map<String, Integer> neu = new HashMap<>();
            for (String eintrag : liste) {
                int gleich = eintrag.indexOf('=');
                if (gleich < 0) continue;
                try {
                    neu.put(eintrag.substring(0, gleich).trim(), Integer.parseInt(eintrag.substring(gleich + 1).trim()));
                } catch (NumberFormatException ignored) {
                    // vom Validator schon ausgeschlossen
                }
            }
            gewichteCache = neu;
            gewichteQuelle = liste;
        }
        return gewichteCache.get(typ);
    }

    /** Mengenfaktor für den Platzierungs-Modifier {@code riftverse:config_menge}. */
    public static double density(String option) {
        return switch (option) {
            case "zinn_dichte" -> TIN_DENSITY.get();
            case "xenit_dichte" -> XENIT_DENSITY.get();
            case "flux_dichte" -> FLUX_DENSITY.get();
            default -> 1.0;
        };
    }

    public enum OvervoltageMode {
        EXPLODE, BURNOUT, OFF
    }

    private Config() {}
}
