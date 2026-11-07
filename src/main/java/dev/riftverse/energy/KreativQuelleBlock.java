package dev.riftverse.energy;

import com.mojang.serialization.MapCodec;
import dev.riftverse.tech.TechBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Nur im Kreativmodus: unendliche Quelle mit wählbarer Spannungsstufe (Rechtsklick).
 * Zum Testen von Überspannung, Kabelverlust und Schlag.
 */
public class KreativQuelleBlock extends BaseEntityBlock implements RiftEnergyConnector {
    public static final MapCodec<KreativQuelleBlock> CODEC = simpleCodec(KreativQuelleBlock::new);

    public KreativQuelleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new KreativQuelleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return TechBlock.serverTicker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof KreativQuelleBlockEntity source) {
            VoltageTier tier = source.cycleTier();
            player.displayClientMessage(Component.translatable("message.riftverse.kreativ_quelle", tier.describe()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
