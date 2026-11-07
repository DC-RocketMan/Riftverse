package dev.riftverse.worldgen;

import dev.riftverse.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Abgestürztes Alien-Wrack in der Overworld (selten): ein halb vergrabener Rumpf aus Eisen und oxidiertem Kupfer,
 * darum Schrotthaufen, aus denen Alien-Schrott fällt.
 */
public class AlienWrackFeature extends Feature<NoneFeatureConfiguration> {
    private static final Block[] HULL = {Blocks.IRON_BLOCK, Blocks.OXIDIZED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER,
            Blocks.EXPOSED_CUT_COPPER, Blocks.IRON_BLOCK};

    public AlienWrackFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos origin = context.origin();
        BlockPos below = origin.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(origin).isEmpty()) return false;

        BlockState scrap = ModBlocks.SCHROTT_HAUFEN.get().defaultBlockState();
        boolean alongX = r.nextBoolean();
        int length = 4 + r.nextInt(3);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < length; i++) {
            int rise = i * 2 / length;
            for (int w = -1; w <= 1; w++) {
                for (int h = 0; h <= 2; h++) {
                    int x = origin.getX() + (alongX ? i : w);
                    int z = origin.getZ() + (alongX ? w : i);
                    pos.set(x, origin.getY() - 1 + h + rise, z);
                    boolean shell = w != 0 || h != 1 || i == 0 || i == length - 1;
                    if (!shell) {
                        level.setBlock(pos, r.nextFloat() < 0.4f ? scrap : Blocks.AIR.defaultBlockState(), 2);
                    } else if (r.nextFloat() < 0.75f) {
                        Block b = r.nextFloat() < 0.2f ? ModBlocks.SCHROTT_HAUFEN.get() : HULL[r.nextInt(HULL.length)];
                        level.setBlock(pos, b.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int k = 0; k < 3 + r.nextInt(4); k++) {
            int x = origin.getX() + r.nextInt(9) - 4;
            int z = origin.getZ() + r.nextInt(9) - 4;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            pos.set(x, y, z);
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()) level.setBlock(pos, scrap, 2);
        }
        return true;
    }
}
