package dev.riftverse.item;

import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Universumsprobe: leer gebaut, Rechtsklick in einem Universum füllt sie mit dessen Signatur. Der
 * Dimensions-Scanner macht daraus eine Koordinatenkarte.
 */
public class UniversumsprobeItem extends KoordinatenkarteItem {
    public UniversumsprobeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(ModDataComponents.ZIEL.get())) return InteractionResultHolder.pass(stack);
        UniverseId id = Multiverse.at(level, player.blockPosition());
        if (id == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.riftverse.probe_heimat").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            ItemStack filled = stack.split(1);
            filled.set(ModDataComponents.ZIEL.get(), id);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, filled);
            } else if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.3f);
            player.displayClientMessage(Component.translatable("message.riftverse.probe_gefuellt", "Universum " + id.code())
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(ModDataComponents.ZIEL.get())) {
            tooltip.add(Component.translatable("tooltip.riftverse.probe_leer").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.riftverse.probe_scanner").withStyle(ChatFormatting.DARK_GRAY));
    }
}
