package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.UniverseCleanup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

/**
 * Rift-Kabelknoten: Kopplungskarte erst an einen Knoten halten (der wird Sender), dann an den zweiten (Empfänger).
 * Rechtsklick zeigt den Zustand, Schleichen + Rechtsklick tauscht Sender und Empfänger. Knoten verankern ihr Universum.
 */
public class RiftKabelknotenBlock extends TechBlock {
    private final MapCodec<RiftKabelknotenBlock> codec;

    public RiftKabelknotenBlock(Properties properties) {
        super(properties, ModBlockEntities.RIFT_KABELKNOTEN, true);
        this.codec = simpleCodec(RiftKabelknotenBlock::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) UniverseCleanup.anchorChanged(level, pos, true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            UniverseCleanup.anchorChanged(level, pos, false);
            if (level.getBlockEntity(pos) instanceof RiftKabelknotenBlockEntity k) k.entkoppeln();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.KOPPLUNGSKARTE.get())) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof RiftKabelknotenBlockEntity here) {
            GlobalPos stored = stack.get(ModDataComponents.KOPPLUNG.get());
            GlobalPos self = GlobalPos.of(level.dimension(), pos);
            if (stored == null) {
                stack.set(ModDataComponents.KOPPLUNG.get(), self);
                player.displayClientMessage(Component.translatable("message.riftverse.knoten.gemerkt").withStyle(ChatFormatting.AQUA), true);
            } else if (stored.equals(self)) {
                player.displayClientMessage(Component.translatable("message.riftverse.knoten.derselbe").withStyle(ChatFormatting.YELLOW), true);
            } else {
                ServerLevel otherLevel = sl.getServer().getLevel(stored.dimension());
                if (otherLevel != null && otherLevel.getBlockEntity(stored.pos()) instanceof RiftKabelknotenBlockEntity first) {
                    RiftKabelknotenBlockEntity.koppeln(first, here);
                    player.displayClientMessage(Component.translatable("message.riftverse.knoten.gekoppelt",
                            stored.dimension().location().getPath()).withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.riftverse.knoten.weg").withStyle(ChatFormatting.RED), true);
                }
                stack.remove(ModDataComponents.KOPPLUNG.get());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RiftKabelknotenBlockEntity k) {
            if (player.isShiftKeyDown() && k.partner() != null) k.rollenTauschen();
            Component msg;
            if (k.partner() == null) {
                msg = Component.translatable("message.riftverse.knoten.frei");
            } else {
                GlobalPos p = k.partner();
                msg = Component.translatable(k.sender() ? "message.riftverse.knoten.sendet" : "message.riftverse.knoten.empfaengt",
                        p.dimension().location().getPath(), p.pos().getX(), p.pos().getY(), p.pos().getZ(),
                        String.format(Locale.GERMANY, "%,d", k.energy().stored()));
            }
            player.displayClientMessage(msg.copy().withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
