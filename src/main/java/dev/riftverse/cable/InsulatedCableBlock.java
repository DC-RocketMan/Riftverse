package dev.riftverse.cable;

import dev.riftverse.energy.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Isolierte Leitung: sicher anzufassen und mit Farbstoff einfärbbar.
 * Unterschiedlich gefärbte Kabel verbinden sich nicht, so laufen zwei Netze nebeneinander.
 */
public class InsulatedCableBlock extends CableBlock {
    /** 0 = ungefärbt, 1–16 = DyeColor-ID + 1 */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);

    /** Farbe der Streifen bei ungefärbten Kabeln (Xenit-Petrol, hell). */
    public static final int DEFAULT_TINT = 0x5AA9B0;
    /** Streifen der ungefärbten Flux-Leitung (Labor-Weiß). */
    public static final int DEFAULT_TINT_II = 0xE6EAEC;
    /** Streifen der ungefärbten Gravitit-Leitung (Gravitit-Blau). */
    public static final int DEFAULT_TINT_III = 0x4A62A8;

    public InsulatedCableBlock(Properties properties, VoltageTier tier, double lossPerBlock, int thicknessPx) {
        this(properties, tier, lossPerBlock, thicknessPx, 0.0f);
    }

    /** @param shockDamage Gravitit-Leitung mit nur einer Isolierschicht: noch ein Schlag */
    public InsulatedCableBlock(Properties properties, VoltageTier tier, double lossPerBlock, int thicknessPx, float shockDamage) {
        super(properties, tier, lossPerBlock, thicknessPx, true, shockDamage);
        registerDefaultState(defaultBlockState().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int color = dye.getDyeColor().getId() + 1;
        if (state.getValue(COLOR) == color) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            BlockState dyed = withConnections(state.setValue(COLOR, color), level, pos);
            level.setBlock(pos, dyed, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Tint für Modell und Partikel. */
    public static int tint(BlockState state) {
        int c = color(state);
        if (c != 0) return DyeColor.byId(c - 1).getFireworkColor();
        if (!(state.getBlock() instanceof CableBlock cable)) return DEFAULT_TINT;
        return switch (cable.tier()) {
            case II -> DEFAULT_TINT_II;
            case III, IV -> DEFAULT_TINT_III;
            default -> DEFAULT_TINT;
        };
    }
}
