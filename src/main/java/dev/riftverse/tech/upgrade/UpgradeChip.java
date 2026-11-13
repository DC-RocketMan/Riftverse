package dev.riftverse.tech.upgrade;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Maschinen-Upgrades (für alle Verarbeitungsmaschinen ab T1). Jede Sorte passt einmal in eine Maschine.
 * <ul>
 *   <li>Tempo: doppelte Geschwindigkeit, 2,5-facher Verbrauch.</li>
 *   <li>Effizienz: 30 % weniger Verbrauch.</li>
 *   <li>Parallel: zwei Vorgänge gleichzeitig (doppelte Zutaten, doppelter Verbrauch).</li>
 *   <li>Transformator: verträgt eine Spannungsstufe mehr.</li>
 *   <li>KI: Persönlichkeit, +10 % Tempo, gelegentlich 5 Sekunden existenzielle Krise.</li>
 * </ul>
 */
public enum UpgradeChip {
    TEMPO, EFFIZIENZ, PARALLEL, TRANSFORMATOR, KI;

    public String itemName() {
        return name().toLowerCase(Locale.ROOT) + "_chip";
    }

    @Nullable
    public static UpgradeChip byName(String name) {
        for (UpgradeChip c : values()) {
            if (c.name().equals(name)) return c;
        }
        return null;
    }
}
