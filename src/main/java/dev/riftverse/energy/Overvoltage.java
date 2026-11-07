package dev.riftverse.energy;

import dev.riftverse.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Was passiert, wenn ein Paket zu groß für eine Maschine ist: sofort, ohne Vorwarnung, wie in IC2.
 */
public final class Overvoltage {

    /**
     * Entfernt den Block (sein Inventar fällt dabei über onRemove heraus) und lässt ihn je nach Config
     * explodieren oder durchbrennen.
     */
    public static void blowUp(Level level, BlockPos pos, VoltageTier tier) {
        if (!(level instanceof ServerLevel sl)) return;
        Config.OvervoltageMode mode = Config.OVERVOLTAGE.get();
        if (mode == Config.OvervoltageMode.OFF) return;

        sl.removeBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (mode == Config.OvervoltageMode.EXPLODE) {
            sl.explode(null, x, y, z, 1.5f + tier.level(), Level.ExplosionInteraction.BLOCK);
        } else {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 20, 0.3, 0.3, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 30, 0.4, 0.4, 0.4, 0.2);
            sl.playSound(null, pos, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 1.0f, 0.7f);
        }
    }

    private Overvoltage() {}
}
