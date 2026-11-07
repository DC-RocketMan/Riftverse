package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kurbel-Dynamo (T0): 8 FE/t, Stufe I. Holzkiste mit Kurbel und alter Autolichtmaschine.
 * Kurbeln baut Schwung auf; der Dynamo läuft danach noch kurz nach.
 */
public class KurbelDynamoBlockEntity extends GeneratorBlockEntity implements Crankable {
    public static final int OUTPUT = 8;
    public static final int MAX_MOMENTUM = 60;
    public static final int SLOT_BATTERY = 0;

    private int momentum;

    public KurbelDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KURBEL_DYNAMO.get(), pos, state, 2_000, 1);
    }

    @Override
    public void crank(Player player) {
        momentum = Math.min(MAX_MOMENTUM, momentum + 2);
    }

    @Override
    protected boolean generate() {
        if (momentum <= 0) return false;
        momentum--;
        energy.add(OUTPUT);
        return true;
    }

    @Override
    public int outputPerTick() {
        return OUTPUT;
    }

    @Override
    public int batterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return ItemEnergy.of(stack) != null;
    }

    @Override
    protected int work() {
        return momentum;
    }

    @Override
    protected int workMax() {
        return MAX_MOMENTUM;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Momentum", momentum);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        momentum = tag.getInt("Momentum");
    }
}
