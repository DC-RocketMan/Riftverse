package dev.riftverse.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Schwebstein (und der Schwebstein-Kern des Pendels): Partikel steigen langsam nach oben. */
public class SchwebsteinBlock extends Block {
    public SchwebsteinBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        double x = pos.getX() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + 1.05, z, 0, 0.015 + random.nextDouble() * 0.02, 0);
    }
}
