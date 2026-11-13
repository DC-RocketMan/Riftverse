package dev.riftverse.universe;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.story.NameGen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * Alle Eigenschaften eines Universums – vollständig deterministisch aus seiner {@link UniverseId} abgeleitet.
 * Der {@link UniverseType} legt Physik, exklusive Ressource und Gefahr fest; innerhalb des Typs würfelt jedes
 * Universum Gelände, Gesteins-Palette und 1–3 {@link UniverseModifier Modifikatoren}.
 * <p>
 * gespeichert wird davon nichts außer Typ und Zelle: Wird ein Universum beim Aufräumen zurückgesetzt, entsteht
 * es aus demselben Seed wieder gleich.
 */
public final class UniverseProfile {

    // ------------------------------------------------------------------ Enums

    public enum Terrain {
        ROLLING("Hügelland"), TERRACED("Tafelberge"), CANYON("Schluchtenland"), SPIKES("Nadelfelsen"),
        ARCHIPELAGO("Seenplatte"), CRATERED("Kraterfeld"), HALDEN("Müllberge"),
        // Meilenstein 3 (immer hinten anhängen, die Gewichte zählen nach Position)
        SCHWEBEINSELN("Schwebeinseln");
        public final String label;
        Terrain(String label) { this.label = label; }
    }

    public enum Atmosphere {
        NORMAL("atembar"), TOXIC("giftig – ohne Helm Vergiftung");
        public final String label;
        Atmosphere(String label) { this.label = label; }
    }

    public enum Temperature {
        TEMPERATE("gemäßigt"), HOT("heiß"), COLD("eisig");
        public final String label;
        Temperature(String label) { this.label = label; }
    }

    public record Palette(String name, Block surface, Block under, Block stone, Block seabed) {}

    private static final Map<UniverseType, List<Palette>> PALETTES = Map.of(
            UniverseType.SCHROTT, List.of(
                    new Palette("Rostige Halde", Blocks.COARSE_DIRT, Blocks.DIRT, Blocks.TUFF, Blocks.GRAVEL),
                    new Palette("Aschehalde", Blocks.GRAVEL, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.GRAVEL),
                    new Palette("Ölschlamm", Blocks.MUD, Blocks.PACKED_MUD, Blocks.STONE, Blocks.MUD),
                    new Palette("Rostwüste", Blocks.RED_SAND, Blocks.RED_SANDSTONE, Blocks.TERRACOTTA, Blocks.RED_SAND)),
            UniverseType.KRISTALL, List.of(
                    new Palette("Kalkglanz", Blocks.CALCITE, Blocks.CALCITE, Blocks.DEEPSLATE, Blocks.CALCITE),
                    new Palette("Tuffglas", Blocks.TUFF, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.GRAVEL),
                    new Palette("Basaltkristall", Blocks.SMOOTH_BASALT, Blocks.BASALT, Blocks.DEEPSLATE, Blocks.SMOOTH_BASALT),
                    new Palette("Glassand", Blocks.SAND, Blocks.SANDSTONE, Blocks.DEEPSLATE, Blocks.SAND)),
            UniverseType.TOXISCH, List.of(
                    new Palette("Giftsumpf", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, Blocks.MUD),
                    new Palette("Moderschlamm", Blocks.MUD, Blocks.PACKED_MUD, Blocks.DEEPSLATE, Blocks.CLAY),
                    new Palette("Myzelfäule", Blocks.MYCELIUM, Blocks.DIRT, Blocks.STONE, Blocks.MUD),
                    new Palette("Moosteppich", Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.DEEPSLATE, Blocks.MUD)),
            UniverseType.NIEDRIGGRAVITATION, List.of(
                    new Palette("Wolkenweide", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, Blocks.GRAVEL),
                    new Palette("Kalkschollen", Blocks.CALCITE, Blocks.CALCITE, Blocks.STONE, Blocks.CALCITE),
                    new Palette("Mondgeröll", Blocks.ANDESITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.GRAVEL),
                    new Palette("Moosinseln", Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.DEEPSLATE, Blocks.CLAY)),
            UniverseType.ZEITDILATATION, List.of(
                    new Palette("Zeitsandwüste", Blocks.SAND, Blocks.SANDSTONE, Blocks.STONE, Blocks.SAND),
                    new Palette("Uhrwerkwiese", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, Blocks.GRAVEL),
                    new Palette("Violette Steppe", Blocks.PODZOL, Blocks.DIRT, Blocks.DEEPSLATE, Blocks.MUD),
                    new Palette("Bernsteinheide", Blocks.ROOTED_DIRT, Blocks.DIRT, Blocks.TUFF, Blocks.SAND)),
            UniverseType.ORGANISCH, List.of(
                    new Palette("Fleischsteppe", ModBlocks.BIOMASSE_BLOCK.get(), Blocks.NETHER_WART_BLOCK, Blocks.DRIPSTONE_BLOCK, Blocks.MUD),
                    new Palette("Pilzgewebe", Blocks.MYCELIUM, ModBlocks.BIOMASSE_BLOCK.get(), Blocks.DEEPSLATE, Blocks.MUD),
                    new Palette("Moosfleisch", Blocks.MOSS_BLOCK, ModBlocks.BIOMASSE_BLOCK.get(), Blocks.STONE, Blocks.CLAY),
                    new Palette("Wurzelgeflecht", Blocks.ROOTED_DIRT, Blocks.NETHER_WART_BLOCK, Blocks.DEEPSLATE, Blocks.MUD)));

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
    public final UniverseType type;
    public final long seed;
    public final String name;
    public final List<UniverseModifier> modifiers;
    public final Terrain terrain;
    public final Palette palette;
    public final Atmosphere atmosphere;
    public final Temperature temperature;
    public final double gravity;
    public final double oreRichness;
    public final int danger;
    public final int biomeRoll;
    public final boolean caves;
    public final int fluidLevel;
    /** Anteil der Oberfläche mit Deko (Kristallnadeln, tote Bäume, Wracks). */
    public final float featureDensity;
    public final float plantDensity;

