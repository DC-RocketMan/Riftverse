package dev.riftverse.energy;

import com.mojang.serialization.MapCodec;
import dev.riftverse.tech.TechBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Umspanner zwischen zwei Spannungsstufen, wie in IndustrialCraft. Die Front (mit den Punkten) ist die
 * Hochspannungsseite, die übrigen fünf Seiten führen die niedrigere Stufe.
 * <ul>
 *   <li>Ohne Redstone: spannt herunter – nimmt an der Front die hohe Stufe an und gibt an den Seiten die niedrige ab.</li>
 *   <li>Mit Redstone: spannt hoch – nimmt an den Seiten die niedrige Stufe an und gibt an der Front die hohe ab.</li>
 * </ul>
 */
public class UmspannerBlock extends BaseEntityBlock implements RiftEnergyConnector {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private final VoltageTier low;
    private final MapCodec<UmspannerBlock> codec;

    /** @param low niedrigere der beiden Stufen (I für den Umspanner I ↔ II) */
    public UmspannerBlock(Properties properties, VoltageTier low) {
        super(properties);
        this.low = low;
        this.codec = simpleCodec(p -> new UmspannerBlock(p, low));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(TechBlock.ACTIVE, false));
    }

    public VoltageTier low() {
        return low;
    }

    public VoltageTier high() {
        return low.next();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, TechBlock.ACTIVE);
    }

    /** Die Hochspannungsseite zeigt zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            EnergyNet.markDirty(level);   // die Seiten haben ihre Rolle getauscht
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectEnergy(BlockState state, Direction side) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UmspannerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return TechBlock.serverTicker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof UmspannerBlockEntity be) {
            boolean up = state.getValue(POWERED);
            player.displayClientMessage(Component.translatable(up ? "message.riftverse.umspanner.hoch" : "message.riftverse.umspanner.runter",
                    String.format(Locale.GERMANY, "%,d", be.energy().stored()),
                    String.format(Locale.GERMANY, "%,d", be.energy().capacity()),
                    low.displayName(), high().displayName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
