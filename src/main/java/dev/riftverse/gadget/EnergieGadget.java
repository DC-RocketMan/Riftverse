package dev.riftverse.gadget;

import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Gadget mit eigenem Akku (reines FE, lädt in jedem Ladegerät und im Akku-Slot jedes Generators). Im Kreativmodus
 * kostet nichts etwas.
 */
public class EnergieGadget extends Item implements Gadget {
    private final int capacity;
    private final int transfer;

    public EnergieGadget(Properties properties, int capacity, int transfer) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.transfer = transfer;
    }

    public int capacity() {
        return capacity;
    }

    public int transfer() {
        return transfer;
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int value) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, value));
    }

    /** Zieht {@code amount} FE ab; ohne genug Ladung passiert nichts und der Spieler bekommt einen Hinweis. */
    public boolean ziehen(Player player, ItemStack stack, int amount) {
        if (player.getAbilities().instabuild) return true;
        int e = energy(stack);
        if (e < amount) {
            player.displayClientMessage(Component.translatable("message.riftverse.gadget.leer", stack.getHoverName())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        setEnergy(stack, e - amount);
        return true;
    }

    /** Wie {@link #ziehen}, aber ohne Hinweis (für Dauerbetrieb). */
    public boolean ziehenLeise(Player player, ItemStack stack, int amount) {
        if (player.getAbilities().instabuild) return true;
        int e = energy(stack);
        if (e < amount) return false;
        setEnergy(stack, e - amount);
        return true;
    }

    /** Voll geladen (Kreativ-Tab, Rauchtest). */
    public ItemStack charged() {
        ItemStack s = new ItemStack(this);
        setEnergy(s, capacity);
        return s;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * energy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.energy", String.format(Locale.GERMANY, "%,d", energy(stack)),
                String.format(Locale.GERMANY, "%,d", capacity)).withStyle(ChatFormatting.GRAY));
        Gadget.beschreibung(this, tooltip);
    }
}
