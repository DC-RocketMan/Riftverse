package dev.riftverse.dorf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hängt von Dächern, Ästen und Laub: die leuchtende Lichtranke (mit {@link LeuchtLogik}) und das Kräuterbündel.
 * Lässt sich zu langen Ranken aneinanderhängen.
 */
public class HaengendBlock extends Block {
    public static final MapCodec<HaengendBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("farbe").forGetter(b -> b.farbe),
            propertiesCodec()
    ).apply(i, HaengendBlock::new));
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    /** Leuchtfarbe; 0 = leuchtet nicht (Kräuterbündel). */
    private final int farbe;

    public HaengendBlock(int farbe, Properties properties) {
        super(properties);
        this.farbe = farbe;
        registerDefaultState(stateDefinition.any().setValue(LeuchtLogik.LEUCHTET, false).setValue(LeuchtLogik.ANGEREGT, false));
    }

    public boolean leuchtend() {
        return farbe != 0;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LeuchtLogik.LEUCHTET, LeuchtLogik.ANGEREGT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LeuchtLogik.LEUCHTET, leuchtend() && context.getLevel().isNight());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos oben = pos.above();
        BlockState o = level.getBlockState(oben);
        return o.is(this) || o.is(BlockTags.LEAVES) || o.isFaceSturdy(level, oben, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return leuchtend();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LeuchtLogik.tageszeit(state, level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (leuchtend()) LeuchtLogik.durchlaufen(state, level, pos, entity, this);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LeuchtLogik.abklingen(state, level, pos, this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (leuchtend()) LeuchtLogik.partikel(state, level, pos, random, farbe, 0.9);
    }
}
