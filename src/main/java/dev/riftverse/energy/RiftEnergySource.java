package dev.riftverse.energy;

/**
 * Block-Entity, die Pakete ins Riftverse-Netz abgibt (Generatoren, Einspeiser).
 */
public interface RiftEnergySource {
    /** Energie, die diese Quelle in diesem Tick als ein Paket abgeben kann (FE). */
    int getOfferedEnergy();

    /** Spannungsstufe der Pakete. Größere Stufe als der Verbraucher verträgt = Überspannung. */
    VoltageTier getSourceTier();

    /** Das Netz hat {@code amount} FE abgenommen (inklusive Leitungsverlust). */
    void drawEnergy(int amount);
}
