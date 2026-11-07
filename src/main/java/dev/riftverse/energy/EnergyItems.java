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
    }

    private EnergyItems() {}
}
