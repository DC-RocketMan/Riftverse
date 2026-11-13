package dev.riftverse.compat;

import dev.riftverse.Riftverse;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.recipe.CountedIngredient;
import dev.riftverse.recipe.FluidMixerRecipe;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.recipe.WerkbankRecipe;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.FluidMixerBlockEntity;
import dev.riftverse.tech.MachineKind;
import dev.riftverse.tech.RaffinerieBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Gemeinsames Anzeigemodell für JEI und EMI: Jedes Riftverse-Rezept wird einmal in Slots mit Koordinaten,
 * einen Pfeil und ein paar Textzeilen übersetzt. Die beiden Plugins zeichnen nur noch, was hier steht – so sehen
 * die Kategorien in beiden Mods gleich aus und bleiben in einer Klasse gepflegt.
 * <p>
 * Läuft ohne Client-Klassen, damit der Rauchtest die Abdeckung auf dem Server prüfen kann.
 */
public record RezeptAnsicht(ResourceLocation id, Kategorie kategorie, List<Slot> slots, int pfeilX, int pfeilY,
                            int zeilenY, List<Component> zeilen, int breite, int hoehe, boolean formlos) {

    public static final int SLOT = 18, PFEIL_B = 24, PFEIL_H = 17, ZEILE = 10, MIN_BREITE = 160;
    /** Feste Größe für JEI: Platz für 6 Eingänge in zwei Reihen oder das 3 × 3-Raster und 5 Textzeilen. */
    public static final int JEI_BREITE = 176, JEI_HOEHE = 3 * SLOT + 3 + 4 * ZEILE + 2;

    /** Ein Slot: Items (mehrere = Auswahl) oder Fluid, mit Chance (1 = sicher) und optionalem Hinweis. */
    public record Slot(int x, int y, boolean eingang, List<ItemStack> items, @Nullable FluidStack fluid,
                       List<FluidStack> fluidAuswahl, float chance, @Nullable Component hinweis) {
        static Slot item(int x, int y, boolean eingang, List<ItemStack> items, float chance, @Nullable Component hinweis) {
            return new Slot(x, y, eingang, List.copyOf(items), null, List.of(), chance, hinweis);
        }

        static Slot fluid(int x, int y, boolean eingang, List<FluidStack> fluids) {
            return new Slot(x, y, eingang, List.of(), fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0), List.copyOf(fluids), 1f, null);
        }

        public boolean istFluid() {
            return fluid != null;
        }
    }

    /**
     * Eine Rezeptkategorie: eigener Rezepttyp, Titel, Symbol und die Blöcke, in denen die Rezepte laufen.
     * Jede Maschine bekommt ihre eigene Kategorie.
     */
    public record Kategorie(String id, RecipeType<?> typ, String station) {
        public ResourceLocation rl() {
            return ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, id);
        }

        public Component titel() {
            return Component.translatable("block." + Riftverse.MODID + "." + station);
        }

        public ItemStack symbol() {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, station)));
        }
    }

    /** Alle Kategorien in Spielreihenfolge: Werkbank, die Maschinen von T0 bis T4, Fluid-Mixer und Raffinerie. */
    public static List<Kategorie> kategorien() {
        List<Kategorie> out = new ArrayList<>();
        out.add(new Kategorie("garagen_werkbank", ModRecipes.WERKBANK_TYPE.get(), "garagen_werkbank"));
        for (MachineKind kind : MachineKind.values()) out.add(new Kategorie(kind.id(), kind.recipeType(), kind.id()));
        out.add(new Kategorie("fluid_mixer", ModRecipes.FLUID_MIXER_TYPE.get(), "fluid_mixer"));
        out.add(new Kategorie("raffinerie", ModRecipes.RAFFINERIE_TYPE.get(), "portalfluid_raffinerie"));
        return out;
    }

    /** Alle Rezepte einer Kategorie als Ansicht. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<RezeptAnsicht> alle(RecipeManager manager, Kategorie kategorie) {
        List<RezeptAnsicht> out = new ArrayList<>();
        List<RecipeHolder<?>> holders = (List<RecipeHolder<?>>) (List) manager.getAllRecipesFor((RecipeType) kategorie.typ());
        for (RecipeHolder<?> holder : holders) {
            RezeptAnsicht a = von(holder, kategorie);
            if (a != null) out.add(a);
        }
        out.sort(java.util.Comparator.comparing(a -> a.id().toString()));
        return out;
    }

    @Nullable
    public static RezeptAnsicht von(RecipeHolder<?> holder, Kategorie kategorie) {
        Recipe<?> recipe = holder.value();
        if (recipe instanceof MachineRecipe m) return maschine(holder.id(), kategorie, m);
        if (recipe instanceof WerkbankRecipe w) return werkbank(holder.id(), kategorie, w);
        if (recipe instanceof FluidMixerRecipe f) return fluidMixer(holder.id(), kategorie, f);
        return null;
    }

    /** Alle Eingangs-Items (für Suche und Tests). */
    public List<ItemStack> eingaenge() {
        List<ItemStack> out = new ArrayList<>();
        for (Slot s : slots) if (s.eingang() && !s.istFluid()) out.addAll(s.items());
        return out;
    }

    public List<ItemStack> ausgaenge() {
        List<ItemStack> out = new ArrayList<>();
        for (Slot s : slots) if (!s.eingang() && !s.istFluid()) out.addAll(s.items());
        return out;
    }

    // ------------------------------------------------------------------ Maschinen

    private static RezeptAnsicht maschine(ResourceLocation id, Kategorie kat, MachineRecipe r) {
        MachineKind kind = r.kind();
        List<Slot> slots = new ArrayList<>();
        List<CountedIngredient> ins = r.ingredients();
        int spalten = Math.max(1, Math.min(3, ins.size()));
        for (int i = 0; i < ins.size(); i++) {
            Component hinweis = kind.ordered() ? Component.translatable("jei.riftverse.schritt", i + 1) : null;
            slots.add(Slot.item((i % spalten) * SLOT, (i / spalten) * SLOT, true, stacks(ins.get(i)), 1f, hinweis));
        }
        int reihen = (ins.size() + spalten - 1) / spalten;
        int pfeilX = spalten * SLOT + 4;
        int ausX = pfeilX + PFEIL_B + 4;

        List<ItemStack> outs = new ArrayList<>();
        List<Float> chancen = new ArrayList<>();
        outs.add(r.result());
        chancen.add(1f);
        for (MachineRecipe.Byproduct b : r.byproducts()) {
            outs.add(b.result());
            chancen.add(b.chance());
        }
        int ausSpalten = outs.size() > 2 ? 2 : outs.size();
        for (int i = 0; i < outs.size(); i++) {
            slots.add(Slot.item(ausX + (i % ausSpalten) * SLOT, (i / ausSpalten) * SLOT, false, List.of(outs.get(i)), chancen.get(i), null));
        }
        int ausReihen = (outs.size() + ausSpalten - 1) / ausSpalten;

        List<Component> zeilen = new ArrayList<>();
        if (!kind.electric()) {
            zeilen.add(Component.translatable("jei.riftverse.kurbel", r.time()));
        } else {
            zeilen.add(Component.translatable("jei.riftverse.dauer", sekunden(r.time()), zahl(kind.energyPerTick())));
            zeilen.add(Component.translatable("jei.riftverse.energie", zahl((long) kind.energyPerTick() * r.time())));
        }
        if (kind.tier() != VoltageTier.I) zeilen.add(Component.translatable("jei.riftverse.stufe", kind.tier().displayName()));
        if (kind.hasModes() && r.mode() != null && !r.mode().isEmpty()) {
            zeilen.add(Component.translatable("jei.riftverse.modus", Component.translatable("gui.riftverse.mode." + r.mode())));
        }
        if (kind.ordered()) zeilen.add(Component.translatable("jei.riftverse.reihenfolge"));
        if (kind.multiblock() != null) zeilen.add(Component.translatable("jei.riftverse.aufbau"));
        if (kind == MachineKind.DIMENSIONS_SCANNER) zeilen.add(Component.translatable("jei.riftverse.scanner"));

        int oben = Math.max(reihen, ausReihen) * SLOT;
        int pfeilY = Math.max(0, (oben - PFEIL_H) / 2);
        int breite = Math.max(MIN_BREITE, ausX + ausSpalten * SLOT);
        return new RezeptAnsicht(id, kat, slots, pfeilX, pfeilY, oben + 3, zeilen, breite, oben + 3 + zeilen.size() * ZEILE, false);
    }

    // ------------------------------------------------------------------ Garagen-Werkbank

    private static RezeptAnsicht werkbank(ResourceLocation id, Kategorie kat, WerkbankRecipe r) {
        List<Slot> slots = new ArrayList<>();
        List<Ingredient> ins = r.getIngredients();
        int b = r.geformt() ? r.breite() : Math.min(3, Math.max(1, (int) Math.ceil(Math.sqrt(ins.size()))));
        if (!r.geformt() && ins.size() > b * b) b = 3;
        for (int i = 0; i < ins.size() && i < 9; i++) {
            Ingredient ing = ins.get(i);
            List<ItemStack> items = ing.isEmpty() ? List.of() : Arrays.asList(ing.getItems());
            slots.add(Slot.item((i % b) * SLOT, (i / b) * SLOT, true, items, 1f, null));
        }
        int pfeilX = 3 * SLOT + 6;
        int ausX = pfeilX + PFEIL_B + 6;
        slots.add(Slot.item(ausX, SLOT, false, List.of(r.result()), 1f, null));

        List<Component> zeilen = new ArrayList<>();
        if (r.hammer() > 0) zeilen.add(Component.translatable("jei.riftverse.hammer", r.hammer()));
        if (r.loetzinn() > 0) zeilen.add(Component.translatable("jei.riftverse.loetzinn", r.loetzinn()));
        int oben = 3 * SLOT;
        int breite = Math.max(MIN_BREITE, ausX + SLOT);
        return new RezeptAnsicht(id, kat, slots, pfeilX, SLOT, oben + 3, zeilen, breite, oben + 3 + zeilen.size() * ZEILE, !r.geformt());
    }

    // ------------------------------------------------------------------ Fluid-Mixer und Raffinerie

    private static RezeptAnsicht fluidMixer(ResourceLocation id, Kategorie kat, FluidMixerRecipe r) {
        List<Slot> slots = new ArrayList<>();
        List<CountedIngredient> ins = r.ingredients();
        for (int i = 0; i < ins.size(); i++) slots.add(Slot.item((i % 2) * SLOT, (i / 2) * SLOT, true, stacks(ins.get(i)), 1f, null));
        int itemSpalten = Math.min(2, ins.size());
        int fluidX = itemSpalten * SLOT + (itemSpalten > 0 ? 4 : 0);
        slots.add(Slot.fluid(fluidX, 0, true, Arrays.asList(r.fluid().getFluids())));
        int pfeilX = fluidX + SLOT + 4;
        int ausX = pfeilX + PFEIL_B + 4;
        slots.add(Slot.fluid(ausX, 0, false, List.of(r.result())));

        boolean raffinerie = r.machine() == FluidMixerRecipe.Machine.RAFFINERIE;
        int verbrauch = raffinerie ? RaffinerieBlockEntity.CONSUMPTION : FluidMixerBlockEntity.CONSUMPTION;
        List<Component> zeilen = new ArrayList<>();
        zeilen.add(Component.translatable("jei.riftverse.dauer", sekunden(r.time()), zahl(verbrauch)));
        zeilen.add(Component.translatable("jei.riftverse.energie", zahl((long) verbrauch * r.time())));
        if (raffinerie) zeilen.add(Component.translatable("jei.riftverse.stufe", VoltageTier.II.displayName()));
        int reihen = Math.max(1, (ins.size() + 1) / 2);
        int oben = reihen * SLOT;
        int breite = Math.max(MIN_BREITE, ausX + SLOT);
        return new RezeptAnsicht(id, kat, slots, pfeilX, Math.max(0, (oben - PFEIL_H) / 2), oben + 3, zeilen, breite,
                oben + 3 + zeilen.size() * ZEILE, false);
    }

    // ------------------------------------------------------------------ Hilfen

    private static List<ItemStack> stacks(CountedIngredient ing) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : ing.ingredient().getItems()) out.add(s.copyWithCount(ing.count()));
        return out;
    }

    private static String sekunden(int ticks) {
        double s = ticks / 20.0;
        return s == Math.floor(s) ? String.valueOf((int) s) : String.format(Locale.GERMANY, "%.1f", s);
    }

    private static String zahl(long n) {
        return String.format(Locale.GERMANY, "%,d", n);
    }

    /** Text für die Chance eines Nebenprodukts, z. B. „25 % Chance“. */
    public static Component chanceText(float chance) {
        int pct = Math.round(chance * 100);
        return Component.translatable("jei.riftverse.chance", pct);
    }
}
