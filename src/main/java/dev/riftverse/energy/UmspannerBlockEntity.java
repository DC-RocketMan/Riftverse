package dev.riftverse.energy;

import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.ServerTickable;
import dev.riftverse.tech.TechBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Umspanner: Puffer für acht Pakete der hohen Stufe. Heruntergespannt gibt er bis zu vier Pakete der niedrigen
 * Stufe pro Tick ab, hochgespannt ein Paket der hohen Stufe.
 */
public class UmspannerBlockEntity extends RiftEnergyBlockEntity
        implements RiftEnergySource, RiftEnergySink, RiftEnergyFaces, ServerTickable {
    /** So viele Pakete der niedrigen Stufe gibt der Umspanner pro Tick ab, wenn er herunterspannt. */
    public static final int PACKETS_DOWN = 4;

    private int flowTicks;

    public UmspannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UMSPANNER.get(), pos, state, highOf(state).maxPacket() * 8);
    }

    private static VoltageTier lowOf(BlockState state) {
        return state.getBlock() instanceof UmspannerBlock u ? u.low() : VoltageTier.I;
    }

    private static VoltageTier highOf(BlockState state) {
        return lowOf(state).next();
    }

    public VoltageTier low() {
        return lowOf(getBlockState());
    }

    public VoltageTier high() {
        return highOf(getBlockState());
    }

    /** Mit Redstone spannt der Umspanner hoch. */
    public boolean stepUp() {
        BlockState state = getBlockState();
        return state.hasProperty(UmspannerBlock.POWERED) && state.getValue(UmspannerBlock.POWERED);
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(UmspannerBlock.FACING) ? state.getValue(UmspannerBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (flowTicks > 0) flowTicks--;
        boolean active = flowTicks > 0;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Seiten

    @Override
    public boolean emitsTo(Direction side) {
        return stepUp() == (side == front());
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return stepUp() != (side == front());
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        int max = stepUp() ? high().emitPacket() : low().maxPacket() * PACKETS_DOWN;
        return Math.min(energy.stored(), max);
    }

    @Override
    public VoltageTier getSourceTier() {
        return stepUp() ? high() : low();
    }

    @Override
    public void drawEnergy(int amount) {
        if (energy.take(amount) > 0) flowTicks = 10;
    }

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    /** Heruntergespannt verträgt die Front die hohe Stufe, hochgespannt nehmen die Seiten nur die niedrige an. */
    @Override
    public VoltageTier getSinkTier() {
        return stepUp() ? low() : high();
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
