package dev.riftverse.universe.dichte;

import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Die 3D-Dichte eines Universums (Meilenstein 11): berechnet die Dichte eines {@link Gelaende}s im groben Raster,
 * interpoliert trilinear dazwischen und baut daraus einen Chunk.
 * <ul>
 *     <li>Raster wie Vanilla (Standard 4 × 8 × 4 Blöcke, Entscheidung M11-03). Zellen, deren acht Ecken alle Luft
 *     sind, werden übersprungen; Zellen, deren Ecken alle Fels sind, ohne Interpolation gefüllt.</li>
 *     <li>Oberfläche im zweiten Durchlauf (M11-04): Oberflächenblock nur mit Luft darüber, darunter 3 Blöcke
 *     Unterschicht; Unterseiten (Luft darunter) aus Gestein.</li>
 *     <li>Jeder Hohlraum unter dem Wasserspiegel wird geflutet, Höhlen werden danach tief im Fels herausgeschnitten
 *     und bleiben trocken; keine Aquifere (M11-05).</li>
 *     <li>Einzelne Luftblöcke mitten im Fels und einzelne schwebende Felsblöcke, die die Interpolation an dünnen
 *     Stellen erzeugen kann, werden geglättet (Lochtest M11-15).</li>
 *     <li>{@link #dichte}, {@link #oberste} und damit {@code getBaseHeight}/{@code getBaseColumn} lesen dieselben
 *     Rasterpunkte über einen kleinen Zwischenspeicher pro Thread (M11-07).</li>
 * </ul>
 */
public final class Dichtefeld {
    public static final int NO_LAND = Integer.MIN_VALUE;
    /** So viele Rasterspalten merkt sich jeder Generator-Thread (ein Chunk braucht 49 bei Raster 4). */
    private static final int SPEICHER = 200;
    /** Chunk plus ein Block Rand auf jeder Seite, damit die Glättung die Nachbarn sieht. */
    private static final int W = 18;

    private final Gelaende gelaende;
    private final int minY, height, rxz, ry, ny;
    private final ThreadLocal<LinkedHashMap<Long, double[]>> spalten;

    public Dichtefeld(Gelaende gelaende, int minY, int height) {
        this.gelaende = gelaende;
        this.minY = minY;
        this.height = height;
        this.rxz = gelaende.rasterXZ();
        this.ry = gelaende.rasterY();
        if (rxz <= 0 || ry <= 0 || 16 % rxz != 0 || 16 % ry != 0 || height % ry != 0) {
            throw new IllegalArgumentException("Raster " + rxz + " × " + ry + " passt nicht zur Höhe " + height);
        }
        this.ny = height / ry + 1;
        this.spalten = ThreadLocal.withInitial(() -> new LinkedHashMap<Long, double[]>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, double[]> eldest) {
                return size() > SPEICHER;
            }
        });
    }

    public Gelaende gelaende() {
        return gelaende;
    }

    public int minY() {
        return minY;
    }

    public int height() {
        return height;
    }

    // ------------------------------------------------------------------ Abfragen

    /** Dichte an den Rasterpunkten der Rasterspalte (gx, gz), von unten nach oben. */
    double[] rasterSpalte(int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
        LinkedHashMap<Long, double[]> speicher = spalten.get();
        double[] d = speicher.get(key);
        if (d == null) {
            Gelaende.Spalte s = gelaende.spalte(gx * rxz, gz * rxz);
            d = new double[ny];
            for (int j = 0; j < ny; j++) d[j] = s.dichte(minY + j * ry);
            speicher.put(key, d);
        }
        return d;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Trilinear; Ecken als c[x][y][z]. Fülle und Abfragen nutzen genau diese Rechnung, damit sie übereinstimmen. */
    private static double tri(double c000, double c100, double c010, double c110,
                              double c001, double c101, double c011, double c111,
                              double tx, double ty, double tz) {
        double y0 = lerp(lerp(c000, c100, tx), lerp(c001, c101, tx), tz);
        double y1 = lerp(lerp(c010, c110, tx), lerp(c011, c111, tx), tz);
        return lerp(y0, y1, ty);
    }

    /** Interpolierte Dichte an einem Block: über 0 ist Fels. */
    public double dichte(int x, int y, int z) {
        if (y < minY) return 1;
        if (y >= minY + height) return -1;
        int gx = Math.floorDiv(x, rxz), gz = Math.floorDiv(z, rxz);
        double tx = (double) (x - gx * rxz) / rxz, tz = (double) (z - gz * rxz) / rxz;
        int ly = y - minY;
        int j = Math.min(ly / ry, ny - 2);
        double ty = (double) (ly - j * ry) / ry;
        double[] a = rasterSpalte(gx, gz), b = rasterSpalte(gx + 1, gz);
        double[] c = rasterSpalte(gx, gz + 1), d = rasterSpalte(gx + 1, gz + 1);
        return tri(a[j], b[j], a[j + 1], b[j + 1], c[j], d[j], c[j + 1], d[j + 1], tx, ty, tz);
    }

    public boolean fest(int x, int y, int z) {
        return dichte(x, y, z) > 0;
    }

    /** Oberster Fels-Block der Spalte oder {@link #NO_LAND}. */
    public int oberste(int x, int z) {
        int gx = Math.floorDiv(x, rxz), gz = Math.floorDiv(z, rxz);
        double tx = (double) (x - gx * rxz) / rxz, tz = (double) (z - gz * rxz) / rxz;
        double[] a = rasterSpalte(gx, gz), b = rasterSpalte(gx + 1, gz);
        double[] c = rasterSpalte(gx, gz + 1), d = rasterSpalte(gx + 1, gz + 1);
        for (int j = ny - 2; j >= 0; j--) {
            double max = Math.max(Math.max(Math.max(a[j], a[j + 1]), Math.max(b[j], b[j + 1])),
                    Math.max(Math.max(c[j], c[j + 1]), Math.max(d[j], d[j + 1])));
            if (max <= 0) continue;
            for (int ly = j * ry + ry - 1; ly >= j * ry; ly--) {
                double ty = (double) (ly - j * ry) / ry;
                if (tri(a[j], b[j], a[j + 1], b[j + 1], c[j], d[j], c[j + 1], d[j + 1], tx, ty, tz) > 0) return minY + ly;
            }
        }
        return NO_LAND;
    }

    // ------------------------------------------------------------------ Chunk bauen

    /**
     * Füllt einen Chunk aus der Dichte.
     *
     * @return pro Spalte die oberste Oberfläche (Fels mit Luft oder Flüssigkeit darüber), sonst {@link #NO_LAND}
     */
    public int[][] fuellen(ChunkAccess chunk, UniverseProfile p) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        boolean[] fest = rasterFuellen(x0, z0);
        glaetten(fest);

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState surface = p.palette.surface().defaultBlockState();
        BlockState under = p.palette.under().defaultBlockState();
        BlockState seabed = p.palette.seabed().defaultBlockState();
        BlockState fluid = p.hasFluid() ? p.fluidBlock() : null;
        int fluidLevel = p.fluidLevel;

        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap world = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int[][] tops = new int[16][16];
        for (int[] row : tops) Arrays.fill(row, NO_LAND);

        LevelChunkSection[] sections = chunk.getSections();
        for (LevelChunkSection s : sections) s.acquire();
        try {
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = x0 + lx, z = z0 + lz;
                    if (Multiverse.inBorder(x, z)) continue;
                    Gelaende.Bloecke bl = gelaende.bloecke(x, z);
                    int base = index(lx, lz, 0);
                    int tiefe = -1;
                    boolean nass = false, obenNass = false;
                    for (int ly = height - 1; ly >= 0; ly--) {
                        int y = minY + ly;
                        if (!fest[base + ly]) {
                            tiefe = -1;
                            nass = fluid != null && y <= fluidLevel;
                            if (nass) setzen(chunk, ocean, world, lx, y, lz, fluid);
                            continue;
                        }
                        if (tiefe < 0) {
                            tiefe = 0;
                            obenNass = nass;
                        } else {
                            tiefe++;
                        }
                        boolean untenLuft = ly > 0 && !fest[base + ly - 1];
                        BlockState state;
                        if (ly == 0) {
                            state = bedrock;
                        } else if (tiefe == 0) {
                            if (tops[lx][lz] == NO_LAND) tops[lx][lz] = y;
                            state = obenNass ? seabed : oder(bl.oberflaeche(y), surface);
                        } else if (untenLuft) {
                            state = oder(bl.unterseite(y), p.stoneAt(y));
                        } else if (tiefe <= 3) {
                            state = obenNass ? seabed : oder(bl.unterschicht(y), under);
                        } else {
                            // Höhlen nur tief im Fels: Sie brechen nie zu Seen durch und bleiben trocken
                            if (tiefe >= 6 && ly > 2 && p.isCave(x, y, z)) {
                                if (ly < 10) setzen(chunk, ocean, world, lx, y, lz, p.caveFluid());
                                continue;
                            }
                            BlockState ore = p.oreAt(x, y, z);
                            state = ore != null ? ore : p.stoneAt(y);
                        }
                        setzen(chunk, ocean, world, lx, y, lz, state);
                    }
                }
            }
        } finally {
            for (LevelChunkSection s : sections) s.release();
        }
        return tops;
    }

    private static BlockState oder(@Nullable BlockState eigen, BlockState standard) {
        return eigen != null ? eigen : standard;
    }

    private static void setzen(ChunkAccess chunk, Heightmap ocean, Heightmap world, int lx, int y, int lz, BlockState state) {
        LevelChunkSection sec = chunk.getSection(chunk.getSectionIndex(y));
        sec.setBlockState(lx, y & 15, lz, state, false);
        ocean.update(lx, y, lz, state);
        world.update(lx, y, lz, state);
    }

    /** Index in das Fels-Feld: bx, bz von -1 bis 16, ly von 0 bis height-1. */
    private int index(int bx, int bz, int ly) {
        return ((bx + 1) * W + (bz + 1)) * height + ly;
    }

    /** Fels-Feld für den Chunk plus einen Block Rand, Zelle für Zelle aus dem Raster. */
    boolean[] rasterFuellen(int x0, int z0) {
        boolean[] fest = new boolean[W * W * height];
        int gx0 = Math.floorDiv(x0 - 1, rxz), gx1 = Math.floorDiv(x0 + 16, rxz);
        int gz0 = Math.floorDiv(z0 - 1, rxz), gz1 = Math.floorDiv(z0 + 16, rxz);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                double[] a = rasterSpalte(gx, gz), b = rasterSpalte(gx + 1, gz);
                double[] c = rasterSpalte(gx, gz + 1), d = rasterSpalte(gx + 1, gz + 1);
                int bxA = Math.max(gx * rxz, x0 - 1), bxB = Math.min(gx * rxz + rxz - 1, x0 + 16);
                int bzA = Math.max(gz * rxz, z0 - 1), bzB = Math.min(gz * rxz + rxz - 1, z0 + 16);
                for (int j = 0; j < ny - 1; j++) {
                    double c000 = a[j], c100 = b[j], c010 = a[j + 1], c110 = b[j + 1];
                    double c001 = c[j], c101 = d[j], c011 = c[j + 1], c111 = d[j + 1];
                    double max = Math.max(Math.max(Math.max(c000, c100), Math.max(c010, c110)),
                            Math.max(Math.max(c001, c101), Math.max(c011, c111)));
                    if (max <= 0) continue;
                    double min = Math.min(Math.min(Math.min(c000, c100), Math.min(c010, c110)),
                            Math.min(Math.min(c001, c101), Math.min(c011, c111)));
                    boolean voll = min > 0;
                    for (int bx = bxA; bx <= bxB; bx++) {
                        double tx = (double) (bx - gx * rxz) / rxz;
                        for (int bz = bzA; bz <= bzB; bz++) {
                            double tz = (double) (bz - gz * rxz) / rxz;
                            int base = index(bx - x0, bz - z0, 0);
                            for (int ly = j * ry; ly < j * ry + ry; ly++) {
                                if (voll) {
                                    fest[base + ly] = true;
                                    continue;
                                }
                                double ty = (double) (ly - j * ry) / ry;
                                if (tri(c000, c100, c010, c110, c001, c101, c011, c111, tx, ty, tz) > 0) fest[base + ly] = true;
                            }
                        }
                    }
                }
            }
        }
        return fest;
    }

    /**
     * Glättet das Innere des Chunks: ein Luftblock mit Fels auf allen sechs Seiten wird Fels, ein Felsblock ohne
     * jeden Fels-Nachbarn wird Luft. Entschieden wird auf dem ungeglätteten Feld, damit die Reihenfolge egal ist.
     */
    void glaetten(boolean[] fest) {
        int stepX = W * height, stepZ = height;
        int[] aendern = new int[256];
        int n = 0;
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                int base = index(bx, bz, 0);
                for (int ly = 1; ly < height - 1; ly++) {
                    int i = base + ly;
                    boolean f = fest[i];
                    int nachbarn = (fest[i - 1] ? 1 : 0) + (fest[i + 1] ? 1 : 0)
                            + (fest[i - stepX] ? 1 : 0) + (fest[i + stepX] ? 1 : 0)
                            + (fest[i - stepZ] ? 1 : 0) + (fest[i + stepZ] ? 1 : 0);
                    if ((f && nachbarn == 0) || (!f && nachbarn == 6)) {
                        if (n == aendern.length) aendern = Arrays.copyOf(aendern, n * 2);
                        aendern[n++] = i;
                    }
                }
            }
        }
        for (int k = 0; k < n; k++) fest[aendern[k]] = !fest[aendern[k]];
    }
}
