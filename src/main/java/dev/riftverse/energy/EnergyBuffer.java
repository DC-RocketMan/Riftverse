package dev.riftverse.energy;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Einfacher interner FE-Speicher einer Maschine. Bewusst kein {@link IEnergyStorage}:
 * fremde Mods docken nur am Ein- und Ausspeiser an.
 */
public class EnergyBuffer {
    private final int capacity;
    private int stored;
    private final Runnable onChange;

    public EnergyBuffer(int capacity, Runnable onChange) {
        this.capacity = capacity;
        this.onChange = onChange;
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return capacity;
    }

    public int space() {
        return capacity - stored;
    }

    public boolean isFull() {
        return stored >= capacity;
    }

    /** @return aufgenommene Menge */
    public int add(int amount) {
        int accepted = Math.max(0, Math.min(amount, space()));
        if (accepted > 0) {
            stored += accepted;
            onChange.run();
        }
        return accepted;
    }

    /** @return entnommene Menge */
    public int take(int amount) {
        int taken = Math.max(0, Math.min(amount, stored));
        if (taken > 0) {
            stored -= taken;
            onChange.run();
        }
        return taken;
    }

    public void set(int value) {
        stored = Math.max(0, Math.min(capacity, value));
    }

    public void save(CompoundTag tag) {
        tag.putInt("Energy", stored);
    }

    public void load(CompoundTag tag) {
        set(tag.getInt("Energy"));
    }

    /** Nur lesbar – damit Jade und Integrated Dynamics den Füllstand anzeigen, ohne Strom einzuspeisen. */
    public IEnergyStorage readOnly() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return stored;
            }

            @Override
            public int getMaxEnergyStored() {
                return capacity;
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
    }
}
