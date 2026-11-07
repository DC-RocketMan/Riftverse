package dev.riftverse.item;

import dev.riftverse.fluid.Zerfall;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Volle Fluid-Kartusche: 1.000 mB Roh-Portalfluid, genug für 4 Schüsse der Kanone Mk I.
 * Das Fluid zerfällt 10 Minuten nach der Herstellung im Fluid-Mixer; danach ist die Kartusche wieder leer.
 */
public class FluidKartuscheItem extends Item {
    public static final int INHALT = 1_000;

    public FluidKartuscheItem(Properties properties) {
        super(properties);
    }

    /** Zerfallszeitpunkt; 0 = stabil (nur Kreativmodus). */
    public static long zerfall(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ZERFALL.get(), 0L);
    }

    public static boolean zerfallen(ItemStack stack, long now) {
        return Zerfall.zerfallen(zerfall(stack), now);
    }

    /** Frisch gefüllte Kartusche, die zum angegebenen Zeitpunkt zerfällt. */
    public static ItemStack gefuellt(long zerfall) {
        ItemStack stack = new ItemStack(ModItems.FLUID_KARTUSCHE_VOLL.get());
        if (zerfall > 0) stack.set(ModDataComponents.ZERFALL.get(), zerfall);
        return stack;
    }

    /** Zerfallene Kartuschen im Inventar werden wieder zu leeren. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || entity.tickCount % 20 != 0) return;
        if (!zerfallen(stack, Zerfall.now(level))) return;
        ItemStack empty = new ItemStack(ModItems.FLUID_KARTUSCHE.get(), stack.getCount());
        Inventory inv = player.getInventory();
        if (slot >= 0 && slot < inv.items.size() && inv.items.get(slot) == stack) {
            inv.items.set(slot, empty);
        } else if (player.getOffhandItem() == stack) {
            player.setItemInHand(InteractionHand.OFF_HAND, empty);
        } else {
            return;
        }
        player.displayClientMessage(Component.translatable("message.riftverse.kartusche_zerfallen").withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.kartusche", INHALT).withStyle(ChatFormatting.GRAY));
        long zerfall = zerfall(stack);
        Level level = context.level();
        if (zerfall <= 0) {
            tooltip.add(Component.translatable("tooltip.riftverse.stabil").withStyle(ChatFormatting.DARK_PURPLE));
        } else if (level != null) {
            long now = level.getGameTime();
            tooltip.add(Zerfall.zerfallen(zerfall, now)
                    ? Component.translatable("tooltip.riftverse.zerfallen").withStyle(ChatFormatting.RED)
                    : Component.translatable("tooltip.riftverse.zerfaellt_in", Zerfall.rest(zerfall, now)).withStyle(ChatFormatting.YELLOW));
        }
    }
}
