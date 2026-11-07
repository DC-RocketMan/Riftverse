package dev.riftverse.universe;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.story.NameGen;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alle Eigenschaften eines Universums – vollständig deterministisch aus seiner
 * {@link UniverseId} abgeleitet: Gelände, Gesteins-Palette, Flora, Flüssigkeiten
 * und die physikalischen Regeln (Gravitation, Atmosphäre, Temperatur).
 * Die Physik orientiert sich bewusst an unserer: Abweichungen sind plausibel,
 * nicht absurd (0,3 g bis 1,8 g, dünne/dichte/giftige Luft, Hitze/Kälte).
 */
public final class UniverseProfile {

    // ------------------------------------------------------------------ Enums

    public enum Terrain {
        ROLLING("Hügelland"), TERRACED("Tafelberge"), CANYON("Schluchtenland"), SPIKES("Steinnadeln"),
        ARCHIPELAGO("Inselmeer"), CRATERED("Kraterfeld"), FLOATING("Schwebende Inseln");
        public final String label;
        Terrain(String label) { this.label = label; }
    }

    public enum Atmosphere {
        NORMAL("atembar"), DENSE("dicht – Bewegung zäh, Stürze gedämpft"),
        THIN("dünn – Sprinten kostet viel Kraft"), TOXIC("giftig – ohne Helm Vergiftung");
        public final String label;
        Atmosphere(String label) { this.label = label; }
    }

    public enum Temperature {
        TEMPERATE("gemäßigt"), HOT("heiß – höherer Hunger, Lava statt Wasser"),
        COLD("eisig – ohne Lederrüstung erfrierst du");
        public final String label;
        Temperature(String label) { this.label = label; }
    }

    public enum Flora { NONE, GRASS, TREES, GLOW_TREES, CRYSTALS, MUSHROOMS, DESERT }

    public record Palette(String name, Block surface, Block under, Block stone, Block seabed,
                          boolean lush, boolean allowHot, boolean allowCold, boolean dry, boolean banded) {}

    private static final List<Palette> PALETTES = List.of(
            new Palette("Erdähnlich", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, Blocks.SAND, true, false, true, false, false),
            new Palette("Moosteppich", Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.DEEPSLATE, Blocks.CLAY, true, false, false, false, false),
            new Palette("Myzel-Welt", Blocks.MYCELIUM, Blocks.DIRT, Blocks.STONE, Blocks.GRAVEL, true, false, false, false, false),
            new Palette("Rote Wüste", Blocks.RED_SAND, Blocks.RED_SANDSTONE, Blocks.TERRACOTTA, Blocks.RED_SAND, false, true, false, true, false),
            new Palette("Weiße Dünen", Blocks.SAND, Blocks.SANDSTONE, Blocks.STONE, Blocks.SAND, false, true, false, false, false),
            new Palette("Vulkanisch", Blocks.BLACKSTONE, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.SMOOTH_BASALT, false, true, false, false, false),
            new Palette("Mondstaub", Blocks.CALCITE, Blocks.TUFF, Blocks.TUFF, Blocks.GRAVEL, false, false, true, true, false),
            new Palette("Warped-Flora", Blocks.WARPED_NYLIUM, Blocks.NETHERRACK, Blocks.DEEPSLATE, Blocks.GRAVEL, true, true, true, false, false),
            new Palette("Karmesin-Flora", Blocks.CRIMSON_NYLIUM, Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.SOUL_SAND, true, true, false, false, false),
            new Palette("Eiswüste", Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.STONE, Blocks.GRAVEL, false, false, true, false, false),
            new Palette("Sculk-Land", Blocks.SCULK, Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.SCULK, false, false, true, false, false),
            new Palette("Endstein-Ebene", Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE, Blocks.GRAVEL, false, false, true, true, false),
            new Palette("Schlammwelt", Blocks.MUD, Blocks.PACKED_MUD, Blocks.STONE, Blocks.MUD, true, false, false, false, false),
            new Palette("Terrakotta-Mesa", Blocks.ORANGE_TERRACOTTA, Blocks.TERRACOTTA, Blocks.TERRACOTTA, Blocks.RED_SAND, false, true, false, false, true));

