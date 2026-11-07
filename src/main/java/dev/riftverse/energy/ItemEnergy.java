package dev.riftverse.energy;

import dev.riftverse.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Items bleiben reines FE: Energiezellen laden in jeder Riftverse-Maschine und in jedem FE-Ladegerät.
 */
public final class ItemEnergy {

    /** FE-Speicher eines beliebigen Items (auch aus anderen Mods), oder null. */
    @Nullable
    public static IEnergyStorage of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    /** Zieht bis zu {@code max} FE aus dem Item in den Puffer (Akku-Slot einer Maschine). */
    public static int discharge(ItemStack stack, EnergyBuffer into, int max) {
        IEnergyStorage cell = of(stack);
        if (cell == null || !cell.canExtract() || into.isFull()) return 0;
        int got = cell.extractEnergy(Math.min(max, into.space()), false);
        return into.add(got);
    }

    /** Lädt das Item mit bis zu {@code max} FE aus dem Puffer (Akku-Slot eines Generators). */
    public static int charge(ItemStack stack, EnergyBuffer from, int max) {
        IEnergyStorage cell = of(stack);
        if (cell == null || !cell.canReceive() || from.stored() <= 0) return 0;
        int accepted = cell.receiveEnergy(Math.min(max, from.stored()), false);
        return from.take(accepted);
    }

    /** IEnergyStorage für Items mit dem Data Component {@code riftverse:energy}. */
    public static class Storage implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;
        private final int maxTransfer;

        public Storage(ItemStack stack, int capacity, int maxTransfer) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxTransfer = maxTransfer;
        }

        private int get() {
            return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
        }

        private void set(int value) {
            stack.set(ModDataComponents.ENERGY.get(), Math.max(0, Math.min(capacity, value)));
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(Math.min(toReceive, maxTransfer), capacity - get()));
            if (!simulate && accepted > 0) set(get() + accepted);
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int taken = Math.max(0, Math.min(Math.min(toExtract, maxTransfer), get()));
            if (!simulate && taken > 0) set(get() - taken);
            return taken;
        }

        @Override
        public int getEnergyStored() {
            return get();
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private ItemEnergy() {}
}
