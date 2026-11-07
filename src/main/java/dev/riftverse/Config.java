package dev.riftverse;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Riftverse-Einstellungen (config/riftverse-common.toml).
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    /** Was bei Überspannung passiert. */
    public static final ModConfigSpec.EnumValue<OvervoltageMode> OVERVOLTAGE;
    /** Blanke Kabel unter Strom verpassen einen Schlag. */
    public static final ModConfigSpec.BooleanValue CABLE_SHOCK;
    /** Zinnerz in der Oberwelt erzeugen (im Pack ggf. aus, wenn ein anderer Mod Zinn liefert). */
    public static final ModConfigSpec.BooleanValue TIN_ORE;

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
        b.pop();

        b.push("welt");
        TIN_ORE = b
                .comment("Zinnerz in der Oberwelt erzeugen. Wirkt beim nächsten Start der Welt.")
                .define("tin_ore", true);
        b.pop();

        SPEC = b.build();
    }

    /** Für datengetriebene Schalter (z. B. Biome-Modifier mit "config": "tin_ore"). */
    public static boolean isEnabled(String option) {
        return switch (option) {
            case "tin_ore" -> TIN_ORE.get();
            default -> true;
        };
    }

    public enum OvervoltageMode {
        EXPLODE, BURNOUT, OFF
    }

    private Config() {}
}
