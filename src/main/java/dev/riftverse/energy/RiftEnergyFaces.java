package dev.riftverse.energy;

import net.minecraft.core.Direction;

/**
 * Block-Entity, deren Seiten im Netz unterschiedliche Aufgaben haben (Umspanner): Pakete gehen nur über
 * {@link #emitsTo} hinaus und kommen nur über {@link #acceptsFrom} herein.
 * <p>
 * Ändern sich die Seiten (Redstone schaltet den Umspanner um), muss der Block {@link EnergyNet#markDirty}
 * aufrufen, weil das Netz die Seiten beim Aufbau zwischenspeichert.
 */
public interface RiftEnergyFaces {
    /** @param side Seite dieses Blocks */
    boolean emitsTo(Direction side);

    /** @param side Seite dieses Blocks */
    boolean acceptsFrom(Direction side);
}
