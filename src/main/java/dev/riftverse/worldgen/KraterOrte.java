package dev.riftverse.worldgen;

import dev.riftverse.Riftverse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wo liegen die Meteoritenkrater? Die Position ergibt sich allein aus dem Welt-Seed und dem Structure-Set
 * {@code riftverse:meteoritenkrater} (random_spread, spacing 62, separation 56 Chunks), deshalb können
 * Xenit-Zone und Meteoritenpeiler sie berechnen, ohne Chunks zu laden.
 * <p>
 * Ein Krater entsteht in jeder Region, außer seine Mitte liegt in einem Fluss-Biom. Struktur, Zone und Peiler
 * prüfen das mit derselben Regel ({@link #isRiver}), damit alle drei übereinstimmen.
 */
public final class KraterOrte {
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, Riftverse.id("meteoritenkrater"));
    /** Radius der Xenit-Zone um den Krater: Dichte fällt linear von 100 % auf 0. */
    public static final int ZONE = 200;
    /** Höhe, in der das Biom der Kratermitte geprüft wird. */
    public static final int BIOME_Y = 64;

    private record Key(long seed, int regionX, int regionZ) {}

    private static final Map<Key, Optional<BlockPos>> CACHE = new ConcurrentHashMap<>();

    public static boolean isRiver(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_RIVER);
    }

    public static Holder<Biome> centerBiome(BiomeSource source, Climate.Sampler sampler, int x, int z) {
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(BIOME_Y), QuartPos.fromBlock(z), sampler);
    }

    @Nullable
    private static RandomSpreadStructurePlacement placement(ServerLevel level) {
        return level.registryAccess().registry(Registries.STRUCTURE_SET)
                .flatMap(r -> r.getOptional(SET))
                .map(StructureSet::placement)
                .filter(p -> p instanceof RandomSpreadStructurePlacement)
                .map(p -> (RandomSpreadStructurePlacement) p)
                .orElse(null);
    }

    /** Kratermitte in der Region (Einheit: spacing Chunks), oder null. */
    @Nullable
    private static BlockPos craterIn(ServerLevel level, RandomSpreadStructurePlacement placement, int regionX, int regionZ) {
        Key key = new Key(level.getSeed(), regionX, regionZ);
        Optional<BlockPos> cached = CACHE.get(key);
        if (cached != null) return cached.orElse(null);

        int spacing = placement.spacing();
        ChunkPos chunk = placement.getPotentialStructureChunk(level.getSeed(), regionX * spacing, regionZ * spacing);
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        BlockPos result = isRiver(centerBiome(source, sampler, x, z)) ? null : new BlockPos(x, BIOME_Y, z);

        if (CACHE.size() > 8192) CACHE.clear();
        CACHE.put(key, Optional.ofNullable(result));
        return result;
    }

    /**
     * Nächster Krater (Mitte, y = 64) im Umkreis von {@code regions} Regionen, oder null.
     * Nur in der Overworld.
     */
    @Nullable
    public static BlockPos nearest(ServerLevel level, int x, int z, int regions) {
        if (level.dimension() != Level.OVERWORLD) return null;
        RandomSpreadStructurePlacement placement = placement(level);
        if (placement == null) return null;
        int spacing = placement.spacing();
        int rx = Math.floorDiv(Math.floorDiv(x, 16), spacing);
        int rz = Math.floorDiv(Math.floorDiv(z, 16), spacing);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -regions; dx <= regions; dx++) {
            for (int dz = -regions; dz <= regions; dz++) {
                BlockPos c = craterIn(level, placement, rx + dx, rz + dz);
                if (c == null) continue;
                double d = horizontal(c, x, z);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        }
        return best;
    }

    /** Waagrechter Abstand zum nächsten Krater; sehr groß, wenn keiner in der Nähe ist. */
    public static double distanceToNearest(ServerLevel level, int x, int z) {
        BlockPos c = nearest(level, x, z, 1);
        return c == null ? Double.MAX_VALUE : horizontal(c, x, z);
    }

    public static double horizontal(BlockPos c, double x, double z) {
        double dx = c.getX() + 0.5 - x, dz = c.getZ() + 0.5 - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Dichtefaktor der Xenit-Zone: 1 am Krater, 0,25 bei 150 Blöcken, 0 ab 200 Blöcken. */
    public static double zoneWeight(double distance) {
        return Math.max(0.0, 1.0 - distance / ZONE);
    }

    private KraterOrte() {}
}
