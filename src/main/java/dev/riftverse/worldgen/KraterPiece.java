package dev.riftverse.worldgen;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Der Krater selbst: eine Schüssel aus Einschlaggestein mit Auswurfwall, 8–12 sichtbaren Xenit-Erzen am Grund,
 * dem Meteoritenkern in der Mitte und darunter eine trichterförmige Einschlagsader aus 20–30 Erz.
 * <p>
 * Alles ist aus dem Seed des Stücks berechnet, damit jeder Chunk denselben Krater sieht.
 */
public class KraterPiece extends StructurePiece {
    private static final Block[] ROCK = {Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.TUFF,
            Blocks.TUFF, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.SMOOTH_BASALT};
    private static final Block[] EJECTA = {Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.BLACKSTONE, Blocks.TUFF, Blocks.GRAVEL};
    private static final int FUNNEL_DEPTH = 24;

    private final BlockPos center;
    private final int radius;
    private final long seed;
    private final boolean sunken;
    private final int seaLevel;

    public KraterPiece(BlockPos center, int radius, long seed, boolean sunken, int seaLevel) {
        super(ModWorldgen.KRATER_PIECE.get(), 0, box(center, radius));
        this.center = center;
        this.radius = radius;
        this.seed = seed;
        this.sunken = sunken;
        this.seaLevel = seaLevel;
    }

    public KraterPiece(CompoundTag tag) {
        super(ModWorldgen.KRATER_PIECE.get(), tag);
        this.center = BlockPos.of(tag.getLong("Center"));
        this.radius = tag.getInt("Radius");
        this.seed = tag.getLong("Seed");
        this.sunken = tag.getBoolean("Sunken");
        this.seaLevel = tag.getInt("SeaLevel");
    }

    private static BoundingBox box(BlockPos c, int radius) {
        int r = (int) Math.ceil(radius * 1.6) + 1;
        return new BoundingBox(c.getX() - r, c.getY() - FUNNEL_DEPTH - 8, c.getZ() - r, c.getX() + r, c.getY() + 14, c.getZ() + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        tag.putLong("Seed", seed);
        tag.putBoolean("Sunken", sunken);
        tag.putInt("SeaLevel", seaLevel);
    }

    private int depth() {
        return Math.max(3, Math.round(radius * 0.6f));
    }

    private int floorY(double d) {
        return center.getY() - (int) Math.round(depth() * (1 - d * d));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        Set<Long> visible = visibleOres(r);
        List<BlockPos> vein = impactVein(r);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        int cx = center.getX(), cz = center.getZ(), cy = center.getY();
        double rim = radius * 1.6;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / radius;
                if (d >= rim / radius) continue;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (d < 1.0) {
                    int floor = floorY(d);
                    int top = Math.min(Math.max(ground, cy) + 2, cy + 12);
                    for (int y = floor + 1; y <= top; y++) {
                        pos.set(x, y, z);
                        if (!box.isInside(pos)) continue;
                        boolean wet = sunken ? y < seaLevel : !level.getFluidState(pos).isEmpty() && y < seaLevel;
                        level.setBlock(pos, wet ? water : air, 2);
                    }
                    for (int y = floor - 2; y <= floor; y++) {
                        pos.set(x, y, z);
                        if (!box.isInside(pos)) continue;
                        Block b = visible.contains(BlockPos.asLong(x, 0, z)) && y == floor ? ore(y) : rock(x, y, z);
                        level.setBlock(pos, b.defaultBlockState(), 2);
                    }
                } else if (!sunken) {
                    // Auswurfwall: bis zu 2 Blöcke hoch, nach außen flacher
                    int height = (int) Math.round(2.2 * (1 - (d - 1.0) / (rim / radius - 1.0)));
                    for (int k = 1; k <= height; k++) {
                        pos.set(x, ground + k, z);
                        if (!box.isInside(pos) || !level.getBlockState(pos).canBeReplaced()) continue;
                        int h = (int) Math.floorMod(BlockPos.asLong(x, ground + k, z) * 31 + seed, (long) EJECTA.length);
                        level.setBlock(pos, EJECTA[h].defaultBlockState(), 2);
                    }
                }
            }
        }

        // Meteoritenkern in der Mitte, halb im Grund
        pos.set(cx, floorY(0), cz);
        if (box.isInside(pos)) level.setBlock(pos, ModBlocks.METEORITENKERN.get().defaultBlockState(), 2);

        // Einschlagsader unter dem Krater
        for (BlockPos p : vein) {
            if (!box.isInside(p)) continue;
            BlockState there = level.getBlockState(p);
            if (there.isAir() || !there.getFluidState().isEmpty() || there.is(Blocks.BEDROCK)) continue;
            Block ore = there.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || p.getY() < 0
                    ? ModBlocks.DEEPSLATE_XENIT_ORE.get() : ModBlocks.XENIT_ORE.get();
            level.setBlock(p, ore.defaultBlockState(), 2);
        }
    }

    private Block ore(int y) {
        return y < 0 ? ModBlocks.DEEPSLATE_XENIT_ORE.get() : ModBlocks.XENIT_ORE.get();
    }

    private Block rock(int x, int y, int z) {
        int h = (int) Math.floorMod(BlockPos.asLong(x, y, z) * 0x9E3779B1L + seed, (long) ROCK.length);
        return ROCK[h];
    }

    /** 8–12 Spalten am Grund mit sichtbarem Erz (Schlüssel: x/z, y = 0). */
    private Set<Long> visibleOres(RandomSource r) {
        int count = 8 + r.nextInt(5);
        Set<Long> set = new HashSet<>();
        for (int i = 0; i < 200 && set.size() < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double dist = (0.15 + r.nextDouble() * 0.7) * radius;
            int x = center.getX() + (int) Math.round(Math.cos(a) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(a) * dist);
            if (x == center.getX() && z == center.getZ()) continue;
            set.add(BlockPos.asLong(x, 0, z));
        }
        return set;
    }

    /** 20–30 Erz, trichterförmig: oben breit, nach unten schmal. */
    private List<BlockPos> impactVein(RandomSource r) {
        int count = 20 + r.nextInt(11);
        int top = floorY(0) - 1;
        List<BlockPos> list = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 400 && list.size() < count; i++) {
            int k = r.nextInt(FUNNEL_DEPTH);
            double width = 3.0 * (1.0 - k / (double) FUNNEL_DEPTH) + 0.5;
            int dx = (int) Math.round((r.nextDouble() * 2 - 1) * width);
            int dz = (int) Math.round((r.nextDouble() * 2 - 1) * width);
            BlockPos p = new BlockPos(center.getX() + dx, top - k, center.getZ() + dz);
            if (seen.add(p.asLong())) list.add(p);
        }
        return list;
    }
}
