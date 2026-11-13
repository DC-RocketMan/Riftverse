package dev.riftverse.tech;

import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Rift-Kabelknoten (T3): Antennenmast mit einem kleinen Riss an der Spitze. Zwei gekoppelte Knoten übertragen FE,
 * Items und Fluids zwischen Dimensionen – damit lohnen sich Außenbasen in anderen Universen.
 * <ul>
 *   <li>Der zuerst angeklickte Knoten sendet, der zweite empfängt; Schleichen + Rechtsklick tauscht die Rollen.</li>
 *   <li>Grundlast 100 FE/t beim Sender, 5 % Verlust auf dem Strom. Der Empfänger speist mit Stufe IV ein, dahinter
 *       gehört ein Umspanner.</li>
 *   <li>Beide Knoten halten ihren Chunk geladen und ihr Universum verankert (es wird nie zurückgesetzt).</li>
 * </ul>
 */
public class RiftKabelknotenBlockEntity extends RiftEnergyBlockEntity
        implements RiftEnergySource, RiftEnergySink, ServerTickable, DropsContents {
    public static final int GRUNDLAST = 100;
    public static final int PAKET = VoltageTier.IV.emitPacket();
    public static final double VERLUST = 0.05;
    public static final int TANK = 8_000;
    public static final int FLUID_PRO_TICK = 1_000;

    private final ItemStackHandler items = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private FluidStack fluid = FluidStack.EMPTY;
    @Nullable
    private GlobalPos partner;
    private boolean sender = true;
    private int flowTicks;

    public RiftKabelknotenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_KABELKNOTEN.get(), pos, state, PAKET * 4);
    }

    @Nullable
    public GlobalPos partner() {
        return partner;
    }

    public boolean sender() {
        return sender;
    }

    public FluidStack fluid() {
        return fluid;
    }

    public ItemStackHandler items() {
        return items;
    }

    // ------------------------------------------------------------------ Kopplung

    /** Koppelt zwei Knoten: {@code a} sendet, {@code b} empfängt. */
    public static void koppeln(RiftKabelknotenBlockEntity a, RiftKabelknotenBlockEntity b) {
        if (a.level == null || b.level == null) return;
        a.entkoppeln();
        b.entkoppeln();
        a.partner = GlobalPos.of(b.level.dimension(), b.worldPosition);
        b.partner = GlobalPos.of(a.level.dimension(), a.worldPosition);
        a.sender = true;
        b.sender = false;
        a.forceChunk(true);
        b.forceChunk(true);
        a.changed();
        b.changed();
    }

    /** Löst die Kopplung auf beiden Seiten. */
    public void entkoppeln() {
        RiftKabelknotenBlockEntity other = partnerEntity();
        partner = null;
        forceChunk(false);
        changed();
        if (other != null && other.partner != null && other.partner.pos().equals(worldPosition)) {
            other.partner = null;
            other.forceChunk(false);
            other.changed();
        }
    }

    /** Sender und Empfänger tauschen (Schleichen + Rechtsklick). */
    public void rollenTauschen() {
        RiftKabelknotenBlockEntity other = partnerEntity();
        sender = !sender;
        changed();
        if (other != null) {
            other.sender = !sender;
            other.changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            dev.riftverse.energy.EnergyNet.markDirty(level);   // Quelle/Verbraucher haben gewechselt
        }
    }

    @Nullable
    public RiftKabelknotenBlockEntity partnerEntity() {
        if (partner == null || !(level instanceof ServerLevel sl)) return null;
        ServerLevel target = sl.getServer().getLevel(partner.dimension());
        if (target == null || !target.isLoaded(partner.pos())) return null;
        BlockEntity be = target.getBlockEntity(partner.pos());
        return be instanceof RiftKabelknotenBlockEntity k && !k.isRemoved() ? k : null;
    }

    private void forceChunk(boolean force) {
        if (!(level instanceof ServerLevel sl)) return;
        ChunkPos c = new ChunkPos(worldPosition);
        if (!force) {
            // Ein anderer gekoppelter Knoten im selben Chunk hält ihn weiter
            for (BlockEntity be : sl.getChunk(c.x, c.z).getBlockEntities().values()) {
                if (be != this && be instanceof RiftKabelknotenBlockEntity k && k.partner != null) return;
            }
        }
        sl.setChunkForced(c.x, c.z, force);
    }

    // ------------------------------------------------------------------ Tick: der Sender schiebt

    @Override
    public void serverTick() {
        if (level == null) return;
        if (flowTicks > 0) flowTicks--;
        if (sender && partner != null) {
            RiftKabelknotenBlockEntity other = partnerEntity();
            if (other != null && energy.stored() >= GRUNDLAST) {
                energy.take(GRUNDLAST);
                boolean moved = uebertragen(other);
                if (moved) {
                    flowTicks = 10;
                    other.flowTicks = 10;
                }
            }
        }
        BlockState state = level.getBlockState(worldPosition);
        boolean active = flowTicks > 0;
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        if (active && level instanceof ServerLevel sl && level.getGameTime() % 8 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    1, 0.05, 0.1, 0.05, 0.01);
        }
    }

    private boolean uebertragen(RiftKabelknotenBlockEntity other) {
        boolean moved = false;
        // Strom: 5 % Verlust
        int send = Math.min(energy.stored(), PAKET);
        int arrive = Math.min((int) Math.floor(send * (1.0 - VERLUST)), other.energy.space());
        if (arrive > 0) {
            energy.take((int) Math.ceil(arrive / (1.0 - VERLUST)));
            other.energy.add(arrive);
            moved = true;
        }
        // Items
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(other.items, stack.copy(), false);
            if (rest.getCount() != stack.getCount()) {
                items.setStackInSlot(i, rest);
                moved = true;
            }
        }
        // Fluids
        if (!fluid.isEmpty() && (other.fluid.isEmpty() || FluidStack.isSameFluidSameComponents(other.fluid, fluid))) {
            int amount = Math.min(Math.min(FLUID_PRO_TICK, fluid.getAmount()), TANK - other.fluid.getAmount());
            if (amount > 0) {
                if (other.fluid.isEmpty()) other.fluid = fluid.copyWithAmount(amount);
                else other.fluid.grow(amount);
                fluid.shrink(amount);
                if (fluid.isEmpty()) fluid = FluidStack.EMPTY;
                other.setChanged();
                setChanged();
                moved = true;
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return sender && partner != null ? energy.space() : 0;
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.IV;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        // verträgt jede Stufe
    }

    @Override
    public int getOfferedEnergy() {
        return sender ? 0 : Math.min(energy.stored(), PAKET);
    }

    @Override
    public VoltageTier getSourceTier() {
        return VoltageTier.IV;
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    // ------------------------------------------------------------------ Trichter und Rohre

    /** Sender: nur hinein; Empfänger: nur heraus. */
    public IItemHandler itemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return sender ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return sender ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return sender;
            }
        };
    }

    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return fluid;
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return sender;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!sender || resource.isEmpty()) return 0;
                if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, resource)) return 0;
                int accepted = Math.min(resource.getAmount(), TANK - fluid.getAmount());
                if (accepted > 0 && action.execute()) {
                    if (fluid.isEmpty()) fluid = resource.copyWithAmount(accepted);
                    else fluid.grow(accepted);
                    setChanged();
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (sender || resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (sender) return FluidStack.EMPTY;
                int amount = Math.min(maxDrain, fluid.getAmount());
                if (amount <= 0) return FluidStack.EMPTY;
                FluidStack drained = fluid.copyWithAmount(amount);
                if (action.execute()) {
                    fluid.shrink(amount);
                    if (fluid.isEmpty()) fluid = FluidStack.EMPTY;
                    setChanged();
                }
                return drained;
            }
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.save(registries));
        tag.putBoolean("Sender", sender);
        if (partner != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, partner).result().ifPresent(t -> tag.put("Partner", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        fluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
        sender = !tag.contains("Sender") || tag.getBoolean("Sender");
        partner = tag.contains("Partner") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Partner")).result().orElse(null) : null;
    }
}
