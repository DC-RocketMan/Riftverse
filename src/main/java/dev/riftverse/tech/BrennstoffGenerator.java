package dev.riftverse.tech;

import net.minecraft.world.item.ItemStack;

/**
 * Generator mit Brennstoff-Slot, Ausgabe-Slot für Nebenprodukte und Akku-Slot (Tonnenreaktor, Biomasse-Kessel).
 * Die GUI zeigt dann Flamme und Ausgabe statt eines Balkens.
 */
public interface BrennstoffGenerator {
    int SLOT_FUEL = 0, SLOT_OUT = 1, SLOT_BATTERY = 2;

    boolean isFuel(ItemStack stack);

    /** Tooltip über der Flamme. */
    String fuelTooltipKey();

    /** Beschriftung unter dem Ausgabe-Slot. */
    String outputLabelKey();
}