    private final double base, amplitude, frequency;
    private final double stepHeight, spikeHeight, canyonDepth, craterDensity, haldenHeight;
    private final SimplexNoise heightNoise, detailNoise, featureNoise, fieldNoise;
    private final ImprovedNoise caveNoise, oreNoise;

    private UniverseProfile(UniverseId id) {
        this.id = id;
        this.type = id.type();
        this.seed = id.seed();
        RandomSource r = RandomSource.create(seed);

        this.name = NameGen.universeName(r);
        this.modifiers = UniverseModifier.roll(r);
        this.gravity = type.gravity();
        this.atmosphere = type == UniverseType.TOXISCH ? Atmosphere.TOXIC : Atmosphere.NORMAL;
        this.temperature = Temperature.TEMPERATE;

        this.terrain = switch (type) {
            case SCHROTT -> weighted(r, Terrain.values(), 15, 0, 0, 0, 0, 25, 60);
            case KRISTALL -> weighted(r, Terrain.values(), 30, 15, 20, 35, 0, 0, 0);
            case TOXISCH -> weighted(r, Terrain.values(), 35, 0, 15, 0, 35, 15, 0);
            case NIEDRIGGRAVITATION -> weighted(r, Terrain.values(), 0, 0, 0, 0, 0, 0, 0, 1);
            case ZEITDILATATION -> weighted(r, Terrain.values(), 40, 25, 15, 0, 10, 10, 0);
            case ORGANISCH -> weighted(r, Terrain.values(), 45, 0, 15, 10, 30, 0, 0);
        };
        List<Palette> palettes = PALETTES.get(type);
        this.palette = palettes.get(r.nextInt(palettes.size()));
        this.featureDensity = switch (type) {
            case SCHROTT -> 0.15f + r.nextFloat() * 0.2f;
            case KRISTALL -> 0.006f + r.nextFloat() * 0.012f;
            case TOXISCH -> 0.004f + r.nextFloat() * 0.012f;
            case NIEDRIGGRAVITATION -> 0.004f + r.nextFloat() * 0.008f;
            case ZEITDILATATION -> 0.003f + r.nextFloat() * 0.006f;
            case ORGANISCH -> 0.006f + r.nextFloat() * 0.01f;
        };
        this.plantDensity = switch (type) {
            case SCHROTT -> 0.01f + r.nextFloat() * 0.03f;
            case KRISTALL -> 0.02f + r.nextFloat() * 0.05f;
            case TOXISCH -> 0.08f + r.nextFloat() * 0.2f;
            case NIEDRIGGRAVITATION -> 0.05f + r.nextFloat() * 0.1f;
            case ZEITDILATATION -> 0.1f + r.nextFloat() * 0.2f;
            case ORGANISCH -> 0.03f + r.nextFloat() * 0.05f;
        };

        double richness = Mth.clamp(0.5 + r.nextGaussian() * 0.25, 0.1, 1.0);
        this.oreRichness = modifiers.contains(UniverseModifier.DOPPELTE_ERZE) ? Math.min(2.0, richness * 2.0) : richness;
        this.danger = 1 + r.nextInt(5);
        this.biomeRoll = r.nextInt(1 << 20);
        this.caves = r.nextFloat() < 0.75f;

        // Schwebeinseln haben keine Seen: unter ihnen ist nur das Nichts
        boolean dry = palette.surface() == Blocks.RED_SAND || type == UniverseType.NIEDRIGGRAVITATION;
        this.fluidLevel = dry ? Integer.MIN_VALUE : (terrain == Terrain.ARCHIPELAGO ? 63 : 50 + r.nextInt(18));

        switch (terrain) {
            case ROLLING -> { base = 66 + r.nextInt(20); amplitude = 10 + r.nextInt(30); frequency = 1.0 / (90 + r.nextInt(130)); }
            case TERRACED -> { base = 70 + r.nextInt(20); amplitude = 25 + r.nextInt(35); frequency = 1.0 / (120 + r.nextInt(120)); }
            case CANYON -> { base = 82 + r.nextInt(18); amplitude = 12 + r.nextInt(20); frequency = 1.0 / (140 + r.nextInt(100)); }
            case SPIKES -> { base = 68 + r.nextInt(14); amplitude = 8 + r.nextInt(16); frequency = 1.0 / (100 + r.nextInt(100)); }
            case ARCHIPELAGO -> { base = 52; amplitude = 18 + r.nextInt(14); frequency = 1.0 / (120 + r.nextInt(100)); }
            case CRATERED -> { base = 70 + r.nextInt(16); amplitude = 4 + r.nextInt(9); frequency = 1.0 / (160 + r.nextInt(120)); }
            case SCHWEBEINSELN -> { base = 96 + r.nextInt(24); amplitude = 26 + r.nextInt(26); frequency = 1.0 / (70 + r.nextInt(50)); }
            default -> { base = 66 + r.nextInt(10); amplitude = 6 + r.nextInt(10); frequency = 1.0 / (110 + r.nextInt(90)); }
        }
        this.stepHeight = 5 + r.nextInt(8);
        this.spikeHeight = 30 + r.nextInt(45);
        this.canyonDepth = 25 + r.nextInt(30);
        this.craterDensity = 0.2 + r.nextDouble() * 0.4;
        this.haldenHeight = 18 + r.nextInt(28);

        this.heightNoise = new SimplexNoise(r);
        this.detailNoise = new SimplexNoise(r);
        this.featureNoise = new SimplexNoise(r);
        this.fieldNoise = new SimplexNoise(r);
        this.caveNoise = new ImprovedNoise(r);
        this.oreNoise = new ImprovedNoise(r);
    }

