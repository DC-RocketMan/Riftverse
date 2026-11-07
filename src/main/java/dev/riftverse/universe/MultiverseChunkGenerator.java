package dev.riftverse.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
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
 * Generiert alle Universen einer Rift-Dimension. Jede 4096×4096-Zelle wird mit
 * ihrem eigenen {@link UniverseProfile} gebaut; Zellgrenzen fallen immer auf
 * Chunkgrenzen, daher gehört jeder Chunk genau einem Universum.
 */
public class MultiverseChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MultiverseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            Codec.INT.fieldOf("variant").forGetter(gen -> gen.variant)
    ).apply(instance, MultiverseChunkGenerator::new));

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final int NO_LAND = Integer.MIN_VALUE;

    private final int variant;

    public MultiverseChunkGenerator(BiomeSource biomeSource, int variant) {
        super(biomeSource);
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private UniverseProfile profileAt(int x, int z) {
        return UniverseProfile.get(new UniverseId(variant, Multiverse.cellOf(x), Multiverse.cellOf(z)));
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
        BlockState ice = Blocks.ICE.defaultBlockState();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx, z = baseZ + lz;
                int top = surfaceAt(p, x, z, col);
                tops[lx][lz] = top;
                if (top == NO_LAND) continue;

                int bottom = Math.max(col[0], minY);
                boolean underwater = fluid != null && top < p.fluidLevel;

                for (int y = bottom; y <= top; y++) {
                    BlockState state;
                    if (y == minY) {
                        state = bedrock;
                    } else if (y == top) {
                        state = underwater ? seabed : surface;
                    } else if (y >= top - 3) {
                        state = underwater ? seabed : under;
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
                        BlockState s = (p.iceCap() && y == p.fluidLevel) ? ice : fluid;
                        chunk.setBlockState(pos.set(x, y, z), s, false);
                    }
                }
            }
        }

        decorate(chunk, p, tops, pos);
        return CompletableFuture.completedFuture(chunk);
    }

    // ------------------------------------------------------------------ Flora & Deko

    private record TreeKind(Block log, Block crown) {}

    private static final List<TreeKind> TREES = List.of(
            new TreeKind(Blocks.OAK_LOG, Blocks.OAK_LEAVES),
            new TreeKind(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES),
            new TreeKind(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES),
            new TreeKind(Blocks.DARK_OAK_LOG, Blocks.AZALEA_LEAVES),
            new TreeKind(Blocks.MANGROVE_LOG, Blocks.FLOWERING_AZALEA_LEAVES),
            new TreeKind(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES),
            new TreeKind(Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK),
            new TreeKind(Blocks.CRIMSON_STEM, Blocks.NETHER_WART_BLOCK),
            new TreeKind(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES));

    private static final List<TreeKind> GLOW_TREES = List.of(
            new TreeKind(Blocks.WARPED_STEM, Blocks.SHROOMLIGHT),
            new TreeKind(Blocks.CRIMSON_STEM, Blocks.OCHRE_FROGLIGHT),
            new TreeKind(Blocks.BONE_BLOCK, Blocks.VERDANT_FROGLIGHT),
            new TreeKind(Blocks.MUSHROOM_STEM, Blocks.PEARLESCENT_FROGLIGHT),
            new TreeKind(Blocks.STRIPPED_WARPED_STEM, Blocks.SEA_LANTERN),
            new TreeKind(Blocks.BLACKSTONE_WALL, Blocks.GLOWSTONE),
            new TreeKind(Blocks.QUARTZ_PILLAR, Blocks.SHROOMLIGHT),
            new TreeKind(Blocks.DARK_OAK_LOG, Blocks.VERDANT_FROGLIGHT),
            new TreeKind(Blocks.BAMBOO_BLOCK, Blocks.OCHRE_FROGLIGHT));

    private void decorate(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        ChunkPos cp = chunk.getPos();
        RandomSource rnd = RandomSource.create(UniverseId.mix(p.seed ^ cp.toLong()));
        BlockState snow = Blocks.SNOW.defaultBlockState();
        boolean cold = p.temperature == UniverseProfile.Temperature.COLD;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                if (top == NO_LAND) continue;
                if (p.hasFluid() && top < p.fluidLevel) continue;
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                BlockState ground = chunk.getBlockState(pos.set(x, top, z));
                if (!ground.is(p.palette.surface())) continue;

                boolean inner = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
                if (inner && rnd.nextFloat() < p.treeDensity) {
                    placeBig(chunk, p, rnd, pos, x, top + 1, z);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity) {
                    BlockState plant = smallPlant(ground.getBlock(), p, rnd);
                    if (plant != null && chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                        chunk.setBlockState(pos, plant, false);
                        continue;
                    }
                }
                if (cold && chunk.getBlockState(pos.set(x, top + 1, z)).isAir() && !ground.is(Blocks.SNOW_BLOCK)) {
                    chunk.setBlockState(pos, snow, false);
                }
            }
        }
    }

    private static BlockState persistent(Block block) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(LeavesBlock.PERSISTENT)) s = s.setValue(LeavesBlock.PERSISTENT, true);
        return s;
    }

    private void placeBig(ChunkAccess chunk, UniverseProfile p, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        switch (p.flora) {
            case TREES -> tree(chunk, TREES.get(p.treeStyle % TREES.size()), rnd, pos, x, y, z, false);
            case GLOW_TREES -> tree(chunk, GLOW_TREES.get(p.treeStyle % GLOW_TREES.size()), rnd, pos, x, y, z, true);
            case MUSHROOMS -> mushroom(chunk, rnd, pos, x, y, z);
            case CRYSTALS -> crystal(chunk, p, rnd, pos, x, y, z);
            case DESERT -> cactus(chunk, rnd, pos, x, y, z);
            default -> { }
        }
    }

    private void tree(ChunkAccess chunk, TreeKind kind, RandomSource rnd, BlockPos.MutableBlockPos pos,
                      int x, int y, int z, boolean glow) {
        int height = 4 + rnd.nextInt(glow ? 6 : 4);
        BlockState log = kind.log().defaultBlockState();
        for (int i = 0; i < height; i++) chunk.setBlockState(pos.set(x, y + i, z), log, false);
        BlockState crown = persistent(kind.crown());
        int cy = y + height;
        if (glow) {
            // Schirmform mit Leuchtkern
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= 3) setIfAir(chunk, pos.set(x + dx, cy, z + dz), crown);
            setIfAir(chunk, pos.set(x, cy + 1, z), crown);
        } else {
            int radius = 2;
            for (int dy = -1; dy <= 2; dy++)
                for (int dx = -radius; dx <= radius; dx++)
                    for (int dz = -radius; dz <= radius; dz++) {
                        double d = dx * dx + dz * dz + (dy - 0.5) * (dy - 0.5) * 1.6;
                        if (d <= radius * radius + 1.2 && rnd.nextFloat() > 0.08f)
                            setIfAir(chunk, pos.set(x + dx, cy + dy, z + dz), crown);
                    }
        }
    }

    private void mushroom(ChunkAccess chunk, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        int height = 4 + rnd.nextInt(4);
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        BlockState cap = (rnd.nextBoolean() ? Blocks.RED_MUSHROOM_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK).defaultBlockState();
        for (int i = 0; i < height; i++) chunk.setBlockState(pos.set(x, y + i, z), stem, false);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                if (dx * dx + dz * dz <= 5) setIfAir(chunk, pos.set(x + dx, y + height, z + dz), cap);
    }

    private void crystal(ChunkAccess chunk, UniverseProfile p, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        boolean ice = p.temperature == UniverseProfile.Temperature.COLD && rnd.nextBoolean();
        BlockState body = (ice ? Blocks.PACKED_ICE : Blocks.AMETHYST_BLOCK).defaultBlockState();
        int height = 3 + rnd.nextInt(7);
        for (int i = 0; i < height; i++) {
            chunk.setBlockState(pos.set(x, y + i, z), body, false);
            if (i < height / 2) {
                if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x + 1, y + i, z), body);
                if (rnd.nextBoolean()) setIfAir(chunk, pos.set(x, y + i, z + 1), body);
            }
        }
        if (!ice) setIfAir(chunk, pos.set(x, y + height, z), Blocks.AMETHYST_CLUSTER.defaultBlockState());
    }

    private void cactus(ChunkAccess chunk, RandomSource rnd, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        BlockState below = chunk.getBlockState(pos.set(x, y - 1, z));
        if (!below.is(Blocks.SAND) && !below.is(Blocks.RED_SAND)) {
            setIfAir(chunk, pos.set(x, y, z), Blocks.DEAD_BUSH.defaultBlockState());
            return;
        }
        int height = 1 + rnd.nextInt(3);
        for (int i = 0; i < height; i++) setIfAir(chunk, pos.set(x, y + i, z), Blocks.CACTUS.defaultBlockState());
    }

    private static void setIfAir(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, state, false);
    }

    private static BlockState smallPlant(Block ground, UniverseProfile p, RandomSource rnd) {
        if (ground == Blocks.GRASS_BLOCK || ground == Blocks.MOSS_BLOCK || ground == Blocks.MUD) {
            if (p.temperature == UniverseProfile.Temperature.COLD) return null;
            int r = rnd.nextInt(10);
            if (r < 6) return Blocks.SHORT_GRASS.defaultBlockState();
            if (r < 8) return Blocks.FERN.defaultBlockState();
            Block[] flowers = {Blocks.POPPY, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.BLUE_ORCHID, Blocks.OXEYE_DAISY};
            return flowers[rnd.nextInt(flowers.length)].defaultBlockState();
        }
        if (ground == Blocks.MYCELIUM) return (rnd.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
        if (ground == Blocks.SAND || ground == Blocks.RED_SAND || ground == Blocks.ORANGE_TERRACOTTA)
            return rnd.nextInt(4) == 0 ? Blocks.DEAD_BUSH.defaultBlockState() : null;
        if (ground == Blocks.WARPED_NYLIUM) {
            int r = rnd.nextInt(6);
            return (r < 3 ? Blocks.WARPED_ROOTS : r < 5 ? Blocks.NETHER_SPROUTS : Blocks.WARPED_FUNGUS).defaultBlockState();
        }
        if (ground == Blocks.CRIMSON_NYLIUM)
            return (rnd.nextInt(4) < 3 ? Blocks.CRIMSON_ROOTS : Blocks.CRIMSON_FUNGUS).defaultBlockState();
        return null;
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
        info.add("Riftverse: " + p.title() + " \"" + p.name + "\" | " + p.terrain.label + " | " + p.gravityText());
    }
}
