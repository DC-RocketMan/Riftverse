package dev.riftverse.block;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Knospender Fluxkristall: In der Kristallwelt wachsen an ihm abgebaute Cluster nach.
 * Lässt sich nicht mitnehmen (wie knospender Amethyst) und wächst nur in der Kristallwelt.
 */
public class KnospenderFluxkristallBlock extends Block {
    public KnospenderFluxkristallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        dev.riftverse.universe.UniverseProfile profile = dev.riftverse.universe.Multiverse.profileAt(level, pos);
        if (profile == null || profile.look != UniverseType.KRISTALL || random.nextInt(4) != 0) return;
        Direction dir = Direction.getRandom(random);
        BlockPos target = pos.relative(dir);
        BlockState there = level.getBlockState(target);
        boolean water = there.getFluidState().getType() == Fluids.WATER && there.getFluidState().isSource();
        if (!there.isAir() && !water) return;
        BlockState cluster = ModBlocks.FLUXKRISTALL_CLUSTER.get().defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, dir)
                .setValue(AmethystClusterBlock.WATERLOGGED, water);
        level.setBlockAndUpdate(target, cluster);
    }
}
