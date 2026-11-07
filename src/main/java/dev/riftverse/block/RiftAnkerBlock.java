package dev.riftverse.block;

import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseCleanup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.List;

/**
 * Rift-Anker: kurzer Eisenpfahl mit einer Fluxlinse an der Spitze. Ein Universum mit Anker wird beim
 * Aufräumen nie zurückgesetzt, auch wenn dort nichts gebaut wurde – etwa wegen der Landschaft.
 */
public class RiftAnkerBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),
            Block.box(7, 2, 7, 9, 12, 9),
            Block.box(5.5, 12, 5.5, 10.5, 16, 10.5));
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(0.22f, 1.0f, 0.42f), 1.0f);

    public RiftAnkerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) UniverseCleanup.anchorChanged(level, pos, true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) UniverseCleanup.anchorChanged(level, pos, false);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Die Linse pulsiert, solange das Universum verankert ist. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!Multiverse.isMultiverse(level) || random.nextInt(2) != 0) return;
        double a = random.nextDouble() * Math.PI * 2;
        level.addParticle(GLOW, pos.getX() + 0.5 + Math.cos(a) * 0.35, pos.getY() + 0.85, pos.getZ() + 0.5 + Math.sin(a) * 0.35,
                0, 0.03, 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.rift_anker").withStyle(ChatFormatting.GRAY));
    }
}
