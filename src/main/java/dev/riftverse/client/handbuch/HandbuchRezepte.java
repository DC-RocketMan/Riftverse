package dev.riftverse.client.handbuch;

import dev.riftverse.compat.RezeptAnsicht;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import guideme.compiler.tags.RecipeTypeMappingSupplier;
import guideme.document.block.LytBlock;
import guideme.document.block.LytParagraph;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Bringt GuideME bei, Riftverse-Rezepte zu zeichnen ({@code <RecipeFor id="..."/>}, {@code <Recipe id="..."/>}).
 * <p>
 * Grundlage ist dasselbe Anzeigemodell wie für JEI und EMI ({@link RezeptAnsicht}): Eingänge links, Pfeil,
 * Ausgänge rechts, darunter die Textzeilen (Dauer, FE/t, Stufe, Hammer, Lötzinn …). Fluide stehen als passende
 * Kartusche oder Eimer im Slot und zusätzlich mit ihrer Menge im Text.
 */
public final class HandbuchRezepte implements RecipeTypeMappingSupplier {
    private static final int SLOT = RezeptAnsicht.SLOT;

    @Override
    public void collect(RecipeTypeMappings mappings) {
        for (RezeptAnsicht.Kategorie kategorie : RezeptAnsicht.kategorien()) {
            hinzufuegen(mappings, kategorie);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hinzufuegen(RecipeTypeMappings mappings, RezeptAnsicht.Kategorie kategorie) {
        Function<RecipeHolder<?>, LytBlock> fabrik = holder -> box(holder, kategorie);
        mappings.add((RecipeType) kategorie.typ(), (Function) fabrik);
    }

    static LytBlock box(RecipeHolder<?> holder, RezeptAnsicht.Kategorie kategorie) {
        RezeptAnsicht ansicht = RezeptAnsicht.von(holder, kategorie);
        if (ansicht == null) return LytParagraph.of(holder.id().toString());

        List<RezeptAnsicht.Slot> ein = new ArrayList<>(), aus = new ArrayList<>();
        List<Component> unten = new ArrayList<>();
        for (RezeptAnsicht.Slot s : ansicht.slots()) {
            (s.eingang() ? ein : aus).add(s);
            if (s.istFluid() && s.fluid() != null && !s.fluid().isEmpty()) {
                unten.add(Component.translatable(s.eingang() ? "guide.riftverse.fluid_rein" : "guide.riftverse.fluid_raus",
                        String.format(Locale.GERMANY, "%,d", s.fluid().getAmount()), s.fluid().getHoverName()));
            }
            if (!s.eingang() && !s.istFluid() && s.chance() < 1f && !s.items().isEmpty()) {
                unten.add(Component.translatable("guide.riftverse.nebenprodukt", s.items().get(0).getHoverName(),
                        Math.round(s.chance() * 100)));
            }
        }
        unten.addAll(ansicht.zeilen());
        if (ansicht.formlos()) unten.add(Component.translatable("guide.riftverse.formlos"));

        LytStandardRecipeBox.Builder builder = LytStandardRecipeBox.builder()
                .icon(kategorie.symbol())
                .title(kategorie.titel().getString());
        if (!ein.isEmpty()) builder.input(raster(ein, kategorie.id().equals("garagen_werkbank")));
        if (!aus.isEmpty()) builder.output(raster(aus, false));
        for (Component zeile : unten) builder.addBottom(LytParagraph.of(zeile.getString()));
        return builder.build(holder);
    }

    /** Slots mit Pixel-Koordinaten in ein Raster legen; die Werkbank zeigt immer ihr volles 3 × 3-Feld. */
    private static LytSlotGrid raster(List<RezeptAnsicht.Slot> slots, boolean werkbank) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (RezeptAnsicht.Slot s : slots) {
            minX = Math.min(minX, s.x() / SLOT);
            minY = Math.min(minY, s.y() / SLOT);
            maxX = Math.max(maxX, s.x() / SLOT);
            maxY = Math.max(maxY, s.y() / SLOT);
        }
        if (werkbank) {
            minX = 0;
            minY = 0;
            maxX = Math.max(maxX, 2);
            maxY = Math.max(maxY, 2);
        }
        LytSlotGrid grid = new LytSlotGrid(maxX - minX + 1, maxY - minY + 1);
        for (RezeptAnsicht.Slot s : slots) {
            List<ItemStack> items = s.istFluid() ? fluidSymbol(s.fluid()) : s.items();
            if (items.isEmpty()) continue;
            grid.setIngredient(s.x() / SLOT - minX, s.y() / SLOT - minY, Ingredient.of(items.stream()));
        }
        return grid;
    }

    /** Fluide als Gegenstand zeigen: Portalfluide in ihrer Kartusche, Wasser und Säure im Eimer. */
    private static List<ItemStack> fluidSymbol(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        Fluid fluid = stack.getFluid();
        if (fluid.isSame(Fluids.WATER)) return List.of(new ItemStack(Items.WATER_BUCKET));
        if (fluid.isSame(ModFluids.ROH_PORTALFLUID.get())) return List.of(new ItemStack(ModItems.FLUID_KARTUSCHE_VOLL.get()));
        if (fluid.isSame(ModFluids.PORTALFLUID.get())) return List.of(new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get()));
        if (fluid.isSame(ModFluids.PORTALFLUID_KONZENTRAT.get())) return List.of(new ItemStack(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get()));
        ItemStack eimer = new ItemStack(fluid.getBucket());
        return eimer.isEmpty() ? List.of() : List.of(eimer);
    }
}
