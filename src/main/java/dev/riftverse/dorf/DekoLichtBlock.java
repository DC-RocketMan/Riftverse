package dev.riftverse.dorf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Lichter von Bridge's Inn, die ohne Halt in der Luft stehen: die Schwebelaterne (violettes Glas, wie an einem
 * unsichtbaren Faden) und das Feenlicht (ein kleines, funkelndes Licht ohne Hitbox).
 */
public class DekoLichtBlock extends Block {
    public static final MapCodec<DekoLichtBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("fee").forGetter(b -> b.fee),
            propertiesCodec()
    ).apply(i, DekoLichtBlock::new));
    private static final VoxelShape LATERNE = Block.box(4, 2, 4, 12, 12, 12);
    private static final VoxelShape FEE = Block.box(6, 6, 6, 10, 10, 10);

    private final boolean fee;

    public DekoLichtBlock(boolean fee, Properties properties) {
        super(properties);
        this.fee = fee;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fee ? FEE : LATERNE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (fee) {
            if (random.nextInt(2) == 0) {
                level.addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.85f, 1.0f), 0.5f),
                        pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3 + random.nextDouble() * 0.4,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
            }
        } else if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0, -0.01, 0);
        }
    }
}
