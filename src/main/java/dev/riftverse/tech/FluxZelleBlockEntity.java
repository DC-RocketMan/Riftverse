package dev.riftverse.tech;

import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flux-Zelle (T2): 16 Mio. FE, Stufe II. Der Füllstand leuchtet als Kristallband an den Seiten.
 * Abgebaut behält sie ihre Ladung wie die Xenit-Zelle.
 */
public class FluxZelleBlockEntity extends XenitZelleBlockEntity {
    public static final int FLUX_CAPACITY = 16_000_000;
    public static final VoltageTier FLUX_TIER = VoltageTier.II;

    public FluxZelleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_ZELLE.get(), pos, state, FLUX_CAPACITY, FLUX_TIER);
    }

    /** Band 0–4: leer, ein Viertel … voll. */
    public static int band(int stored, int capacity) {
        if (stored <= 0) return 0;
        return Math.min(4, 1 + (int) (3L * stored / Math.max(1, capacity)));
    }

    @Override
    protected BlockState updateState(BlockState state) {
        if (!state.hasProperty(FluxZelleBlock.FUELLUNG)) return state;
        int band = band(energy.stored(), energy.capacity());
        return state.getValue(FluxZelleBlock.FUELLUNG) == band ? state : state.setValue(FluxZelleBlock.FUELLUNG, band);
    }
}
