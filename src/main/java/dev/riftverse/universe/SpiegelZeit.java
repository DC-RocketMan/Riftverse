package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import net.minecraft.resources.ResourceLocation;

/** Vertauschte Tageszeit der Spiegelwelt (siehe {@code DimensionTypeMixin}). */
public final class SpiegelZeit {
    /** Himmelseffekte des Dimensionstyps der Spiegelwelt; daran erkennt der Mixin ihn. */
    public static final ResourceLocation EFFECTS = Riftverse.id("spiegelwelt");
    public static final long HALBER_TAG = 12_000L;

    public static long verschieben(ResourceLocation effects, long dayTime) {
        return EFFECTS.equals(effects) ? dayTime + HALBER_TAG : dayTime;
    }

    private SpiegelZeit() {}
}
