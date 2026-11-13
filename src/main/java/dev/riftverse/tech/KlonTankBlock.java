package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.energy.RiftEnergyConnector;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Klon-Tank (T3): ein 1 × 2 hoher Glaszylinder mit grüner Flüssigkeit. Die Block-Entity sitzt in der unteren Hälfte,
 * nur dort docken Leitungen an. Wer den Tank aufstellt, dem gehört der Klon; Schleichen + Rechtsklick mit leerer
 * Hand bindet ihn an einen anderen Spieler. Bewusst ein Einzelblock: man soll mehrere bauen können.
 */
public class KlonTankBlock extends BaseEntityBlock implements RiftEnergyConnector {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public static final MapCodec<KlonTankBlock> CODEC = simpleCodec(KlonTankBlock::new);

    public KlonTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TechBlock.FACING, Direction.NORTH).setValue(TechBlock.ACTIVE, false)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TechBlock.FACING, TechBlock.ACTIVE, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(ctx)) return null;
        // In der Leeren Welt gibt es keinen Respawn-Anker – auch keinen Klon-Tank
        if (dev.riftverse.universe.LeereWelt.isLeere(level)) {
            if (ctx.getPlayer() != null && !level.isClientSide) {
                ctx.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.riftverse.leere.kein_anker")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            }
            return null;
        }
        return defaultBlockState().setValue(TechBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof KlonTankBlockEntity tank) {
            tank.bind(player);
        }
    }

    /** Die beiden Hälften gehören zusammen: fehlt eine, verschwindet auch die andere (wie bei Türen). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            if (!neighbor.is(this) || neighbor.getValue(HALF) == half) return Blocks.AIR.defaultBlockState();
            // Die obere Hälfte leuchtet mit der unteren
            return half == DoubleBlockHalf.UPPER ? state.setValue(TechBlock.ACTIVE, neighbor.getValue(TechBlock.ACTIVE)) : state;
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    public static BlockPos lower(BlockState state, BlockPos pos) {
        return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectEnergy(BlockState state, Direction side) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new KlonTankBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? TechBlock.serverTicker(level) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = lower(state, pos);
        if (!level.isClientSide && level.getBlockEntity(base) instanceof KlonTankBlockEntity tank) {
            if (player.isShiftKeyDown()) {
                tank.bind(player);
                player.displayClientMessage(Component.translatable("message.riftverse.klon.gebunden", player.getName())
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.openMenu((MenuProvider) tank, buf -> buf.writeBlockPos(base));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof KlonTankBlockEntity tank) {
            tank.dropContents(level, pos);
            tank.unregister();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TechBlock.ACTIVE) || random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + random.nextDouble(),
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.04, 0);
    }
}
