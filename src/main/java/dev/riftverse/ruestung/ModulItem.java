package dev.riftverse.ruestung;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Rüstungsmodul zum Einbauen an der Werkbank. */
public class ModulItem extends Item {
    private final Modul modul;

    public ModulItem(Properties properties, Modul modul) {
        super(properties);
        this.modul = modul;
    }

    public Modul modul() {
        return modul;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.modul." + modul.key()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.modul_teil." + modul.slot().getName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
