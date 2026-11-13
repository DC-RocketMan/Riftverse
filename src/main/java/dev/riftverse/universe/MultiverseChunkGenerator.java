package dev.riftverse.universe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generiert alle Universen eines {@link UniverseType Typs}. Jede 4096×4096-Zelle wird mit ihrem eigenen
 * {@link UniverseProfile} gebaut; Zellgrenzen fallen immer auf Chunkgrenzen, daher gehört jeder Chunk genau
 * einem Universum.
 */
public class MultiverseChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MultiverseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            UniverseType.CODEC.fieldOf("universe").forGetter(gen -> gen.type)
    ).apply(instance, MultiverseChunkGenerator::new));

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final int NO_LAND = Integer.MIN_VALUE;

    private final UniverseType type;

    public MultiverseChunkGenerator(BiomeSource biomeSource, UniverseType type) {
        super(biomeSource);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public UniverseType type() {
        return type;
    }

    private UniverseProfile profileAt(int x, int z) {
        return UniverseProfile.get(new UniverseId(type, Multiverse.cellOf(x), Multiverse.cellOf(z)));
    }

    /** Oberster fester Block oder NO_LAND. */
    private int surfaceAt(UniverseProfile p, int x, int z, int[] col) {
        if (Multiverse.inBorder(x, z)) return NO_LAND;
        if (!p.column(x, z, col)) return NO_LAND;
        return Math.min(col[1], MIN_Y + HEIGHT - 24);
    }

    // ------------------------------------------------------------------ Hauptgenerierung

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        UniverseProfile p = profileAt(baseX, baseZ);
        int minY = chunk.getMinBuildHeight();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] col = new int[2];
        int[][] tops = new int[16][16];

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState surface = p.palette.surface().defaultBlockState();
        BlockState under = p.palette.under().defaultBlockState();
        BlockState seabed = p.palette.seabed().defaultBlockState();
        BlockState fluid = p.hasFluid() ? p.fluidBlock() : null;
        BlockState scrap = ModBlocks.SCHROTT_HAUFEN.get().defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState schwebstein = ModBlocks.SCHWEBSTEIN.get().defaultBlockState();
        boolean islands = p.type == UniverseType.NIEDRIGGRAVITATION;
        RandomSource rnd = RandomSource.create(UniverseId.mix(p.seed ^ cp.toLong() ^ 0x5EEDL));

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx, z = baseZ + lz;
                int top = surfaceAt(p, x, z, col);
                tops[lx][lz] = top;
                if (top == NO_LAND) continue;

                int bottom = Math.max(col[0], minY);
                boolean underwater = fluid != null && top < p.fluidLevel;
                // Schrottwelt: Die Müllberge tragen eine Kruste aus Schrott und Geröll
                double halde = p.type == UniverseType.SCHROTT && !underwater ? p.halde(x, z) : -1;

                for (int y = bottom; y <= top; y++) {
                    BlockState state;
                    if (y == minY) {
                        state = bedrock;
                    } else if (islands && y <= bottom + 2 && y < top - 3) {
                        state = schwebstein;   // die Unterseite der Inseln besteht aus Schwebstein
                    } else if (y == top) {
                        if (underwater) state = seabed;
                        else if (halde > 0.15) state = rnd.nextFloat() < 0.35f ? scrap : gravel;
                        else state = surface;
                    } else if (y >= top - 3) {
                        if (underwater) state = seabed;
                        else if (halde > 0.3 && rnd.nextFloat() < 0.2f) state = scrap;
                        else state = under;
                    } else {
                        if (y < top - 6 && y > minY + 2 && p.isCave(x, y, z)) {
                            if (y < minY + 10) chunk.setBlockState(pos.set(x, y, z), p.caveFluid(), false);
                            continue;
                        }
                        BlockState ore = p.oreAt(x, y, z);
                        state = ore != null ? ore : p.stoneAt(y);
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }

                if (underwater) {
                    for (int y = top + 1; y <= p.fluidLevel; y++) {
                        chunk.setBlockState(pos.set(x, y, z), fluid, false);
                    }
                }
            }
        }

        switch (p.type) {
            case SCHROTT -> decorateScrap(chunk, p, tops, pos);
            case KRISTALL -> decorateCrystal(chunk, p, tops, pos);
            case TOXISCH -> decorateToxic(chunk, p, tops, pos);
            case NIEDRIGGRAVITATION -> decorateFloating(chunk, p, tops, pos);
            case ZEITDILATATION -> decorateTime(chunk, p, tops, pos);
            case ORGANISCH -> decorateOrganic(chunk, p, tops, pos);
            case HOCHGRAVITATION -> decorateHeavy(chunk, p, tops, pos);
            case SPIEGEL -> decorateMirror(chunk, p, tops, pos);
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private static RandomSource decoRandom(UniverseProfile p, ChunkAccess chunk) {
        return RandomSource.create(UniverseId.mix(p.seed ^ chunk.getPos().toLong()));
    }

    private static boolean dryLand(UniverseProfile p, int top) {
        return top != NO_LAND && !(p.hasFluid() && top < p.fluidLevel);
    }

    // ------------------------------------------------------------------ Schrottwelt

    private static final Block[] HULL = {Blocks.IRON_BLOCK, Blocks.IRON_BLOCK, Blocks.OXIDIZED_CUT_COPPER,
            Blocks.WEATHERED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.POLISHED_DEEPSLATE};

    private void decorateScrap(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        int cx = cp.getMinBlockX() + 8, cz = cp.getMinBlockZ() + 8;
        boolean field = p.wreckField(cx, cz);
        // Wracks: im Wrackfeld fast in jedem Chunk, sonst selten
        float chance = field ? 0.55f : p.featureDensity * 0.25f;
        if (rnd.nextFloat() < chance) wreck(chunk, p, tops, pos, rnd, field);

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                float roll = rnd.nextFloat();
                if (roll < p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    if (ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.RED_SAND) || ground.is(Blocks.MUD)) {
                        chunk.setBlockState(pos.set(x, top + 1, z), Blocks.DEAD_BUSH.defaultBlockState(), false);
                    }
                } else if (roll < p.plantDensity + 0.006f) {
                    chunk.setBlockState(pos.set(x, top + 1, z), ModBlocks.SCHROTT_HAUFEN.get().defaultBlockState(), false);
                }
            }
        }
    }

    /** Ein halb vergrabenes Alien-Wrack, darunter Xenit-Erz. */
    private void wreck(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos, RandomSource rnd, boolean field) {
        int length = 5 + rnd.nextInt(5);
        boolean alongX = rnd.nextBoolean();
        int sx = 3 + rnd.nextInt(Math.max(1, 11 - (alongX ? length : 2)));
        int sz = 3 + rnd.nextInt(Math.max(1, 11 - (alongX ? 2 : length)));
        int top = tops[sx][sz];
        if (!dryLand(p, top)) return;
        int baseY = top - 1;
        ChunkPos cp = chunk.getPos();
        int ox = cp.getMinBlockX(), oz = cp.getMinBlockZ();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int i = 0; i < length; i++) {
            int rise = i * 2 / length;   // Bug steckt im Boden, Heck ragt heraus
            for (int w = -1; w <= 1; w++) {
                for (int h = 0; h <= 2; h++) {
                    int lx = alongX ? sx + i : sx + w;
                    int lz = alongX ? sz + w : sz + i;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    boolean shell = w != 0 || h != 1 || i == 0 || i == length - 1;
                    pos.set(ox + lx, baseY + h + rise, oz + lz);
                    if (!shell) {
                        chunk.setBlockState(pos, rnd.nextFloat() < 0.3f ? ModBlocks.SCHROTT_HAUFEN.get().defaultBlockState() : air, false);
                    } else if (rnd.nextFloat() < 0.78f) {
                        Block b = rnd.nextFloat() < 0.18f ? ModBlocks.SCHROTT_HAUFEN.get() : HULL[rnd.nextInt(HULL.length)];
                        chunk.setBlockState(pos, b.defaultBlockState(), false);
                    }
                }
            }
        }
        // Xenit im Einschlagsboden unter dem Wrack
        int ores = field ? 3 + rnd.nextInt(4) : 1 + rnd.nextInt(2);
        for (int k = 0; k < ores; k++) {
            int lx = Math.min(15, Math.max(0, sx + rnd.nextInt(alongX ? length : 3) - (alongX ? 0 : 1)));
            int lz = Math.min(15, Math.max(0, sz + rnd.nextInt(alongX ? 3 : length) - (alongX ? 1 : 0)));
            int y = baseY - 1 - rnd.nextInt(4);
            Block ore = y < 0 ? ModBlocks.DEEPSLATE_XENIT_ORE.get() : ModBlocks.XENIT_ORE.get();
            chunk.setBlockState(pos.set(ox + lx, y, oz + lz), ore.defaultBlockState(), false);
        }
        // Trümmer rundherum
        for (int k = 0; k < 6; k++) {
            int lx = rnd.nextInt(16), lz = rnd.nextInt(16);
            int t = tops[lx][lz];
            if (!dryLand(p, t)) continue;
            pos.set(ox + lx, t + 1, oz + lz);
            if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, ModBlocks.SCHROTT_HAUFEN.get().defaultBlockState(), false);
        }
    }

    // ------------------------------------------------------------------ Kristallwelt

    private void decorateCrystal(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState cluster = ModBlocks.FLUXKRISTALL_CLUSTER.get().defaultBlockState();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                boolean inner = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
                if (inner && rnd.nextFloat() < p.featureDensity) {
                    crystalSpire(chunk, rnd, pos, x, top + 1, z);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity && chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                    chunk.setBlockState(pos, cluster.setValue(AmethystClusterBlock.FACING, Direction.UP), false);
                }
            }
        }
    }

    /** Kristallnadel: Amethyst mit knospenden Fluxkristallen, an denen Cluster wachsen. */
    private void crystalSpire(ChunkAccess chunk, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        BlockState body = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState bud = ModBlocks.KNOSPENDER_FLUXKRISTALL.get().defaultBlockState();
        BlockState glow = ModBlocks.FLUXKRISTALL_BLOCK.get().defaultBlockState();
        BlockState cluster = ModBlocks.FLUXKRISTALL_CLUSTER.get().defaultBlockState();
        int height = 4 + rnd.nextInt(8);
        for (int i = 0; i < height; i++) {
            boolean budHere = i > 0 && i < height - 1 && rnd.nextFloat() < 0.35f;
            chunk.setBlockState(pos.set(x, y + i, z), budHere ? bud : body, false);
            if (i < height / 2) {
                if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x + 1, y + i, z), body);
                if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x, y + i, z + 1), body);
            }
            if (budHere) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (rnd.nextFloat() < 0.5f) {
                        setIfAir(chunk, pos.set(x + d.getStepX(), y + i, z + d.getStepZ()),
                                cluster.setValue(AmethystClusterBlock.FACING, d));
                    }
                }
            }
        }
        chunk.setBlockState(pos.set(x, y + height, z), glow, false);
        setIfAir(chunk, pos.set(x, y + height + 1, z), cluster.setValue(AmethystClusterBlock.FACING, Direction.UP));
    }

    // ------------------------------------------------------------------ Toxische Welt

    private void decorateToxic(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                boolean inner = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
                if (inner && rnd.nextFloat() < p.featureDensity) {
                    deadTree(chunk, rnd, pos, x, top + 1, z);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    BlockState plant = toxicPlant(ground, rnd);
                    if (plant != null) chunk.setBlockState(pos.set(x, top + 1, z), plant, false);
                }
            }
        }
    }

    private static BlockState toxicPlant(BlockState ground, RandomSource rnd) {
        if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK)) {
            int r = rnd.nextInt(10);
            if (r < 6) return Blocks.SHORT_GRASS.defaultBlockState();
            if (r < 8) return Blocks.FERN.defaultBlockState();
            return Blocks.BROWN_MUSHROOM.defaultBlockState();
        }
        if (ground.is(Blocks.MYCELIUM)) return (rnd.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
        if (ground.is(Blocks.MUD)) return rnd.nextBoolean() ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.BROWN_MUSHROOM.defaultBlockState();
        return null;
    }

    /** Abgestorbener Baum ohne Laub, mit ein, zwei kahlen Ästen. */
    private void deadTree(ChunkAccess chunk, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        Block log = rnd.nextBoolean() ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.MANGROVE_LOG;
        int height = 3 + rnd.nextInt(5);
        for (int i = 0; i < height; i++) chunk.setBlockState(pos.set(x, y + i, z), log.defaultBlockState(), false);
        int branches = 1 + rnd.nextInt(2);
        for (int b = 0; b < branches; b++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
            int by = y + height - 1 - rnd.nextInt(2);
            BlockState branch = log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis());
            for (int k = 1; k <= 1 + rnd.nextInt(2); k++) {
                setIfAir(chunk, pos.set(x + d.getStepX() * k, by + (k == 2 ? 1 : 0), z + d.getStepZ() * k), branch);
            }
        }
    }

    // ------------------------------------------------------------------ Niedriggravitationswelt

    private void decorateFloating(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState schwebstein = ModBlocks.SCHWEBSTEIN.get().defaultBlockState();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (top == NO_LAND) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                boolean inner = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
                float roll = rnd.nextFloat();
                if (inner && roll < p.featureDensity) {
                    // Schwebende Brocken: ein paar Blöcke über dem Boden, ohne Verbindung
                    int y = top + 3 + rnd.nextInt(6);
                    setIfAir(chunk, pos.set(x, y, z), schwebstein);
                    if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x + 1, y, z), schwebstein);
                    if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x, y + 1, z), schwebstein);
                    continue;
                }
                if (roll < p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK)) {
                        BlockState plant = switch (rnd.nextInt(6)) {
                            case 0 -> Blocks.AZURE_BLUET.defaultBlockState();
                            case 1 -> Blocks.WHITE_TULIP.defaultBlockState();
                            default -> Blocks.SHORT_GRASS.defaultBlockState();
                        };
                        chunk.setBlockState(pos.set(x, top + 1, z), plant, false);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Zeitdilatationswelt

    private void decorateTime(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState sand = ModBlocks.CHRONOGLAS_SAND.get().defaultBlockState();
        BlockState glass = ModBlocks.CHRONOGLAS.get().defaultBlockState();
        // Ein Feld aus Chronoglas-Sand pro paar Chunks, das in der Zeitlupe nachrieselt
        if (rnd.nextFloat() < 0.35f) {
            int cx = 3 + rnd.nextInt(10), cz = 3 + rnd.nextInt(10), r = 2 + rnd.nextInt(3);
            for (int lx = Math.max(0, cx - r); lx <= Math.min(15, cx + r); lx++) {
                for (int lz = Math.max(0, cz - r); lz <= Math.min(15, cz + r); lz++) {
                    if ((lx - cx) * (lx - cx) + (lz - cz) * (lz - cz) > r * r) continue;
                    int top = tops[lx][lz];
                    if (!dryLand(p, top)) continue;
                    int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                    for (int d = 0; d < 2 + rnd.nextInt(2); d++) chunk.setBlockState(pos.set(x, top - d, z), sand, false);
                }
            }
        }
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                boolean inner = lx >= 1 && lx <= 14 && lz >= 1 && lz <= 14;
                if (inner && rnd.nextFloat() < p.featureDensity) {
                    // Eingefrorene Zeit: Säule aus Chronoglas
                    int h = 2 + rnd.nextInt(5);
                    for (int i = 0; i < h; i++) chunk.setBlockState(pos.set(x, top + 1 + i, z), glass, false);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    BlockState plant = null;
                    if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.ROOTED_DIRT)) {
                        plant = switch (rnd.nextInt(8)) {
                            case 0 -> Blocks.SWEET_BERRY_BUSH.defaultBlockState();
                            case 1 -> Blocks.ALLIUM.defaultBlockState();
                            case 2 -> Blocks.FERN.defaultBlockState();
                            default -> Blocks.SHORT_GRASS.defaultBlockState();
                        };
                    } else if (ground.is(Blocks.SAND)) {
                        plant = rnd.nextInt(4) == 0 ? Blocks.CACTUS.defaultBlockState() : Blocks.DEAD_BUSH.defaultBlockState();
                    }
                    if (plant != null) chunk.setBlockState(pos.set(x, top + 1, z), plant, false);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Hochgravitationswelt

    /** Plattgedrückte Ebenen: Gravitit-Brocken ragen aus dem Boden, sonst kaum etwas wächst hoch. */
    private void decorateHeavy(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState gravitit = ModBlocks.GRAVITIT_ORE.get().defaultBlockState();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                float roll = rnd.nextFloat();
                if (roll < p.featureDensity) {
                    // halb versunkener Gravitit-Brocken
                    chunk.setBlockState(pos.set(x, top, z), gravitit, false);
                    chunk.setBlockState(pos.set(x, top - 1, z), gravitit, false);
                } else if (roll < p.featureDensity + p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    if (!ground.is(Blocks.DEEPSLATE)) chunk.setBlockState(pos.set(x, top + 1, z), Blocks.DEAD_BUSH.defaultBlockState(), false);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Spiegelwelt

    /** Spiegelkristall-Nadeln und helle Blumen. */
    private void decorateMirror(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState kristall = ModBlocks.SPIEGELKRISTALL.get().defaultBlockState();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                boolean inner = lx >= 1 && lx <= 14 && lz >= 1 && lz <= 14;
                if (inner && rnd.nextFloat() < p.featureDensity) {
                    int h = 1 + rnd.nextInt(3);
                    for (int i = 0; i < h; i++) chunk.setBlockState(pos.set(x, top + 1 + i, z), kristall, false);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    BlockState plant = null;
                    if (ground.is(Blocks.GRASS_BLOCK)) {
                        plant = switch (rnd.nextInt(5)) {
                            case 0 -> Blocks.WHITE_TULIP.defaultBlockState();
                            case 1 -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
                            case 2 -> Blocks.AZURE_BLUET.defaultBlockState();
                            default -> Blocks.SHORT_GRASS.defaultBlockState();
                        };
                    } else if (ground.is(Blocks.SNOW_BLOCK)) {
                        plant = Blocks.SNOW.defaultBlockState();
                    }
                    if (plant != null) chunk.setBlockState(pos.set(x, top + 1, z), plant, false);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Organische Welt

    private void decorateOrganic(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        RandomSource rnd = decoRandom(p, chunk);
        ChunkPos cp = chunk.getPos();
        BlockState flora = ModBlocks.FRESSENDE_FLORA.get().defaultBlockState();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (!dryLand(p, top)) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                boolean inner = lx >= 3 && lx <= 12 && lz >= 3 && lz <= 12;
                if (inner && rnd.nextFloat() < p.featureDensity) {
                    rib(chunk, rnd, pos, x, top + 1, z, bone);
                    continue;
                }
                float roll = rnd.nextFloat();
                if (roll < p.plantDensity) {
                    chunk.setBlockState(pos.set(x, top + 1, z), flora, false);
                } else if (roll < p.plantDensity + 0.004f) {
                    chunk.setBlockState(pos.set(x, top, z), Blocks.SHROOMLIGHT.defaultBlockState(), false);
                } else if (roll < p.plantDensity + 0.05f) {
                    BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                    if (ground.is(Blocks.MYCELIUM) || ground.is(Blocks.MOSS_BLOCK)) {
                        chunk.setBlockState(pos.set(x, top + 1, z), (rnd.nextBoolean() ? Blocks.CRIMSON_ROOTS : Blocks.BROWN_MUSHROOM).defaultBlockState(), false);
                    }
                }
            }
        }
    }

    /** Ein Rippenbogen aus Knochenblöcken, als läge hier ein riesiges Skelett. */
    private void rib(ChunkAccess chunk, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState bone) {
        boolean alongX = rnd.nextBoolean();
        int span = 2 + rnd.nextInt(2);
        int height = 3 + rnd.nextInt(3);
        for (int s = -span; s <= span; s++) {
            double t = (double) s / span;
            int h = (int) Math.round(height * (1 - t * t));
            int bx = alongX ? x + s : x, bz = alongX ? z : z + s;
            for (int i = 0; i <= h; i++) setIfAir(chunk, pos.set(bx, y + i, bz), bone);
        }
    }

    private static void setIfAir(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, state, false);
    }

    // ------------------------------------------------------------------ Pflicht-Overrides

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // Höhlen werden direkt in fillFromNoise erzeugt.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // Oberfläche wird in fillFromNoise gesetzt.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        UniverseProfile p = profileAt(x, z);
        int top = surfaceAt(p, x, z, new int[2]);
        if (top == NO_LAND) return level.getMinBuildHeight();
        boolean floorOnly = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        if (!floorOnly && p.hasFluid() && top < p.fluidLevel) return p.fluidLevel + 1;
        return top + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minY = level.getMinBuildHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        UniverseProfile p = profileAt(x, z);
        int[] col = new int[2];
        int top = surfaceAt(p, x, z, col);
        if (top != NO_LAND) {
            for (int y = Math.max(col[0], minY); y <= top; y++) states[y - minY] = p.stoneAt(y);
            states[top - minY] = p.palette.surface().defaultBlockState();
            if (p.hasFluid()) {
                for (int y = top + 1; y <= p.fluidLevel && y - minY < states.length; y++) states[y - minY] = p.fluidBlock();
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        UniverseProfile p = profileAt(pos.getX(), pos.getZ());
        info.add("Riftverse: " + p.title() + " \"" + p.name + "\" | " + type.getSerializedName() + " | "
                + p.terrain.label + " | " + p.modifiers);
    }
}
