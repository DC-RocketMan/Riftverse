package dev.riftverse.ruestung;

import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Module ein- und ausbauen an der Werkbank:
 * <ul>
 *   <li>Rüstungsteil + Modul → Teil mit Modul (wenn es zum Teil passt und ein Platz frei ist).</li>
 *   <li>Rüstungsteil allein → Teil ohne das zuletzt eingebaute Modul; das Modul bleibt im Raster liegen.</li>
 * </ul>
 */
public class ModulEinbauRecipe extends CustomRecipe {

    public ModulEinbauRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Teile(ItemStack armor, int armorSlot, @Nullable ItemStack modul) {}

    @Nullable
    private static Teile find(CraftingInput input) {
        ItemStack armor = ItemStack.EMPTY, modul = null;
        int armorSlot = -1;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof MultiversalArmorItem) {
                if (!armor.isEmpty()) return null;
                armor = s;
                armorSlot = i;
            } else if (s.getItem() instanceof ModulItem) {
                if (modul != null) return null;
                modul = s;
            } else {
                return null;
            }
        }
        return armor.isEmpty() ? null : new Teile(armor, armorSlot, modul);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Teile t = find(input);
        if (t == null) return false;
        MultiversalArmorItem item = (MultiversalArmorItem) t.armor().getItem();
        if (t.modul() == null) return !MultiversalArmorItem.module(t.armor()).isEmpty();
        return item.canInstall(t.armor(), ((ModulItem) t.modul().getItem()).modul());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Teile t = find(input);
        if (t == null) return ItemStack.EMPTY;
        ItemStack out = t.armor().copyWithCount(1);
        List<Modul> module = new ArrayList<>(MultiversalArmorItem.module(out));
        if (t.modul() == null) {
            if (module.isEmpty()) return ItemStack.EMPTY;
            module.remove(module.size() - 1);
        } else {
            module.add(((ModulItem) t.modul().getItem()).modul());
        }
        MultiversalArmorItem.setModule(out, module);
        return out;
    }

    /** Beim Ausbau bleibt das Modul im Raster liegen (an der Stelle des Rüstungsteils). */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rest = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        Teile t = find(input);
        if (t == null || t.modul() != null) return rest;
        List<Modul> module = MultiversalArmorItem.module(t.armor());
        if (module.isEmpty()) return rest;
        Modul last = module.get(module.size() - 1);
        rest.set(t.armorSlot(), new ItemStack(ModItems.modul(last)));
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MODUL_EINBAU.get();
    }
}