    public boolean has(UniverseModifier modifier) {
        return modifiers.contains(modifier);
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
        double h;
        switch (terrain) {
            case TERRACED -> {
                h = base + amplitude * n;
                h = Math.floor(h / stepHeight) * stepHeight + Math.max(0, detail * 0.5);
            }
            case CANYON -> {
                h = base + amplitude * n + detail;
                double ridge = Math.abs(featureNoise.getValue(x * frequency * 0.7, z * frequency * 0.7));
                if (ridge < 0.09) h -= (1.0 - ridge / 0.09) * canyonDepth;
            }
            case SPIKES -> {
                h = base + amplitude * n + detail;
                double s = featureNoise.getValue(x * 0.045, z * 0.045);
                if (s > 0.55) {
                    double k = (s - 0.55) / 0.45;
                    h += k * k * spikeHeight;
                }
            }
            case ARCHIPELAGO -> h = base + amplitude * (n + 0.1) + detail;
            case CRATERED -> h = base + amplitude * n + craters(x, z) + detail * 0.5;
            case HALDEN -> {
                double m = Math.max(0, halde(x, z));
                h = base + amplitude * n + m * m * haldenHeight + detail;
            }
            case SCHWEBEINSELN -> {
                return island(x, z, n, detail, out);
            }
            default -> h = base + amplitude * n + detail;
        }
        out[0] = minY;
        out[1] = Mth.clamp((int) h, minY + 6, 300);
        return true;
    }

