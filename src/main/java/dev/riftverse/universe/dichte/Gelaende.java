package dev.riftverse.universe.dichte;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Gelände mit 3D-Dichte (Meilenstein 11 der neuen Universen): Wo {@code dichte(x, y, z) > 0} ist, liegt Fels.
 * Damit gehen Überhänge, Bögen, schwebende Berge und Inselunterseiten, die eine 2D-Höhenkarte nicht kann.
 * <p>
 * Die Dichte wird nur in einem groben Raster berechnet ({@link #rasterXZ()} × {@link #rasterY()} × {@link #rasterXZ()}
 * Blöcke) und dazwischen trilinear interpoliert, siehe {@link Dichtefeld}. Weil dieselben Rasterpunkte auch
 * {@code getBaseHeight} und {@code getBaseColumn} liefern, landen Portale und Strukturen nie in der Luft.
 * <p>
 * Ein Gelände wird von mehreren Generator-Threads gleichzeitig benutzt und darf deshalb keinen veränderlichen
 * Zustand haben; alles Spaltenweise steckt in {@link Spalte} und {@link Bloecke}.
 */
public interface Gelaende {

    /**
     * Alles, was nur von x und z abhängt (Höhenrauschen, Masken), wird einmal pro Rasterspalte vorbereitet; danach
     * fragt das Dichtefeld die Spalte für jede Rasterhöhe ab.
     */
    Spalte spalte(int x, int z);

    /** Eine vorbereitete Rasterspalte. */
    @FunctionalInterface
    interface Spalte {
        /** Dichte an der Höhe y: über 0 ist Fels, sonst Luft (oder Wasser unter dem Wasserspiegel). */
        double dichte(int y);
    }

    /** Rasterweite waagrecht (Teiler von 16). 4 ist der Standard, feine Bögen und Nadeln brauchen 2. */
    default int rasterXZ() {
        return 4;
    }

    /** Rasterweite senkrecht (Teiler von 16). */
    default int rasterY() {
        return 8;
    }

    /** Welche Blöcke in der Blockspalte (x, z) liegen; Standard: die Gesteins-Palette des Universums. */
    default Bloecke bloecke(int x, int z) {
        return Bloecke.PALETTE;
    }

    /** Blöcke einer Blockspalte. null heißt jeweils: Block aus der Gesteins-Palette. */
    interface Bloecke {
        Bloecke PALETTE = new Bloecke() {
        };

        /** Oberflächenblock (Luft darüber). */
        @Nullable
        default BlockState oberflaeche(int y) {
            return null;
        }

        /** Unterschicht: bis 3 Blöcke unter jeder freien Oberseite. */
        @Nullable
        default BlockState unterschicht(int y) {
            return null;
        }

        /**
         * Unterseite: Fels mit Luft darunter (unter Überhängen, Bögen und schwebenden Bergen). null heißt Gestein;
         * Gras und Erde wachsen nie an Decken (Entscheidung M11-04).
         */
        @Nullable
        default BlockState unterseite(int y) {
            return null;
        }
    }
}
