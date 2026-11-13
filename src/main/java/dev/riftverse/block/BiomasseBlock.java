package dev.riftverse.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Biomasse: der Boden der organischen Welt. Er „atmet“ (pulsierende Textur) und stößt ab und zu Sporen aus. */
public class BiomasseBlock extends Block {
    public BiomasseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) != 0 || !level.getBlockState(pos.above()).isAir()) return;
        level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }
}
