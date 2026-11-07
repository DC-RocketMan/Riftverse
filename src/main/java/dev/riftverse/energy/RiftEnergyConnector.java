package dev.riftverse.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wird von Blöcken implementiert, an die Riftverse-Leitungen andocken dürfen.
 * Leitungen verbinden sich nur mit solchen Blöcken, nie mit fremden FE-Blöcken.
 */
public interface RiftEnergyConnector {
    /**
     * @param side Seite dieses Blocks, an der die Leitung anliegt
     */
    boolean canConnectEnergy(BlockState state, Direction side);
}
