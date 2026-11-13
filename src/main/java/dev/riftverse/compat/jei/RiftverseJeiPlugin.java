package dev.riftverse.compat.jei;

import dev.riftverse.Riftverse;
import dev.riftverse.compat.Infoseiten;
import dev.riftverse.compat.RezeptAnsicht;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI-Anbindung: eine Kategorie pro Maschine (plus Garagen-Werkbank, Fluid-Mixer und Raffinerie), die Maschinenblöcke
 * als Katalysatoren und Infoseiten für alles ohne Rezeptfenster. Das Layout kommt aus {@link RezeptAnsicht}.
 * Wird nur geladen, wenn JEI installiert ist.
 */
@JeiPlugin
public class RiftverseJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, "jei");
    private final Map<String, RecipeType<RezeptAnsicht>> typen = new HashMap<>();

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    private RecipeType<RezeptAnsicht> typ(RezeptAnsicht.Kategorie k) {
        return typen.computeIfAbsent(k.id(), id -> RecipeType.create(Riftverse.MODID, id, RezeptAnsicht.class));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (RezeptAnsicht.Kategorie k : RezeptAnsicht.kategorien()) {
            registration.addRecipeCategories(new Kategorie(typ(k), k, gui));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
        for (RezeptAnsicht.Kategorie k : RezeptAnsicht.kategorien()) {
            registration.addRecipes(typ(k), RezeptAnsicht.alle(manager, k));
        }
        for (Infoseiten.Seite seite : Infoseiten.alle()) {
            List<ItemStack> stacks = seite.itemListe().stream().map(Item::getDefaultInstance).toList();
            if (!stacks.isEmpty()) registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, seite.text());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RezeptAnsicht.Kategorie k : RezeptAnsicht.kategorien()) {
            ItemStack station = k.symbol();
            if (!station.isEmpty()) registration.addRecipeCatalyst(station, typ(k));
        }
    }

    /** Eine Kategorie; zeichnet Slots, Pfeil und Textzeilen aus der Ansicht. */
    private static final class Kategorie implements IRecipeCategory<RezeptAnsicht> {
        private final RecipeType<RezeptAnsicht> typ;
        private final RezeptAnsicht.Kategorie kategorie;
        private final IDrawable symbol;
        private final IDrawableStatic pfeil;
        private final int breite;
        private final int hoehe;

        Kategorie(RecipeType<RezeptAnsicht> typ, RezeptAnsicht.Kategorie kategorie, IGuiHelper gui) {
            this.typ = typ;
            this.kategorie = kategorie;
            this.symbol = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, kategorie.symbol());
            this.pfeil = gui.getRecipeArrow();
            // Platz für das größte Layout der Kategorie: 6 Eingänge, 4 Ausgänge, 5 Textzeilen
            this.breite = 160;
            this.hoehe = 2 * RezeptAnsicht.SLOT + 3 + 5 * RezeptAnsicht.ZEILE + 2;
        }

        @Override
        public RecipeType<RezeptAnsicht> getRecipeType() {
            return typ;
        }

        @Override
        public Component getTitle() {
            return kategorie.titel();
        }

        @Override
        public int getWidth() {
            return breite;
        }

        @Override
        public int getHeight() {
            return hoehe;
        }

        @Override
        public @Nullable IDrawable getIcon() {
            return symbol;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RezeptAnsicht a, IFocusGroup focuses) {
            for (RezeptAnsicht.Slot s : a.slots()) {
                IRecipeSlotBuilder slot = builder.addSlot(s.eingang() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT,
                        s.x() + 1, s.y() + 1);
                slot.setStandardSlotBackground();
                if (s.istFluid()) {
                    List<FluidStack> fluids = s.fluidAuswahl();
                    long menge = fluids.isEmpty() ? 1000 : Math.max(1, fluids.get(0).getAmount());
                    slot.addIngredients(NeoForgeTypes.FLUID_STACK, fluids);
                    slot.setFluidRenderer(menge, false, 16, 16);
                } else {
                    slot.addItemStacks(s.items());
                }
                float chance = s.chance();
                Component hinweis = s.hinweis();
                if (chance < 1f || hinweis != null) {
                    slot.addRichTooltipCallback((view, tooltip) -> {
                        if (hinweis != null) tooltip.add(hinweis);
                        if (chance < 1f) tooltip.add(RezeptAnsicht.chanceText(chance));
                    });
                }
            }
            if (a.formlos()) builder.setShapeless();
        }

        @Override
        public void draw(RezeptAnsicht a, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            pfeil.draw(g, a.pfeilX(), a.pfeilY());
            var font = Minecraft.getInstance().font;
            int y = a.zeilenY();
            for (Component zeile : a.zeilen()) {
                g.drawString(font, zeile, 0, y, 0xFF404040, false);
                y += RezeptAnsicht.ZEILE;
            }
        }

        @Override
        public @Nullable ResourceLocation getRegistryName(RezeptAnsicht a) {
            return a.id();
        }
    }
}
