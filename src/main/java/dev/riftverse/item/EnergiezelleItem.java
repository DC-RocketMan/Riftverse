package dev.riftverse.item;

import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Energiezelle: passt in den Akku-Slot jeder Riftverse-Maschine und lädt in jedem FE-Ladegerät.
 * Maschinen ziehen Strom aus der Zelle, Generatoren laden sie.
 */
public class EnergiezelleItem extends Item {
    public static final int CAPACITY = 40_000;
    public static final int TRANSFER = 128;

    public EnergiezelleItem(Properties properties) {
        super(properties);
    }

    public static int stored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    /** Voll geladene Zelle für den Kreativ-Tab. */
    public ItemStack charged() {
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.ENERGY.get(), CAPACITY);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * stored(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.energy",
                String.format(Locale.GERMANY, "%,d", stored(stack)),
                String.format(Locale.GERMANY, "%,d", CAPACITY)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.energiezelle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
