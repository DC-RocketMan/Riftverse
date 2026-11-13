package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.energy.RiftEnergyConnector;
import dev.riftverse.item.HandkurbelItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.function.Supplier;

/**
 * Gemeinsamer Block für Riftverse-Maschinen und -Generatoren: Front zeigt zum Spieler,
 * {@code active} schaltet die Front-Textur um, Rechtsklick öffnet die GUI.
 */
public class TechBlock extends BaseEntityBlock implements RiftEnergyConnector {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final Supplier<? extends BlockEntityType<?>> type;
    private final boolean energy;
    private final MapCodec<TechBlock> codec;

    /**
     * @param energy ob Riftverse-Leitungen andocken dürfen
     */
    public TechBlock(Properties properties, Supplier<? extends BlockEntityType<?>> type, boolean energy) {
        super(properties);
        this.type = type;
        this.energy = energy;
        this.codec = simpleCodec(p -> new TechBlock(p, type, energy));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectEnergy(BlockState state, Direction side) {
        return energy;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return serverTicker(level);
    }

    @Nullable
    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker(Level level) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ServerTickable t) t.serverTick();
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // Mit der Handkurbel in der Hand nicht die GUI öffnen, sondern kurbeln (HandkurbelItem#useOn)
        if (stack.getItem() instanceof HandkurbelItem && level.getBlockEntity(pos) instanceof Crankable) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        // Upgrade-Chips einbauen und der Schlüssel des Multitools: kein GUI, sondern das Item handelt (useOn)
        if (stack.getItem() instanceof dev.riftverse.tech.upgrade.UpgradeChipItem
                && level.getBlockEntity(pos) instanceof dev.riftverse.tech.upgrade.Aufruestbar) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof dev.riftverse.gadget.MultitoolItem
                && dev.riftverse.gadget.MultitoolItem.modus(stack) == dev.riftverse.gadget.MultitoolItem.SCHLUESSEL) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof dev.riftverse.tech.multiblock.MultiblockController mb) {
            net.minecraft.network.chat.Component missing = mb.checkStructure();
            if (missing != null) player.displayClientMessage(missing.copy().withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DropsContents contents) {
            contents.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(4) != 0) return;
        Direction dir = state.getValue(FACING);
        double x = pos.getX() + 0.5 + dir.getStepX() * 0.52 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.52 + (random.nextDouble() - 0.5) * 0.6;
        level.addParticle(energy ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CRIT, x, y, z, 0, 0.02, 0);
    }
}
