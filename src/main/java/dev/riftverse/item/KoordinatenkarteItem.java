package dev.riftverse.item;

import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Koordinatenkarte aus dem Dimensions-Scanner: Typ, Name und Modifikatoren eines Universums. In der anderen Hand
 * führt die Portal-Kanone ab Mk II gezielt dorthin.
 */
public class KoordinatenkarteItem extends Item {
    public KoordinatenkarteItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.ZIEL.get()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UniverseId id = stack.get(ModDataComponents.ZIEL.get());
        if (id == null) {
            tooltip.add(Component.translatable("tooltip.riftverse.karte_leer").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        UniverseProfile p = UniverseProfile.get(id);
        tooltip.add(Component.literal(p.title() + " „" + p.name + "“").withStyle(ChatFormatting.GREEN));
        tooltip.add(id.type().displayName().copy().withStyle(ChatFormatting.GRAY));
        if (p.zucht != null) tooltip.add(p.zucht.describe().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(p.modifierText().withStyle(ChatFormatting.DARK_AQUA));
        if (stack.getItem() == this && getClass() == KoordinatenkarteItem.class) {
            tooltip.add(Component.translatable("tooltip.riftverse.karte_benutzen").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