    private static final Block[] BANDS = {Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA,
            Blocks.WHITE_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA};

    // ------------------------------------------------------------------ Cache

    private static final Map<UniverseId, UniverseProfile> CACHE = new ConcurrentHashMap<>();

    public static UniverseProfile get(UniverseId id) {
        UniverseProfile p = CACHE.get(id);
        if (p == null) {
            if (CACHE.size() > 256) CACHE.clear();
            p = new UniverseProfile(id);
            CACHE.put(id, p);
        }
        return p;
    }

    // ------------------------------------------------------------------ Felder

    public final UniverseId id;
    public final long seed;
    public final String name;
    public final Terrain terrain;
    public final Palette palette;
    public final Flora flora;
    public final Atmosphere atmosphere;
    public final Temperature temperature;
    public final double gravity;
    public final double oreRichness;
    public final int danger;
    public final int biomeRoll;
    public final boolean caves;
    public final int fluidLevel;
    public final float treeDensity;
    public final float plantDensity;
    public final int treeStyle;

    private final double base, amplitude, frequency;
    private final double stepHeight, spikeHeight, canyonDepth, craterDensity;
    private final SimplexNoise heightNoise, detailNoise, featureNoise;
    private final ImprovedNoise caveNoise, oreNoise;

    private UniverseProfile(UniverseId id) {
        this.id = id;
        this.seed = id.seed();
        RandomSource r = RandomSource.create(seed);
        int v = id.variant();

        this.name = NameGen.universeName(r);

        // --- Physik ---
        this.temperature = switch (v) {
            case 2 -> Temperature.HOT;
            case 1 -> weighted(r, Temperature.values(), 40, 5, 55);
            case 3 -> weighted(r, Temperature.values(), 50, 0, 50);
            default -> weighted(r, Temperature.values(), 60, 20, 20);
        };
        this.atmosphere = switch (v) {
            case 2 -> weighted(r, Atmosphere.values(), 40, 25, 0, 35);
            case 3 -> weighted(r, Atmosphere.values(), 25, 0, 55, 20);
            default -> weighted(r, Atmosphere.values(), 65, 12, 13, 10);
        };
        double g = switch (v) {
            case 3 -> 0.3 + r.nextDouble() * 0.5;
            case 2 -> Mth.clamp(1.15 + r.nextGaussian() * 0.2, 0.6, 1.8);
            default -> Mth.clamp(1.0 + r.nextGaussian() * 0.22, 0.45, 1.7);
        };
        this.gravity = Math.round(g * 100.0) / 100.0;

        // --- Gelände ---
        this.terrain = switch (v) {
            case 1 -> weighted(r, Terrain.values(), 25, 10, 15, 20, 10, 10, 10);
            case 2 -> weighted(r, Terrain.values(), 20, 15, 25, 25, 0, 15, 0);
            case 3 -> weighted(r, Terrain.values(), 0, 0, 0, 10, 0, 35, 55);
            default -> weighted(r, Terrain.values(), 30, 15, 15, 10, 20, 5, 5);
        };
        this.palette = pickPalette(r, temperature, v);
        this.flora = pickFlora(r, palette, temperature, v);
        this.treeStyle = r.nextInt(9);
        this.treeDensity = switch (flora) {
            case TREES, GLOW_TREES -> 0.004f + r.nextFloat() * 0.03f;
            case MUSHROOMS, CRYSTALS -> 0.003f + r.nextFloat() * 0.012f;
            case DESERT -> 0.004f + r.nextFloat() * 0.01f;
            default -> 0f;
        };
        this.plantDensity = flora == Flora.NONE ? 0.02f : 0.05f + r.nextFloat() * 0.25f;

        this.oreRichness = Mth.clamp(0.5 + r.nextGaussian() * 0.25, 0.1, 1.0);
        this.danger = 1 + r.nextInt(5);
        this.biomeRoll = r.nextInt(1 << 20);
        this.caves = terrain != Terrain.FLOATING && r.nextFloat() < 0.7f;

        boolean hasFluid = !palette.dry() && terrain != Terrain.FLOATING;
        this.fluidLevel = hasFluid ? (terrain == Terrain.ARCHIPELAGO ? 63 : 48 + r.nextInt(22)) : Integer.MIN_VALUE;

        switch (terrain) {
            case ROLLING -> { base = 66 + r.nextInt(20); amplitude = 10 + r.nextInt(30); frequency = 1.0 / (90 + r.nextInt(130)); }
            case TERRACED -> { base = 70 + r.nextInt(20); amplitude = 25 + r.nextInt(35); frequency = 1.0 / (120 + r.nextInt(120)); }
            case CANYON -> { base = 82 + r.nextInt(18); amplitude = 12 + r.nextInt(20); frequency = 1.0 / (140 + r.nextInt(100)); }
            case SPIKES -> { base = 68 + r.nextInt(14); amplitude = 8 + r.nextInt(16); frequency = 1.0 / (100 + r.nextInt(100)); }
            case ARCHIPELAGO -> { base = 50; amplitude = 22 + r.nextInt(16); frequency = 1.0 / (120 + r.nextInt(100)); }
            case CRATERED -> { base = 70 + r.nextInt(16); amplitude = 4 + r.nextInt(9); frequency = 1.0 / (160 + r.nextInt(120)); }
            default -> { base = 90 + r.nextInt(50); amplitude = 20 + r.nextInt(30); frequency = 1.0 / (70 + r.nextInt(60)); }
        }
        this.stepHeight = 5 + r.nextInt(8);
        this.spikeHeight = 30 + r.nextInt(45);
        this.canyonDepth = 25 + r.nextInt(30);
        this.craterDensity = 0.2 + r.nextDouble() * 0.4;

        this.heightNoise = new SimplexNoise(r);
        this.detailNoise = new SimplexNoise(r);
        this.featureNoise = new SimplexNoise(r);
        this.caveNoise = new ImprovedNoise(r);
        this.oreNoise = new ImprovedNoise(r);
    }

