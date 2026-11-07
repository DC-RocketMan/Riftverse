package dev.riftverse.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Jeder Universumstyp ist eine Dimension, die in ein Raster aus Zellen von {@link #CELL} Blöcken aufgeteilt ist.
 * Jede Zelle ist ein komplett eigenes, zufällig generiertes Universum; zwischen den Zellen liegt ein Abgrund
 * aus Leere (das „Ende des Universums“).
 * <p>
 * Eine Zelle umfasst genau 8 × 8 Region-Dateien (4096 / 512), deshalb lässt sich ein Universum beim Aufräumen
 * sauber löschen, ohne seine Nachbarn zu berühren.
 */
public final class Multiverse {
    public static final int CELL = 4096;
    public static final int BORDER = 40;
    /** Region-Dateien (512 Blöcke) pro Zelle und Richtung. */
    public static final int REGIONS_PER_CELL = CELL / 512;
    /** Zellen pro Richtung → (2*150+1)² ≈ 90.000 Universen je Typ. */
    public static final int MAX_CELL = 150;

    public static boolean isMultiverse(Level level) {
        return UniverseType.of(level.dimension()) != null;
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
        UniverseType type = UniverseType.of(level.dimension());
        if (type == null) return null;
        return new UniverseId(type, cellOf(pos.getX()), cellOf(pos.getZ()));
    }

    @Nullable
    public static UniverseProfile profileAt(Level level, BlockPos pos) {
        UniverseId id = at(level, pos);
        return id == null ? null : UniverseProfile.get(id);
    }

    /** Ein zufälliges Universum eines der erlaubten Typen. */
    public static UniverseId random(RandomSource random, List<UniverseType> types) {
        UniverseType type = types.get(random.nextInt(types.size()));
        int cx = random.nextInt(MAX_CELL * 2 + 1) - MAX_CELL;
        int cz = random.nextInt(MAX_CELL * 2 + 1) - MAX_CELL;
        return new UniverseId(type, cx, cz);
    }

    /** Faktor für den Leitungsverlust: supraleitend 0, hochohmig 2, sonst 1. */
    public static double cableLossFactor(Level level, BlockPos pos) {
        UniverseProfile p = profileAt(level, pos);
        if (p == null) return 1.0;
        if (p.has(UniverseModifier.SUPRALEITEND)) return 0.0;
        if (p.has(UniverseModifier.HOCHOHMIG)) return 2.0;
        return 1.0;
    }

    private Multiverse() {}
}
