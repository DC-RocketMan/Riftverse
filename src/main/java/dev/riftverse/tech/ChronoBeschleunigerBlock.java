package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Chrono-Beschleuniger: Rechtsklick schaltet zwischen 2×, 4× und 8× um. Die Rückseite zeigt zur Maschine. */
public class ChronoBeschleunigerBlock extends TechBlock {
    private final MapCodec<ChronoBeschleunigerBlock> codec;

    public ChronoBeschleunigerBlock(Properties properties) {
        super(properties, ModBlockEntities.CHRONO_BESCHLEUNIGER, true);
        this.codec = simpleCodec(ChronoBeschleunigerBlock::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChronoBeschleunigerBlockEntity be) {
            if (!player.isShiftKeyDown()) be.cycleMode();
            player.displayClientMessage(be.status(), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 0.6f + be.mode() * 0.4f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
