package dev.riftverse.energy;

import dev.riftverse.item.EnergiezelleItem;
import dev.riftverse.registry.ModItems;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** FE-Capabilities der Items: reines FE, damit sie in jedem Ladegerät jedes Mods laden. */
public final class EnergyItems {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> new ItemEnergy.Storage(stack, EnergiezelleItem.CAPACITY, EnergiezelleItem.TRANSFER),
                ModItems.ENERGIEZELLE.get());
        // Meilenstein 6: Gadgets mit Akku und die Multiversal-Rüstung
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (entry.get() instanceof dev.riftverse.gadget.EnergieGadget g) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ItemEnergy.Storage(stack, g.capacity(), g.transfer()), g);
            } else if (entry.get() instanceof dev.riftverse.ruestung.MultiversalArmorItem a) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ItemEnergy.Storage(stack, a.capacity(), 20_000), a);
            }
        }
    }

    private EnergyItems() {}
}
