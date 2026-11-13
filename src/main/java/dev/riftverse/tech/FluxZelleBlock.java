package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Flux-Zelle: Speicherblock mit leuchtendem Füllstandsband (Blockzustand {@code fuellung} 0–4). */
public class FluxZelleBlock extends TechBlock {
    public static final IntegerProperty FUELLUNG = IntegerProperty.create("fuellung", 0, 4);

    private final MapCodec<FluxZelleBlock> codec;

    public FluxZelleBlock(Properties properties) {
        super(properties, ModBlockEntities.FLUX_ZELLE, true);
        this.codec = simpleCodec(FluxZelleBlock::new);
        registerDefaultState(defaultBlockState().setValue(FUELLUNG, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FUELLUNG);
    }
}