    /**
     * Schwebeinseln der Niedriggravitationswelt: Inseln in verschiedenen Höhen über dem Nichts, unten spitz
     * zulaufend. Um die Zellmitte liegt immer eine große Heimatinsel, auf der die Rifts landen.
     */
    private boolean island(int x, int z, double n, double detail, int[] out) {
        double m = fbm(featureNoise, x * frequency * 1.3, z * frequency * 1.3, 3);
        double cx = x - Multiverse.cellCenter(id.cellX()), cz = z - Multiverse.cellCenter(id.cellZ());
        double home = Math.max(0, 1.0 - Math.sqrt(cx * cx + cz * cz) / 190.0);
        m = Math.max(m, home * 1.4 - 0.3);
        if (m < 0.08) return false;
        double k = Math.min(1.0, (m - 0.08) / 0.6);
        double layer = fbm(fieldNoise, x / 400.0, z / 400.0, 2) * 40 * (1.0 - home);
        int top = (int) (base + layer + amplitude * 0.25 * n + k * 6 + detail);
        int depth = (int) (3 + k * k * 34 + Math.abs(detail) * 2);
        out[0] = Mth.clamp(top - depth, -60, 300);
        out[1] = Mth.clamp(top, out[0] + 1, 300);
        return true;
    }

    /** Müllberge der Schrottwelt: -1..1, über 0 liegt eine Halde. */
    public double halde(int x, int z) {
        return fbm(featureNoise, x * frequency * 1.8, z * frequency * 1.8, 3) * 1.4;
    }

    /** Wrackfelder der Schrottwelt: dort liegen viele Wracks mit Xenit-Erz. */
    public boolean wreckField(int x, int z) {
        return fieldNoise.getValue(x / 260.0, z / 260.0) > 0.35;
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

    /** Seen: Säure in der toxischen Welt, sonst Wasser. */
    public BlockState fluidBlock() {
        return type == UniverseType.TOXISCH ? ModBlocks.SAEURE.get().defaultBlockState() : Blocks.WATER.defaultBlockState();
    }

    public BlockState stoneAt(int y) {
        if (palette.stone() == Blocks.STONE && y < 0) return Blocks.DEEPSLATE.defaultBlockState();
        return palette.stone().defaultBlockState();
    }

    public boolean isCave(int x, int y, int z) {
        if (!caves) return false;
        double n = caveNoise.noise(x / 26.0, y / 14.0, z / 26.0);
        return Math.abs(n) < 0.055;
    }

    public BlockState caveFluid() {
        return type == UniverseType.TOXISCH ? ModBlocks.SAEURE.get().defaultBlockState() : Blocks.LAVA.defaultBlockState();
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
        // Typ-Erz zuerst: Fluxkristall in der Kristallwelt, etwas Xenit in der Schrottwelt
        if (type == UniverseType.KRISTALL && roll < 220) return ModBlocks.FLUXKRISTALL_ORE.get().defaultBlockState();
        if (type == UniverseType.SCHROTT && roll < 60) {
            return (deep ? ModBlocks.DEEPSLATE_XENIT_ORE : ModBlocks.XENIT_ORE).get().defaultBlockState();
        }
        // T2: Schwebstein-Adern in den Inseln, Biomasse-Nester in der organischen Welt
        if (type == UniverseType.NIEDRIGGRAVITATION && roll < 180) return ModBlocks.SCHWEBSTEIN.get().defaultBlockState();
        if (type == UniverseType.ORGANISCH && roll < 120) return ModBlocks.BIOMASSE_BLOCK.get().defaultBlockState();
        if (roll < 300) return (deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
        if (roll < 470) return (deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
        if (roll < 590) return (deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE).defaultBlockState();
        if (roll < 680) return (deep ? ModBlocks.DEEPSLATE_TIN_ORE : ModBlocks.TIN_ORE).get().defaultBlockState();
        if (roll < 760) return (deep ? Blocks.DEEPSLATE_REDSTONE_ORE : Blocks.REDSTONE_ORE).defaultBlockState();
        if (roll < 810) return (deep ? Blocks.DEEPSLATE_GOLD_ORE : Blocks.GOLD_ORE).defaultBlockState();
        if (roll < 840) return (deep ? Blocks.DEEPSLATE_LAPIS_ORE : Blocks.LAPIS_ORE).defaultBlockState();
        if (roll < 856 && deep) return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
        return null;
    }

    // ------------------------------------------------------------------ Texte

    public String gravityText() {
        return String.format(Locale.GERMAN, "%.2f g", gravity);
    }

    public String title() {
        return "Universum " + id.code();
    }

    /** „Riesen-Mobs, Supraleitend“ */
    public MutableComponent modifierText() {
        MutableComponent c = Component.empty();
        for (int i = 0; i < modifiers.size(); i++) {
            if (i > 0) c.append(", ");
            c.append(modifiers.get(i).displayName());
        }
        return c;
    }
}
