package dev.riftverse.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Basis für alles, was am Riftverse-Netz hängt: Puffer speichern und sich beim Netz an- und abmelden.
 */
public abstract class RiftEnergyBlockEntity extends BlockEntity {
    protected final EnergyBuffer energy;

    protected RiftEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.energy = new EnergyBuffer(capacity, this::setChanged);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    /** Hängt dieser Block überhaupt am Netz? (Garagenpresse z. B. nicht) */
    protected boolean joinsNetwork() {
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && joinsNetwork()) EnergyNet.addNode(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) EnergyNet.removeNode(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) EnergyNet.removeNode(level, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.load(tag);
    }
}
