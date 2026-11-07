package dev.riftverse.energy;

/**
 * Block-Entity, die Pakete aus dem Riftverse-Netz annimmt (Maschinen, Ausspeiser).
 */
public interface RiftEnergySink {
    /** Wie viel FE die Senke in diesem Tick noch aufnehmen möchte. */
    int getDemandedEnergy();

    /** Höchste Spannungsstufe, die die Senke verträgt. */
    VoltageTier getSinkTier();

    /**
     * Nimmt ein Paket an. Die Spannung wurde vom Netz bereits geprüft.
     *
     * @return tatsächlich angenommene Menge in FE
     */
    int injectEnergy(int amount, VoltageTier tier);

    /** Ein zu großes Paket ist angekommen. Wird nach dem Netz-Tick aufgerufen. */
    void onOvervoltage(VoltageTier tier);
}
