package dev.riftverse.dorf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * Leuchtpflanze aus Bridge's Inn (Sternenblume, Leuchtglocke, Mondkraut, Funkenmohn, Zauberpilz) in einer festen
 * Leuchtfarbe. Tagsüber normal, nachts leuchtend, beim Durchlaufen hell aufstrahlend (siehe {@link LeuchtLogik}).
 */
public class LeuchtpflanzeBlock extends BushBlock {
    public enum Art implements StringRepresentable {
        STERNENBLUME(10, 0, 0.7), LEUCHTGLOCKE(11, 0, 0.8), MONDKRAUT(8, 0, 0.6), FUNKENMOHN(10, 0, 0.8), ZAUBERPILZ(10, 4, 0.5);

        public static final Codec<Art> CODEC = StringRepresentable.fromEnum(Art::values);
        /** Licht nachts und am Tag. */
        public final int nacht, tag;
        /** Höhe der Partikelwolke. */
        public final double hoehe;

        Art(int nacht, int tag, double hoehe) {
            this.nacht = nacht;
            this.tag = tag;
            this.hoehe = hoehe;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<LeuchtpflanzeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Art.CODEC.fieldOf("art").forGetter(b -> b.art),
            Codec.INT.fieldOf("farbe").forGetter(b -> b.farbe),
            propertiesCodec()
    ).apply(i, LeuchtpflanzeBlock::new));

    private static final VoxelShape KLEIN = Block.box(4, 0, 4, 12, 10, 12);
    private static final VoxelShape HOCH = Block.box(3, 0, 3, 13, 14, 13);

    private final Art art;
    /** Leuchtfarbe als RGB (für die Partikel). */
    private final int farbe;

    public LeuchtpflanzeBlock(Art art, int farbe, Properties properties) {
        super(properties);
        this.art = art;
        this.farbe = farbe;
        registerDefaultState(stateDefinition.any().setValue(LeuchtLogik.LEUCHTET, false).setValue(LeuchtLogik.ANGEREGT, false));
    }

    public Art art() {
        return art;
    }

    public int farbe() {
        return farbe;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LeuchtLogik.LEUCHTET, LeuchtLogik.ANGEREGT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LeuchtLogik.LEUCHTET, context.getLevel().isNight());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return art == Art.LEUCHTGLOCKE || art == Art.MONDKRAUT ? HOCH : KLEIN;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND) || (art == Art.ZAUBERPILZ && state.is(BlockTags.BASE_STONE_OVERWORLD));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LeuchtLogik.tageszeit(state, level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        LeuchtLogik.durchlaufen(state, level, pos, entity, this);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LeuchtLogik.abklingen(state, level, pos, this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        LeuchtLogik.partikel(state, level, pos, random, farbe, art.hoehe);
        if (art == Art.FUNKENMOHN && state.getValue(LeuchtLogik.LEUCHTET) && random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
        }
        if (art == Art.STERNENBLUME && state.getValue(LeuchtLogik.LEUCHTET) && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0.01, 0);
        }
        if (art == Art.ZAUBERPILZ && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
