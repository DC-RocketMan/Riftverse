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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Einspeiser und Ausspeiser: die einzigen Stellen, an denen fremde Mods ans Riftverse-Netz andocken.
 * Die Front (Stecker-Symbol) zeigt zum fremden Block, die übrigen fünf Seiten gehören dem Riftverse-Netz.
 */
public class EnergyPortBlock extends BaseEntityBlock implements RiftEnergyConnector {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final boolean input;
    private final MapCodec<EnergyPortBlock> codec;

    public EnergyPortBlock(Properties properties, boolean input) {
        super(properties);
        this.input = input;
        this.codec = simpleCodec(p -> new EnergyPortBlock(p, input));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TechBlock.ACTIVE, false));
    }

    /** true = Einspeiser (FE rein), false = Ausspeiser (FE raus). */
    public boolean isInput() {
        return input;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TechBlock.ACTIVE);
    }

    /** Die Front zeigt zu dem Block, an den der Spieler den Speiser gesetzt hat. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectEnergy(BlockState state, Direction side) {
        return side != state.getValue(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return TechBlock.serverTicker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyPortBlockEntity port) {
            player.displayClientMessage(Component.translatable(input ? "message.riftverse.einspeiser" : "message.riftverse.ausspeiser",
                    String.format(Locale.GERMANY, "%,d", port.energy().stored()),
                    String.format(Locale.GERMANY, "%,d", port.energy().capacity()),
                    VoltageTier.I.displayName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
