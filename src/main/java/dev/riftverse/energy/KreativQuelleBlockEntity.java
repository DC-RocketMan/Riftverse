package dev.riftverse.energy;

import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.ServerTickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Unendliche Quelle: gibt jeden Tick ein volles Paket der gewählten Stufe ab. */
public class KreativQuelleBlockEntity extends RiftEnergyBlockEntity implements RiftEnergySource, ServerTickable {
    private VoltageTier tier = VoltageTier.I;

    public KreativQuelleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KREATIV_QUELLE.get(), pos, state, 1);
    }

    public VoltageTier cycleTier() {
        tier = tier.next();
        setChanged();
        return tier;
    }

    @Override
    public void serverTick() {
    }

    @Override
    public int getOfferedEnergy() {
        return tier.emitPacket();
    }

    @Override
    public VoltageTier getSourceTier() {
        return tier;
    }

    @Override
    public void drawEnergy(int amount) {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Tier", tier.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            tier = VoltageTier.valueOf(tag.getString("Tier"));
        } catch (IllegalArgumentException e) {
            tier = VoltageTier.I;
        }
    }
}
