package dev.riftverse.tech;

import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Garagen-Akku (T0): der erste Speicher, ganz ohne Xenit – Bronze, Kupfer, Zinn und Redstone in einer
 * Holzkiste. 100.000 FE auf Stufe I; arbeitet sonst genau wie die Xenit-Zelle (lädt und entlädt Items,
 * behält die Ladung beim Abbauen, gibt Energie nur an Verbraucher ab).
 */
public class GaragenAkkuBlockEntity extends XenitZelleBlockEntity {
    public static final int AKKU_CAPACITY = 100_000;
    public static final VoltageTier AKKU_TIER = VoltageTier.I;

    public GaragenAkkuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGEN_AKKU.get(), pos, state, AKKU_CAPACITY, AKKU_TIER);
    }
}
