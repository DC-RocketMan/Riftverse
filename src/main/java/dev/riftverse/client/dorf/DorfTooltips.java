package dev.riftverse.client.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.dorf.HaengendBlock;
import dev.riftverse.dorf.LeuchtpflanzeBlock;
import dev.riftverse.registry.ModDorf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Kurze Hinweise unter den Items von Bridge's Inn. */
@EventBusSubscriber(modid = Riftverse.MODID, value = Dist.CLIENT)
public final class DorfTooltips {
    private DorfTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack s = event.getItemStack();
        String key = null;
        if (s.getItem() instanceof BlockItem b && (b.getBlock() instanceof LeuchtpflanzeBlock
                || b.getBlock() instanceof HaengendBlock h && h.leuchtend())) {
            key = "tooltip.riftverse.leuchtpflanze";
        } else if (s.is(ModDorf.STERNENSTAUB.get())) {
            key = "tooltip.riftverse.sternenstaub";
        } else if (s.is(ModDorf.STERNEN_CHIP.get())) {
            key = "tooltip.riftverse.sternen_chip";
        }
        if (key != null) event.getToolTip().add(1, Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
