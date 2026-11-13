package dev.riftverse.gadget;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Markiert Gadgets: Die Gadget-Halterung im Kernmantel löst per Taste das erste beziehungsweise zweite Gadget im
 * Inventar aus, als hielte man es in der Hand.
 */
public interface Gadget {

    /** Zwei Zeilen Beschreibung aus {@code tooltip.riftverse.gadget.<name>} und {@code ….bedienung}. */
    static void beschreibung(Item item, List<Component> tooltip) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        tooltip.add(Component.translatable("tooltip.riftverse.gadget." + name).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.gadget." + name + ".bedienung").withStyle(ChatFormatting.DARK_GRAY));
    }
}
