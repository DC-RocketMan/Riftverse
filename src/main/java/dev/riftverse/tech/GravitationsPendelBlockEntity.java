package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.tech.multiblock.MultiblockController;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Gravitations-Pendel (T2, Multiblock 3 × 5 × 3): 400 FE/t, Stufe II, ganz ohne Brennstoff. Ein Schwebstein-Kern
 * schwingt im Rahmen. In einer Welt mit hoher Gravitation liefert es das Doppelte, bei niedriger nur die Hälfte –
 * eine Außenbasis lohnt sich.
 */
public class GravitationsPendelBlockEntity extends GeneratorBlockEntity implements MultiblockController {
    public static final int OUTPUT = 400;
    public static final int STATUS_OK = 0, STATUS_INCOMPLETE = 1;

    private boolean formed;
    private int checkIn;
    private int phase;

    public GravitationsPendelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITATIONS_PENDEL.get(), pos, state, 40_000, 1);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    @Override
    public Component checkStructure() {
        if (level == null) return null;
        Component missing = Multiblock.PENDEL.missingMessage(level, worldPosition, facing());
        formed = missing == null;
        checkIn = 20;
        return missing;
    }

    /** Hochgravitation 2×, Niedriggravitation 0,5×, sonst 1×. */
    public double gravityFactor() {
        if (level == null) return 1.0;
        UniverseProfile p = Multiverse.profileAt(level, worldPosition);
        if (p == null || Math.abs(p.gravity - 1.0) < 1.0e-3) return 1.0;
        return p.gravity > 1.0 ? 2.0 : 0.5;
    }

    public boolean formed() {
        return formed;
    }

    @Override
    protected boolean generate() {
        if (level == null) return false;
        if (--checkIn <= 0) checkStructure();
        if (!formed || energy.isFull()) return false;
        phase = (phase + 1) % 80;
        energy.add(currentOutput());
        if (phase == 0 && level instanceof ServerLevel sl) {
            Vec3 c = Vec3.atCenterOf(Multiblock.at(worldPosition, facing(), new Multiblock.Part(0, 2, 1, s -> true, "")));
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.01);
        }
        return true;
    }

    @Override
    public int outputPerTick() {
        return OUTPUT * 2;   // Paketgröße im besten Fall (Hochgravitation)
    }

    @Override
    protected int currentOutput() {
        return formed ? (int) Math.round(OUTPUT * gravityFactor()) : 0;
    }

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), Math.max(OUTPUT, currentOutput()));
    }

    @Override
    protected int status() {
        return formed ? STATUS_OK : STATUS_INCOMPLETE;
    }

    @Override
    public VoltageTier tier() {
        return VoltageTier.II;
    }

    @Override
    public int batterySlot() {
        return 0;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return ItemEnergy.of(stack) != null;
    }

    /** Balken = Ausschlag des Pendels. */
    @Override
    protected int work() {
        if (!formed) return 0;
        return (int) Math.round(50 + 50 * Math.sin(phase * Math.PI * 2 / 80));
    }

    @Override
    protected int workMax() {
        return 100;
    }

    @Override
    public String barLabelKey() {
        return "gui.riftverse.pendel";
    }

    @Override
    public List<Component> barTooltip(int work, int workMax, int output, int status) {
        if (status == STATUS_INCOMPLETE) {
            return List.of(Component.translatable("tooltip.riftverse.multiblock_unvollstaendig"),
                    Component.translatable("tooltip.riftverse.multiblock_rechtsklick"));
        }
        return List.of(Component.translatable("tooltip.riftverse.pendel_faktor",
                        String.format(Locale.GERMANY, "%.1f", output / (double) OUTPUT)),
                Component.translatable("tooltip.riftverse.output", output));
    }
}
