package dev.riftverse.item;

import dev.riftverse.handbuch.Handbuch;
import guideme.GuidesCommon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Das Riftverse-Handbuch als Item. Rechtsklick öffnet den GuideME-Guide an der zuletzt gelesenen Seite.
 * Ohne Buch geht es auch: In jedem Item-Tooltip G gedrückt halten springt zur passenden Seite.
 */
public class HandbuchItem extends Item {
    public HandbuchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) GuidesCommon.openGuide(player, Handbuch.ID);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.handbuch").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.handbuch_g").withStyle(ChatFormatting.DARK_GRAY));
    }
}
