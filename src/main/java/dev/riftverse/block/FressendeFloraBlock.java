package dev.riftverse.block;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fressende Flora der organischen Welt: hält fest, wer hineinläuft, beißt jede Sekunde zu und frisst
 * herumliegende Items.
 */
public class FressendeFloraBlock extends BushBlock {
    public static final MapCodec<FressendeFloraBlock> CODEC = simpleCodec(FressendeFloraBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public FressendeFloraBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(ModBlocks.BIOMASSE_BLOCK.get()) || state.is(Blocks.NETHER_WART_BLOCK)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.MUD);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel sl)) return;
        if (entity instanceof ItemEntity item) {
            if (sl.getGameTime() % 20 == 0 && sl.random.nextInt(4) == 0) {
                item.discard();
                sl.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 0.6f, 0.7f);
            }
            return;
        }
        if (!(entity instanceof LivingEntity living)) return;
        if (living instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        entity.makeStuckInBlock(state, new Vec3(0.5, 0.6, 0.5));
        if (sl.getGameTime() % 20 == 0) {
            living.hurt(ModDamageTypes.source(sl, ModDamageTypes.FRESSENDE_FLORA), 2.0f);
            sl.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.7f, 0.6f);
        }
    }
}
