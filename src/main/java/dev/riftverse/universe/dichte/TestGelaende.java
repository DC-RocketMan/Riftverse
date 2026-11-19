package dev.riftverse.universe.dichte;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.universe.Multiverse;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Gelände der Dichte-Testwelt (Meilenstein 11, Entscheidungen M11-12 und M11-13): Hügelland mit Überhängen,
 * schwebende Berge über dem Boden und in der Nähe der Ankunft immer ein großer schwebender Testberg und ein
 * Felsbogen. Jede Region hat ihre eigene Bodenfarbe (Beton, darunter Terrakotta), im Saum sieht man die Mischung;
 * die Unterseiten der schwebenden Berge sind aus Schwebstein, damit Löcher sofort auffallen.
 */
public final class TestGelaende implements Gelaende {

    /** Bodenhöhe ungefähr; alles über {@link #SCHWEBE_Y} gehört zu schwebenden Bergen. */
    public static final int BODEN = 72, SCHWEBE_Y = 130;
    /** Der feste Testberg schwebt östlich der Ankunft, der Bogen steht westlich davon. */
    public static final int BERG_DX = 72, BERG_DZ = 0, BERG_Y = 240, BERG_R = 52;
    public static final int BOGEN_DX = -64, BOGEN_DZ = 24, BOGEN_R = 22, BOGEN_DICKE = 5;

    private static final Block[] BETON = {Blocks.LIME_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.MAGENTA_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.WHITE_CONCRETE};
    private static final Block[] TERRAKOTTA = {Blocks.LIME_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.MAGENTA_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.WHITE_TERRACOTTA};
    /** „Pflanzen“ der Regionen: Kerzen überleben auf Beton und zeigen die Mischung im Saum. */
    public static final Block[] KERZEN = {Blocks.LIME_CANDLE, Blocks.ORANGE_CANDLE, Blocks.LIGHT_BLUE_CANDLE,
            Blocks.MAGENTA_CANDLE, Blocks.YELLOW_CANDLE, Blocks.WHITE_CANDLE};
    public static final int ARTEN = BETON.length;

    private final Regionskarte regionen;
    private final int mitteX, mitteZ;
    private final SimplexNoise huegel, detail, ueberhangMaske, schwebeMaske, schwebeHoehe;
    private final ImprovedNoise ueberhang, rauh;
    /** Bodenhöhe unter der Mitte des Bogens, damit er nie ganz vergraben ist oder in der Luft hängt. */
    private final double bogenBoden;

    public TestGelaende(long seed, int cellX, int cellZ, Regionskarte regionen) {
        this.regionen = regionen;
        this.mitteX = Multiverse.cellCenter(cellX);
        this.mitteZ = Multiverse.cellCenter(cellZ);
        RandomSource r = RandomSource.create(seed ^ 0x44494348544EL);
        this.huegel = new SimplexNoise(r);
        this.detail = new SimplexNoise(r);
        this.ueberhangMaske = new SimplexNoise(r);
        this.schwebeMaske = new SimplexNoise(r);
        this.schwebeHoehe = new SimplexNoise(r);
        this.ueberhang = new ImprovedNoise(r);
        this.rauh = new ImprovedNoise(r);
        this.bogenBoden = boden(mitteX + BOGEN_DX, mitteZ + BOGEN_DZ);
    }

    private double boden(int x, int z) {
        return BODEN + 26 * fbm(huegel, x / 180.0, z / 180.0, 4) + 5 * fbm(detail, x / 40.0, z / 40.0, 2);
    }

    public Regionskarte regionen() {
        return regionen;
    }

    private static double fbm(SimplexNoise n, double x, double z, int octaves) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += n.getValue(x * freq, z * freq) * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / norm;
    }

    @Override
    public Spalte spalte(int x, int z) {
        double boden = boden(x, z);
        double ueber = Mth.clamp(fbm(ueberhangMaske, x / 300.0, z / 300.0, 2) * 1.6 + 0.35, 0, 1);

        // Zufällige schwebende Berge: flach oben, spitz nach unten
        double oben = Double.NaN, unten = Double.NaN;
        double m = fbm(schwebeMaske, x / 220.0, z / 220.0, 3);
        if (m > 0.3) {
            double k = (m - 0.3) / 0.7;
            double mitte = 240 + 40 * fbm(schwebeHoehe, x / 500.0, z / 500.0, 2);
            oben = mitte + 6 + k * 28;
            unten = mitte - 4 - k * k * 56;   // tiefstens Y 140, immer frei über dem Boden
        }
        // Der feste Testberg
        double bx = x - (mitteX + BERG_DX), bz = z - (mitteZ + BERG_DZ);
        double bd = Math.sqrt(bx * bx + bz * bz) / BERG_R;
        if (bd < 1) {
            double k = 1 - bd * bd;
            double o = BERG_Y + 6 + k * 22, u = BERG_Y - 4 - k * 92;
            if (Double.isNaN(oben) || o - u > oben - unten) {
                oben = o;
                unten = u;
            }
        }
        final double schwebOben = oben, schwebUnten = unten;
        final double bogenX = x - (mitteX + BOGEN_DX), bogenZ = z - (mitteZ + BOGEN_DZ);
        final boolean bogenNah = Math.abs(bogenX) <= BOGEN_R + BOGEN_DICKE + 4 && Math.abs(bogenZ) <= BOGEN_DICKE + 4;

        return y -> {
            // Boden: Gefälle von einem Zwölftel pro Block, im Überhang-Band verbogen durch 3D-Rauschen
            double d = (boden - y) / 12.0;
            double band = Math.abs(y - boden);
            if (ueber > 0 && band < 40) {
                d += ueber * (1 - band / 40.0) * 0.95 * ueberhang.noise(x / 36.0, y / 22.0, z / 36.0);
            }
            if (!Double.isNaN(schwebOben) && y > schwebUnten - 9 && y < schwebOben + 9) {
                double f = Math.min(schwebOben - y, y - schwebUnten) / 6.0 + 0.2 * rauh.noise(x / 18.0, y / 18.0, z / 18.0);
                d = Math.max(d, f);
            }
            if (bogenNah) {
                // Halber Ring in der x-y-Ebene, der Fuß steckt im Boden
                double ry = y - (bogenBoden - 4);
                double ring = Math.sqrt(bogenX * bogenX + ry * ry) - BOGEN_R;
                double rohr = BOGEN_DICKE - Math.sqrt(ring * ring + bogenZ * bogenZ);
                if (ry > -12) d = Math.max(d, rohr / 2.0);
            }
            return d;
        };
    }

    @Override
    public Bloecke bloecke(int x, int z) {
        int art = regionen.bei(x, z).gestreut(x, z, regionen.seed());
        BlockState oben = BETON[art].defaultBlockState();
        BlockState unter = TERRAKOTTA[art].defaultBlockState();
        BlockState schweb = ModBlocks.SCHWEBSTEIN.get().defaultBlockState();
        return new Bloecke() {
            @Override
            public BlockState oberflaeche(int y) {
                return oben;
            }

            @Override
            public BlockState unterschicht(int y) {
                return unter;
            }

            @Nullable
            @Override
            public BlockState unterseite(int y) {
                return y >= SCHWEBE_Y ? schweb : null;
            }
        };
    }

    /** Art der Region an einer Stelle (mit gestreuter Mischung im Saum). */
    public int art(int x, int z) {
        return regionen.bei(x, z).gestreut(x, z, regionen.seed());
    }
}
