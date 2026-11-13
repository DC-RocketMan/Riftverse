package dev.riftverse.compat.emi;

import dev.riftverse.Riftverse;
import dev.riftverse.compat.Infoseiten;
import dev.riftverse.compat.RezeptAnsicht;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI-Anbindung mit denselben Kategorien, Stationen und Infoseiten wie bei JEI. Das Layout kommt aus
 * {@link RezeptAnsicht}. Wird nur geladen, wenn EMI installiert ist.
 */
@EmiEntrypoint
public class RiftverseEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        for (RezeptAnsicht.Kategorie k : RezeptAnsicht.kategorien()) {
            ItemStack symbol = k.symbol();
            EmiRecipeCategory kategorie = new EmiRecipeCategory(k.rl(), EmiStack.of(symbol)) {
                @Override
                public Component getName() {
                    return k.titel();
                }
            };
            registry.addCategory(kategorie);
            if (!symbol.isEmpty()) registry.addWorkstation(kategorie, EmiStack.of(symbol));
            for (RezeptAnsicht a : RezeptAnsicht.alle(registry.getRecipeManager(), k)) {
                registry.addRecipe(new Rezept(kategorie, a));
            }
        }
        for (Infoseiten.Seite seite : Infoseiten.alle()) {
            List<EmiIngredient> stacks = new ArrayList<>();
            for (Item item : seite.itemListe()) stacks.add(EmiStack.of(item));
            if (stacks.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, "/info/" + seite.schluessel());
            registry.addRecipe(new EmiInfoRecipe(stacks, List.of(seite.text()), id));
        }
    }

    private static EmiIngredient zutat(RezeptAnsicht.Slot s) {
        if (s.istFluid()) {
            List<EmiStack> fluids = new ArrayList<>();
            for (FluidStack f : s.fluidAuswahl()) fluids.add(EmiStack.of(f.getFluid(), f.getAmount()));
            return fluids.size() == 1 ? fluids.get(0) : EmiIngredient.of(fluids);
        }
        List<EmiStack> items = new ArrayList<>();
        for (ItemStack stack : s.items()) items.add(EmiStack.of(stack));
        if (items.isEmpty()) return EmiStack.EMPTY;
        return items.size() == 1 ? items.get(0) : EmiIngredient.of(items);
    }

    private static final class Rezept extends BasicEmiRecipe {
        private final RezeptAnsicht ansicht;

        Rezept(EmiRecipeCategory kategorie, RezeptAnsicht a) {
            super(kategorie, a.id(), a.breite(), a.hoehe());
            this.ansicht = a;
            for (RezeptAnsicht.Slot s : a.slots()) {
                EmiIngredient zutat = zutat(s);
                if (zutat.isEmpty()) continue;
                if (s.eingang()) {
                    inputs.add(zutat);
                } else {
                    for (EmiStack stack : zutat.getEmiStacks()) {
                        if (s.chance() < 1f) stack.setChance(s.chance());
                        outputs.add(stack);
                    }
                }
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            for (RezeptAnsicht.Slot s : ansicht.slots()) {
                EmiIngredient zutat = zutat(s);
                if (!s.eingang() && s.chance() < 1f) {
                    for (EmiStack stack : zutat.getEmiStacks()) stack.setChance(s.chance());
                }
                SlotWidget slot = widgets.addSlot(zutat, s.x(), s.y());
                if (!s.eingang()) slot.recipeContext(this);
                if (s.hinweis() != null) slot.appendTooltip(s.hinweis());
            }
            widgets.addTexture(EmiTexture.EMPTY_ARROW, ansicht.pfeilX(), ansicht.pfeilY());
            if (ansicht.formlos()) widgets.addTexture(EmiTexture.SHAPELESS, ansicht.breite() - 18, 0);
            int y = ansicht.zeilenY();
            for (Component zeile : ansicht.zeilen()) {
                widgets.addText(zeile, 0, y, 0xFF404040, false);
                y += RezeptAnsicht.ZEILE;
            }
        }
    }
}
