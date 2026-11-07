package dev.riftverse.fluid;

import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Roh-Portalfluid ist instabil und zerfällt 10 Minuten nach seiner Herstellung. Gespeichert wird der
 * Zeitpunkt des Zerfalls (Spielzeit der Overworld); 0 heißt stabil (nur Kreativmodus).
 * Mischt sich frisches mit älterem Fluid, gilt der frühere Zeitpunkt: Die Kette muss durchlaufen statt zu lagern.
 */
public final class Zerfall {
    /** 10 Minuten in Ticks. */
    public static final long DAUER = 12_000;

    /** Spielzeit, mit der überall gerechnet wird (alle Dimensionen teilen sich die Overworld-Zeit). */
    public static long now(Level level) {
        return level.getServer() != null ? level.getServer().overworld().getGameTime() : level.getGameTime();
    }

    public static long frisch(Level level) {
        return now(level) + DAUER;
    }

    public static boolean zerfallen(long zerfallAt, long now) {
        return zerfallAt > 0 && now >= zerfallAt;
    }

    /** Früherer von zwei Zerfallszeitpunkten; 0 (stabil) zählt nicht. */
    public static long frueher(long a, long b) {
        if (a <= 0) return b;
        if (b <= 0) return a;
        return Math.min(a, b);
    }

    /** "9:42" */
    public static String rest(long zerfallAt, long now) {
        long ticks = Math.max(0, zerfallAt - now);
        long seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private Zerfall() {}
}
