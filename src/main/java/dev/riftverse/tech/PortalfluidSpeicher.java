package dev.riftverse.tech;

import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Tank für stabiles Portalfluid in den T3-Geräten (Materie-Rekonfigurator, Parallelzapfer). Er nimmt stabilisiertes
 * Portalfluid an und Portalfluid-Konzentrat, das doppelt zählt (1 mB Konzentrat = 2 mB Portalfluid).
 * Befüllt wird über Kartuschen oder Rohre; abgepumpt wird nichts.
 */
public final class PortalfluidSpeicher {
    private final int capacity;
    private final Runnable changed;
    private int mb;

    public PortalfluidSpeicher(int capacity, Runnable changed) {
        this.capacity = capacity;
        this.changed = changed;
    }

    public int mb() {
        return mb;
    }

    public int capacity() {
        return capacity;
    }

    public int space() {
        return capacity - mb;
    }

    public void set(int value) {
        mb = Math.max(0, Math.min(capacity, value));
    }

    /** @return ob genug da war (dann ist es abgezogen) */
    public boolean take(int amount) {
        if (mb < amount) return false;
        mb -= amount;
        changed.run();
        return true;
    }

    /** Portalfluid-Wert einer vollen Kartusche: stabil 1.000 mB, Konzentrat 2.000 mB, sonst 0. */
    public static int valueOf(ItemStack cartridge) {
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_STABIL.get())) return FluidKartuscheItem.INHALT;
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get())) return FluidKartuscheItem.INHALT * 2;
        return 0;
    }

    /** Leert eine volle Kartusche aus {@code in} in den Tank; die leere Kartusche kommt nach {@code out}. */
    public void emptyCartridge(ItemStackHandler items, int in, int out) {
        ItemStack cart = items.getStackInSlot(in);
        int value = valueOf(cart);
        if (value <= 0 || value > space()) return;
        ItemStack o = items.getStackInSlot(out);
        if (!o.isEmpty() && (!o.is(ModItems.FLUID_KARTUSCHE.get()) || o.getCount() >= o.getMaxStackSize())) return;
        items.extractItem(in, 1, false);
        if (o.isEmpty()) items.setStackInSlot(out, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
        else items.setStackInSlot(out, o.copyWithCount(o.getCount() + 1));
        mb += value;
        changed.run();
    }

    public IFluidHandler handler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return mb > 0 ? new FluidStack(ModFluids.PORTALFLUID.get(), mb) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return capacity;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.is(ModFluids.PORTALFLUID.get()) || stack.is(ModFluids.PORTALFLUID_KONZENTRAT.get());
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
                int factor = resource.is(ModFluids.PORTALFLUID_KONZENTRAT.get()) ? 2 : 1;
                int accepted = Math.min(resource.getAmount(), space() / factor);
                if (accepted > 0 && action.execute()) {
                    mb += accepted * factor;
                    changed.run();
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public void save(CompoundTag tag) {
        tag.putInt("Portalfluid", mb);
    }

    public void load(CompoundTag tag) {
        set(tag.getInt("Portalfluid"));
    }
}
