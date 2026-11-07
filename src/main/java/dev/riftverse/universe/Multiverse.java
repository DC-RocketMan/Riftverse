package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Das Multiversum besteht aus vier Dimensionen (unterschiedliche Himmel/Lichtregeln).
 * Jede Dimension ist in ein Raster aus Zellen von {@link #CELL} Blöcken aufgeteilt –
 * jede Zelle ist ein komplett eigenes, zufällig generiertes Universum.
 * Zwischen den Zellen liegt ein Abgrund aus Leere (das „Ende des Universums“).
 */
public final class Multiverse {
    public static final int CELL = 4096;
    public static final int BORDER = 40;
    /** Zellen pro Richtung → (2*150+1)² * 4 ≈ 360.000 Universen. */
    public static final int MAX_CELL = 150;

    public static final List<ResourceKey<Level>> DIMENSIONS = List.of(
            dim("verse_sun"),      // 0: Tag/Nacht wie gewohnt
            dim("verse_night"),    // 1: ewige Nacht
            dim("verse_inferno"),  // 2: Höllen-Atmosphäre, Wasser verdampft
            dim("verse_void")      // 3: Sternenleere, schwebende Inseln/Mondlandschaften
    );

    private static ResourceKey<Level> dim(String name) {
        return ResourceKey.create(Registries.DIMENSION, Riftverse.id(name));
    }

    public static ResourceKey<Level> dimension(int variant) {
        return DIMENSIONS.get(Math.floorMod(variant, DIMENSIONS.size()));
    }

    public static int variantOf(ResourceKey<Level> key) {
        return DIMENSIONS.indexOf(key);
    }

    public static boolean isMultiverse(Level level) {
        return variantOf(level.dimension()) >= 0;
    }

    public static int cellOf(int blockCoord) {
        return Math.floorDiv(blockCoord, CELL);
    }

    public static int cellCenter(int cell) {
        return cell * CELL + CELL / 2;
    }

    /** Liegt die Position im leeren Grenzbereich zwischen zwei Universen? */
    public static boolean inBorder(int x, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        return lx < BORDER || lz < BORDER || lx >= CELL - BORDER || lz >= CELL - BORDER;
    }

    @Nullable
    public static UniverseId at(Level level, BlockPos pos) {
        int variant = variantOf(level.dimension());
        if (variant < 0) return null;
        return new UniverseId(variant, cellOf(pos.getX()), cellOf(pos.getZ()));
    }

    public static UniverseId random(RandomSource random) {
        int variant = random.nextInt(DIMENSIONS.size());
        int cx = random.nextInt(MAX_CELL * 2 + 1) - MAX_CELL;
        int cz = random.nextInt(MAX_CELL * 2 + 1) - MAX_CELL;
        return new UniverseId(variant, cx, cz);
    }

    private Multiverse() {}
}
