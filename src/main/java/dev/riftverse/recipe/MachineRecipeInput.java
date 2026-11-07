package dev.riftverse.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * Eingangsslots einer Maschine plus Betriebsart (nur Walzbank: walzen, schneiden, ziehen).
 */
public record MachineRecipeInput(List<ItemStack> stacks, String mode) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
