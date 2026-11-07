package dev.riftverse.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Meteoritenkern in der Mitte jedes Kraters: verschmolzener schwarzer Stein mit Petrol-Adern.
 * Glüht schwach grün und sprüht Funken, wenn man ihn bearbeitet.
 */
public class MeteoritenkernBlock extends DropExperienceBlock {
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(0.22f, 1.0f, 0.42f), 0.8f);

    public MeteoritenkernBlock(Properties properties) {
        super(UniformInt.of(3, 7), properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 1.02;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(GLOW, x, y, z, 0, 0.02, 0);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        super.attack(state, level, pos, player);
        if (level.isClientSide) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        (level.random.nextDouble() - 0.5) * 0.4, 0.2, (level.random.nextDouble() - 0.5) * 0.4);
            }
        }
    }
}
