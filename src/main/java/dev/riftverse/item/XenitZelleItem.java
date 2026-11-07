package dev.riftverse.item;

import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.tech.XenitZelleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/** Xenit-Zelle als Item: zeigt die gespeicherte Ladung, die beim Abbauen erhalten bleibt. */
public class XenitZelleItem extends BlockItem {
    public XenitZelleItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * stack.getOrDefault(ModDataComponents.ENERGY.get(), 0) / XenitZelleBlockEntity.CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.energy",
                String.format(Locale.GERMANY, "%,d", stack.getOrDefault(ModDataComponents.ENERGY.get(), 0)),
                String.format(Locale.GERMANY, "%,d", XenitZelleBlockEntity.CAPACITY)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.xenit_zelle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
