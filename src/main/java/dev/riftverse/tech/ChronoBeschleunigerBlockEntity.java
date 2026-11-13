package dev.riftverse.tech;

import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Chrono-Beschleuniger (T2, Stufe II): beschleunigt die Maschine hinter sich 2×, 4× oder 8× für 100, 400 oder
 * 800 FE/t. Pflanzen wachsen schneller. Generatoren und andere Beschleuniger lassen sich nicht beschleunigen.
 */
public class ChronoBeschleunigerBlockEntity extends RiftEnergyBlockEntity implements RiftEnergySink, ServerTickable {
    public static final int[] FACTOR = {2, 4, 8};
    public static final int[] CONSUMPTION = {100, 400, 800};

    private int mode;
    private boolean working;

    public ChronoBeschleunigerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONO_BESCHLEUNIGER.get(), pos, state, 800 * 200);
    }

    public int mode() {
        return mode;
    }

    public void cycleMode() {
        mode = (mode + 1) % FACTOR.length;
        setChanged();
    }

    public Component status() {
        return Component.translatable(working ? "message.riftverse.chrono.aktiv" : "message.riftverse.chrono.leerlauf",
                FACTOR[mode], CONSUMPTION[mode], String.format(Locale.GERMANY, "%,d", energy.stored()));
    }

    private BlockPos target() {
        BlockState state = getBlockState();
        Direction front = state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
        return worldPosition.relative(front.getOpposite());
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean was = working;
        working = false;
        int cost = CONSUMPTION[mode];
        if (energy.stored() >= cost) {
            BlockPos pos = target();
            BlockState state = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            int extra = FACTOR[mode] - 1;
            if (be != null && accelerable(be)) {
                tickExtra(level, pos, state, be, extra);
                working = true;
            } else if (be == null && state.isRandomlyTicking() && level instanceof ServerLevel sl) {
                // Pflanzen: zusätzliche Zufallsticks
                if (sl.random.nextInt(64) < extra) state.randomTick(sl, pos, sl.random);
                working = true;
            }
            if (working) energy.take(cost);
        }
        if (working && level.getGameTime() % 20 == 0 && level instanceof ServerLevel sl) {
            BlockPos t = target();
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, 4, 0.35, 0.35, 0.35, 0.01);
        }
        BlockState own = level.getBlockState(worldPosition);
        if (own.hasProperty(TechBlock.ACTIVE) && own.getValue(TechBlock.ACTIVE) != working) {
            level.setBlock(worldPosition, own.setValue(TechBlock.ACTIVE, working), Block.UPDATE_ALL);
        }
        if (was != working) setChanged();
    }

    /** Maschinen ja, Generatoren (reine Quellen) und andere Beschleuniger nein. */
    private static boolean accelerable(BlockEntity be) {
        if (be instanceof ChronoBeschleunigerBlockEntity) return false;
        return !(be instanceof RiftEnergySource) || be instanceof RiftEnergySink;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tickExtra(Level level, BlockPos pos, BlockState state, T be, int times) {
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) be.getType());
        if (ticker == null) return;
        for (int i = 0; i < times && !be.isRemoved(); i++) ticker.tick(level, pos, state, be);
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.II;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = Math.floorMod(tag.getInt("Mode"), FACTOR.length);
    }
}
