package dev.riftverse.universe.dichte;

import dev.riftverse.universe.UniverseId;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Regionskarte für Welten mit mehreren Biomen (Meilenstein 11, Entscheidungen M11-10 und M11-11): ein Voronoi-Muster
 * mit verbogenen Rändern. Jede Zelle ist eine Region mit einer Art (0 bis {@code arten - 1}); an der Grenze zu einer
 * anderen Art liegt ein Übergangssaum, in dem gestreut gemischt wird.
 * <p>
 * Alles ist deterministisch aus dem Seed, damit Nachbar-Chunks, {@code getBaseColumn} und die F3-Anzeige dasselbe
 * sehen.
 */
public final class Regionskarte {

    /**
     * Ergebnis einer Abfrage.
     *
     * @param art      Art der Region an dieser Stelle
     * @param nachbar  Art der nächsten anderen Region (gleich {@code art}, wenn ringsum alles dieselbe Art ist)
     * @param abstand  Abstand zur Grenze in Blöcken
     * @param saum     Gewicht der Nachbarregion: 0 außerhalb des Saums, 0,5 genau auf der Grenze
     */
    public record Region(int art, int nachbar, double abstand, double saum) {
        /** Gestreute Mischung: Mit der Wahrscheinlichkeit {@link #saum} gilt an dieser Stelle die Nachbarregion. */
        public int gestreut(int x, int z, long seed) {
            if (saum <= 0 || nachbar == art) return art;
            long h = UniverseId.mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL));
            return (h & 0xFFFF) / 65536.0 < saum ? nachbar : art;
        }
    }

    private final long seed;
    private final int groesse, arten;
    private final double saumBreite, verbiegung;
    private final SimplexNoise warpX, warpZ;

    /**
     * @param groesse    mittlere Größe einer Region in Blöcken
     * @param arten      wie viele verschiedene Regionsarten es gibt
     * @param saumBreite Breite des Übergangssaums in Blöcken (16 nach M11-11)
     */
    public Regionskarte(long seed, int groesse, int arten, int saumBreite) {
        this.seed = seed;
        this.groesse = Math.max(16, groesse);
        this.arten = Math.max(1, arten);
        this.saumBreite = Math.max(1, saumBreite);
        this.verbiegung = this.groesse * 0.3;
        RandomSource r = RandomSource.create(seed ^ 0x524547494F4EL);
        this.warpX = new SimplexNoise(r);
        this.warpZ = new SimplexNoise(r);
    }

    public int groesse() {
        return groesse;
    }

    public int arten() {
        return arten;
    }

    public Region bei(int x, int z) {
        // Ränder verbiegen: zwei Oktaven Rauschen verschieben die Abfragestelle
        double f = 1.0 / (groesse * 0.8);
        double wx = x + verbiegung * (warpX.getValue(x * f, z * f) + 0.4 * warpX.getValue(x * f * 3.1, z * f * 3.1));
        double wz = z + verbiegung * (warpZ.getValue(x * f, z * f) + 0.4 * warpZ.getValue(x * f * 3.1, z * f * 3.1));

        int gx = (int) Math.floor(wx / groesse), gz = (int) Math.floor(wz / groesse);
        double d1 = Double.MAX_VALUE, p1x = 0, p1z = 0;
        long z1 = 0;
        double[] px = new double[25], pz = new double[25];
        long[] zelle = new long[25];
        int n = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                long cx = gx + dx, cz = gz + dz;
                long h = hash(cx, cz);
                double qx = (cx + 0.1 + 0.8 * ((h & 0xFFFF) / 65535.0)) * groesse;
                double qz = (cz + 0.1 + 0.8 * (((h >>> 16) & 0xFFFF) / 65535.0)) * groesse;
                px[n] = qx;
                pz[n] = qz;
                zelle[n] = h;
                n++;
                double d = (wx - qx) * (wx - qx) + (wz - qz) * (wz - qz);
                if (d < d1) {
                    d1 = d;
                    p1x = qx;
                    p1z = qz;
                    z1 = h;
                }
            }
        }
        int art = art(z1);
        // Abstand zur nächsten Grenze mit einer anderen Art: Abstand zur Mittelsenkrechten
        double best = Double.MAX_VALUE;
        int nachbar = art;
        double gleich = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (px[i] == p1x && pz[i] == p1z) continue;
            double ex = px[i] - p1x, ez = pz[i] - p1z;
            double len = Math.sqrt(ex * ex + ez * ez);
            if (len < 1e-6) continue;
            double di = (wx - px[i]) * (wx - px[i]) + (wz - pz[i]) * (wz - pz[i]);
            double grenze = (di - d1) / (2 * len);
            int a = art(zelle[i]);
            if (a == art) {
                gleich = Math.min(gleich, grenze);
                continue;
            }
            if (grenze < best) {
                best = grenze;
                nachbar = a;
            }
        }
        double abstand = nachbar == art ? gleich : best;
        double halb = saumBreite / 2.0;
        double saum = nachbar == art || abstand >= halb ? 0 : 0.5 * (1.0 - abstand / halb);
        return new Region(art, nachbar, abstand, saum);
    }

    private long hash(long cx, long cz) {
        return UniverseId.mix(seed ^ (cx * 0x632BE59BD9B4E019L) ^ (cz * 0x85157AF5L) ^ 0x5245474EL);
    }

    private int art(long h) {
        return (int) Math.floorMod(h >>> 32, (long) arten);
    }

    /** Das Seed für die gestreute Mischung im Saum. */
    public long seed() {
        return seed;
    }
}
