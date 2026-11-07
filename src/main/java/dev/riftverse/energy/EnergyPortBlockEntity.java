package dev.riftverse.energy;

import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.ServerTickable;
import dev.riftverse.tech.TechBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Einspeiser: nimmt FE von jedem Mod an und speist es mit fester Stufe (I) ins Riftverse-Netz.
 * Ausspeiser: nimmt Riftverse-Strom an und gibt ihn als normales FE ab; fremde Maschinen explodieren nie.
 */
public class EnergyPortBlockEntity extends RiftEnergyBlockEntity implements RiftEnergySource, RiftEnergySink, ServerTickable {
    public static final int CAPACITY = 4_000;
    public static final VoltageTier TIER = VoltageTier.I;
    /** Höchstens so viel FE gibt der Ausspeiser pro Tick an fremde Blöcke ab. */
    public static final int MAX_PUSH = 1_024;

    private int flowTicks;

    private final IEnergyStorage external = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!isInput()) return 0;
            int accepted = Math.max(0, Math.min(toReceive, energy.space()));
            if (!simulate && accepted > 0) {
                energy.add(accepted);
                flowTicks = 20;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (isInput()) return 0;
            int taken = Math.max(0, Math.min(toExtract, energy.stored()));
            if (!simulate && taken > 0) {
                energy.take(taken);
                flowTicks = 20;
            }
            return taken;
        }

        @Override
        public int getEnergyStored() {
            return energy.stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.capacity();
        }

        @Override
        public boolean canExtract() {
            return !isInput();
        }

        @Override
        public boolean canReceive() {
            return isInput();
        }
    };

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_PORT.get(), pos, state, CAPACITY);
    }

    public boolean isInput() {
        return getBlockState().getBlock() instanceof EnergyPortBlock port && port.isInput();
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(EnergyPortBlock.FACING) ? state.getValue(EnergyPortBlock.FACING) : Direction.NORTH;
    }

    /** FE-Capability nur an der Front; die anderen Seiten gehören dem Riftverse-Netz. */
    @Nullable
    public IEnergyStorage externalStorage(@Nullable Direction side) {
        return side == null || side == front() ? external : null;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (!isInput() && energy.stored() > 0) {
            Direction front = front();
            BlockPos target = worldPosition.relative(front);
            // nie zurück ins eigene Netz schieben (z. B. in einen Einspeiser)
            if (!(level.getBlockState(target).getBlock() instanceof RiftEnergyConnector)) {
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, front.getOpposite());
                if (storage != null && storage.canReceive()) {
                    int sent = storage.receiveEnergy(Math.min(energy.stored(), MAX_PUSH), false);
                    if (sent > 0) {
                        energy.take(sent);
                        flowTicks = 20;
                    }
                }
            }
        }
        if (flowTicks > 0) flowTicks--;
        boolean active = flowTicks > 0;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        return isInput() ? Math.min(energy.stored(), TIER.emitPacket()) : 0;
    }

    @Override
    public VoltageTier getSourceTier() {
        return TIER;
    }

    @Override
    public void drawEnergy(int amount) {
        if (energy.take(amount) > 0) flowTicks = 20;
    }

    @Override
    public int getDemandedEnergy() {
        return isInput() ? 0 : energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return TIER;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }
}