    // ------------------------------------------------------------------ Auswahl-Helfer

    private static <T> T weighted(RandomSource r, T[] values, int... weights) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = r.nextInt(Math.max(1, total));
        for (int i = 0; i < weights.length && i < values.length; i++) {
            roll -= weights[i];
            if (roll < 0) return values[i];
        }
        return values[0];
    }

    private static Palette pickPalette(RandomSource r, Temperature t, int variant) {
        List<Palette> ok = PALETTES.stream().filter(p -> switch (t) {
            case HOT -> p.allowHot();
            case COLD -> p.allowCold();
            default -> !p.name().equals("Eiswüste");
        }).filter(p -> variant != 3 || !p.lush() || p.surface() == Blocks.WARPED_NYLIUM).toList();
        if (ok.isEmpty()) return PALETTES.get(0);
        return ok.get(r.nextInt(ok.size()));
    }

    private static Flora pickFlora(RandomSource r, Palette p, Temperature t, int variant) {
        Block s = p.surface();
        if (s == Blocks.MYCELIUM) return r.nextFloat() < 0.8f ? Flora.MUSHROOMS : Flora.GRASS;
        if (s == Blocks.SAND || s == Blocks.RED_SAND || s == Blocks.ORANGE_TERRACOTTA)
            return r.nextFloat() < 0.6f ? Flora.DESERT : (r.nextBoolean() ? Flora.CRYSTALS : Flora.NONE);
        if (s == Blocks.SNOW_BLOCK || s == Blocks.CALCITE || s == Blocks.END_STONE)
            return r.nextFloat() < 0.5f ? Flora.CRYSTALS : (variant == 1 ? Flora.GLOW_TREES : Flora.NONE);
        if (s == Blocks.BLACKSTONE || s == Blocks.SCULK)
            return r.nextFloat() < 0.5f ? Flora.GLOW_TREES : Flora.NONE;
        if (variant == 1) return r.nextFloat() < 0.7f ? Flora.GLOW_TREES : Flora.GRASS;
        float f = r.nextFloat();
        if (f < 0.5f) return Flora.TREES;
        if (f < 0.75f) return Flora.GRASS;
        if (f < 0.9f) return Flora.GLOW_TREES;
        return Flora.CRYSTALS;
    }

    // ------------------------------------------------------------------ Gelände

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

    /**
     * Berechnet die Landsäule an (x, z).
     * @param out out[0] = unterster fester Block, out[1] = oberster fester Block
     * @return false, wenn hier kein Land ist (Leere)
     */
    public boolean column(int x, int z, int[] out) {
        double n = fbm(heightNoise, x * frequency, z * frequency, 4);
        double detail = fbm(detailNoise, x / 24.0, z / 24.0, 2) * 2.0;
        int minY = -64;

        switch (terrain) {
            case FLOATING -> {
                double m = fbm(featureNoise, x * frequency, z * frequency, 3);
                if (m < 0.18) return false;
                double t = m - 0.18;
                double center = base + amplitude * 0.35 * fbm(heightNoise, x * frequency * 0.5, z * frequency * 0.5, 2);
                out[1] = (int) Math.round(center + t * amplitude * 0.9 + 1 + detail * 0.5);
                out[0] = (int) Math.round(center - t * amplitude * 3.0 - 2);
                if (out[0] > out[1]) out[0] = out[1];
                out[0] = Math.max(out[0], minY + 1);
                return true;
            }
            case TERRACED -> {
                double h = base + amplitude * n;
                h = Math.floor(h / stepHeight) * stepHeight + Math.max(0, detail * 0.5);
                out[1] = (int) h;
            }
            case CANYON -> {
                double h = base + amplitude * n + detail;
                double ridge = Math.abs(featureNoise.getValue(x * frequency * 0.7, z * frequency * 0.7));
                if (ridge < 0.09) h -= (1.0 - ridge / 0.09) * canyonDepth;
                out[1] = (int) h;
            }
            case SPIKES -> {
                double h = base + amplitude * n + detail;
                double s = featureNoise.getValue(x * 0.045, z * 0.045);
                if (s > 0.55) {
                    double k = (s - 0.55) / 0.45;
                    h += k * k * spikeHeight;
                }
                out[1] = (int) h;
            }
            case ARCHIPELAGO -> {
                double h = base + amplitude * (n + 0.1) + detail;
                out[1] = (int) h;
            }
            case CRATERED -> {
                double h = base + amplitude * n + craters(x, z) + detail * 0.5;
                out[1] = (int) h;
            }
            default -> {
                double h = base + amplitude * n + detail;
                out[1] = (int) h;
            }
        }
        out[0] = minY;
        out[1] = Mth.clamp(out[1], minY + 6, 300);
        return true;
    }

    private double craters(int x, int z) {
        final int grid = 56;
        int gx = Math.floorDiv(x, grid), gz = Math.floorDiv(z, grid);
        double h = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long cx = gx + dx, cz = gz + dz;
                long hs = UniverseId.mix(seed ^ (cx * 0x632BE59BD9B4E019L) ^ (cz * 0x85157AF5L));
                double p = (hs & 0xFFFF) / 65535.0;
                if (p > craterDensity) continue;
                double px = cx * grid + ((hs >>> 16) & 0xFF) / 255.0 * grid;
                double pz = cz * grid + ((hs >>> 24) & 0xFF) / 255.0 * grid;
                double radius = 6 + ((hs >>> 32) & 0xFF) / 255.0 * 20;
                double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz)) / radius;
                double depth = radius * 0.45;
                if (d < 1) h -= depth * (1 - d * d);
                else if (d < 1.5) h += depth * 0.35 * (1 - (d - 1) / 0.5);
            }
        }
        return h;
    }

    // ------------------------------------------------------------------ Blöcke

    public boolean hasFluid() {
        return fluidLevel != Integer.MIN_VALUE;
    }

    public BlockState fluidBlock() {
        return temperature == Temperature.HOT ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
    }

    public boolean iceCap() {
        return temperature == Temperature.COLD && fluidBlock().is(Blocks.WATER);
    }

    public BlockState stoneAt(int y) {
        if (palette.banded()) {
            int band = Math.floorMod(y / 3 + (int) (seed & 7), BANDS.length);
            return BANDS[band].defaultBlockState();
        }
        if (palette.stone() == Blocks.STONE && y < 0) return Blocks.DEEPSLATE.defaultBlockState();
        return palette.stone().defaultBlockState();
    }

    public boolean isCave(int x, int y, int z) {
        if (!caves) return false;
        double n = caveNoise.noise(x / 26.0, y / 14.0, z / 26.0);
        return Math.abs(n) < 0.055;
    }

    public BlockState caveFluid() {
        return temperature == Temperature.COLD ? Blocks.WATER.defaultBlockState() : Blocks.LAVA.defaultBlockState();
    }

    /** Erz-Adern über 3D-Rauschen: zusammenhängende Klumpen statt Einzelblöcke. */
    @Nullable
    public BlockState oreAt(int x, int y, int z) {
        double n = oreNoise.noise(x / 5.0, y / 5.0, z / 5.0);
        double threshold = 0.66 - oreRichness * 0.08;
        if (n < threshold) return null;
        long h = UniverseId.mix(seed ^ ((long) Math.floorDiv(x, 8) * 0x9E3779B97F4A7C15L)
                ^ ((long) Math.floorDiv(y, 8) * 0xC2B2AE3D27D4EB4FL) ^ ((long) Math.floorDiv(z, 8) * 0x165667B19E3779F9L));
        int roll = (int) Math.floorMod(h, 1000);
        boolean deep = y < 0;
        int quantum = (int) (120 + oreRichness * 260);
        if (roll < quantum) return ModBlocks.QUANTUM_ORE.get().defaultBlockState();
        roll -= quantum;
        if (roll < 200) return (deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
        if (roll < 360) return (deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
        if (roll < 460) return (deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE).defaultBlockState();
        if (roll < 520) return (deep ? Blocks.DEEPSLATE_REDSTONE_ORE : Blocks.REDSTONE_ORE).defaultBlockState();
        if (roll < 560) return (deep ? Blocks.DEEPSLATE_GOLD_ORE : Blocks.GOLD_ORE).defaultBlockState();
        if (roll < 590) return (deep ? Blocks.DEEPSLATE_LAPIS_ORE : Blocks.LAPIS_ORE).defaultBlockState();
        if (roll < 610 && deep) return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
        return null;
    }

    // ------------------------------------------------------------------ Texte

    public String gravityLabel() {
        if (gravity < 0.7) return "schwach";
        if (gravity < 0.92) return "leicht";
        if (gravity <= 1.08) return "erdähnlich";
        if (gravity <= 1.35) return "schwer";
        return "erdrückend";
    }

    public String gravityText() {
        return String.format(Locale.GERMAN, "%.2f g (%s)", gravity, gravityLabel());
    }

    public String richnessLabel() {
        if (oreRichness > 0.75) return "sehr reich";
        if (oreRichness > 0.5) return "ergiebig";
        if (oreRichness > 0.3) return "mäßig";
        return "karg";
    }

    public String title() {
        return "Universum " + id.code();
    }
}
